package kz.kimep.ticketing.catalog.service;

import kz.kimep.ticketing.catalog.api.dto.SeatResponse;
import kz.kimep.ticketing.catalog.repo.SeatRepository;
import kz.kimep.ticketing.catalog.repo.SectionRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;

@Service
public class SeatService {

    private final SeatRepository seatRepository;
    private final SectionRepository sectionRepository;

    public SeatService(
            SeatRepository seatRepository,
            SectionRepository sectionRepository
    ) {
        this.seatRepository = seatRepository;
        this.sectionRepository = sectionRepository;
    }

    // Get all seats belonging to a section
    @Transactional(readOnly = true)
    public List<SeatResponse> getSeatsBySection(Long sectionId) {

        if (!sectionRepository.existsById(sectionId)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Section not found: " + sectionId
            );
        }

        return seatRepository.findAllBySection_Id(sectionId)
                .stream()
                .map(SeatResponse::from)
                .toList();
    }
}
