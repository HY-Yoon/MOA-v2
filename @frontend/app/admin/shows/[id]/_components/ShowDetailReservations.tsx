'use client';

import { ReservationList } from '@/components/organisms';
import { ADMIN_ROUTES } from '@/constants/route/adminRoutes';
import { fetchShowReservationList } from '@/lib/api/admin/reservation';

interface Props {
  showId: number;
}

export default function ShowDetailReservations({ showId }: Props) {
  return (
    <ReservationList
      getDetailLink={(r) => `${ADMIN_ROUTES.RESERVATION}/${r.reservationId}`}
      getListQuery={fetchShowReservationList}
      extraParams={{ showId }}
      variant="admin"
    />
  );
}
