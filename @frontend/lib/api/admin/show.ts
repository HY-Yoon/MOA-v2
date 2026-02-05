import { axiosInstance } from '@/lib/api-client';
import { UseMutationOptions } from '@tanstack/react-query';

type ShowUpsert = { showId?: number; message?: string };

const BASE_URL = '/api/v1/admin/shows' as const;

// 공연 목록 조회
export const getShowList = (params: Show.ListParams) => ({
  queryKey: ['admin', 'show', 'list', params],
  queryFn: async () => {
    const response = await axiosInstance.get(BASE_URL, { params });
    return response?.data.data;
  },
});

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
export const createShow = (): UseMutationOptions<Api.Response<ShowUpsert>, Error, FormData> => ({
  mutationKey: ['admin', 'show', 'create'],
  mutationFn: async (requestBody: FormData) => {
    const response = await axiosInstance.post(BASE_URL, requestBody, {
      headers: { 'Content-Type': 'multipart/form-data' },
    });
    return response?.data;
  },
});

// 공연 수정
export const updateShow = (
  id: number,
): UseMutationOptions<Api.Response<ShowUpsert>, Error, FormData> => ({
  mutationKey: ['admin', 'show', 'update', id],
  mutationFn: async (requestBody: FormData) => {
    const response = await axiosInstance.patch(`${BASE_URL}/${id}`, requestBody, {
      headers: { 'Content-Type': 'multipart/form-data' },
    });
    return response?.data;
  },
});

// 공연 판매 설정 (ALLOWED ↔ SUSPENDED)
type ChangeParams = { id: number; saleStatus: Show.SaleStatus };
export const changeSaleStatus = () => ({
  mutationKey: ['admin', 'show', 'saleStatus'],
  mutationFn: async ({ id, saleStatus }: ChangeParams) => {
    const response = await axiosInstance.patch(`${BASE_URL}/${id}/sale-status`, { saleStatus });
    return response?.data;
  },
});

// 공연 삭제
export const deleteShow = () => ({
  mutationKey: ['admin', 'show', 'delete'],
  mutationFn: async (id: number) => await axiosInstance.delete(`${BASE_URL}/${id}`),
});
