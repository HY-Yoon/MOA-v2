import { axiosInstance } from '@/lib/api-client';

const BASE_URL = '/api/v1/user/reservations' as const;

export interface ScheduleSeatMapCanvas {
  width: number;
  height: number;
  seatRadius: number;
  rowGap: number;
  columnGap: number;
}

export interface ScheduleSeatMapSection {
  sectionId: string;
  name: string;
  price: number;
  color: string;
}

export interface ScheduleSeatMapSeat {
  scheduleSeatId?: number;
  seatId: string;
  sectionId: string;
  row: string;
  number: number;
  x: number;
  y: number;
  status?: 'AVAILABLE' | 'RESERVED' | 'BLOCKED' | string;
}

export interface ScheduleSeatMapData {
  canvas: ScheduleSeatMapCanvas;
  sections: ScheduleSeatMapSection[];
  seats?: ScheduleSeatMapSeat[];
}

export interface ScheduleSeatStatusData {
  maxSelectable: number;
  seats: ScheduleSeatStatusSeat[];
}

export type ScheduleSeatStatusSeat =
  | {
      section: string;
      seatNumber: string;
    }
  | {
      scheduleSeatId?: number;
      seatId?: string;
      sectionId?: string;
      row?: string;
      number?: number;
      x?: number;
      y?: number;
      status?: string;
    };

export interface ConfirmSeatsRequest {
  scheduleId: number;
  scheduleSeatIds: number[];
}

export interface ConfirmSeatsConflictData {
  code: 'SEAT_CONFLICT';
  conflictSeatIds: string[];
}

export interface ConfirmSeatsQueueExpiredData {
  code: 'QUEUE_EXPIRED';
}

export interface ConfirmSeatsSuccessData {
  seatCount: number;
  remainingSeconds: number;
  totalAmount: number;
  message: string;
}

export interface ConfirmSeatsResponse {
  success: boolean;
  data: ConfirmSeatsConflictData | ConfirmSeatsQueueExpiredData | ConfirmSeatsSuccessData;
  message: string | null;
}

export const getScheduleSeatMap = (scheduleId: number) => ({
  queryKey: ['reservation', 'seatmap', scheduleId],
  queryFn: async (): Promise<ScheduleSeatMapData> => {
    const response = await axiosInstance.get<Api.Response<ScheduleSeatMapData>>(
      `/api/v1/schedules/${scheduleId}/seatmap`,
    );
    const payload = response?.data?.data;
    if (!payload) {
      throw new Error('좌석 배치도 응답 데이터가 없습니다.');
    }
    return payload;
  },
  enabled: !!scheduleId && scheduleId > 0,
});

export const getScheduleSeats = (scheduleId: number, refetchKey = 0) => {
  const headers: Record<string, string> = {
    'Cache-Control': 'no-store, no-cache, must-revalidate',
    Pragma: 'no-cache',
    Expires: '0',
  };
  return {
    queryKey: ['reservation', 'seats', scheduleId, refetchKey],
    queryFn: async (): Promise<ScheduleSeatStatusData> => {
      const response = await axiosInstance.get<Api.Response<ScheduleSeatStatusData>>(
        `/api/v1/schedules/${scheduleId}/seats`,
        {
          withCredentials: true,
          headers,
        },
      );
      const payload = response?.data?.data;
      if (!payload) {
        throw new Error('좌석 상태 응답 데이터가 없습니다.');
      }
      return payload;
    },
    enabled: !!scheduleId && scheduleId > 0,
    retry: false,
  };
};

export const confirmScheduleSeats = async (
  request: ConfirmSeatsRequest,
): Promise<ConfirmSeatsResponse> => {
  const response = await axiosInstance.post<ConfirmSeatsResponse>(
    '/api/v2/reservations/reserve',
    request,
    {
      withCredentials: true,
    },
  );

  if (!response?.data) {
    throw new Error('좌석 선점 응답 데이터가 없습니다.');
  }
  return response.data;
};


export const fetchReservationList = (params: Reservation.ListParams) => ({
  queryKey: ['user', 'reservations', 'list', params],
  queryFn: async () => {
    const response = await axiosInstance.get<Api.Response<Api.ListResponse<Reservation.List>>>(
      BASE_URL,
      { params },
    );
    return response?.data.data;
  },
});

export const fetchReservationDetail = (id: number) => ({
  queryKey: ['user', 'reservations', 'detail', id],
  queryFn: async () => {
    const response = await axiosInstance.get<Api.Response<Reservation.Detail>>(`${BASE_URL}/${id}`);
    return response?.data.data;
  },
  enabled: !!id && id > 0,
});

export const cancelReservation = () => ({
  mutationKey: ['user', 'reservations', 'cancel'],
  mutationFn: async (id: number) => await axiosInstance.delete(`${BASE_URL}/${id}`),
});
