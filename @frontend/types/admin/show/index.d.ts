namespace Show {
  type Genre = import('@shared/enums').Genre;
  type Region = import('@shared/enums').Region;
  type ShowStatus = import('@shared/enums').ShowStatus;
  type SaleStatus = import('@shared/enums').SaleStatus;

  // 공연 목록
  // interface List {
  //   id: number;
  //   title: string;
  //   genre: Genre;
  //   region: Region;
  //   venueName: string;
  //   startDate: string;
  //   endDate: string;
  //   thumbnailUrl?: string;
  // }

  // interface ListResponse {
  //   data: ShowListItem[];
  //   totalCount: number;
  //   message: string;
  // }

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
    seatPrices: [];
    createdAt: string;
    updatedAt: string;
  }
}
