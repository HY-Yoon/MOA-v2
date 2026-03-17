'use client';

import { Skeleton } from '@/components/atoms';
import { useAlert } from '@/components/molecules/AlertContext';
import { useMutation, useQuery } from '@tanstack/react-query';
import { useMemo } from 'react';
import ReservationBasicInfo from './ReservationBasicInfo';
import { PageCard } from '@/components/molecules/PageCard';
import { cancelReservation, fetchReservationDetail } from '@/lib/api/reservation';
import dayjs from '@/plugins/dayjs';
import { DATE_UNIT } from '@/constants/common/dateFormat';
import ReservationPaymentInfo from '@/app/my-page/reservations/[id]/_components/ReservationPaymentInfo';
import ReservationNotice from '@/app/my-page/reservations/[id]/_components/ReservationNotice';
import { queryClient } from '@/lib/query-client';
import { MY_PAGE_ROUTES } from '@/constants/route/userRoutes';
import { useRouter } from 'next/navigation';

interface Props {
  id: string;
}

export default function ReservationDetail({ id }: Props) {
  const router = useRouter();
  const reservationId = Number(id) || -1;

  const { confirm } = useAlert();

  const { data, isFetching } = useQuery(fetchReservationDetail(reservationId));
  const { mutateAsync: onCancelReservation, isPending } = useMutation(cancelReservation());

  const loading = isFetching || isPending;

  const cancelDeadline = dayjs(data?.schedule.showDate).subtract(1, DATE_UNIT.DAY);

  const canCancel = useMemo(() => {
    if (!data) return false;
    const notCancelled =
      data.reservationStatus !== 'CANCELLED' && data.payment.paymentStatus !== 'CANCELLED';
    const beforeDeadline = data.schedule?.showDate
      ? dayjs().isSameOrBefore(cancelDeadline, DATE_UNIT.DAY)
      : false;
    return data.canCancel || (notCancelled && beforeDeadline);
  }, [cancelDeadline, data]);

  const handleReservationCancel = async () => {
    if (!data || !canCancel) return;

    const confirmed = await confirm({
      title: '예매 취소',
      description: `${data.show.title} 예매를 취소하시겠습니까?`,
    });
    if (!confirmed) return;

    await onCancelReservation(reservationId);
    await queryClient.invalidateQueries({ queryKey: ['user', 'reservations', 'list'] });
    router.push(MY_PAGE_ROUTES.RESERVATIONS);
  };

  if (!reservationId) return;

  const detailData = data as unknown as Reservation.Detail;
  return (
    <section>
      <h1 className="mb-6 text-2xl font-bold">예매내역 상세</h1>
      <PageCard>
        <PageCard.Content>
          {loading ? (
            <div className="space-y-8">
              <Skeleton className="h-64 w-full" />
              <Skeleton className="h-40 w-full" />
              <Skeleton className="h-32 w-full" />
            </div>
          ) : !data ? (
            <p className="text-slate-500">데이터를 불러올 수 없습니다.</p>
          ) : (
            <div className="flex flex-col gap-4">
              <ReservationBasicInfo data={detailData} />
              <ReservationPaymentInfo
                data={detailData}
                cancelDeadline={cancelDeadline}
                canCancel={canCancel}
                onCancel={handleReservationCancel}
              />
              <ReservationNotice />
            </div>
          )}
        </PageCard.Content>
      </PageCard>
    </section>
  );
}
