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
    time: {
      hour: number;
      minute: number;
      second: number;
      nano: number;
    };
    session: number;
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
    salePeriod: {
      startDate: string;
      endDate: string;
    };
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
}
