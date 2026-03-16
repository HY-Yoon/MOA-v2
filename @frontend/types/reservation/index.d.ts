namespace Reservation {
  type ReservationStatus = import('@shared/enums').ReservationStatus;
  type PaymentStatus = import('@shared/enums').PaymentStatus;

  type DateType = 'RESERVATION' | 'SHOW';
  interface ListParams {
    page: number;
    size: number;
    startDate?: string;
    endDate?: string;
    dataType?: DateType;
  }

  interface Show {
    showId: number;
    title: string;
    posterUrl: string;
    genre: string;
    runningTime: string;
    cast: string;
  }

  interface Schedule {
    scheduleId: number;
    showDate: string;
    showTime: string;
    location: {
      region: string;
      venue: string;
      hallName: string;
      address: string;
    };
  }

  interface List {
    reservationId: number;
    reservationNumber: string;
    reservationDate: string;
    reservationStatus: ReservationStatus;
    paymentStatus: PaymentStatus;
    show: Show;
    schedule: Schedule;
    seatCount: number;
    totalAmount: number;
    canCancel: boolean;
    cancellationDeadline: string | null;
  }
}
