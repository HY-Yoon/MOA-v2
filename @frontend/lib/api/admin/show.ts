import { axiosInstance } from '@/lib/api-client';
import { UseMutationOptions } from '@tanstack/react-query';

const BASE_URL = '/api/v1/admin/shows' as const;

// 공연 상세 조회
export const getShow = (id: number) => ({
  queryKey: ['admin', 'show', 'detail', id],
  queryFn: async () => {
    const response = await axiosInstance.get(`${BASE_URL}/${id}`);
    return response?.data.data;
  },
  enabled: !!id && id > 0,
});

// 공연 신규 등록
type CreateResponse = { showId: number; message: string };
export const createShow = (): UseMutationOptions<
  Api.Response<CreateResponse>,
  Error,
  FormData
> => ({
  mutationKey: ['admin', 'show', 'create'],
  mutationFn: async (requestBody: FormData) => {
    const response = await axiosInstance.post(BASE_URL, requestBody, {
      headers: { 'Content-Type': 'multipart/form-data' },
    });
    return response?.data;
  },
});
