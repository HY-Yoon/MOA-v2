import { axiosInstance } from '@/lib/api-client';

const BASE_URL = '/api/auth' as const;

// 회원 정보 조회
export const getUserInfo = () => ({
  queryKey: ['auth', 'user'],
  queryFn: async () => {
    const response = await axiosInstance.get(`${BASE_URL}/user`, { withCredentials: true });
    return response?.data.data;
  },
});

// 로그아웃
export const logout = () => ({
  mutationKey: ['auth', 'logout'],
  mutationFn: async () =>
    await axiosInstance.post(`${BASE_URL}/logout`, undefined, { withCredentials: true }),
});
