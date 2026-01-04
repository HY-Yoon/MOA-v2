const ADMIN = "/admin" as const;

export const ADMIN_ROUTES = {
  DASHBOARD: ADMIN,
  SHOW: `${ADMIN}/show`,
  SEAT: `${ADMIN}/seat`,
  USER: `${ADMIN}/user`,
} as const;

export const ADMIN_ROUTE_LABELS: Record<string, string> = {
  [ADMIN_ROUTES.DASHBOARD]: "대시보드",
  [ADMIN_ROUTES.SHOW]: "공연관리",
  [ADMIN_ROUTES.SEAT]: "좌석관리",
  [ADMIN_ROUTES.USER]: "회원관리",
} as const;
