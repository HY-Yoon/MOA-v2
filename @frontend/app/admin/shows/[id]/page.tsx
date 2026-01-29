interface Props {
  params: Promise<{ id: string }>;
}

export default async function ShowDetailPage({ params }: Props) {
  const { id } = await params;
  return `공연 상세 예정: ${id}`;
}
