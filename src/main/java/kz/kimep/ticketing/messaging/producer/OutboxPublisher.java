package kz.kimep.ticketing.messaging.producer;

import kz.kimep.ticketing.messaging.config.RabbitMQConfig;
import kz.kimep.ticketing.messaging.domain.OutboxMessage;
import kz.kimep.ticketing.messaging.repo.OutboxMessageRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
public class OutboxPublisher {

    private static final Logger log =
            LoggerFactory.getLogger(OutboxPublisher.class);

    private final OutboxMessageRepository outboxMessageRepository;
    private final RabbitTemplate rabbitTemplate;

    @Value("${ticketing.outbox.confirm-timeout-ms:5000}")
    private long confirmTimeoutMs;

    public OutboxPublisher(
            OutboxMessageRepository outboxMessageRepository,
            RabbitTemplate rabbitTemplate
    ) {
        this.outboxMessageRepository = outboxMessageRepository;
        this.rabbitTemplate = rabbitTemplate;
    }

    // Periodically publish pending Outbox messages
    @Scheduled(
            fixedDelayString =
                    "${ticketing.outbox.publish-interval-ms:2000}"
    )
    public void publishPendingMessages() {

        List<OutboxMessage> pendingMessages =
                outboxMessageRepository
                        .findTop100ByPublishedAtIsNullOrderByCreatedAtAsc();

        for (OutboxMessage message : pendingMessages) {
            publishMessage(message);
        }
    }

    // Publish one message and wait for RabbitMQ confirmation
    private void publishMessage(OutboxMessage message) {

        try {
            CorrelationData correlationData =
                    new CorrelationData("outbox-" + message.getId());


            if (message.getId() == null) {
                throw new IllegalStateException(
                        "Outbox message must have an ID before publishing"
                );
            }

            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.EVENT_EXCHANGE,
                    RabbitMQConfig.EVENT_ROUTING_KEY,
                    message.getPayload(),
                    rabbitMessage -> {
                        rabbitMessage.getMessageProperties().setMessageId(
                                message.getId().toString()
                        );

                        rabbitMessage.getMessageProperties().setHeader(
                                "eventType",
                                message.getEventType()
                        );

                        rabbitMessage.getMessageProperties().setHeader(
                                "aggregateType",
                                message.getAggregateType()
                        );

                        rabbitMessage.getMessageProperties().setHeader(
                                "aggregateId",
                                message.getAggregateId()
                        );

                        return rabbitMessage;
                    },
                    correlationData
            );

            CorrelationData.Confirm confirm =
                    correlationData.getFuture().get(
                            confirmTimeoutMs,
                            TimeUnit.MILLISECONDS
                    );

            if (!confirm.isAck()) {
                throw new IllegalStateException(
                        "RabbitMQ rejected message: " + confirm.getReason()
                );
            }

            if (correlationData.getReturned() != null) {
                throw new IllegalStateException(
                        "RabbitMQ returned message: "
                                + correlationData.getReturned().getReplyText()
                );
            }

            // Mark as published only after successful confirmation
            message.setPublishedAt(OffsetDateTime.now());

            outboxMessageRepository.save(message);

            log.info(
                    "Outbox message {} successfully published",
                    message.getId()
            );

        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();

            log.warn(
                    "Publishing interrupted for Outbox message {}",
                    message.getId()
            );

        } catch (Exception exception) {
            log.error(
                    "Failed to publish Outbox message {}. It will be retried.",
                    message.getId(),
                    exception
            );
        }
    }
}
