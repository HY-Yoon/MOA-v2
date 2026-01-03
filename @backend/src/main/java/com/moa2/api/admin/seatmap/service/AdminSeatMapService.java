package com.moa2.api.admin.seatmap.service;

import com.moa2.api.admin.seatmap.dto.*;
import com.moa2.domain.seatmap.entity.SeatMap;
import com.moa2.domain.seatmap.repository.SeatMapRepository;
import com.moa2.domain.show.entity.Venue;
import com.moa2.domain.show.entity.VenueSeatSection;
import com.moa2.domain.show.repository.VenueRepository;
import com.moa2.domain.show.repository.VenueSeatSectionRepository;
import com.moa2.global.model.Region;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
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
    public Page<SeatMapListResponse> getSeatMapList(SeatMapListRequest request, Pageable pageable) {
        // 한글 지역명을 Region enum으로 변환
        Region region = null;
        if (request.getRegion() != null && !request.getRegion().trim().isEmpty()) {
            region = convertRegionFromKorean(request.getRegion());
        }
        
        // 필터링된 목록 조회
        Page<SeatMap> seatMaps = seatMapRepository.findByFilters(
            region,
            request.getVenueName(),
            request.getHallName(),
            pageable
        );
        
        // DTO 변환
        List<SeatMapListResponse> content = seatMaps.getContent().stream()
            .map(seatMap -> SeatMapListResponse.builder()
                .seatMapId(generateSeatMapId(seatMap.getId()))
                .region(seatMap.getRegion().getName())
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
    public SeatMapDuplicateCheckResponse checkDuplicate(SeatMapDuplicateCheckRequest request) {
        // 모든 필드가 제공되어야 중복 검사 가능
        if (request.getRegion() == null || request.getVenueName() == null || request.getHallName() == null) {
            return SeatMapDuplicateCheckResponse.builder()
                .isDuplicate(false)
                .build();
        }
        
        // 한글 지역명을 Region enum으로 변환
        Region region = convertRegionFromKorean(request.getRegion());
        
        // 중복 검사
        boolean isDuplicate = seatMapRepository.findByRegionAndVenueNameAndHallName(
            region,
            request.getVenueName(),
            request.getHallName()
        ).isPresent();
        
        return SeatMapDuplicateCheckResponse.builder()
            .isDuplicate(isDuplicate)
            .build();
    }
    
    /**
     * 좌석배치도 등록
     */
    @Transactional
    public SeatMapCreateResponse createSeatMap(SeatMapCreateRequest request) {
        // 한글 지역명을 Region enum으로 변환
        Region region = convertRegionFromKorean(request.getRegion());
        
        // 중복 검사
        seatMapRepository.findByRegionAndVenueNameAndHallName(region, request.getVenueName(), request.getHallName())
            .ifPresent(existing -> {
                throw new RuntimeException(
                    String.format("이미 등록된 좌석배치도입니다: %s, %s, %s", 
                        request.getRegion(), request.getVenueName(), request.getHallName())
                );
            });
        
        // Canvas를 Map으로 변환
        Map<String, Object> canvasMap = new java.util.HashMap<>();
        canvasMap.put("width", request.getCanvas().getWidth());
        canvasMap.put("height", request.getCanvas().getHeight());
        canvasMap.put("seatRadius", request.getCanvas().getSeatRadius());
        
        // Sections를 List<Map>으로 변환
        List<Map<String, Object>> sectionsList = request.getSections().stream()
            .map(section -> {
                Map<String, Object> sectionMap = new java.util.HashMap<>();
                sectionMap.put("sectionId", section.getSectionId());
                sectionMap.put("name", section.getName());
                sectionMap.put("color", section.getColor());
                sectionMap.put("price", section.getPrice());
                return sectionMap;
            })
            .collect(Collectors.toList());
        
        // Seats를 List<Map>으로 변환
        List<Map<String, Object>> seatsList = request.getSeats().stream()
            .map(seat -> {
                Map<String, Object> seatMap = new java.util.HashMap<>();
                seatMap.put("seatId", seat.getSeatId());
                seatMap.put("sectionId", seat.getSectionId());
                seatMap.put("row", seat.getRow());
                seatMap.put("number", seat.getNumber());
                seatMap.put("x", seat.getX());
                seatMap.put("y", seat.getY());
                return seatMap;
            })
            .collect(Collectors.toList());
        
        // Venue 찾기 또는 생성
        Venue venue = venueRepository.findByNameAndHallNameAndRegion(
            request.getVenueName(),
            request.getHallName(),
            region
        ).orElseGet(() -> {
            log.info("Venue를 찾을 수 없어 새로 생성: name={}, hallName={}, region={}", 
                request.getVenueName(), request.getHallName(), region);
            Venue newVenue = Venue.builder()
                .name(request.getVenueName())
                .hallName(request.getHallName())
                .region(region)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
            return venueRepository.save(newVenue);
        });
        
        // VenueSeatSection 생성 (sections 정보를 VenueSeatSection으로 변환)
        // 좌석배치도에 등록된 구역 정보를 VenueSeatSection으로 저장하여 공연 등록 시 사용
        int displayOrder = 0;
        for (SeatMapCreateRequest.SectionRequest sectionReq : request.getSections()) {
            // 이미 존재하는 구역인지 확인 (venue_id + name으로)
            List<VenueSeatSection> existingSections = venueSeatSectionRepository.findByVenueId(venue.getId());
            VenueSeatSection existingSection = existingSections.stream()
                .filter(section -> section.getName().equals(sectionReq.getName()))
                .findFirst()
                .orElse(null);
            
            if (existingSection == null) {
                // 새 구역 생성
                VenueSeatSection venueSection = VenueSeatSection.builder()
                    .venue(venue)
                    .name(sectionReq.getName())
                    .displayOrder(displayOrder++)
                    .defaultPrice(sectionReq.getPrice())
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();
                venueSeatSectionRepository.save(venueSection);
                log.info("VenueSeatSection 생성: venueId={}, name={}, price={}", 
                    venue.getId(), sectionReq.getName(), sectionReq.getPrice());
            } else {
                // 이미 존재하면 가격만 업데이트
                if (existingSection.getDefaultPrice() == null || 
                    !existingSection.getDefaultPrice().equals(sectionReq.getPrice())) {
                    existingSection.setDefaultPrice(sectionReq.getPrice());
                    existingSection.setUpdatedAt(LocalDateTime.now());
                    venueSeatSectionRepository.save(existingSection);
                    log.info("VenueSeatSection 가격 업데이트: venueId={}, name={}, price={}", 
                        venue.getId(), sectionReq.getName(), sectionReq.getPrice());
                }
            }
        }
        
        // SeatMap 엔티티 생성
        SeatMap seatMap = SeatMap.builder()
            .region(region)
            .venueName(request.getVenueName())
            .hallName(request.getHallName())
            .canvas(canvasMap)
            .sections(sectionsList)
            .seats(seatsList)
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();
        
        seatMap = seatMapRepository.save(seatMap);
        
        return SeatMapCreateResponse.builder()
            .seatMapId(generateSeatMapId(seatMap.getId()))
            .build();
    }
    
    /**
     * 한글 지역명을 Region enum으로 변환
     */
    private Region convertRegionFromKorean(String koreanRegion) {
        for (Region region : Region.values()) {
            if (region.getName().equals(koreanRegion)) {
                return region;
            }
        }
        throw new IllegalArgumentException("지원하지 않는 지역입니다: " + koreanRegion);
    }
    
    /**
     * SeatMap ID를 "SM001" 형식으로 생성
     */
    private String generateSeatMapId(Long id) {
        return String.format("SM%03d", id);
    }
}

