// /admin/seat - 좌석 관리 페이지 
"use client";

import { Button } from "@/components/atoms";
import { PageCard } from "@/components/molecules/PageCard";
import { useRouter } from "next/navigation";

export default function SeatPage() {
  const router = useRouter();

  return (
    <PageCard>
      <PageCard.Title useRouteBack={false}>
        좌석 관리
      </PageCard.Title>
      <PageCard.Content>
        <div className="space-y-4">
          <div className="flex justify-end">
            <Button onClick={() => router.push('/admin/seat/upsert')}>
              좌석 등록
            </Button>
          </div>
          <div className="text-slate-500">
            여기에 테이블 들어감
          </div>
        </div>
      </PageCard.Content>
    </PageCard>
  )
}
