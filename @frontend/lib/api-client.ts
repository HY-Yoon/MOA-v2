// API 클라이언트 (axios + 환경별 엔드포인트 분기)
// 개발 환경: 직접 백엔드 API 호출 (네트워크 탭에서 확인 가능)
// 프로덕션 환경: Next.js API Routes를 통해 호출 (백엔드 API 노출 방지)

import type { AlertOptions } from '@/components/molecules/AlertContext';
import axios, { type AxiosInstance } from 'axios';

// axios 인스턴스 설정
export const axiosInstance: AxiosInstance = axios.create({
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json',
  },
});

// axios interceptor
let globalRouter: any = null;
let globalAlert: ((options: AlertOptions) => void) | null = null;
let globalOnUnauthorized: (() => void) | null = null;

export const setGlobalRouter = (router: any) => (globalRouter = router);
export const setGlobalAlertHandler = (handler: (options: AlertOptions) => void) =>
  (globalAlert = handler);
export const setGlobalOnUnauthorized = (handler: (() => void) | null) =>
  (globalOnUnauthorized = handler);

axiosInstance.interceptors.response.use(
  (response) => response,
  async (error) => {
    console.error('API Error:', error.response?.data || error.message);

    if (error.response?.status === 401) {
      // 로그인 여부 확인은 401 리다이렉트 제외 (users/me)
      const isAuthCheckRequest =
        error.config?.method?.toLowerCase() === 'get' &&
        (error.config?.url?.includes('users/me') ?? false);
      if (!isAuthCheckRequest) {
        globalOnUnauthorized?.();
      }
      return Promise.reject(error);
    }

    if (error.response?.status === 404) {
      const message = error.response?.data?.message || '요청한 데이터를 찾을 수 없습니다';
      const confirmed = globalAlert?.({ title: '404 Error', description: message });
      if (confirmed) globalRouter?.back();
    }

    return Promise.reject(error);
  },
);
