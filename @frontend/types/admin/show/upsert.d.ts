namespace ShowUpsert {
  interface Location {
    region: import('@shared/enums').Region;
    venueName: string;
    hallName: string;
  }

  interface Schedule {
    scheduleId?: number;
    showDate: string; // YYYY-MM-DD
    showTime: string; // HH:mm
    ticketOpenTime: string; // YYYY-MM-DDTHH:mm
  }

  interface CreateForm {
    title: string;
    genre: import('@shared/enums').Genre;
    location: Location;
    runningTime: string;
    cast: string;
    salePeriod: {
      startDate: string; // YYYY-MM-DD
      endDate: string; // YYYY-MM-DD
    };
    schedules: Schedule[];
  }

  type UpdateForm = CreateForm & { deletedScheduleIds: string[] };
}
