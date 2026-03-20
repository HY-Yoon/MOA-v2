import ReservationScheduleSelection from '../_components/ReservationScheduleSelection';

type ReservationSchedulePageProps = {
  searchParams: Promise<{
    showId?: string | string[];
    scheduleId?: string | string[];
  }>;
};

const toSingleValue = (value?: string | string[]) => (Array.isArray(value) ? value[0] : value);

export default async function ReservationSchedulePage({ searchParams }: ReservationSchedulePageProps) {
  const params = await searchParams;
  const showIdValue = toSingleValue(params?.showId);
  const showId = Number(showIdValue ?? 0);
  const scheduleIdValue = toSingleValue(params?.scheduleId);
  const initialScheduleKeyId = Number(scheduleIdValue ?? 0);

  return (
    <ReservationScheduleSelection
      showId={showId}
      initialScheduleKeyId={initialScheduleKeyId > 0 ? initialScheduleKeyId : undefined}
    />
  );
}
