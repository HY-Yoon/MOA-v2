'use client';

import { getShowDetail } from '@/lib/api/show';
import dayjs from '@/plugins/dayjs';
import { useQuery } from '@tanstack/react-query';
import { useParams } from 'next/navigation';
import { useEffect, useMemo, useState } from 'react';
import { SIDEBAR_PHASE, type SidebarPhase } from '@/constants/shows/details';
import ShowDetailSidebar from './_components/ShowDetailSidebar';
import ShowDetailMain from './_components/ShowDetailMain';
import ShowDetailTabs from './_components/ShowDetailTabs';

export default function ShowDetailPage() {
  const params = useParams();
  const id = Number(params?.id);

  const { data, isFetching } = useQuery(getShowDetail(id));

  const showDetail = useMemo(() => data as ShowCatalog.Detail, [data]);

  const initialPhase = useMemo(() => getInitialSidebarPhase(showDetail), [showDetail]);
  const [sidebarPhase, setSidebarPhase] = useState<SidebarPhase>(initialPhase);

  const handleOpenTimeReached = () => setSidebarPhase(SIDEBAR_PHASE.AFTER_OPENING);

  useEffect(() => {
    setSidebarPhase(initialPhase);
  }, [initialPhase]);

  function getInitialSidebarPhase(show: ShowCatalog.Detail): SidebarPhase {
    const openAt = show?.salePeriod?.startDate ? dayjs(show.salePeriod.startDate) : null;
    return openAt?.isValid() && !dayjs().isBefore(openAt)
      ? SIDEBAR_PHASE.AFTER_OPENING
      : SIDEBAR_PHASE.BEFORE_OPEN;
  }

  if (!id || id < 0) {
    return (
      <div className="text-muted-foreground container mx-auto max-w-7xl px-5 py-10 text-center sm:px-6 lg:px-8">
        존재하지 않는 공연입니다.
      </div>
    );
  }

  if (!showDetail || isFetching) {
    return (
      <div className="container mx-auto flex min-h-[60vh] max-w-7xl items-center justify-center px-5 sm:px-6 lg:px-8">
        <div className="text-muted-foreground">로딩 중...</div>
      </div>
    );
  }

  return (
    <div className="container mx-auto max-w-7xl py-6 pr-0 pl-5 sm:pl-6 lg:pl-8">
      <div className="flex flex-col gap-8 lg:flex-row lg:items-start">
        <div className="min-w-0 flex-1 space-y-6">
          <ShowDetailMain show={showDetail} />
          <ShowDetailTabs show={showDetail} />
        </div>

        <ShowDetailSidebar
          phase={sidebarPhase}
          salePeriod={showDetail.salePeriod}
          schedules={showDetail.schedules}
          onOpenTimeReached={handleOpenTimeReached}
        />
      </div>
    </div>
  );
}
