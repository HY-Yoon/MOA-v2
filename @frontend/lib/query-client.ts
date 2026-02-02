import { QueryClient } from '@tanstack/react-query';

// QueryClient 설정
export const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      // 기본 설정
      retry: 1, // 실패 시 1번 재시도
      refetchOnWindowFocus: false, // 창 포커스 시 리패칭 비활성화
      refetchOnMount: false, // 마운트 시 재조회 비활성화
      refetchOnReconnect: false, // 재연결 시 재조회 비활성화
      staleTime: 5 * 60 * 1000, // 5분
      gcTime: 10 * 60 * 1000, // 10분
    },
    mutations: {
      // 뮤테이션 기본 설정
      retry: false, // 뮤테이션은 재시도하지 않음
    },
  },
});
