// API 클라이언트 (axios + 환경별 엔드포인트 분기)
// 개발 환경: 직접 백엔드 API 호출 (네트워크 탭에서 확인 가능)
// 프로덕션 환경: Next.js API Routes를 통해 호출 (백엔드 API 노출 방지)

import axios, { AxiosInstance } from 'axios';

const isProduction = process.env.NODE_ENV === 'production';
const backendUrl = process.env.NEXT_PUBLIC_BACKEND_URL;

// axios 인스턴스 설정
export const axiosInstance: AxiosInstance = axios.create({
  baseURL: isProduction ? '' : backendUrl, // 개발: 백엔드 직접, 프로덕션: 상대경로
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json',
  },
});

// 공통 응답 인터셉터 (에러 처리)
axiosInstance.interceptors.response.use(
  (response) => response,
  (error) => {
    console.error('API Error:', error.response?.data || error.message);
    return Promise.reject(error);
  },
);
