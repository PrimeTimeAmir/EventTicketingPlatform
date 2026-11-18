package kz.kimep.ticketing.booking.service;

import kz.kimep.ticketing.booking.domain.HoldStatus;
import kz.kimep.ticketing.booking.repo.SeatHoldRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.OffsetDateTime;

@Service
public class SeatHoldExpiryService {

    private static final Logger log =
            LoggerFactory.getLogger(SeatHoldExpiryService.class);

    private final SeatHoldRepository seatHoldRepository;

    public SeatHoldExpiryService(
            SeatHoldRepository seatHoldRepository
    ) {
        this.seatHoldRepository = seatHoldRepository;
    }

    @Scheduled(fixedDelayString = "${app.booking.expiry-scan-ms:30000}")
    @Transactional
    public void expireExpiredHolds() {

        int expiredCount = seatHoldRepository.expireHolds(
                OffsetDateTime.now(),
                HoldStatus.ACTIVE,
                HoldStatus.EXPIRED
        );

        if (expiredCount > 0) {
            log.info("Expired {} seat holds", expiredCount);
        }
    }
}