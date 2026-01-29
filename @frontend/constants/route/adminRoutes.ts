const ADMIN = '/admin' as const;

export const ADMIN_ROUTES = {
  DASHBOARD: ADMIN,
  SHOW: `${ADMIN}/shows`,
  SHOW_UPSERT: `${ADMIN}/shows/upsert`,
  SEAT: `${ADMIN}/seat`,
  USER: `${ADMIN}/user`,
} as const;

type LabelType = string | { CREATE: string; EDIT: string };
export const ADMIN_ROUTE_LABELS: Record<string, LabelType> = {
  [ADMIN_ROUTES.DASHBOARD]: '대시보드',
  [ADMIN_ROUTES.SHOW]: '공연 관리',
  [ADMIN_ROUTES.SHOW_UPSERT]: {
    CREATE: '공연 등록',
    EDIT: '공연 수정',
  },
  [ADMIN_ROUTES.SEAT]: '좌석 관리',
  [ADMIN_ROUTES.USER]: '회원 관리',
} as const;
