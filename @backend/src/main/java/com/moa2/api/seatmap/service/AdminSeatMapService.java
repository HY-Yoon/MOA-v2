package com.moa2.api.seatmap.service;

import com.moa2.api.seatmap.dto.*;
import com.moa2.api.seatmap.entity.SeatMap;
import com.moa2.api.seatmap.seatmap.repository.SeatMapRepository;
import com.moa2.api.show.domain.entity.Seat;
import com.moa2.api.show.domain.entity.Venue;
import com.moa2.api.show.domain.entity.VenueSeatSection;
import com.moa2.api.show.domain.repository.SeatRepository;
import com.moa2.global.model.SeatStatus;
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
        private final SeatRepository seatRepository;

        /**
         * 좌석배치도 목록 조회 (필터링 및 페이징)
         */
        public Page<SeatmapDto.ListResponse> getSeatMapList(SeatmapDto.ListRequest request, Pageable pageable) {
                // Record 방식: getRegion() -> region()
                Page<SeatMap> seatMaps = seatMapRepository.findByFilters(
                                request.region(),
                                request.venueName(),
                                request.hallName(),
                                pageable);

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
                                request.hallName()).isPresent();

                return new SeatmapDto.DuplicateCheckResponse(isDuplicate);
        }

        /**
         * 좌석배치도 등록
         */
        @Transactional
        public SeatmapDto.CreateResponse createSeatMap(SeatmapDto.CreateRequest request) {
                // 중복 검사 (Record 메서드 호출)
                seatMapRepository
                                .findByRegionAndVenueNameAndHallName(request.region(), request.venueName(),
                                                request.hallName())
                                .ifPresent(existing -> {
                                        throw new RuntimeException(
                                                        String.format("이미 등록된 좌석배치도입니다: %s, %s, %s",
                                                                        request.region().name(), request.venueName(),
                                                                        request.hallName()));
                                });

                // ============================================
                // 1. Canvas 변환 (배치도 캔버스 정보)
                // - width: 캔버스 너비
                // - height: 캔버스 높이
                // - seatRadius: 좌석 반지름
                // - rowGap: 행 간격
                // - columnGap: 열 간격
                // ============================================
                Map<String, Object> canvasMap = new HashMap<>();
                canvasMap.put("width", request.canvas().width());
                canvasMap.put("height", request.canvas().height());
                canvasMap.put("seatRadius", request.canvas().seatRadius());
                canvasMap.put("rowGap", request.canvas().rowGap());
                canvasMap.put("columnGap", request.canvas().columnGap());

                // ============================================
                // 2. Sections 변환 (구역 정보)
                // - sectionId: 구역 ID (예: "A", "B")
                // - name: 구역명 (예: "A구역", "VIP")
                // - color: 구역 색상 (예: "#FF6B6B")
                // - price: 구역 기본 가격
                // ============================================
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

                // ============================================
                // 3. Seats 변환 (좌석 정보 - JSON 저장용)
                // - seatId: 좌석 ID (예: "A-1")
                // - sectionId: 구역 ID (어떤 구역에 속하는지)
                // - row: 행 (예: "A")
                // - number: 번호 (예: 1)
                // - x: X 좌표
                // - y: Y 좌표
                // 
                // 주의: 이 정보는 seat_maps 테이블의 seats 컬럼에 JSON으로 저장되며,
                // 실제 물리 좌석은 아래에서 Seat 엔티티로 별도 생성됩니다.
                // ============================================
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
                                request.region()).orElseGet(() -> {
                                        log.info("Venue 생성: name={}, hallName={}, region={}, totalSeats={}",
                                                        request.venueName(), request.hallName(), request.region(),
                                                        totalSeatsCount);
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
                        List<VenueSeatSection> existingSections = venueSeatSectionRepository
                                        .findByVenueId(venue.getId());
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
                        } else if (existingSection.getDefaultPrice() == null
                                        || !existingSection.getDefaultPrice().equals(sectionReq.price())) {
                                existingSection.setDefaultPrice(sectionReq.price());
                                venueSeatSectionRepository.save(existingSection);
                        }
                }

                // ============================================
                // 4. Seat 엔티티 생성 및 저장 (물리 좌석 동기화)
                // - seat_maps의 seats JSON 정보를 기반으로
                // - 실제 seats 테이블에 물리 좌석 데이터 생성
                // - 기존 좌석이 있다면 삭제하고 다시 생성 (초기화)
                // ============================================
                if (seatRepository.countByVenueId(venue.getId()) > 0) {
                        java.util.List<Seat> existingSeats = seatRepository.findByVenueId(venue.getId());
                        seatRepository.deleteAll(existingSeats);
                }

                java.util.List<Seat> seatsToSave = new java.util.ArrayList<>();
                java.util.List<VenueSeatSection> finalVenueSections = venueSeatSectionRepository
                                .findByVenueId(venue.getId());

                // Section 매핑을 위한 Map 생성: sectionId -> SectionRequest
                java.util.Map<String, SeatmapDto.CreateRequest.SectionRequest> sectionReqMap = request.sections()
                                .stream()
                                .collect(Collectors.toMap(SeatmapDto.CreateRequest.SectionRequest::sectionId, s -> s));

                for (var seatReq : request.seats()) {
                        SeatmapDto.CreateRequest.SectionRequest matchedSectionReq = sectionReqMap
                                        .get(seatReq.sectionId());
                        if (matchedSectionReq == null)
                                continue;

                        VenueSeatSection matchedVenueSection = finalVenueSections.stream()
                                        .filter(vs -> vs.getName().equals(matchedSectionReq.name()))
                                        .findFirst()
                                        .orElseThrow(() -> new RuntimeException(
                                                        "Section not found for name: " + matchedSectionReq.name()));

                        Seat seat = Seat.builder()
                                        .venue(venue)
                                        .section(matchedVenueSection)
                                        .seatRow(seatReq.row())
                                        .seatNumber(seatReq.number())
                                        .x(seatReq.x())
                                        .y(seatReq.y())
                                        .status(SeatStatus.AVAILABLE)
                                        .build();
                        seatsToSave.add(seat);
                }
                seatRepository.saveAll(seatsToSave);

                // ============================================
                // 5. SeatMap 엔티티 생성 및 저장
                // - canvas: 배치도 캔버스 정보 (JSON)
                // - sections: 구역 목록 (JSON 배열)
                // - seats: 좌석 정보 (JSON 배열) - 참고용
                // ============================================
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