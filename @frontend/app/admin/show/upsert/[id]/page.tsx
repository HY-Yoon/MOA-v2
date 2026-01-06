import ShowUpsertForm from '@/components/admin/show/upsert/ShowUpsertForm';

interface Props {
  params: Promise<{ id: string }>;
}

export default async function ShowUpsert({ params }: Props) {
  const { id } = await params;
  return <ShowUpsertForm id={id} />;
}
