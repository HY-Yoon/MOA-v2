import { Genre, Region } from '@shared/enums';

declare global {
  namespace ShowUpsertType {
    interface ScheduleItem {
      showDate: string;
      showTime: string;
      ticketOpenTime: string;
    }

    interface ShowUpsertForm {
      title: string;
      genre: Genre;
      location: {
        region: Region;
        venueName: string;
        hallName: string;
      };
      runningTime: string;
      cast: string;
      bookingPeriod: {
        startDate: Date;
        endDate: Date;
      };
      schedules: ScheduleItem[];
    }
  }
}
