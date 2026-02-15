export const USER_ROUTES = {
  HOME: '/',
  SHOW: '/shows',
  COMMUNITY: '/community',
} as const;

export const USER_ROUTES_LABELS: Record<string, string> = {
  [USER_ROUTES.HOME]: '홈',
  [USER_ROUTES.SHOW]: '공연 예매',
  [USER_ROUTES.COMMUNITY]: '커뮤니티',
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
    [HEADER_ROUTES.JOIN]: '회원가입',
  },
} as const;

// 마이페이지 LNB(사이드바) 서브 라우트
export const MY_PAGE_ROUTES = {
  HOME: '/my-page',
  RESERVATIONS: '/my-page/reservations',
  NOTIFICATIONS: '/my-page/notifications',
  INFO: '/my-page/info',
} as const;

export const MY_PAGE_ROUTES_LABELS: Record<string, string> = {
  [MY_PAGE_ROUTES.RESERVATIONS]: '예매내역',
  [MY_PAGE_ROUTES.NOTIFICATIONS]: '알림함',
  [MY_PAGE_ROUTES.INFO]: '내 정보',
} as const;
