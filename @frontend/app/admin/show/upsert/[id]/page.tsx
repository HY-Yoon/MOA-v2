import ShowUpsertForm from '../_components/ShowUpsertForm';

interface Props {
  params: Promise<{ id: string }>;
}

export default async function ShowUpsert({ params }: Props) {
  const { id } = await params;
  return <ShowUpsertForm id={id} />;
}
