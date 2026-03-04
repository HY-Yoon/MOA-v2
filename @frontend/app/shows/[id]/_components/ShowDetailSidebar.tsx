'use client';

import { SIDEBAR_PHASE, type SidebarPhase } from '@/constants/shows/details';
import { useState } from 'react';
import CalendarSeatSelection from './CalendarSeatSelection';
import TicketOpenBefore from './TicketOpenBefore';

interface ShowDetailSidebarProps {
  phase: SidebarPhase;
  saleOpenAt?: string;
  schedules?: Array<ShowCatalog.Schedule>;
  onMoveToSeatSelection?: () => void;
  onOpenTimeReached?: () => void;
}

function formatTime(t: { hour: number; minute: number }) {
  const h = String(t.hour).padStart(2, '0');
  const m = String(t.minute).padStart(2, '0');
  return `${h}:${m}`;
}

export default function ShowDetailSidebar({
  phase,
  saleOpenAt,
  schedules = [],
  onOpenTimeReached,
}: ShowDetailSidebarProps) {
  const [selectedScheduleKeyId, setSelectedScheduleKeyId] = useState<number | null>(null);

  const scheduleOptions = schedules.map((s) => ({
    keyId: s.keyId,
    date: s.date,
    time: formatTime(s.time),
    session: s.session,
  }));

  const content =
    phase === SIDEBAR_PHASE.BEFORE_OPEN ? (
      <TicketOpenBefore saleOpenAt={saleOpenAt} onOpenTimeReached={onOpenTimeReached} />
    ) : (
      <CalendarSeatSelection
        schedules={scheduleOptions}
        selectedScheduleKeyId={selectedScheduleKeyId}
        onSelectSchedule={setSelectedScheduleKeyId}
      />
    );

  const wrapperClass =
    phase === SIDEBAR_PHASE.BEFORE_OPEN
      ? 'sticky top-4 flex flex-col gap-3'
      : 'sticky top-4 rounded-xl border bg-white p-4';

  return (
    <aside className="w-full shrink-0 lg:w-[320px]">
      <div className={wrapperClass}>{content}</div>
    </aside>
  );
}
