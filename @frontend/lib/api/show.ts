import { axiosInstance } from '@/lib/api-client';

const BASE_URL = '/api/v1/shows' as const;

export const fetchShowCatalogList = async (params: ShowCatalog.ListParams) => {
  const response = await axiosInstance.get<Api.Response<Api.ListResponse<ShowCatalog.List>>>(BASE_URL, {
    params,
  });
  return response?.data.data;
};

export const getShowCatalogList = (params: ShowCatalog.ListParams) => ({
  queryKey: ['show', 'catalog', 'list', params],
  queryFn: () => fetchShowCatalogList(params),
});
