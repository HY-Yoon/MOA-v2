import {
  SHOW_STATUS_COLORS,
  SHOW_STATUS_LABELS,
  SALE_STATUS_COLORS,
  SALE_STATUS_LABELS,
} from '@/constants/common';
import { Badge } from '@/components/atoms/badge';
import { SaleStatus, ShowStatus } from '@shared/enums';

interface Props {
  type: 'show' | 'sale';
  status: ShowStatus | SaleStatus;
}

export default function StatusBadge({ type, status }: Props) {
  const isShowType = type === 'show';
  const colorMap = isShowType ? SHOW_STATUS_COLORS : SALE_STATUS_COLORS;
  const labelMap = isShowType ? SHOW_STATUS_LABELS : SALE_STATUS_LABELS;

  return <Badge className={colorMap[status as never]}>{labelMap[status as never]}</Badge>;
}
