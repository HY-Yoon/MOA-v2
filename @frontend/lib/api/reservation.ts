import { axiosInstance } from '@/lib/api-client';

const BASE_URL = '/api/v1/user/reservations' as const;

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
