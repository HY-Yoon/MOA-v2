import QueueWaitingScreen from './_components/QueueWaitingScreen';
type ReservationPageProps = {
  searchParams: Promise<{
    showId?: string | string[];
    scheduleId?: string | string[];
    title?: string | string[];
    showDate?: string | string[];
  }>;
};

const toSingleValue = (value?: string | string[]) => (Array.isArray(value) ? value[0] : value);

export default async function ShowReservationPage({ searchParams }: ReservationPageProps) {
  const params = await searchParams;
  const showId = toSingleValue(params?.showId);
  const title = toSingleValue(params?.title) ?? '-';
  const scheduleIdParam = toSingleValue(params?.scheduleId);
  const scheduleId = Number(scheduleIdParam ?? showId ?? 0);
  const showDate = toSingleValue(params?.showDate) ?? '-';

  return (
    <QueueWaitingScreen scheduleId={scheduleId} title={title} showDate={showDate} />
  );
}
