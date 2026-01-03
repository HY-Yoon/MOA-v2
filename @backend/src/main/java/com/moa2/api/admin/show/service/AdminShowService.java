package com.moa2.api.admin.show.service;

import com.moa2.api.admin.show.dto.*;
import com.moa2.domain.show.entity.*;
import com.moa2.domain.show.repository.*;
import com.moa2.domain.reservation.repository.ReservationRepository;
import com.moa2.global.model.*;
import com.moa2.global.service.FileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminShowService {

    private final ShowRepository showRepository;
    private final ShowScheduleRepository showScheduleRepository;
    private final ShowSeatGradeRepository showSeatGradeRepository;
    private final VenueSeatSectionRepository venueSeatSectionRepository;
    private final VenueRepository venueRepository;
    private final ReservationRepository reservationRepository;
    private final SeatRepository seatRepository;
    private final FileService fileService;

    public Page<ShowListResponse> getShowList(ShowListRequest request, Pageable pageable) {
        // keyword가 있으면 검색 패턴 생성 (null이거나 빈 문자열이면 null)
        // 대소문자 구분 없이 검색하기 위해 패턴만 생성 (LIKE는 기본적으로 대소문자 구분)
        String keywordPattern = null;
        if (request.getKeyword() != null && !request.getKeyword().trim().isEmpty()) {
            keywordPattern = "%" + request.getKeyword().trim() + "%";
        }

        LocalDate startDate = request.getStartDate();
        LocalDate endDate = request.getEndDate();
        
        Page<Show> shows = showRepository.findShowsWithFilters(
            request.getShowStatus(),
            request.getSaleStatus(),
            keywordPattern,
            startDate,  // null이면 쿼리에서 IS NULL 체크
            endDate,    // null이면 쿼리에서 IS NULL 체크
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
                    .hallName(show.getVenue() != null ? show.getVenue().getHallName() : null)
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

        // 좌석 가격 정보 구성
        List<ShowDetailResponse.SeatPriceInfo> seatPriceInfos = seatGrades.stream()
            .map(grade -> ShowDetailResponse.SeatPriceInfo.builder()
                .sectionId(grade.getSection().getId().toString())
                .sectionName(grade.getSection().getName())
                .price(grade.getPrice())
                .build())
            .collect(Collectors.toList());

        // Venue에서 hallName 가져오기
        String hallName = show.getVenue() != null ? show.getVenue().getHallName() : null;

        return ShowDetailResponse.builder()
            .id(show.getId())
            .title(show.getTitle())
            .genre(show.getGenre() != null ? show.getGenre().name() : null)
            .venueName(show.getVenue() != null ? show.getVenue().getName() : null)
            .hallName(hallName)
            .region(show.getVenue() != null && show.getVenue().getRegion() != null 
                ? show.getVenue().getRegion().name() : null)
            .runningTime(show.getRunningTime())
            .posterUrl(show.getPosterUrl())
            .detailImageUrls(show.getDetailImageUrls())
            .cast(show.getCast())
            .status(show.getStatus() != null ? show.getStatus().name() : null)
            .saleStatus(show.getSaleStatus() != null ? show.getSaleStatus().name() : null)
            .saleStartDate(show.getSaleStartDate())
            .saleEndDate(show.getSaleEndDate())
            .schedules(scheduleInfos)
            .seatPrices(seatPriceInfos)
            .createdAt(show.getCreatedAt())
            .updatedAt(show.getUpdatedAt())
            .build();
    }

    @Transactional
    public ShowCreateResponse createShow(ShowCreateRequest request, MultipartFile poster, List<MultipartFile> detailImages) {
        // 파일 업로드 처리
        String posterUrl = null;
        if (poster != null && !poster.isEmpty()) {
            posterUrl = fileService.uploadFile(poster, "posters");
        } else {
            throw new IllegalArgumentException("포스터 이미지는 필수입니다");
        }

        String[] detailImageUrls = null;
        if (detailImages != null && !detailImages.isEmpty()) {
            detailImageUrls = fileService.uploadFiles(detailImages, "details");
        }

        // location 정보로 Region enum 변환 (한글 -> enum)
        Region region = convertRegionFromKorean(request.getLocation().getRegion());
        
        // location 정보로 Venue 조회, 없으면 자동 생성
        String venueName = request.getLocation().getVenueName();
        String hallName = request.getLocation().getHallName();
        
        log.debug("Venue 조회 시도: name={}, hallName={}, region={}", venueName, hallName, region);
        
        Venue venue = venueRepository.findByNameAndHallNameAndRegion(
            venueName,
            hallName,
            region
        ).orElseGet(() -> {
            // Venue가 없으면 자동 생성
            log.info("Venue를 찾을 수 없어 새로 생성: name={}, hallName={}, region={}", venueName, hallName, region);
            Venue newVenue = Venue.builder()
                .name(venueName)
                .hallName(hallName)
                .region(region)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
            return venueRepository.save(newVenue);
        });
        
        log.debug("사용할 Venue: id={}, name={}, hallName={}, region={}", venue.getId(), venue.getName(), venue.getHallName(), venue.getRegion());

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
            .posterUrl(posterUrl)
            .detailImageUrls(detailImageUrls)
            .cast(request.getCast())
            .status(ShowStatus.WAITING)
            .saleStatus(SaleStatus.ALLOWED)
            .saleStartDate(LocalDateTime.of(request.getBookingPeriod().getStartDate(), LocalTime.MIN))
            .saleEndDate(LocalDateTime.of(request.getBookingPeriod().getEndDate(), LocalTime.of(23, 59, 59)))
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

        // ShowSeatGrade 생성 - VenueSeatSection의 defaultPrice를 사용하여 자동 생성
        List<VenueSeatSection> venueSections = venueSeatSectionRepository.findByVenueId(venue.getId());
        for (VenueSeatSection section : venueSections) {
            if (section.getDefaultPrice() != null) {
                ShowSeatGrade grade = ShowSeatGrade.builder()
                    .show(show)
                    .section(section)
                    .price(section.getDefaultPrice())
                    .build();
                showSeatGradeRepository.save(grade);
            }
        }

        return ShowCreateResponse.builder()
            .showId(show.getId())
            .message("공연이 등록되었습니다")
            .build();
    }

    @Transactional
    public ShowUpdateResponse updateShow(Long id, ShowUpdateRequest request, MultipartFile poster, List<MultipartFile> detailImages) {
        Show show = showRepository.findByIdAndNotDeleted(id);
        if (show == null) {
            throw new RuntimeException("공연을 찾을 수 없습니다");
        }

        // 수정 권한 검증
        // WAITING 상태: 전체 수정 가능
        // ON_SALE 이후: 장르, 장소, 예매 시작일 수정 불가
        if (show.getStatus() != ShowStatus.WAITING) {
            // ON_SALE 이후에는 제한된 필드만 수정 가능
            if (request.getGenre() != null) {
                throw new RuntimeException("판매중인 공연은 장르를 수정할 수 없습니다");
            }
            if (request.getLocation() != null) {
                throw new RuntimeException("판매중인 공연은 장소를 수정할 수 없습니다");
            }
            if (request.getSaleStartDate() != null) {
                throw new RuntimeException("판매중인 공연은 예매 시작일을 수정할 수 없습니다");
            }
        }

        // 기본 필드 업데이트
        if (request.getTitle() != null) {
            show.setTitle(request.getTitle());
        }
        if (request.getRunningTime() != null) {
            show.setRunningTime(request.getRunningTime());
        }
        if (request.getCast() != null) {
            show.setCast(request.getCast());
        }

        // ON_SALE 이후 수정 불가 필드 (WAITING 상태에서만 수정 가능)
        if (show.getStatus() == ShowStatus.WAITING) {
            if (request.getGenre() != null) {
                show.setGenre(Genre.valueOf(request.getGenre()));
            }
            if (request.getLocation() != null) {
                // location 정보로 Region enum 변환 (한글 -> enum)
                Region region = convertRegionFromKorean(request.getLocation().getRegion());
                
                // location 정보로 Venue 조회, 없으면 자동 생성
                String venueName = request.getLocation().getVenueName();
                String hallName = request.getLocation().getHallName();
                
                log.debug("Venue 조회 시도: name={}, hallName={}, region={}", venueName, hallName, region);
                
                Venue venue = venueRepository.findByNameAndHallNameAndRegion(
                    venueName,
                    hallName,
                    region
                ).orElseGet(() -> {
                    // Venue가 없으면 자동 생성
                    log.info("Venue를 찾을 수 없어 새로 생성: name={}, hallName={}, region={}", venueName, hallName, region);
                    Venue newVenue = Venue.builder()
                        .name(venueName)
                        .hallName(hallName)
                        .region(region)
                        .createdAt(LocalDateTime.now())
                        .updatedAt(LocalDateTime.now())
                        .build();
                    return venueRepository.save(newVenue);
                });
                
                log.debug("사용할 Venue: id={}, name={}, hallName={}, region={}", venue.getId(), venue.getName(), venue.getHallName(), venue.getRegion());
                show.setVenue(venue);
            }
            if (request.getSaleStartDate() != null) {
                show.setSaleStartDate(request.getSaleStartDate());
            }
        }

        // 포스터 이미지 업데이트
        if (poster != null && !poster.isEmpty()) {
            // 기존 포스터 파일 삭제
            if (show.getPosterUrl() != null) {
                fileService.deleteFile(show.getPosterUrl());
            }
            // 새 포스터 업로드
            String newPosterUrl = fileService.uploadFile(poster, "posters");
            show.setPosterUrl(newPosterUrl);
        }

        // 상세 이미지 업데이트
        if (detailImages != null && !detailImages.isEmpty()) {
            // 기존 상세 이미지 파일 삭제
            if (show.getDetailImageUrls() != null) {
                fileService.deleteFiles(show.getDetailImageUrls());
            }
            // 새 상세 이미지 업로드
            String[] newDetailImageUrls = fileService.uploadFiles(detailImages, "details");
            show.setDetailImageUrls(newDetailImageUrls);
        }

        // 스케줄 추가/수정/삭제 처리 (팝업에서 변경한 모든 일정을 최종 저장)
        if (request.getSchedules() != null || (request.getDeletedScheduleIds() != null && !request.getDeletedScheduleIds().isEmpty())) {
            // 1. 스케줄 삭제 처리
            if (request.getDeletedScheduleIds() != null && !request.getDeletedScheduleIds().isEmpty()) {
                for (Long scheduleIdToDelete : request.getDeletedScheduleIds()) {
                    // 스케줄을 직접 ID로 조회 (더 안전함)
                    ShowSchedule scheduleToDelete = showScheduleRepository.findById(scheduleIdToDelete)
                        .orElseThrow(() -> new RuntimeException("삭제할 스케줄을 찾을 수 없습니다: " + scheduleIdToDelete));

                    // 해당 스케줄이 이 공연에 속하는지 확인
                    if (!scheduleToDelete.getShow().getId().equals(id)) {
                        throw new RuntimeException("스케줄이 이 공연에 속하지 않습니다. 스케줄 ID: " + scheduleIdToDelete);
                    }

                    // ON_SALE 이후에는 예매된 좌석이 없는 경우에만 삭제 가능
                    if (show.getStatus() != ShowStatus.WAITING) {
                        Long reservationCount = reservationRepository.countByScheduleId(scheduleToDelete.getId());
                        if (reservationCount > 0) {
                            throw new RuntimeException("예매된 좌석이 있는 스케줄은 삭제할 수 없습니다. 스케줄 ID: " + scheduleToDelete.getId());
                        }
                    }

                    showScheduleRepository.delete(scheduleToDelete);
                }
            }

            // 2. 스케줄 추가/수정 처리
            if (request.getSchedules() != null && !request.getSchedules().isEmpty()) {
                // 삭제 후 최신 스케줄 목록 조회
                List<ShowSchedule> existingSchedules = showScheduleRepository.findByShowIdOrderByDateAndTime(id);
                log.info("=== 스케줄 처리 시작 ===");
                log.info("공연 ID: {}", id);
                log.info("기존 스케줄 개수: {}", existingSchedules.size());
                existingSchedules.forEach(s -> log.info("  - 기존 스케줄: id={}, showDate={}, showTime={}", 
                    s.getId(), s.getShowDate(), s.getShowTime()));
                
                for (ShowUpdateRequest.ScheduleUpdateRequest scheduleReq : request.getSchedules()) {
                    log.info("처리 중인 스케줄: scheduleId={}, showDate={}, showTime={}", 
                        scheduleReq.getScheduleId(), scheduleReq.getShowDate(), scheduleReq.getShowTime());
                    
                    if (scheduleReq.getScheduleId() == null) {
                        // 새 스케줄 추가
                        log.info("새 스케줄 추가: showDate={}, showTime={}", scheduleReq.getShowDate(), scheduleReq.getShowTime());
                        LocalTime showTime = LocalTime.parse(scheduleReq.getShowTime(), DateTimeFormatter.ofPattern("HH:mm"));
                        ShowSchedule newSchedule = ShowSchedule.builder()
                            .show(show)
                            .showDate(scheduleReq.getShowDate())
                            .showTime(showTime)
                            .ticketOpenTime(scheduleReq.getTicketOpenTime())
                            .status(ScheduleStatus.BEFORE_OPEN)
                            .build();
                        showScheduleRepository.save(newSchedule);
                        log.info("새 스케줄 저장 완료: id={}", newSchedule.getId());
                    } else {
                        // 기존 스케줄 수정 - 직접 ID로 조회 (더 안전함)
                        log.info("스케줄 수정 시도: scheduleId={}", scheduleReq.getScheduleId());
                        ShowSchedule existingSchedule = showScheduleRepository.findById(scheduleReq.getScheduleId())
                            .orElseThrow(() -> {
                                log.error("스케줄을 찾을 수 없음: scheduleId={}, 공연 ID={}", scheduleReq.getScheduleId(), id);
                                return new RuntimeException("스케줄을 찾을 수 없습니다: " + scheduleReq.getScheduleId());
                            });

                        log.info("스케줄 조회 성공: scheduleId={}, 속한 공연 ID={}", 
                            existingSchedule.getId(), existingSchedule.getShow().getId());

                        // 해당 스케줄이 이 공연에 속하는지 확인
                        if (!existingSchedule.getShow().getId().equals(id)) {
                            log.error("스케줄이 다른 공연에 속함: scheduleId={}, 요청 공연 ID={}, 실제 공연 ID={}", 
                                scheduleReq.getScheduleId(), id, existingSchedule.getShow().getId());
                            throw new RuntimeException("스케줄이 이 공연에 속하지 않습니다. 스케줄 ID: " + scheduleReq.getScheduleId());
                        }

                        // ON_SALE 이후에는 예매된 좌석이 없는 경우에만 수정 가능
                        if (show.getStatus() != ShowStatus.WAITING) {
                            Long reservationCount = reservationRepository.countByScheduleId(existingSchedule.getId());
                            if (reservationCount > 0) {
                                throw new RuntimeException("예매된 좌석이 있는 스케줄은 수정할 수 없습니다. 스케줄 ID: " + existingSchedule.getId());
                            }
                        }

                        // 스케줄 정보 업데이트
                        LocalTime showTime = LocalTime.parse(scheduleReq.getShowTime(), DateTimeFormatter.ofPattern("HH:mm"));
                        existingSchedule.setShowDate(scheduleReq.getShowDate());
                        existingSchedule.setShowTime(showTime);
                        existingSchedule.setTicketOpenTime(scheduleReq.getTicketOpenTime());
                        showScheduleRepository.save(existingSchedule);
                    }
                }
            }

            // 3. 예매 종료일 재계산 (일정 변경 후)
            updateShowEndDates(show);
        }

        // updatedAt은 @PreUpdate로 자동 설정됨
        showRepository.save(show);

        return ShowUpdateResponse.builder()
            .showId(show.getId())
            .message("공연이 수정되었습니다")
            .build();
    }

    @Transactional
    public ShowDeleteResponse deleteShow(Long id) {
        Show show = showRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("공연을 찾을 수 없습니다"));

        // WAITING 상태의 공연만 삭제 가능
        if (show.getStatus() != ShowStatus.WAITING) {
            throw new RuntimeException("WAITING 상태의 공연만 삭제할 수 있습니다");
        }

        // 예매 확인: 예매가 있으면 삭제 불가
        // 예매가 있으면 reservations, reservation_seats도 함께 보존되어야 함
        List<ShowSchedule> schedules = showScheduleRepository.findByShowIdOrderByDateAndTime(id);
        if (!schedules.isEmpty()) {
            for (ShowSchedule schedule : schedules) {
                Long reservationCount = reservationRepository.countByScheduleId(schedule.getId());
                if (reservationCount > 0) {
                    throw new RuntimeException("예매가 있는 공연은 삭제할 수 없습니다. 스케줄 ID: " + schedule.getId());
                }
            }
        }

        // 예매가 없으면 물리 삭제 진행
        // 1. 연관된 스케줄 삭제
        if (!schedules.isEmpty()) {
            showScheduleRepository.deleteAll(schedules);
            log.info("연관된 스케줄 삭제 완료: {}개", schedules.size());
        }

        // 2. 연관된 좌석 가격 정보 삭제 (show_seat_grades)
        // show_seat_grades는 공연별 좌석 구역 가격 정보 (예: VIP석 10만원, R석 5만원)
        // 예매가 없으므로 삭제 가능
        List<ShowSeatGrade> seatGrades = showSeatGradeRepository.findByShowId(id);
        if (!seatGrades.isEmpty()) {
            showSeatGradeRepository.deleteAll(seatGrades);
            log.info("연관된 좌석 가격 정보 삭제 완료: {}개", seatGrades.size());
        }

        // 3. 공연 삭제 (물리 삭제)
        showRepository.delete(show);
        log.info("공연 물리 삭제 완료: showId={}", id);

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

    /**
     * 공연 일정 추가 (ON_SALE 이후에도 가능)
     * 일정 추가 시 공연의 종료일(endDate, saleEndDate) 자동 재계산
     */
    @Transactional
    public ScheduleAddResponse addSchedule(Long showId, ScheduleAddRequest request) {
        Show show = showRepository.findByIdAndNotDeleted(showId);
        if (show == null) {
            throw new RuntimeException("공연을 찾을 수 없습니다");
        }

        // 새 스케줄 추가
        LocalTime showTime = LocalTime.parse(request.getShowTime(), DateTimeFormatter.ofPattern("HH:mm"));
        ShowSchedule newSchedule = ShowSchedule.builder()
            .show(show)
            .showDate(request.getShowDate())
            .showTime(showTime)
            .ticketOpenTime(request.getTicketOpenTime())
            .status(ScheduleStatus.BEFORE_OPEN)
            .build();
        newSchedule = showScheduleRepository.save(newSchedule);

        // 공연 종료일 재계산 (일정 추가 시 자동 업데이트)
        updateShowEndDates(show);

        return ScheduleAddResponse.builder()
            .scheduleId(newSchedule.getId())
            .message("일정이 추가되었습니다. 예매 종료일이 자동으로 재계산되었습니다.")
            .build();
    }

    /**
     * 공연의 종료일(endDate, saleEndDate) 재계산
     * 모든 일정 중 가장 마지막 공연일을 기준으로 설정
     */
    private void updateShowEndDates(Show show) {
        List<ShowSchedule> allSchedules = showScheduleRepository.findByShowIdOrderByDateAndTime(show.getId());
        if (!allSchedules.isEmpty()) {
            LocalDate lastShowDate = allSchedules.stream()
                .map(ShowSchedule::getShowDate)
                .max(LocalDate::compareTo)
                .orElse(show.getEndDate());
            
            LocalDate firstShowDate = allSchedules.stream()
                .map(ShowSchedule::getShowDate)
                .min(LocalDate::compareTo)
                .orElse(show.getStartDate());

            show.setStartDate(firstShowDate);
            show.setEndDate(lastShowDate);
            show.setSaleEndDate(LocalDateTime.of(lastShowDate, LocalTime.of(23, 59, 59)));
            showRepository.save(show);
        }
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
}

