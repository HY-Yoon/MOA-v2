import ReservationDetail from '@/app/my-page/reservations/[id]/_components/ReservationDetail';

interface Props {
  params: Promise<{ id: string }>;
}

export default async function ReservationDetailPage({ params }: Props) {
  const { id } = await params;
  return <ReservationDetail id={id} />;
}
