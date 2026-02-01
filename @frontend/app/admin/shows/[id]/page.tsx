import ShowDetail from '@/app/admin/shows/[id]/_components/ShowDetail';

interface Props {
  params: Promise<{ id: string }>;
}

export default async function ShowDetailPage({ params }: Props) {
  const { id } = await params;
  return <ShowDetail id={id} />;
}
