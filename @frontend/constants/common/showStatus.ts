import { ShowStatus } from '@shared/enums';

export const SHOW_STATUS_COLORS = {
  WAITING: 'bg-blue-100 text-blue-800 border-blue-100',
  ON_SALE: 'bg-green-100 text-green-800 border-green-300',
  SOLD_OUT: 'bg-red-100 text-red-800 border-red-300',
  ENDED: 'bg-gray-200 text-gray-600 border-gray-300',
  SUSPENDED: 'bg-yellow-100 text-yellow-800 border-yellow-300',
} as const;

export const SHOW_STATUS_LABELS: Record<ShowStatus, string> = {
  WAITING: '예매대기',
  ON_SALE: '판매중',
  SOLD_OUT: '매진',
  ENDED: '판매종료',
  SUSPENDED: '판매중지',
};

export const SHOW_STATUS_OPTIONS = (Object.keys(SHOW_STATUS_LABELS) as ShowStatus[]).map((key) => ({
  label: SHOW_STATUS_LABELS[key],
  value: key,
}));
