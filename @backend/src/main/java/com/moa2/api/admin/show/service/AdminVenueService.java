package com.moa2.api.admin.show.service;

import com.moa2.api.admin.show.dto.*;
import com.moa2.domain.show.entity.Venue;
import com.moa2.domain.show.repository.VenueRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminVenueService {

    private final VenueRepository venueRepository;

    public List<VenueListResponse> getVenueList() {
        List<Venue> venues = venueRepository.findAll();
        return venues.stream()
            .map(venue -> VenueListResponse.builder()
                .id(venue.getId())
                .name(venue.getName())
                .hallName(venue.getHallName())
                .build())
            .collect(Collectors.toList());
    }

    @Transactional
    public VenueCreateResponse createVenue(VenueCreateRequest request) {
        // Venue 엔티티 생성 (Builder 패턴 사용)
        Venue venue = Venue.builder()
            .name(request.getName())
            .hallName(request.getHallName())
            .region(request.getRegion())
            .address(request.getAddress())
            .totalSeats(request.getTotalSeats())
            .latitude(request.getLatitude())
            .longitude(request.getLongitude())
            .seatLayoutImageUrl(request.getSeatLayoutImageUrl())
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();

        venue = venueRepository.save(venue);

        return VenueCreateResponse.builder()
            .venueId(venue.getId())
            .message("장소가 성공적으로 등록되었습니다")
            .build();
    }
}

