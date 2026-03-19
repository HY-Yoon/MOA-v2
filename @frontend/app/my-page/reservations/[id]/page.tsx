import { ReservationDetail } from '@/components/organisms';

interface Props {
  params: Promise<{ id: string }>;
}

export default async function ReservationDetailPage({ params }: Props) {
  const { id } = await params;
  return (
    <section>
      <h1 className="mb-6 text-2xl font-bold">예매내역 상세</h1>
      <ReservationDetail id={id} variant="user" />
    </section>
  );
}
