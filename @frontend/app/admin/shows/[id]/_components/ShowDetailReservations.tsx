'use client';

import { ReservationList } from '@/components/organisms';
import { ADMIN_ROUTES } from '@/constants/route/adminRoutes';
import { fetchShowReservationList } from '@/lib/api/admin/reservation';

interface Props {
  showId: number;
}

export default function ShowDetailReservations({ showId }: Props) {
  const getAdminReservationDetailPath = (reservationId: number) =>
    `${ADMIN_ROUTES.SHOW}/${showId}/reservations/${reservationId}`;

  return (
    <ReservationList
      getDetailLink={(r) => getAdminReservationDetailPath(r.reservationId)}
      getListQuery={fetchShowReservationList}
      extraParams={{ showId }}
    />
  );
}
