import ReservationScheduleSelection from './_components/ReservationScheduleSelection';
type ReservationPageProps = {
  searchParams: Promise<{
    showId?: string | string[];
    scheduleId?: string | string[];
    seatsRefetchKey?: string | string[];
  }>;
};

const toSingleValue = (value?: string | string[]) => (Array.isArray(value) ? value[0] : value);

export default async function ShowReservationPage({ searchParams }: ReservationPageProps) {
  const params = await searchParams;
  const showId = Number(toSingleValue(params?.showId) ?? 0);
  const scheduleIdParam = toSingleValue(params?.scheduleId);
  const initialScheduleKeyId = Number(scheduleIdParam ?? 0);
  const seatsRefetchKeyParam = toSingleValue(params?.seatsRefetchKey);
  const initialSeatsRefetchKey = Number(seatsRefetchKeyParam ?? 0);

  return (
    <ReservationScheduleSelection
      showId={showId}
      initialScheduleKeyId={initialScheduleKeyId > 0 ? initialScheduleKeyId : undefined}
      initialSeatsRefetchKey={Number.isFinite(initialSeatsRefetchKey) ? initialSeatsRefetchKey : 0}
    />
  );
}
