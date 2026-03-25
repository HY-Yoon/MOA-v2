import { ReservationDetail } from '@/components/organisms';

interface Props {
  params: Promise<{ id: string; reservationId: string }>;
}

export default async function ShowReservationDetailPage({ params }: Props) {
  const { reservationId } = await params;
  return <ReservationDetail id={reservationId} variant="admin" />;
}
