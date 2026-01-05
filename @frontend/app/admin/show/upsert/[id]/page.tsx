interface Props {
  params: Promise<{ id: string }>;
}

export default async function ShowUpsert({ params }: Props) {
  const { id } = await params;
  return <>공연 수정 페이지: {id}</>;
}
