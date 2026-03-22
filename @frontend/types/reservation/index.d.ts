namespace Reservation {
  type ReservationStatus = import('@shared/enums').ReservationStatus;
  type PaymentStatus = import('@shared/enums').PaymentStatus;
  type Genre = import('@shared/enums').Genre;
  type Region = import('@shared/enums').Region;

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
    genre: Genre;
    runningTime: string;
    cast: string;
  }

  interface Schedule {
    scheduleId: number;
    showDate: string;
    showTime: string;
    location: {
      region: Region;
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

  interface Seats {
    sectionName: string;
    row: string;
    number: number;
    price: number;
  }

  interface Booker {
    name: string;
    phone: string;
    email: string;
  }

  interface Payment {
    orderId: string;
    paymentKey: string;
    totalAmount: number;
    paymentMethod: string;
    paymentStatus: PaymentStatus;
    paidAt: string;
  }

  interface Detail extends List {
    seats: Seats[];
    booker: Booker;
    payment: Payment;
    canCancel: boolean;
    cancelledAt: string | null;
  }
}
