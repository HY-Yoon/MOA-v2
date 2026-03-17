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
