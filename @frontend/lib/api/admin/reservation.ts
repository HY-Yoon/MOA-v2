import { axiosInstance } from '@/lib/api-client';

const BASE_URL = '/api/v1/admin/reservations';

// 공연(showId) 예매 목록 조회
export const fetchShowReservationList = (
  params: Reservation.ListParams & Record<string, string | number | undefined>,
) => ({
  queryKey: ['admin', 'reservations', 'list', params],
  queryFn: async () => {
    const response = await axiosInstance.get<Api.Response<Api.ListResponse<Reservation.List>>>(
      BASE_URL,
      { params },
    );
    return response?.data.data;
  },
});

// 예매 상세 조회
export const fetchShowReservationDetail = (reservationId: number) => ({
  queryKey: ['admin', 'reservations', 'detail', reservationId],
  queryFn: async () => {
    const response = await axiosInstance.get<Api.Response<{ content: Reservation.Detail[] }>>(
      `/api/v1/admin/reservations/detail?reservationId=${reservationId}`,
    );
    return response?.data?.data?.content[0];
  },
  enabled: !!reservationId && reservationId > 0,
});
