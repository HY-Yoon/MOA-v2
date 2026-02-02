import { axiosInstance } from '@/lib/api-client';
import { UserStatus } from '@shared/enums';

const BASE_URL = '/api/v1/admin/users' as const;

// 회원 목록 조회
export const getUserList = (params: User.ListParams) => ({
  queryKey: ['admin', 'user', 'list', params],
  queryFn: async () => {
    const response = await axiosInstance.get(BASE_URL, { params });
    return response?.data.data;
  },
});

// 회원 상태 설정
type ChangeParams = { id: number; status: UserStatus; reason?: string };
export const changeUserStatus = () => ({
  mutationKey: ['admin', 'user', 'userStatus'],
  mutationFn: async ({ id, status, reason }: ChangeParams) => {
    const response = await axiosInstance.put(`${BASE_URL}/${id}/status`, { status, reason });
    return response?.data;
  },
});
