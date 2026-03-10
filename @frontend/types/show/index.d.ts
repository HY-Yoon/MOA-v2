namespace ShowCatalog {
  type Genre = import('@shared/enums').Genre;
  type Region = import('@shared/enums').Region;
  type ShowStatus = import('@shared/enums').ShowStatus;
  type SaleStatus = import('@shared/enums').SaleStatus;

  interface ListParams {
    page: number;
    size: number;
    genre?: Genre;
    region?: Region;
    keyword?: string;
    startDate?: string;
    endDate?: string;
    orderBy?: string;
    orderDirection?: 'asc' | 'desc';
  }

  interface Schedule {
    keyId: number;
    date: string;
    time:
      | string
      | {
          hour: number;
          minute: number;
          second: number;
          nano: number;
        };
    session: number;
  }

  interface SalePeriod {
    startDate: string;
    endDate: string;
  }

  interface List {
    id: number;
    title: string;
    genre: Genre;
    status: ShowStatus | string;
    saleStatus: SaleStatus | string;
    posterUrl: string;
    location: {
      region: Region | string;
      venue: string;
      hallName: string;
    };
    salePeriod: SalePeriod;
    createdAt: string;
    viewCount: number;
    startDate: string;
    endDate: string;
    schedules: Array<Schedule>;
  }

  interface Detail extends List {
    detailImageUrls: string[];
    runningTime: string;
    cast: string;
  }

  interface SeatGrades {
    sectionName: string;
    price: number;
    remainingSeats: number;
    totalSeats: number;
  }

  interface ScheduleByDate {
    keyId: number;
    date: string;
    time: string;
    isSoldOut: boolean;
    totalSeats: number;
    remainingSeats: number;
    seatGrades: SeatGrades[];
  }
}
