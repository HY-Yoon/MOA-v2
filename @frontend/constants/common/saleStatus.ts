import { SaleStatus } from '@shared/enums';

export const SALE_STATUS_COLORS = {
  ALLOWED: 'bg-green-100 text-green-800 border-green-300',
  SUSPENDED: 'bg-red-100 text-red-800 border-red-300',
} as const;

export const SALE_STATUS_LABELS = {
  ALLOWED: '허용',
  SUSPENDED: '중지',
};

export const SALE_STATUS_OPTIONS = (Object.keys(SALE_STATUS_COLORS) as SaleStatus[]).map((key) => ({
  label: SALE_STATUS_LABELS[key],
  value: key,
}));
