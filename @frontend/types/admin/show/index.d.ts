namespace Show {
  type Genre = import('@shared/enums').Genre;
  type Region = import('@shared/enums').Region;
  type ShowStatus = import('@shared/enums').ShowStatus;
  type SaleStatus = import('@shared/enums').SaleStatus;

  // 공연 목록 파라미터
  interface ListParams {
    page: number; // 현재 페이지
    size: number; // 페이지당 개수 (10개, 20개...)
    sort?: string; // 정렬 기준 {컬럼명}, {기준} (ex. 'id, asc')
    showStatus?: ShowStatus; // 공연 상태 필터
    saleStatus?: SaleStatus; // 판매 상태 필터
    genre?: Genre; // 장르 필터
    startDate?: string; // YYYY-MM-DD
    endDate?: string; // YYYY-MM-DD
    keyword?: string; // 검색어
  }

  // 공연 목록 스케줄
  interface SchedulesList {
    keyId: number;
    date: string; // YYYY-MM-DD
    time: string; // HH:mm
    session: number;
  }

  // 공연 목록
  interface List {
    id: number;
    title: string;
    genre: Genre;
    status: ShowStatus;
    saleStatus: SaleStatus;
    posterUrl: string;
    location: {
      region: Region;
      venue: string;
      hallName: string;
    };
    salePeriod: {
      startDate: string; // YYYY-MM-DDTHH:mm
      endDate: string; // YYYY-MM-DDTHH:mm
    };
    schedules: SchedulesList[];
  }

  // 공연 상세
  interface Schedules {
    scheduleId: number;
    showDate: string;
    showTime: string;
    ticketOpenTime: string;
    remainingSeats: number;
    totalSeats: number;
    reservationCount: number;
  }

  interface SeatPrices {
    sectionId: string;
    sectionName: string;
    price: number;
  }

  interface DetailImages {
    id: number;
    url: string;
  }

  interface Detail {
    id: number;
    title: string;
    genre: Genre;
    venueName: string;
    hallName: string;
    region: Region;
    runningTime: string;
    posterUrl: string;
    detailImageUrls: string[];
    cast: string;
    status: ShowStatus;
    saleStatus: SaleStatus;
    saleStartDate: string;
    saleEndDate: string;
    schedules: Schedules[];
    seatPrices: SeatPrices[];
    detailImages: DetailImages[];
    createdAt: string;
    updatedAt: string;
  }
}
