import { axiosInstance } from '@/lib/api-client';

const USER_URL = '/api/v1/users/me' as const;

/**
 * 유저 정보 조회로 로그인 여부 판단
 * - 200 → 로그인 상태, 유저 정보 반환
 * - 401 → 비로그인, null 반환 (리다이렉트 없음)
 */
export const getAuthUser = () => ({
  queryKey: ['auth', 'user', 'info'],
  queryFn: async () => {
    try {
      const response = await axiosInstance.get(USER_URL, {
        withCredentials: true,
      });
      return response?.data?.data ?? null;
    } catch {
      return null;
    }
  },
  retry: false,
  staleTime: 2 * 60 * 1000, // 2분
});

// 로그아웃
export const logout = () => ({
  mutationKey: ['auth', 'user', 'logout'],
  mutationFn: async () =>
    await axiosInstance.post('/api/auth/logout', undefined, { withCredentials: true }),
});

// 회원탈퇴
export const deleteUser = () => ({
  mutationKey: ['auth', 'user', 'delete'],
  mutationFn: async () => await axiosInstance.delete(USER_URL, { withCredentials: true }),
});
