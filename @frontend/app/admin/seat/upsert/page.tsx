// /admin/seat/upsert - 좌석 등록 페이지
"use client";

import { PageCard } from "@/components/molecules/PageCard";
import SeatUpsertForm from "./_components/SeatUpsertForm";

export default function SeatUpsertPage() {
  return (
    <PageCard>
      <PageCard.Title useRouteBack={true}>
        좌석 등록
      </PageCard.Title>
      <PageCard.Content>
        <SeatUpsertForm />
      </PageCard.Content>
    </PageCard>
  );
}
