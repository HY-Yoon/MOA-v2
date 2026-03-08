'use client';

import { Button } from '@/components/atoms';
import { DATE_FORMAT, DATE_UNIT } from '@/constants/common/dateFormat';
import dayjs from '@/plugins/dayjs';
import { useCallback, useEffect, useRef, useState } from 'react';

interface Props {
  saleOpenAt?: string;
  onOpenTimeReached?: () => void;
}

export default function BeforeOpen({ saleOpenAt, onOpenTimeReached }: Props) {
  const leftDays = saleOpenAt ? getDaysLeft(saleOpenAt) : 0;

  const [remainingSeconds, setRemainingSeconds] = useState<number | null>(null);
  const openTimeReachedRef = useRef(false); // 렌더링 한 번만 호출하기 위한 트리거

  const tick = useCallback(() => {
    if (!saleOpenAt) return;
    const openAt = dayjs(saleOpenAt);
    if (!openAt.isValid()) return;

    const now = dayjs();
    const countdownStart = openAt.subtract(15, DATE_UNIT.MINUTE);

    if (now.isBefore(countdownStart)) {
      setRemainingSeconds(null);
      return;
    }
    if (!now.isBefore(openAt) && !openTimeReachedRef.current) {
      openTimeReachedRef.current = true;
      onOpenTimeReached?.();
      return;
    }

    // 0초가 되면 state 갱신 없이 바로 전환해야 '예매하기' 버튼 안보임
    const remaining = Math.max(0, openAt.diff(now, DATE_UNIT.SECOND));
    if (remaining === 0 && !openTimeReachedRef.current) {
      openTimeReachedRef.current = true;
      onOpenTimeReached?.();
      return;
    }
    setRemainingSeconds(remaining);
  }, [saleOpenAt, onOpenTimeReached]);

  useEffect(() => {
    if (!saleOpenAt) return;
    tick();
    const id = setInterval(tick, 1000);
    return () => clearInterval(id);
  }, [saleOpenAt, tick]);

  function getDaysLeft(isoDate: string): number | null {
    const target = dayjs(isoDate);
    if (!target.isValid()) return null;
    const diffDays = target.diff(dayjs(), DATE_UNIT.DAY, true);
    return Math.max(0, Math.ceil(diffDays));
  }

  function formatCountdown(totalSeconds: number): string {
    const m = Math.floor(totalSeconds / 60);
    const s = totalSeconds % 60;
    return `${m}:${String(s).padStart(2, '0')}`;
  }

  return (
    <div className="flex flex-col items-center gap-3">
      <div className="flex w-full flex-col items-center gap-6 rounded-xl border bg-white px-10 py-12">
        <div className="flex items-stretch gap-4">
          <div className="text-destructive flex min-h-10 flex-col justify-center text-2xl font-semibold">
            <span>D-{leftDays}</span>
          </div>
          <div className="bg-border w-px shrink-0" aria-hidden />
          <div className="flex flex-col justify-center text-sm">
            <span className="font-medium">티켓 오픈</span>
            <span className="text-muted-foreground">
              {saleOpenAt ? dayjs(saleOpenAt).format(DATE_FORMAT.FULL) : '—'}
            </span>
          </div>
        </div>

        <p className="text-muted-foreground text-center text-xs">
          티켓 오픈 시간은 예고없이 변경될 수 있습니다.
        </p>
      </div>

      <Button className="w-full" disabled>
        {remainingSeconds ? `남은 시간 ${formatCountdown(remainingSeconds)}` : '예매하기'}
      </Button>
    </div>
  );
}
