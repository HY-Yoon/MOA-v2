namespace ShowUpsertType {
  interface Location {
    region: import('@shared/enums').Region;
    venueName: string;
    hallName: string;
  }

  interface Schedule {
    showDate: Date;
    showTime: string;
    ticketOpenTime: Date;
  }

  interface ShowForm {
    title: string;
    genre: import('@shared/enums').Genre;
    location: Location;
    runningTime: string;
    cast: string;
    salePeriod: {
      startDate: Date;
      endDate: Date;
    };
    schedules: Schedule[];
  }
}
