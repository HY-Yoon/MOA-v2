namespace ShowUpsert {
  interface Location {
    region: import('@shared/enums').Region;
    venueName: string;
    hallName: string;
  }

  interface Schedule {
    scheduleId?: number;
    showDate: Date;
    showTime: string;
    ticketOpenTime: Date;
  }

  interface CreateForm {
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

  type UpdateForm = CreateForm & { deletedScheduleIds: string[] };
}
