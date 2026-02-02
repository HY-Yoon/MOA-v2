'use client';

import { Sheet, SheetContent, SheetHeader, SheetTitle } from '@/components/atoms';

interface Props {
  scheduleId: number | null;
  open: boolean;
  onOpenChange: (open: boolean) => void;
}

export default function ShowSeatStatusPanel({ scheduleId, open, onOpenChange }: Props) {
  return (
    <Sheet open={open} onOpenChange={onOpenChange}>
      <SheetContent side="right" className="w-[85vw] sm:w-2xl! sm:max-w-2xl!">
        <SheetHeader>
          <SheetTitle>좌석 현황</SheetTitle>
        </SheetHeader>
        <div className="py-4">
          {/* TODO: 좌석 현황 */}
          <p className="text-sm text-slate-500">좌석 현황 예정: {scheduleId}</p>
        </div>
      </SheetContent>
    </Sheet>
  );
}
