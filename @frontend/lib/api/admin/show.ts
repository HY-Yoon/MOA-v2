import { axiosInstance } from '@/lib/api-client';
import { UseQueryOptions } from '@tanstack/react-query';

// 공연 상세 조회
export const getShow = (id: number): UseQueryOptions<Show.DetailResponse> => ({
  queryKey: ['admin', 'show', 'detail', id],
  queryFn: async () => {
    const response = await axiosInstance.get(`/api/v1/admin/shows/${id}`);
    return response?.data.data;
  },
  enabled: !!id && id > 0,
});
