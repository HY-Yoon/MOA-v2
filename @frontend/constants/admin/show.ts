export const SHOW_FORM_FIELDS = {
  TITLE: 'title', // 제목
  GENRE: 'genre', // 장르
  REGION: 'region', // 지역
  LOCATION: 'location', // 위치
  VENUE_NAME: 'venueName', // 장소
  HALL_NAME: 'hallName', // 공연장
  RUNNING_TIME: 'runningTime', // 관람 시간
  CAST: 'cast', // 출연진
  SALE_PERIOD: 'salePeriod', // 예매 일정
  START_DATE: 'startDate', // 예매 시작일
  END_DATE: 'endDate', // 예매 종료일
  SCHEDULES: 'schedules', // 공연 일정
  SCHEDULE_ID: 'scheduleId', // 공연 일정 아이디
  SHOW_DATE: 'showDate', // 공연일
  SHOW_TIME: 'showTime', // 공연 시간
  TICKET_OPEN_TIME: 'ticketOpenTime', // 티켓 오픈 시간
  RESERVATION_COUNT: 'reservationCount', // 예매된 좌석수
} as const;

export const ERROR_MESSAGES = {
  NOT_VALID_FORMAT: '올바른 형식이 아닙니다.',
  START_DATE_EARLY: '예매 시작일은 첫 번째 공연일보다 이전이어야 합니다.',
} as const;
