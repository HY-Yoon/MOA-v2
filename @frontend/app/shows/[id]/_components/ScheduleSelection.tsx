'use client';

import { Button, Calendar } from '@/components/atoms';
import { DATE_FORMAT, DATE_UNIT } from '@/constants/common/dateFormat';
import { getShowSchedulesByDate } from '@/lib/api/show';
import { cn } from '@/lib/utils';
import dayjs from 'dayjs';
import { useParams } from 'next/navigation';
import { useEffect, useMemo, useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { useReservationPopup } from '@/hooks/useReservationPopup';

interface Props {
  schedules?: ShowCatalog.Schedule[];
}

const sectionTitleClass = 'mb-3 text-sm font-medium' as const;
const sessionButtonClass = {
  base: 'w-full min-w-0 cursor-pointer rounded-lg border px-4 py-3 text-sm transition-colors',
  selected: 'border-primary bg-primary/10 text-primary hover:bg-primary/20',
  default: 'border-border bg-background hover:bg-muted/50',
} as const;

export default function ScheduleSelection({ schedules = [] }: Props) {
  const { openReservationPopup } = useReservationPopup();

  const params = useParams();
  const showId = Number(params?.id) ?? 0;

  const defaultSchedule = useMemo(() => {
    if (!schedules.length) return null;

    return (
      [...schedules]
        .filter((s) => !dayjs(s.date).isBefore(dayjs(), DATE_UNIT.DAY))
        .sort((a, b) => {
          const byDate = a.date.localeCompare(b.date);
          if (byDate !== 0) return byDate;
          return a.session - b.session;
        })[0] ?? null
    );
  }, [schedules]);

  const defaultDate = defaultSchedule ? dayjs(defaultSchedule.date).toDate() : undefined;

  const [selectedDate, setSelectedDate] = useState<Date | undefined>(defaultDate);
  const [selectedScheduleKey, setSelectedScheduleKey] = useState<number | null>(
    defaultSchedule?.keyId ?? null,
  );

  useEffect(() => {
    if (!defaultSchedule) return;
    setSelectedDate((d) => d ?? dayjs(defaultSchedule.date).toDate());
    setSelectedScheduleKey((k) => k ?? defaultSchedule.keyId);
  }, [defaultSchedule]);

  const scheduleDatesSet = useMemo(() => new Set(schedules.map((s) => s.date)), [schedules]);

  const dateString = selectedDate ? formatDateOnly(selectedDate) : '';
  const { data: schedulesByDate = [] } = useQuery(getShowSchedulesByDate(showId, dateString));

  const scheduleDetailByKeyId = useMemo(() => {
    const map = new Map<number, ShowCatalog.ScheduleByDate>();
    schedulesByDate.forEach((s) => map.set(s.keyId, s));
    return map;
  }, [schedulesByDate]);

  const selectedDetail = selectedScheduleKey
    ? scheduleDetailByKeyId.get(selectedScheduleKey)
    : null;

  const schedulesOnSelectedDate = selectedDate
    ? [...schedules]
        .filter((s) => s.date === formatDateOnly(selectedDate))
        .sort((a, b) => a.session - b.session)
    : [];

  function formatDateOnly(d: Date): string {
    return dayjs(d).format(DATE_FORMAT.DATE_ONLY);
  }

  function isScheduleDateBlocked(date: Date): boolean {
    return (
      dayjs(date).isBefore(dayjs(), DATE_UNIT.DAY) || !scheduleDatesSet.has(formatDateOnly(date))
    );
  }

  const isSelectedDateBlocked = Boolean(selectedDate && isScheduleDateBlocked(selectedDate));

  function getRemainingSeatLabel(detail?: ShowCatalog.ScheduleByDate | null): string {
    if (!detail) return '잔여석 0';

    if (detail.seatGrades?.length) {
      return detail.seatGrades
        .map((g: ShowCatalog.SeatGrades) => `${g.sectionName}석 ${g.remainingSeats ?? 0}`)
        .join(' / ');
    }
    return `잔여석 ${detail.remainingSeats ?? 0}`;
  }

  function handleSelectDate(date?: Date) {
    setSelectedDate(date);
    if (!date) {
      setSelectedScheduleKey(null);
      return;
    }

    const key = formatDateOnly(date);
    const firstOnDate = schedules.find((s) => s.date === key);
    setSelectedScheduleKey(firstOnDate?.keyId ?? null);
  }

  async function handleBooking() {
    if (showId <= 0 || !selectedScheduleKey) return;

    await openReservationPopup({
      showId,
      scheduleId: selectedScheduleKey,
      showDate: selectedDate ? formatDateOnly(selectedDate) : undefined,
    });
  }

  const isBookingDisabled =
    !selectedDate || isSelectedDateBlocked || !selectedScheduleKey || selectedDetail?.isSoldOut;

  return (
    <div className="flex flex-col gap-4">
      <div className="bg-card rounded-lg border p-4">
        <p className={sectionTitleClass}>관람일</p>
        <Calendar
          mode="single"
          navLayout="around"
          selected={selectedDate}
          onSelect={handleSelectDate}
          defaultMonth={selectedDate ?? defaultDate}
          disabled={(date) => isScheduleDateBlocked(date)}
          className="[&_.rdp-weekday]:first-child:text-red-500 [&_.rdp-weekday]:last-child:text-muted-foreground rounded-lg border-0 p-0"
        />

        <hr className="border-border my-6" />

        <p className={sectionTitleClass}>회차</p>
        {schedulesOnSelectedDate.length > 0 ? (
          <>
            <ul className="grid grid-cols-2 gap-3">
              {schedulesOnSelectedDate.map((s) => {
                const isSelected = selectedScheduleKey === s.keyId;
                return (
                  <li key={s.keyId}>
                    <button
                      type="button"
                      disabled={isSelectedDateBlocked}
                      onClick={() => setSelectedScheduleKey(s.keyId)}
                      className={cn(
                        sessionButtonClass.base,
                        isSelected ? sessionButtonClass.selected : sessionButtonClass.default,
                        isSelectedDateBlocked && 'cursor-not-allowed opacity-50',
                      )}
                    >
                      {s.session}회 {s.time as string}
                    </button>
                  </li>
                );
              })}
            </ul>
            {selectedScheduleKey != null && (
              <>
                <p className="text-muted-foreground mt-4 text-sm font-semibold">
                  {getRemainingSeatLabel(selectedDetail)}
                </p>
                <p className="text-muted-foreground mt-1 text-xs">
                  (실시간 예매 현황에 따라 차이가 발생할 수 있습니다.)
                </p>
              </>
            )}
          </>
        ) : (
          <p className="text-muted-foreground py-4 text-center text-sm">
            날짜를 선택하면 회차 목록이 표시됩니다.
          </p>
        )}
      </div>

      <Button className="w-full" disabled={isBookingDisabled} onClick={handleBooking}>
        {selectedDetail?.isSoldOut ? '매진' : '예매하기'}
      </Button>
    </div>
  );
}
