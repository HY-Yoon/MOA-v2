'use client';

import {
  Separator,
  Sheet,
  SheetContent,
  SheetHeader,
  SheetTitle,
  Skeleton,
} from '@/components/atoms';
import { DATE_FORMAT, DATE_UNIT } from '@/constants/common/dateFormat';
import { getShowSchedulesByDate } from '@/lib/api/show';
import dayjs from 'dayjs';
import { useQuery } from '@tanstack/react-query';
import { useMemo } from 'react';

interface Props {
  showId: number;
  showDate: string | null;
  scheduleId: number | null;
  open: boolean;
  onOpenChange: (open: boolean) => void;
}

const labelClass = 'text-muted-foreground text-xs font-medium';
const valueClass = 'text-foreground mt-2';
const seatUlClass = 'border-border divide-border mt-3 divide-y rounded-md border';
const seatLiClass = 'flex justify-between gap-4 px-4 py-3 text-xs sm:text-sm';

export default function ShowSeatStatusPanel({
  showId,
  scheduleId,
  showDate,
  open,
  onOpenChange,
}: Props) {
  const dateString = useMemo(() => {
    if (!showDate) return '';
    const parsed = dayjs(showDate);
    return parsed.isValid() ? parsed.format(DATE_FORMAT.DATE_ONLY) : '';
  }, [showDate]);

  const isSeatStatusPanelDisabled =
    !open || !showId || !dateString || !scheduleId;

  const { data: schedulesByDate = [], isLoading } = useQuery({
    ...getShowSchedulesByDate(showId, dateString),
    enabled: !isSeatStatusPanelDisabled,
  });

  const detail = useMemo(() => {
    if (!scheduleId) return null;
    return schedulesByDate.find((s) => s.keyId === scheduleId) ?? null;
  }, [schedulesByDate, scheduleId]);

  const infoRows = useMemo(() => {
    if (!detail) return [];

    const showDay = dayjs(detail.date);
    const isPastSchedule = showDay.isValid() && showDay.isBefore(dayjs(), DATE_UNIT.DAY);
    const statusLabel = detail.isSoldOut || isPastSchedule ? '예매 종료' : '예매 가능';

    return [
      { key: 'status', label: '상태', value: statusLabel },
      {
        key: 'date',
        label: '공연일',
        value: dayjs(detail.date).format(DATE_FORMAT.DATE_KR),
      },
      { key: 'time', label: '공연 시간', value: detail.time },
    ];
  }, [detail]);

  const seatRows = useMemo(() => {
    if (!detail) return [];

    if (detail.seatGrades?.length) {
      return detail.seatGrades.map((g) => ({
        key: `grade-${g.sectionName}`,
        leftLabel: `${g.sectionName}석`,
        remaining: g.remainingSeats ?? 0,
        total: g.totalSeats ?? 0,
      }));
    }
    return [
      {
        key: 'all',
        leftLabel: '전체',
        remaining: detail.remainingSeats ?? 0,
        total: detail.totalSeats ?? 0,
      },
    ];
  }, [detail]);

  return (
    <Sheet open={open} onOpenChange={onOpenChange}>
      <SheetContent side="right" className="w-[85vw] gap-0 p-6 sm:w-2xl! sm:max-w-2xl! sm:p-8">
        <SheetHeader className="p-0 pb-4">
          <SheetTitle>좌석 현황</SheetTitle>
        </SheetHeader>

        <Separator />

        <div className="space-y-5 pt-5 pb-2">
          {isSeatStatusPanelDisabled ? null : isLoading ? (
            <div className="space-y-3">
              <Skeleton className="h-5 w-3/4" />
              <Skeleton className="h-5 w-1/2" />
              <Skeleton className="h-5 w-full" />
            </div>
          ) : !detail ? (
            <p className="text-muted-foreground text-sm">해당 회차 정보를 찾을 수 없습니다.</p>
          ) : (
            <div className="space-y-5 text-sm">
              {infoRows.map((row) => (
                <div key={row.key}>
                  <p className={labelClass}>{row.label}</p>
                  <p className={valueClass}>{row.value}</p>
                </div>
              ))}
              <div>
                <p className={labelClass}>좌석</p>
                <ul className={seatUlClass}>
                  {seatRows.map((row) => (
                    <li key={row.key} className={seatLiClass}>
                      <span>{row.leftLabel}</span>
                      <span className="text-muted-foreground shrink-0">
                        잔여 {row.remaining}석 / 총 {row.total}석
                      </span>
                    </li>
                  ))}
                </ul>
              </div>
              <p className="text-muted-foreground text-xs leading-relaxed">
                (실시간 예매 현황에 따라 차이가 발생할 수 있습니다.)
              </p>
            </div>
          )}
        </div>
      </SheetContent>
    </Sheet>
  );
}
