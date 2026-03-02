import { axiosInstance } from '@/lib/api-client';

const BASE_URL = '/api/v1/shows' as const;

export const getShowCatalogList = (params: ShowCatalog.ListParams) => ({
  queryKey: ['show', 'catalog', 'list', params],
  queryFn: async () => {
    const response = await axiosInstance.get<Api.Response<Api.ListResponse<ShowCatalog.List>>>(BASE_URL, {
      params,
    });
    return response?.data.data;
  },
});
