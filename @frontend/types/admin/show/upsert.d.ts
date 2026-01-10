import { Genre, Region } from '@shared/enums';

declare global {
  namespace ShowUpsertType {
    interface Location {
      region: Region;
      venueName: string;
      hallName: string;
    }

    interface Schedule {
      showDate: Date;
      showTime: string;
      ticketOpenTime: Date; // YYYY-MM-DD HH:mm (시간 포함)
    }

    interface ShowUpsertForm {
      title: string;
      genre: Genre;
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
}
