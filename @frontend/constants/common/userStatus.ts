import { UserStatus } from '@shared/enums';

export const USER_STATUS_COLORS = {
  ACTIVE: 'bg-green-100 text-green-800 border-green-100',
  SUSPENDED: 'bg-red-100 text-red-800 border-red-300',
  DELETED: 'bg-gray-200 text-gray-600 border-gray-300',
} as const;

export const USER_STATUS_LABELS = {
  ACTIVE: '정상',
  SUSPENDED: '중지',
  DELETED: '삭제',
};

export const USER_STATUS_OPTIONS = (Object.keys(USER_STATUS_COLORS) as UserStatus[]).map((key) => ({
  label: USER_STATUS_LABELS[key],
  value: key,
}));
