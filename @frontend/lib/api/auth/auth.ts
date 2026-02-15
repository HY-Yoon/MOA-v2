import { axiosInstance } from '@/lib/api-client';

const BASE_URL = '/api/auth' as const;

/** 토큰 검증 (쿠키 등 withCredentials로 전송). 401 시 throw */
// FIXME: 토큰 파라미터 전달 불필요
async function verifyToken(): Promise<void> {
  await axiosInstance.get(`${BASE_URL}/verify?token=test;`, { withCredentials: true });
}

/**
 * 1. verify 성공 → 유저 정보 조회 후 반환
 * 2. verify 401 → 비로그인, null 반환 (리다이렉트 없음)
 */
export const getAuthUser = () => ({
  queryKey: ['auth', 'verify', 'user'],
  queryFn: async () => {
    try {
      await verifyToken();
    } catch (err: unknown) {
      return null;
    }
    const response = await axiosInstance.get(`${BASE_URL}/user`, { withCredentials: true });
    return response?.data?.data ?? null;
  },
  retry: false,
  staleTime: 2 * 60 * 1000, // 2분
});

// 로그아웃
export const logout = () => ({
  mutationKey: ['auth', 'logout'],
  mutationFn: async () =>
    await axiosInstance.post(`${BASE_URL}/logout`, undefined, { withCredentials: true }),
});

// 회원탈퇴
export const deleteUser = () => ({
  mutationKey: ['auth', 'user', 'delete'],
  mutationFn: async () => await axiosInstance.delete('/api/v1/users/me', { withCredentials: true }),
});
