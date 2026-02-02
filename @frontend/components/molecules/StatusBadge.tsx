import {
  SHOW_STATUS_COLORS,
  SHOW_STATUS_LABELS,
  SALE_STATUS_COLORS,
  SALE_STATUS_LABELS,
} from '@/constants/common';
import { USER_STATUS_COLORS, USER_STATUS_LABELS } from '@/constants/common/userStatus';
import { GENDER_COLORS, GENDER_LABELS } from '@/constants/common/gender';
import { Badge } from '@/components/atoms/badge';
import { SaleStatus, ShowStatus } from '@shared/enums';
import type { UserStatus, Gender } from '@shared/enums';

type Props =
  | { type: 'show'; status: ShowStatus }
  | { type: 'sale'; status: SaleStatus }
  | { type: 'user'; status: UserStatus }
  | { type: 'gender'; status: Gender };

type StatusByType = {
  show: { colors: typeof SHOW_STATUS_COLORS; labels: typeof SHOW_STATUS_LABELS };
  sale: { colors: typeof SALE_STATUS_COLORS; labels: typeof SALE_STATUS_LABELS };
  user: { colors: typeof USER_STATUS_COLORS; labels: typeof USER_STATUS_LABELS };
  gender: { colors: typeof GENDER_COLORS; labels: typeof GENDER_LABELS };
};

const BADGE_CONFIG: StatusByType = {
  show: { colors: SHOW_STATUS_COLORS, labels: SHOW_STATUS_LABELS },
  sale: { colors: SALE_STATUS_COLORS, labels: SALE_STATUS_LABELS },
  user: { colors: USER_STATUS_COLORS, labels: USER_STATUS_LABELS },
  gender: { colors: GENDER_COLORS, labels: GENDER_LABELS },
};

export default function StatusBadge(props: Props) {
  const { type, status } = props;

  if (!status) return '-';

  const { colors, labels } = BADGE_CONFIG[type];
  const className = colors[status as keyof typeof colors] ?? '';
  const label = labels[status as keyof typeof labels] ?? status;

  return <Badge className={className}>{label}</Badge>;
}
