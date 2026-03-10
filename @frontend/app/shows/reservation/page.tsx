'use client';

import QueueWaitingScreen from './_components/QueueWaitingScreen';
import { useSearchParams } from 'next/navigation';

export default function ShowReservationPage() {
  const searchParams = useSearchParams();
  const showId = searchParams.get('showId');
  const title = searchParams.get('title') ?? '-';
  const scheduleIdParam = searchParams.get('scheduleId');
  const scheduleId = Number(scheduleIdParam ?? showId ?? 0);
  const showDate = searchParams.get('showDate') ?? '-';

  return (
    <QueueWaitingScreen scheduleId={scheduleId} title={title} showDate={showDate} />
  );
}
