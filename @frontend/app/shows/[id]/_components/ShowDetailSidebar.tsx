'use client';

import { SIDEBAR_PHASE, type SidebarPhase } from '@/constants/shows/details';
import dayjs from 'dayjs';
import ScheduleSelection from './ScheduleSelection';
import BeforeOpen from './BeforeOpen';
import { DATE_UNIT } from '@/constants/common/dateFormat';

interface ShowDetailSidebarProps {
  phase: SidebarPhase;
  salePeriod: ShowCatalog.SalePeriod;
  schedules?: ShowCatalog.Schedule[];
  onMoveToSeatSelection?: () => void;
  onOpenTimeReached?: () => void;
}

export default function ShowDetailSidebar({
  phase,
  salePeriod,
  schedules = [],
  onOpenTimeReached,
}: ShowDetailSidebarProps) {
  const isEnded = salePeriod.endDate
    ? dayjs(salePeriod.endDate).isBefore(dayjs(), DATE_UNIT.DAY)
    : false;

  return (
    <aside className="w-full shrink-0 lg:w-[320px]">
      <div className="sticky top-4 flex flex-col gap-3">
        {isEnded ? (
          <div className="bg-card text-muted-foreground rounded-lg border p-22 text-center text-sm">
            해당 공연의 모든 회차가 종료되었습니다.
          </div>
        ) : phase === SIDEBAR_PHASE.BEFORE_OPEN ? (
          <BeforeOpen saleOpenAt={salePeriod.startDate} onOpenTimeReached={onOpenTimeReached} />
        ) : (
          <ScheduleSelection schedules={schedules} />
        )}
      </div>
    </aside>
  );
}
