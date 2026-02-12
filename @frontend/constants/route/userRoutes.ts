

export const USER_ROUTES = {
  HOME: '/',
  SHOW: '/shows',
  COMUNITY: '/community',
} as const;

export const USER_ROUTES_LABELS: Record<string, string> = {
  [USER_ROUTES.HOME]: '홈',
  [USER_ROUTES.SHOW]: '공연 예매',
  [USER_ROUTES.COMUNITY]: '커뮤니티',
} as const;

// 헤더 유틸 영역 라우트
export const HEADER_ROUTES = {
  LOGIN: '/login',
  LOGOUT: '/logout',
  JOIN: '/join',
  MY_PAGE: '/my-page',
} as const;

// 로그인 상태에 따른 헤더 라벨
export const HEADER_ROUTES_LABELS = {
  authenticated: {
    [HEADER_ROUTES.MY_PAGE]: '마이페이지',
    [HEADER_ROUTES.LOGOUT]: '로그아웃',
  },
  unauthenticated: {
    [HEADER_ROUTES.LOGIN]: '로그인',
    [HEADER_ROUTES.JOIN]  : '회원가입',
  },
} as const;
