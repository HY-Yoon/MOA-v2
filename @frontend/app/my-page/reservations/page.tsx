'use client';

import { ReservationList } from '@/components/organisms';
import { MY_PAGE_ROUTES } from '@/constants/route/userRoutes';
import { fetchReservationList } from '@/lib/api/reservation';
import { PageCard } from '@/components/molecules/PageCard';

export default function ReservationListPage() {
  return (
    <section>
      <h1 className="mb-6 text-2xl font-bold">예매내역</h1>
      <PageCard>
        <PageCard.Content>
          <ReservationList
            getDetailLink={(r) => `${MY_PAGE_ROUTES.RESERVATIONS}/${r.reservationId}`}
            getListQuery={fetchReservationList}
            variant="user"
          />
        </PageCard.Content>
      </PageCard>
    </section>
  );
}
