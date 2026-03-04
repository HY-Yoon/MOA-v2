'use client';

import { Button } from '@/components/atoms';
import { DATE_FORMAT } from '@/constants/common/dateFormat';
import dayjs from '@/plugins/dayjs';
import { useCallback, useEffect, useRef, useState } from 'react';

interface Props {
  saleOpenAt?: string;
  onOpenTimeReached?: () => void;
}

export default function TicketOpenBefore({ saleOpenAt, onOpenTimeReached }: Props) {
  const leftDays = saleOpenAt ? getDaysLeft(saleOpenAt) : 0;

  const [remainingSeconds, setRemainingSeconds] = useState<number | null>(null);
  const openFiredRef = useRef(false);

  const tick = useCallback(() => {
    if (!saleOpenAt) return;
    const openAt = dayjs(saleOpenAt);
    if (!openAt.isValid()) return;

    const now = dayjs();
    const countdownStart = openAt.subtract(15, 'minute');

    if (now.isBefore(countdownStart)) {
      setRemainingSeconds(null);
      return;
    }
    if (!now.isBefore(openAt)) {
      setRemainingSeconds(0);
      if (!openFiredRef.current) {
        openFiredRef.current = true;
        onOpenTimeReached?.();
      }
      return;
    }

    const remaining = Math.max(0, openAt.diff(now, 'second'));
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
    const diffDays = target.diff(dayjs(), 'day', true);
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
