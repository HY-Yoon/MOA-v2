package com.moa2.api.seatmap.service;

import com.moa2.api.seatmap.dto.*;
import com.moa2.api.seatmap.entity.SeatMap;
import com.moa2.api.seatmap.seatmap.repository.SeatMapRepository;
import com.moa2.api.show.domain.entity.Venue;
import com.moa2.api.show.domain.entity.VenueSeatSection;
import com.moa2.api.show.domain.repository.VenueRepository;
import com.moa2.api.show.domain.repository.VenueSeatSectionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminSeatMapService {

    private final SeatMapRepository seatMapRepository;
    private final VenueRepository venueRepository;
    private final VenueSeatSectionRepository venueSeatSectionRepository;

    /**
     * 좌석배치도 목록 조회 (필터링 및 페이징)
     */
    public Page<SeatmapDto.ListResponse> getSeatMapList(SeatmapDto.ListRequest request, Pageable pageable) {
        // Record 방식: getRegion() -> region()
        Page<SeatMap> seatMaps = seatMapRepository.findByFilters(
                request.region(),
                request.venueName(),
                request.hallName(),
                pageable
        );

        List<SeatmapDto.ListResponse> content = seatMaps.getContent().stream()
                .map(seatMap -> SeatmapDto.ListResponse.builder()
                        .seatMapId(generateSeatMapId(seatMap.getId()))
                        .region(seatMap.getRegion().name())
                        .venueName(seatMap.getVenueName())
                        .hallName(seatMap.getHallName())
                        .createdAt(seatMap.getCreatedAt())
                        .updatedAt(seatMap.getUpdatedAt())
                        .build())
                .collect(Collectors.toList());

        return new PageImpl<>(content, pageable, seatMaps.getTotalElements());
    }

    /**
     * 중복 검사
     */
    public SeatmapDto.DuplicateCheckResponse checkDuplicate(SeatmapDto.DuplicateCheckRequest request) {
        // Record 방식 적용
        if (request.region() == null || request.venueName() == null || request.hallName() == null) {
            return new SeatmapDto.DuplicateCheckResponse(false);
        }

        boolean isDuplicate = seatMapRepository.findByRegionAndVenueNameAndHallName(
                request.region(),
                request.venueName(),
                request.hallName()
        ).isPresent();

        return new SeatmapDto.DuplicateCheckResponse(isDuplicate);
    }

    /**
     * 좌석배치도 등록
     */
    @Transactional
    public SeatmapDto.CreateResponse createSeatMap(SeatmapDto.CreateRequest request) {
        // 중복 검사 (Record 메서드 호출)
        seatMapRepository.findByRegionAndVenueNameAndHallName(request.region(), request.venueName(), request.hallName())
                .ifPresent(existing -> {
                    throw new RuntimeException(
                            String.format("이미 등록된 좌석배치도입니다: %s, %s, %s",
                                    request.region().name(), request.venueName(), request.hallName())
                    );
                });

        // Canvas 변환
        Map<String, Object> canvasMap = new HashMap<>();
        canvasMap.put("width", request.canvas().width());
        canvasMap.put("height", request.canvas().height());
        canvasMap.put("seatRadius", request.canvas().seatRadius());

        // Sections 변환
        List<Map<String, Object>> sectionsList = request.sections().stream()
                .map(section -> {
                    Map<String, Object> sectionMap = new HashMap<>();
                    sectionMap.put("sectionId", section.sectionId());
                    sectionMap.put("name", section.name());
                    sectionMap.put("color", section.color());
                    sectionMap.put("price", section.price());
                    return sectionMap;
                })
                .collect(Collectors.toList());

        // Seats 변환
        List<Map<String, Object>> seatsList = request.seats().stream()
                .map(seat -> {
                    Map<String, Object> seatMap = new HashMap<>();
                    seatMap.put("seatId", seat.seatId());
                    seatMap.put("sectionId", seat.sectionId());
                    seatMap.put("row", seat.row());
                    seatMap.put("number", seat.number());
                    seatMap.put("x", seat.x());
                    seatMap.put("y", seat.y());
                    return seatMap;
                })
                .collect(Collectors.toList());

        int totalSeatsCount = request.seats().size();

        // Venue 처리
        Venue venue = venueRepository.findByNameAndHallNameAndRegion(
                request.venueName(),
                request.hallName(),
                request.region()
        ).orElseGet(() -> {
            log.info("Venue 생성: name={}, hallName={}, region={}, totalSeats={}",
                    request.venueName(), request.hallName(), request.region(), totalSeatsCount);
            return venueRepository.save(Venue.builder()
                    .name(request.venueName())
                    .hallName(request.hallName())
                    .region(request.region())
                    .totalSeats(totalSeatsCount)
                    .build());
        });

        // VenueSeatSection 생성 로직 (Record 타입 참조 수정)
        int displayOrder = 0;
        for (var sectionReq : request.sections()) {
            List<VenueSeatSection> existingSections = venueSeatSectionRepository.findByVenueId(venue.getId());
            VenueSeatSection existingSection = existingSections.stream()
                    .filter(section -> section.getName().equals(sectionReq.name()))
                    .findFirst()
                    .orElse(null);

            if (existingSection == null) {
                venueSeatSectionRepository.save(VenueSeatSection.builder()
                        .venue(venue)
                        .name(sectionReq.name())
                        .displayOrder(displayOrder++)
                        .defaultPrice(sectionReq.price())
                        .build());
            } else if (existingSection.getDefaultPrice() == null || !existingSection.getDefaultPrice().equals(sectionReq.price())) {
                existingSection.setDefaultPrice(sectionReq.price());
                venueSeatSectionRepository.save(existingSection);
            }
        }

        // SeatMap 엔티티 생성 및 저장
        SeatMap seatMap = seatMapRepository.save(SeatMap.builder()
                .region(request.region())
                .venueName(request.venueName())
                .hallName(request.hallName())
                .canvas(canvasMap)
                .sections(sectionsList)
                .seats(seatsList)
                .build());

        return new SeatmapDto.CreateResponse(generateSeatMapId(seatMap.getId()));
    }

    private String generateSeatMapId(Long id) {
        return String.format("SM%03d", id);
    }
}