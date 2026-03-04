import { UserRole } from '@shared/enums';

export const ROLE_LABELS: Record<UserRole, string> = {
  USER: '일반',
  ADMIN: '관리자',
} as const;
