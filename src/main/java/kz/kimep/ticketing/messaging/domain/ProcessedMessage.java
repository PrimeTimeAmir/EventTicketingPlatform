package kz.kimep.ticketing.messaging.domain;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "processed_messages")
public class ProcessedMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(
            name = "message_id",
            nullable = false,
            unique = true,
            length = 100
    )
    private String messageId;

    @Column(name = "consumed_at", nullable = false)
    private OffsetDateTime consumedAt;

    protected ProcessedMessage() {
    }

    @PrePersist
    protected void onCreate() {
        if (consumedAt == null) {
            consumedAt = OffsetDateTime.now();
        }
    }

    public Long getId() {
        return id;
    }

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public OffsetDateTime getConsumedAt() {
        return consumedAt;
    }
}
