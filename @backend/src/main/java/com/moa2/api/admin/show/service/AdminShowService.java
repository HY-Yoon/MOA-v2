package com.moa2.api.admin.show.service;

import com.moa2.api.admin.show.dto.*;
import com.moa2.domain.show.entity.*;
import com.moa2.domain.show.repository.*;
import com.moa2.domain.reservation.repository.ReservationRepository;
import com.moa2.global.model.*;
import com.moa2.global.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminShowService {

    private final ShowRepository showRepository;
    private final ShowScheduleRepository showScheduleRepository;
    private final ShowCastRepository showCastRepository;
    private final ShowCastScheduleRepository showCastScheduleRepository;
    private final ShowSeatGradeRepository showSeatGradeRepository;
    private final CastRepository castRepository;
    private final SeatLayoutRepository seatLayoutRepository;
    private final VenueSeatSectionRepository venueSeatSectionRepository;
    private final ReservationRepository reservationRepository;
    private final SeatRepository seatRepository;

    public Page<ShowListResponse> getShowList(ShowListRequest request, Pageable pageable) {
        Page<Show> shows = showRepository.findShowsWithFilters(
            request.getShowStatus(),
            request.getSaleStatus(),
            request.getKeyword(),
            request.getStartDate(),
            request.getEndDate(),
            pageable
        );

        List<ShowListResponse> content = shows.getContent().stream()
            .map(show -> {
                ShowSchedule firstSchedule = showScheduleRepository
                    .findByShowIdOrderByDateAndTime(show.getId())
                    .stream()
                    .findFirst()
                    .orElse(null);

                LocalDateTime firstScheduleDate = null;
                if (firstSchedule != null) {
                    firstScheduleDate = LocalDateTime.of(
                        firstSchedule.getShowDate(),
                        firstSchedule.getShowTime()
                    );
                }

                return ShowListResponse.builder()
                    .id(show.getId())
                    .title(show.getTitle())
                    .genre(show.getGenre() != null ? show.getGenre().name() : null)
                    .status(show.getStatus() != null ? show.getStatus().name() : null)
                    .saleStatus(show.getSaleStatus() != null ? show.getSaleStatus().name() : null)
                    .venue(show.getVenue() != null ? show.getVenue().getName() : null)
                    .region(show.getVenue() != null && show.getVenue().getRegion() != null 
                        ? show.getVenue().getRegion().name() : null)
                    .firstScheduleDate(firstScheduleDate)
                    .saleStartDate(show.getSaleStartDate())
                    .saleEndDate(show.getSaleEndDate())
                    .build();
            })
            .collect(Collectors.toList());

        return new PageImpl<>(content, pageable, shows.getTotalElements());
    }

    public ShowDetailResponse getShowDetail(Long id) {
        Show show = showRepository.findByIdAndNotDeleted(id);
        if (show == null) {
            throw new RuntimeException("공연을 찾을 수 없습니다");
        }

        List<ShowSchedule> schedules = showScheduleRepository.findByShowIdOrderByDateAndTime(id);
        List<ShowCast> casts = showCastRepository.findByShowId(id);
        List<ShowSeatGrade> seatGrades = showSeatGradeRepository.findByShowId(id);

        // 스케줄 정보 구성
        List<ShowDetailResponse.ScheduleInfo> scheduleInfos = schedules.stream()
            .map(schedule -> {
                Long totalSeats = show.getVenue() != null 
                    ? seatRepository.countByVenueId(show.getVenue().getId()) 
                    : 0L;
                Long reservationCount = reservationRepository.countByScheduleId(schedule.getId());
                Long remainingSeats = totalSeats - reservationCount;

                return ShowDetailResponse.ScheduleInfo.builder()
                    .scheduleId(schedule.getId())
                    .showDate(schedule.getShowDate())
                    .showTime(schedule.getShowTime())
                    .ticketOpenTime(schedule.getTicketOpenTime())
                    .remainingSeats(remainingSeats.intValue())
                    .totalSeats(totalSeats.intValue())
                    .reservationCount(reservationCount.intValue())
                    .build();
            })
            .collect(Collectors.toList());

        // 출연진 정보 구성
        List<ShowDetailResponse.CastInfo> castInfos = casts.stream()
            .map(showCast -> {
                List<ShowCastSchedule> castSchedules = showCastScheduleRepository
                    .findByShowCastId(showCast.getId());
                List<Long> scheduleIds = castSchedules.stream()
                    .map(scs -> scs.getSchedule().getId())
                    .collect(Collectors.toList());

                return ShowDetailResponse.CastInfo.builder()
                    .castId(showCast.getCast().getId())
                    .name(showCast.getCast().getName())
                    .role(showCast.getRoleName())
                    .scheduleIds(scheduleIds)
                    .build();
            })
            .collect(Collectors.toList());

        // 좌석 가격 정보 구성
        List<ShowDetailResponse.SeatPriceInfo> seatPriceInfos = seatGrades.stream()
            .map(grade -> ShowDetailResponse.SeatPriceInfo.builder()
                .sectionId(grade.getSection().getId().toString())
                .sectionName(grade.getSection().getName())
                .price(grade.getPrice())
                .build())
            .collect(Collectors.toList());

        // Hall 정보 찾기 (SeatLayout을 통해)
        String hallName = null;
        if (show.getSeatMapId() != null) {
            try {
                Long seatMapIdLong = Long.parseLong(show.getSeatMapId());
                SeatLayout seatLayout = seatLayoutRepository.findById(seatMapIdLong).orElse(null);
                if (seatLayout != null && seatLayout.getHall() != null) {
                    hallName = seatLayout.getHall().getName();
                }
            } catch (NumberFormatException e) {
                // seatMapId가 숫자가 아닌 경우 무시
            }
        }

        return ShowDetailResponse.builder()
            .id(show.getId())
            .title(show.getTitle())
            .genre(show.getGenre() != null ? show.getGenre().name() : null)
            .seatMapId(show.getSeatMapId())
            .venueName(show.getVenue() != null ? show.getVenue().getName() : null)
            .hallName(hallName)
            .region(show.getVenue() != null && show.getVenue().getRegion() != null 
                ? show.getVenue().getRegion().name() : null)
            .runningTime(show.getRunningTime())
            .posterUrl(show.getPosterUrl())
            .detailImageUrls(show.getDetailImageUrls())
            .status(show.getStatus() != null ? show.getStatus().name() : null)
            .saleStatus(show.getSaleStatus() != null ? show.getSaleStatus().name() : null)
            .saleStartDate(show.getSaleStartDate())
            .saleEndDate(show.getSaleEndDate())
            .schedules(scheduleInfos)
            .casts(castInfos)
            .seatPrices(seatPriceInfos)
            .createdAt(show.getCreatedAt())
            .updatedAt(show.getUpdatedAt())
            .build();
    }

    @Transactional
    public ShowCreateResponse createShow(ShowCreateRequest request) {
        // seatMapId 검증 (String 형식이므로 Long으로 파싱 시도)
        SeatLayout seatLayout = null;
        try {
            Long seatMapIdLong = Long.parseLong(request.getSeatMapId());
            seatLayout = seatLayoutRepository.findById(seatMapIdLong)
                .orElseThrow(() -> new RuntimeException("좌석배치도를 찾을 수 없습니다"));
        } catch (NumberFormatException e) {
            throw new RuntimeException("좌석배치도 ID 형식이 올바르지 않습니다");
        }

        // Venue는 SeatLayout을 통해 Hall을 통해 찾아야 함
        // 일단 SeatLayout의 Hall을 통해 Venue를 찾음
        Venue venue = seatLayout.getHall() != null ? seatLayout.getHall().getVenue() : null;
        if (venue == null) {
            throw new RuntimeException("공연장 정보를 찾을 수 없습니다");
        }

        // 마지막 공연일 계산
        LocalDate lastShowDate = request.getSchedules().stream()
            .map(ShowCreateRequest.ScheduleRequest::getShowDate)
            .max(LocalDate::compareTo)
            .orElse(LocalDate.now());
        LocalDate firstShowDate = request.getSchedules().stream()
            .map(ShowCreateRequest.ScheduleRequest::getShowDate)
            .min(LocalDate::compareTo)
            .orElse(LocalDate.now());
        
        // Show 생성 (createdAt, updatedAt은 @PrePersist/@PreUpdate로 자동 설정)
        Show show = Show.builder()
            .venue(venue)
            .title(request.getTitle())
            .genre(Genre.valueOf(request.getGenre()))
            .runningTime(request.getRunningTime())
            .posterUrl(request.getPosterUrl())
            .detailImageUrls(request.getDetailImageUrls())
            .status(ShowStatus.WAITING)
            .saleStatus(SaleStatus.ALLOWED)
            .seatMapId(request.getSeatMapId())
            .saleStartDate(request.getSaleStartDate())
            .saleEndDate(LocalDateTime.of(lastShowDate, LocalTime.of(23, 59, 59)))
            .startDate(firstShowDate)
            .endDate(lastShowDate)
            .viewCount(0L)
            .build();

        show = showRepository.save(show);

        // Schedule 생성
        List<ShowSchedule> savedSchedules = new ArrayList<>();
        for (ShowCreateRequest.ScheduleRequest scheduleReq : request.getSchedules()) {
            // showTime 파싱 ("19:00" -> LocalTime)
            LocalTime showTime = LocalTime.parse(scheduleReq.getShowTime(), 
                DateTimeFormatter.ofPattern("HH:mm"));
            
            ShowSchedule schedule = ShowSchedule.builder()
                .show(show)
                .showDate(scheduleReq.getShowDate())
                .showTime(showTime)
                .ticketOpenTime(scheduleReq.getTicketOpenTime())
                .status(ScheduleStatus.BEFORE_OPEN)
                .build();
            
            savedSchedules.add(showScheduleRepository.save(schedule));
        }

        // ShowCast 및 ShowCastSchedule 생성
        if (request.getCasts() != null) {
            for (ShowCreateRequest.CastRequest castReq : request.getCasts()) {
                Cast cast = castRepository.findById(castReq.getCastId())
                    .orElseThrow(() -> new RuntimeException("출연진을 찾을 수 없습니다: " + castReq.getCastId()));

                ShowCast showCast = ShowCast.builder()
                    .show(show)
                    .cast(cast)
                    .roleName(castReq.getRole())
                    .build();
                showCast = showCastRepository.save(showCast);

                // ShowCastSchedule 생성
                if (castReq.getScheduleIds() != null) {
                    for (Long scheduleId : castReq.getScheduleIds()) {
                        ShowSchedule schedule = savedSchedules.stream()
                            .filter(s -> s.getId().equals(scheduleId))
                            .findFirst()
                            .orElse(null);
                        
                        if (schedule != null) {
                            ShowCastSchedule castSchedule = new ShowCastSchedule(showCast, schedule);
                            showCastScheduleRepository.save(castSchedule);
                        }
                    }
                }
            }
        }

        // ShowSeatGrade 생성 (createdAt, updatedAt은 @PrePersist/@PreUpdate로 자동 설정)
        if (request.getSeatPrices() != null) {
            for (ShowCreateRequest.SeatPriceRequest priceReq : request.getSeatPrices()) {
                VenueSeatSection section = venueSeatSectionRepository.findById(Long.parseLong(priceReq.getSectionId()))
                    .orElseThrow(() -> new RuntimeException("좌석 구역을 찾을 수 없습니다: " + priceReq.getSectionId()));

                ShowSeatGrade grade = ShowSeatGrade.builder()
                    .show(show)
                    .section(section)
                    .price(priceReq.getPrice())
                    .build();
                showSeatGradeRepository.save(grade);
            }
        }

        // 각 Schedule마다 Seat 자동 생성 (SeatTemplate 기반)
        // 이 부분은 AdminSeatMapService를 활용하거나 별도 로직 필요
        // 일단 기본 구조만 구현

        return ShowCreateResponse.builder()
            .showId(show.getId())
            .message("공연이 등록되었습니다")
            .build();
    }

    @Transactional
    public ShowUpdateResponse updateShow(Long id, ShowUpdateRequest request) {
        Show show = showRepository.findByIdAndNotDeleted(id);
        if (show == null) {
            throw new RuntimeException("공연을 찾을 수 없습니다");
        }

        // WAITING 상태: 전체 수정 가능
        // ON_SALE 이후: 장르, seatMapId, 예매 시작일 수정 불가
        if (show.getStatus() != ShowStatus.WAITING) {
            // ON_SALE 이후에는 제한된 필드만 수정 가능
        }

        if (request.getTitle() != null) {
            show.setTitle(request.getTitle());
        }
        if (request.getRunningTime() != null) {
            show.setRunningTime(request.getRunningTime());
        }
        if (request.getPosterUrl() != null) {
            show.setPosterUrl(request.getPosterUrl());
        }
        if (request.getDetailImageUrls() != null) {
            show.setDetailImageUrls(request.getDetailImageUrls());
        }

        // updatedAt은 @PreUpdate로 자동 설정됨
        showRepository.save(show);

        // 출연진 업데이트
        if (request.getCasts() != null) {
            // 기존 출연진 삭제
            showCastScheduleRepository.deleteByShowCastShowId(id);
            showCastRepository.deleteByShowId(id);

            // 새 출연진 추가
            List<ShowSchedule> schedules = showScheduleRepository.findByShowId(id);
            for (ShowUpdateRequest.CastRequest castReq : request.getCasts()) {
                Cast cast = castRepository.findById(castReq.getCastId())
                    .orElseThrow(() -> new RuntimeException("출연진을 찾을 수 없습니다: " + castReq.getCastId()));

                ShowCast showCast = ShowCast.builder()
                    .show(show)
                    .cast(cast)
                    .roleName(castReq.getRole())
                    .build();
                showCast = showCastRepository.save(showCast);

                if (castReq.getScheduleIds() != null) {
                    for (Long scheduleId : castReq.getScheduleIds()) {
                        ShowSchedule schedule = schedules.stream()
                            .filter(s -> s.getId().equals(scheduleId))
                            .findFirst()
                            .orElse(null);
                        
                        if (schedule != null) {
                            ShowCastSchedule castSchedule = new ShowCastSchedule(showCast, schedule);
                            showCastScheduleRepository.save(castSchedule);
                        }
                    }
                }
            }
        }

        return ShowUpdateResponse.builder()
            .showId(show.getId())
            .message("공연이 수정되었습니다")
            .build();
    }

    @Transactional
    public ShowDeleteResponse deleteShow(Long id) {
        Show show = showRepository.findByIdAndNotDeleted(id);
        if (show == null) {
            throw new RuntimeException("공연을 찾을 수 없습니다");
        }

        if (show.getStatus() != ShowStatus.WAITING) {
            throw new RuntimeException("WAITING 상태의 공연만 삭제할 수 있습니다");
        }

        // 논리 삭제 (updatedAt은 @PreUpdate로 자동 설정됨)
        show.setStatus(ShowStatus.SUSPENDED); // DELETED가 없으므로 SUSPENDED 사용
        showRepository.save(show);

        // 연관된 Schedule, ShowCast도 삭제 (논리 삭제)
        // 실제로는 ShowStatus만 변경하면 됨

        return ShowDeleteResponse.builder()
            .message("공연이 삭제되었습니다")
            .build();
    }

    @Transactional
    public ShowSaleStatusUpdateResponse updateSaleStatus(Long id, ShowSaleStatusUpdateRequest request) {
        Show show = showRepository.findByIdAndNotDeleted(id);
        if (show == null) {
            throw new RuntimeException("공연을 찾을 수 없습니다");
        }

        show.setSaleStatus(request.getSaleStatus());
        // updatedAt은 @PreUpdate로 자동 설정됨
        showRepository.save(show);

        return ShowSaleStatusUpdateResponse.builder()
            .showId(show.getId())
            .saleStatus(show.getSaleStatus())
            .message("판매 상태가 변경되었습니다")
            .build();
    }
}

