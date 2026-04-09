package com.moa2.api.show.service;

import com.moa2.api.schedule.service.ScheduleSeatInitService;
import com.moa2.api.show.dto.*;
import com.moa2.api.show.domain.entity.*;
import com.moa2.api.show.domain.repository.*;
import com.moa2.api.reservation.domain.repository.ReservationRepository;
import com.moa2.global.model.*;
import com.moa2.global.model.ScheduleStatus;
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
    private final ScheduleSeatRepository scheduleSeatRepository;
    private final ShowSeatGradeRepository showSeatGradeRepository;
    private final VenueSeatSectionRepository venueSeatSectionRepository;
    private final VenueRepository venueRepository;
    private final ReservationRepository reservationRepository;
    private final DetailImageRepository detailImageRepository;
    private final FileService fileService;
    private final ScheduleSeatInitService scheduleSeatInitService;

    public Page<ShowDto.AdminListResponse> getShowList(ShowDto.AdminListRequest request, Pageable pageable) {
        // keyword가 있으면 검색 패턴 생성 (null이거나 빈 문자열이면 null)
        // 대소문자 구분 없이 검색하기 위해 패턴만 생성 (LIKE는 기본적으로 대소문자 구분)
        String keywordPattern = null;
        if (request.keyword() != null && !request.keyword().trim().isEmpty()) {
            keywordPattern = "%" + request.keyword().trim().toLowerCase() + "%";
        }

        LocalDate startDate = request.startDate();
        LocalDate endDate = request.endDate();

        Page<Show> shows = showRepository.findShowsWithFilters(
                request.showStatus(),
                request.saleStatus(),
                request.genre(), // 장르 필터 추가
                keywordPattern,
                startDate, // null이면 쿼리에서 IS NULL 체크
                endDate, // null이면 쿼리에서 IS NULL 체크
                pageable);

        // N+1 방지: 조회된 공연들의 모든 스케줄을 한 번에 조회
        List<Long> showIds = shows.getContent().stream()
                .map(Show::getId)
                .collect(Collectors.toList());

        List<ShowSchedule> allSchedules = showScheduleRepository.findAllByShowIdInOrderByDateAndTime(showIds);

        // 스케줄을 공연 ID별로 그룹화
        Map<Long, List<ShowSchedule>> schedulesByShowId = allSchedules.stream()
                .collect(Collectors.groupingBy(s -> s.getShow().getId()));

        List<ShowDto.AdminListResponse> content = shows.getContent().stream()
                .map(show -> ShowDto.fromAdmin(show, schedulesByShowId.getOrDefault(show.getId(), List.of())))
                .collect(Collectors.toList());

        return new PageImpl<>(content, pageable, shows.getTotalElements());
    }

    public ShowDto.AdminDetailResponse getShowDetail(Long id) {
        Show show = showRepository.findByIdAndNotDeleted(id);
        if (show == null) {
            throw new RuntimeException("공연을 찾을 수 없습니다");
        }

        List<ShowSchedule> schedules = showScheduleRepository.findByShowIdOrderByDateAndTime(id);
        List<ShowSeatGrade> seatGrades = showSeatGradeRepository.findByShowId(id);

        List<Long> scheduleIds = schedules.stream()
                .map(ShowSchedule::getId)
                .collect(Collectors.toList());

        // schedule_seat 상태 기준 집계 (정확한 잔여석 반영)
        List<Object[]> seatStats = scheduleSeatRepository.countTotalAndRemainingSeatsByScheduleIds(scheduleIds);
        Map<Long, int[]> seatStatsMap = seatStats.stream()
                .collect(Collectors.toMap(
                        row -> (Long) row[0],
                        row -> new int[]{ ((Number) row[1]).intValue(),
                                row[2] == null ? 0 : ((Number) row[2]).intValue() }));

        // 예약 건수 (표시용)
        List<Object[]> reservationStats = reservationRepository.countReservationsByScheduleIds(scheduleIds);
        Map<Long, Long> reservationCounts = reservationStats.stream()
                .collect(Collectors.toMap(
                        row -> (Long) row[0],
                        row -> (Long) row[1]));

        return ShowDto.ofAdminDetail(show, schedules, seatGrades, seatStatsMap, reservationCounts);
    }

    @Transactional
    public ShowDto.CreateResponse createShow(ShowDto.CreateRequest request, MultipartFile poster,
            List<MultipartFile> detailImages) {
        // 파일 업로드 처리
        String posterUrl = uploadPoster(poster);

        // location 정보로 Venue 조회, 없으면 자동 생성
        Venue venue = getOrCreateVenue(request.location());

        // 마지막 공연일, 첫 공연 시간, 종료일 계산
        List<ShowDto.CreateRequest.ScheduleRequest> scheduleRequests = request.schedules();
        LocalDate lastShowDate = getMaxDate(scheduleRequests);
        LocalDate firstShowDate = getMinDate(scheduleRequests);
        LocalTime firstShowTime = getFirstShowTime(scheduleRequests, firstShowDate);

        // Show 생성
        Show show = Show.builder()
                .venue(venue)
                .title(request.title())
                .genre(Genre.valueOf(request.genre()))
                .runningTime(request.runningTime())
                .cast(request.cast())
                .posterUrl(posterUrl)
                .startDate(firstShowDate)
                .endDate(lastShowDate)
                .startTime(firstShowTime)
                .saleStartDate(request.salePeriod().startDate())
                .saleEndDate(request.salePeriod().endDate())
                .status(ShowStatus.WAITING)
                .saleStatus(SaleStatus.SUSPENDED)
                .viewCount(0L)
                .build();

        showRepository.save(show);

        // 상세 이미지 업로드 및 저장
        uploadAndSaveDetailImages(detailImages, show);

        // 좌석 구역별 가격 정보 저장
        createAndSaveSeatGrades(show, venue);

        // 스케줄 생성 및 저장 (schedule_seats 선생성을 위해 seatGrade 생성 이후 실행)
        createAndSaveSchedules(scheduleRequests, show);

        return new ShowDto.CreateResponse(show.getId(), "공연이 등록되었습니다");
    }

    @Transactional
    public ShowDto.UpdateResponse updateShow(Long id, ShowDto.UpdateRequest request, MultipartFile poster,
            List<MultipartFile> detailImages) {
        Show show = showRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("공연을 찾을 수 없습니다"));

        // 기본 정보 업데이트 (title, runningTime, cast)
        // Assuming Show entity has update methods for these fields
        if (request.title() != null)
            show.setTitle(request.title());
        if (request.runningTime() != null)
            show.setRunningTime(request.runningTime());
        if (request.cast() != null)
            show.setCast(request.cast());

        // 포스터 이미지 수정
        if (poster != null && !poster.isEmpty()) {
            // 기존 포스터 삭제
            if (show.getPosterUrl() != null) {
                fileService.deleteFile(show.getPosterUrl());
            }
            String newPosterUrl = fileService.uploadFile(poster, "posters");
            show.setPosterUrl(newPosterUrl);
        }

        // 상태가 WAITING인 경우에만 수정 가능한 정보 업데이트
        if (show.getStatus() == ShowStatus.WAITING) {
            Venue venue = null;
            if (request.location() != null) {
                venue = getOrCreateVenue(request.location());
            }

            Genre genre = request.genre() != null ? Genre.valueOf(request.genre()) : null;

            LocalDateTime saleStartDate = null;
            LocalDateTime saleEndDate = null;
            if (request.salePeriod() != null) {
                saleStartDate = request.salePeriod().startDate();
                saleEndDate = request.salePeriod().endDate();
            }

            // Assuming Show entity has updateWaitStatusFields method
            if (genre != null)
                show.setGenre(genre);
            if (venue != null)
                show.setVenue(venue);
            if (saleStartDate != null)
                show.setSaleStartDate(saleStartDate);
            if (saleEndDate != null)
                show.setSaleEndDate(saleEndDate);

            // 좌석 가격 정보 재생성 (WAITING 상태일 때만 가능하다고 가정)
            // 기존 가격 정보 삭제 후 재생성
            if (request.location() != null) { // 장소가 바뀌면 좌석도 바뀔 수 있으므로
                showSeatGradeRepository.deleteAll(show.getShowSeatGrades());
                createAndSaveSeatGrades(show, show.getVenue());
                rebuildScheduleSeatsForShow(show);
            }
        }

        // 상세 이미지 수정 (기존 이미지 삭제 후 추가)
        // Deleted detail images handling
        if (request.deletedDetailImageIds() != null && !request.deletedDetailImageIds().isEmpty()) {
            for (Long imageIdToDelete : request.deletedDetailImageIds()) {
                DetailImage imageToDelete = detailImageRepository.findById(imageIdToDelete)
                        .orElseThrow(() -> new RuntimeException("삭제할 이미지를 찾을 수 없습니다: " + imageIdToDelete));
                if (!imageToDelete.getShow().getId().equals(id)) {
                    throw new RuntimeException("이미지가 이 공연에 속하지 않습니다. 이미지 ID: " + imageIdToDelete);
                }
                fileService.deleteFile(imageToDelete.getUrl());
                detailImageRepository.delete(imageToDelete);
            }
        }

        // New detail images handling
        if (detailImages != null && !detailImages.isEmpty()) {
            // Find max display order for existing images
            List<DetailImage> existingImages = detailImageRepository.findByShowIdOrderByDisplayOrderAsc(id);
            int maxOrder = existingImages.stream()
                    .mapToInt(img -> img.getDisplayOrder() != null ? img.getDisplayOrder() : -1)
                    .max()
                    .orElse(-1);

            String[] newDetailImageUrls = fileService.uploadFiles(detailImages, "details");
            for (int i = 0; i < newDetailImageUrls.length; i++) {
                DetailImage newDetailImage = DetailImage.builder()
                        .show(show)
                        .url(newDetailImageUrls[i])
                        .displayOrder(maxOrder + 1 + i)
                        .build();
                detailImageRepository.save(newDetailImage);
            }
        }

        // 스케줄 수정 (기존 메서드 로직 유지하되 간소화)
        updateSchedules(show, request.schedules());

        showRepository.save(show);

        return new ShowDto.UpdateResponse(show.getId(), "공연이 수정되었습니다");
    }

    @Transactional
    public ShowDto.DeleteResponse deleteShow(Long id) {
        Show show = showRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("공연을 찾을 수 없습니다"));

        // WAITING 상태의 공연만 삭제 가능
        if (show.getStatus() != ShowStatus.WAITING) {
            throw new RuntimeException("WAITING 상태의 공연만 삭제할 수 있습니다");
        }

        // 예매 확인: 예매가 있으면 삭제 불가
        List<ShowSchedule> schedules = showScheduleRepository.findByShowIdOrderByDateAndTime(id);
        if (!schedules.isEmpty()) {
            List<Long> scheduleIds = schedules.stream().map(ShowSchedule::getId).collect(Collectors.toList());
            List<Object[]> reservationStats = reservationRepository.countReservationsByScheduleIds(scheduleIds);
            for (Object[] row : reservationStats) {
                Long count = (Long) row[1];
                if (count > 0) {
                    throw new RuntimeException("예매가 있는 공연은 삭제할 수 없습니다.");
                }
            }
        }

        // 예매가 없으면 물리 삭제 진행
        if (!schedules.isEmpty()) {
            scheduleSeatRepository.deleteByScheduleIdIn(schedules.stream().map(ShowSchedule::getId).toList());
            showScheduleRepository.deleteAll(schedules);
        }

        // 좌석 라벨 삭제
        List<ShowSeatGrade> seatGrades = showSeatGradeRepository.findByShowId(id);
        if (!seatGrades.isEmpty()) {
            showSeatGradeRepository.deleteAll(seatGrades);
        }

        // 이미지 삭제
        List<DetailImage> detailImages = show.getDetailImages();
        if (detailImages != null) {
            for (DetailImage detailImage : detailImages) {
                fileService.deleteFile(detailImage.getUrl());
            }
        }

        if (show.getPosterUrl() != null) {
            fileService.deleteFile(show.getPosterUrl());
        }

        showRepository.delete(show);

        return new ShowDto.DeleteResponse("공연이 삭제되었습니다");
    }

    @Transactional
    public ShowDto.SaleStatusUpdateResponse updateSaleStatus(Long id, ShowDto.SaleStatusUpdateRequest request) {
        Show show = showRepository.findByIdAndNotDeleted(id);
        if (show == null) {
            throw new RuntimeException("공연을 찾을 수 없습니다");
        }

        show.setSaleStatus(request.saleStatus());
        // updatedAt은 @PreUpdate로 자동 설정됨
        showRepository.save(show);

        return new ShowDto.SaleStatusUpdateResponse(show.getId(), show.getSaleStatus(), "판매 상태가 변경되었습니다");
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

            // 가장 빠른 공연일
            LocalDate firstShowDate = allSchedules.stream()
                    .map(ShowSchedule::getShowDate)
                    .min(LocalDate::compareTo)
                    .orElse(show.getStartDate());

            // 가장 빠른 공연일의 가장 빠른 시간 계산
            LocalTime firstShowTime = allSchedules.stream()
                    .filter(s -> s.getShowDate().equals(firstShowDate))
                    .map(ShowSchedule::getShowTime)
                    .min(LocalTime::compareTo)
                    .orElse(null);

            show.setStartDate(firstShowDate);
            show.setStartTime(firstShowTime); // startTime 캐싱
            show.setEndDate(lastShowDate);
            // saleEndDate는 비즈니스 로직에 따라 다를 수 있으므로 여기서는 자동 업데이트 하지 않음 (기획 확인 필요)
            // show.setSaleEndDate(LocalDateTime.of(lastShowDate, LocalTime.of(23, 59,
            // 59)));
            showRepository.save(show);
        }
    }

    // --- Private Helper Methods ---

    private String uploadPoster(MultipartFile poster) {
        if (poster != null && !poster.isEmpty()) {
            return fileService.uploadFile(poster, "posters");
        } else {
            throw new IllegalArgumentException("포스터 이미지는 필수입니다");
        }
    }

    private Venue getOrCreateVenue(ShowDto.CreateRequest.LocationRequest location) {
        return venueRepository.findByNameAndHallNameAndRegion(
                location.venueName(),
                location.hallName(),
                location.region()).orElseGet(() -> {
                    Venue newVenue = Venue.builder()
                            .name(location.venueName())
                            .hallName(location.hallName())
                            .region(location.region())
                            .build();
                    return venueRepository.save(newVenue);
                });
    }

    private Venue getOrCreateVenue(ShowDto.UpdateRequest.LocationRequest location) {
        return venueRepository.findByNameAndHallNameAndRegion(
                location.venueName(),
                location.hallName(),
                location.region()).orElseGet(() -> {
                    Venue newVenue = Venue.builder()
                            .name(location.venueName())
                            .hallName(location.hallName())
                            .region(location.region())
                            .build();
                    return venueRepository.save(newVenue);
                });
    }

    private LocalDate getMaxDate(List<ShowDto.CreateRequest.ScheduleRequest> schedules) {
        return schedules.stream()
                .map(ShowDto.CreateRequest.ScheduleRequest::showDate)
                .max(LocalDate::compareTo)
                .orElse(LocalDate.now()); // Fallback
    }

    private LocalDate getMinDate(List<ShowDto.CreateRequest.ScheduleRequest> schedules) {
        return schedules.stream()
                .map(ShowDto.CreateRequest.ScheduleRequest::showDate)
                .min(LocalDate::compareTo)
                .orElse(LocalDate.now());
    }

    private LocalTime getFirstShowTime(List<ShowDto.CreateRequest.ScheduleRequest> schedules, LocalDate firstDate) {
        return schedules.stream()
                .filter(s -> s.showDate().equals(firstDate))
                .map(s -> LocalTime.parse(s.showTime(), DateTimeFormatter.ofPattern("HH:mm")))
                .min(LocalTime::compareTo)
                .orElse(null);
    }

    private void uploadAndSaveDetailImages(List<MultipartFile> detailImages, Show show) {
        if (detailImages != null && !detailImages.isEmpty()) {
            for (int i = 0; i < detailImages.size(); i++) {
                MultipartFile detailImage = detailImages.get(i);
                String detailImageUrl = fileService.uploadFile(detailImage, "details");
                DetailImage image = DetailImage.builder()
                        .show(show)
                        .url(detailImageUrl)
                        .displayOrder(i) // 순서 지정
                        .build();
                detailImageRepository.save(image);
            }
        }
    }

    private void createAndSaveSchedules(List<ShowDto.CreateRequest.ScheduleRequest> scheduleRequests, Show show) {
        for (ShowDto.CreateRequest.ScheduleRequest scheduleReq : scheduleRequests) {
            LocalTime showTime = LocalTime.parse(scheduleReq.showTime(), DateTimeFormatter.ofPattern("HH:mm"));
            ShowSchedule schedule = ShowSchedule.builder()
                    .show(show)
                    .showDate(scheduleReq.showDate())
                    .showTime(showTime)
                    .ticketOpenTime(scheduleReq.ticketOpenTime())
                    .status(ScheduleStatus.BEFORE_OPEN)
                    .build();
            showScheduleRepository.save(schedule);
        }
    }

    private void createAndSaveSeatGrades(Show show, Venue venue) {
        List<VenueSeatSection> sections = venueSeatSectionRepository.findByVenueId(venue.getId());
        for (VenueSeatSection section : sections) {
            String sectionName = section.getName();
            int price = 0;
            // 간단한 가격 책정 예시
            if (sectionName.toLowerCase().contains("vip"))
                price = 150000;
            else if (sectionName.toLowerCase().contains("r"))
                price = 120000;
            else if (sectionName.toLowerCase().contains("s"))
                price = 90000;
            else
                price = 60000;

            ShowSeatGrade seatGrade = ShowSeatGrade.builder()
                    .show(show)
                    .section(section)
                    .price(price)
                    .build();
            showSeatGradeRepository.save(seatGrade);
        }
    }

    private void rebuildScheduleSeatsForShow(Show show) {
        List<ShowSchedule> schedules = showScheduleRepository.findByShowIdOrderByDateAndTime(show.getId());
        for (ShowSchedule schedule : schedules) {
            scheduleSeatInitService.rebuildScheduleSeats(schedule);
        }
    }

    private void updateSchedules(Show show, List<ShowDto.UpdateRequest.ScheduleUpdateRequest> scheduleRequests) {
        if (scheduleRequests == null || scheduleRequests.isEmpty())
            return;

        // 기존 스케줄 조회
        List<ShowSchedule> existingSchedules = showScheduleRepository.findByShowIdOrderByDateAndTime(show.getId());

        // 삭제 처리
        List<Long> incomeIds = scheduleRequests.stream()
                .map(ShowDto.UpdateRequest.ScheduleUpdateRequest::scheduleId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        for (ShowSchedule existing : existingSchedules) {
            if (!incomeIds.contains(existing.getId())) {
                // 예매 확인
                Long reservationCount = reservationRepository.countByScheduleId(existing.getId());
                if (reservationCount > 0) {
                    throw new RuntimeException("예매가 있는 스케줄은 삭제할 수 없습니다 id: " + existing.getId());
                }
                scheduleSeatRepository.deleteByScheduleId(existing.getId());
                showScheduleRepository.delete(existing);
            }
        }

        // 추가 및 수정 처리
        for (ShowDto.UpdateRequest.ScheduleUpdateRequest req : scheduleRequests) {
            if (req.scheduleId() == null) { // 추가
                LocalTime showTime = LocalTime.parse(req.showTime(), DateTimeFormatter.ofPattern("HH:mm"));
                ShowSchedule newSchedule = ShowSchedule.builder()
                        .show(show)
                        .showDate(req.showDate())
                        .showTime(showTime)
                        .ticketOpenTime(req.ticketOpenTime())
                        .status(ScheduleStatus.BEFORE_OPEN)
                        .build();
                showScheduleRepository.save(newSchedule);
                scheduleSeatInitService.rebuildScheduleSeats(newSchedule);
            } else { // 수정
                ShowSchedule existing = existingSchedules.stream().filter(s -> s.getId().equals(req.scheduleId()))
                        .findFirst().orElseThrow();
                // 예매 있으면 수정 제한 로직 (기존과 동일하게)
                if (show.getStatus() != ShowStatus.WAITING) {
                    Long reservationCount = reservationRepository.countByScheduleId(existing.getId());
                    if (reservationCount > 0)
                        throw new RuntimeException("예매있는 스케줄 수정 불가 id:" + existing.getId());
                }

                LocalTime showTime = LocalTime.parse(req.showTime(), DateTimeFormatter.ofPattern("HH:mm"));
                existing.setShowDate(req.showDate());
                existing.setShowTime(showTime);
                existing.setTicketOpenTime(req.ticketOpenTime());
                showScheduleRepository.save(existing);
                scheduleSeatInitService.rebuildScheduleSeats(existing);
            }
        }

        // 종료일 업데이트 (필요시)
        updateShowEndDates(show);
    }
}
