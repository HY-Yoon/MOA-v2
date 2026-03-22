import ReservationPaymentScreen from './_components/ReservationPaymentScreen';

type ReservationPaymentPageProps = {
  searchParams: Promise<{
    showId?: string | string[];
    scheduleId?: string | string[];
    showTitle?: string | string[];
    scheduleText?: string | string[];
    scheduleSeatIds?: string | string[];
    seats?: string | string[];
    seatCount?: string | string[];
    totalAmount?: string | string[];
    remainingSeconds?: string | string[];
    expiresAt?: string | string[];
  }>;
};

const toSingleValue = (value?: string | string[]) => (Array.isArray(value) ? value[0] : value);

export default async function ReservationPaymentPage({ searchParams }: ReservationPaymentPageProps) {
  const params = await searchParams;
  const showId = Number(toSingleValue(params?.showId) ?? 0);
  const scheduleId = Number(toSingleValue(params?.scheduleId) ?? 0);
  const showTitle = toSingleValue(params?.showTitle) ?? '공연 정보';
  const scheduleText = toSingleValue(params?.scheduleText) ?? '-';
  const scheduleSeatIdsParam = toSingleValue(params?.scheduleSeatIds) ?? '';
  const scheduleSeatIds = decodeURIComponent(scheduleSeatIdsParam)
    .split(',')
    .map((value) => value.trim())
    .filter(Boolean);
  const seatsParam = toSingleValue(params?.seats) ?? '[]';
  const parsedSeats = (() => {
    try {
      const parsed = JSON.parse(decodeURIComponent(seatsParam));
      if (!Array.isArray(parsed)) return [];
      return parsed.filter((seat) => typeof seat === 'object' && seat !== null);
    } catch {
      return [];
    }
  })() as Array<{
    seatId?: string;
    sectionName?: string;
    row?: string;
    number?: number;
    price?: number;
  }>;
  const seatCount = Number(toSingleValue(params?.seatCount) ?? 0);
  const totalAmount = Number(toSingleValue(params?.totalAmount) ?? 0);
  const remainingSeconds = Number(toSingleValue(params?.remainingSeconds) ?? 0);
  const expiresAt = Number(toSingleValue(params?.expiresAt) ?? 0);

  return (
    <ReservationPaymentScreen
      showId={showId}
      scheduleId={scheduleId}
      showTitle={showTitle}
      scheduleText={scheduleText}
      scheduleSeatIds={scheduleSeatIds}
      selectedSeats={parsedSeats}
      seatCount={seatCount}
      totalAmount={totalAmount}
      remainingSeconds={remainingSeconds}
      expiresAt={expiresAt}
    />
  );
}
