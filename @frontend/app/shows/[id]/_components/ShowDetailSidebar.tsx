'use client';

import { SIDEBAR_PHASE, type SidebarPhase } from '@/constants/shows/details';
import ScheduleSelection from './ScheduleSelection';
import BeforeOpen from './BeforeOpen';

interface ShowDetailSidebarProps {
  phase: SidebarPhase;
  saleOpenAt?: string;
  schedules?: ShowCatalog.Schedule[];
  onMoveToSeatSelection?: () => void;
  onOpenTimeReached?: () => void;
}

export default function ShowDetailSidebar({
  phase,
  saleOpenAt,
  schedules = [],
  onOpenTimeReached,
}: ShowDetailSidebarProps) {
  return (
    <aside className="w-full shrink-0 lg:w-[320px]">
      <div className="sticky top-4 flex flex-col gap-3">
        {phase === SIDEBAR_PHASE.BEFORE_OPEN ? (
          <BeforeOpen saleOpenAt={saleOpenAt} onOpenTimeReached={onOpenTimeReached} />
        ) : (
          <ScheduleSelection schedules={schedules} />
        )}
      </div>
    </aside>
  );
}
