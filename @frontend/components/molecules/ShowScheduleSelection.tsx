'use client';

import { Button, Calendar } from '@/components/atoms';
import { DATE_FORMAT, DATE_UNIT } from '@/constants/common/dateFormat';
import { getShowSchedulesByDate } from '@/lib/api/show';
import { cn } from '@/lib/utils';
import { useQuery } from '@tanstack/react-query';
import dayjs from 'dayjs';
import { useEffect, useMemo, useState } from 'react';

interface ShowScheduleSelectionProps {
  showId: number;
  schedules?: ShowCatalog.Schedule[];
  onBooking?: (params: { showId: number; scheduleKeyId: number }) => void;
  layout?: 'stacked' | 'split';
  bookingButtonLabel?: string;
  hideBookingButton?: boolean;
  initialSelectedScheduleKeyId?: number;
  onSelectionChange?: (params: { scheduleKeyId: number | null; isBookingDisabled: boolean }) => void;
}

const sectionTitleClass = 'mb-3 text-sm font-medium' as const;
const sessionButtonClass = {
  base: 'w-full min-w-0 cursor-pointer rounded-lg border px-4 py-3 text-sm transition-colors',
  selected: 'border-primary bg-primary/10 text-primary hover:bg-primary/20',
  default: 'border-border bg-background hover:bg-muted/50',
} as const;

function formatDateOnly(date: Date): string {
  return dayjs(date).format(DATE_FORMAT.DATE_ONLY);
}

function getRemainingSeatLabel(detail?: ShowCatalog.ScheduleByDate | null): string {
  if (!detail) return '잔여 0석';
  if (detail.seatGrades?.length) {
    return detail.seatGrades
      .map((grade) => `${grade.sectionName} ${grade.remainingSeats ?? 0}`)
      .join(' / ');
  }
  return `잔여 ${detail.remainingSeats ?? 0}석`;
}

export default function ShowScheduleSelection({
  showId,
  schedules = [],
  onBooking,
  layout = 'stacked',
  bookingButtonLabel = '다음 단계',
  hideBookingButton = false,
  initialSelectedScheduleKeyId,
  onSelectionChange,
}: ShowScheduleSelectionProps) {
  const firstSchedule = schedules[0] ?? null;
  const initialSchedule =
    schedules.find((schedule) => schedule.keyId === initialSelectedScheduleKeyId) ?? firstSchedule;
  const defaultDate = initialSchedule ? dayjs(initialSchedule.date).toDate() : undefined;

  const [selectedDate, setSelectedDate] = useState<Date | undefined>(defaultDate);
  const [selectedScheduleKey, setSelectedScheduleKey] = useState<number | null>(
    initialSchedule?.keyId ?? null,
  );

  const scheduleDatesSet = useMemo(() => new Set(schedules.map((schedule) => schedule.date)), [schedules]);

  const dateString = selectedDate ? formatDateOnly(selectedDate) : '';
  const { data: schedulesByDate = [] } = useQuery(getShowSchedulesByDate(showId, dateString));

  const scheduleDetailByKeyId = useMemo(() => {
    const map = new Map<number, ShowCatalog.ScheduleByDate>();
    schedulesByDate.forEach((schedule) => map.set(schedule.keyId, schedule));
    return map;
  }, [schedulesByDate]);

  const selectedDetail = selectedScheduleKey ? scheduleDetailByKeyId.get(selectedScheduleKey) : null;

  const schedulesOnSelectedDate = selectedDate
    ? [...schedules]
        .filter((schedule) => schedule.date === formatDateOnly(selectedDate))
        .sort((a, b) => a.session - b.session)
    : [];

  function handleSelectDate(date?: Date) {
    setSelectedDate(date);
    if (!date) {
      setSelectedScheduleKey(null);
      return;
    }

    const key = formatDateOnly(date);
    const firstOnDate = schedules.find((schedule) => schedule.date === key);
    setSelectedScheduleKey(firstOnDate?.keyId ?? null);
  }

  function handleBooking() {
    if (showId <= 0 || !selectedScheduleKey) return;
    onBooking?.({
      showId,
      scheduleKeyId: selectedScheduleKey,
    });
  }

  const isBookingDisabled = !selectedDate || !selectedScheduleKey || !!selectedDetail?.isSoldOut;

  useEffect(() => {
    if (!schedules.length) return;
    if (!initialSelectedScheduleKeyId) return;
    const matched = schedules.find((schedule) => schedule.keyId === initialSelectedScheduleKeyId);
    if (!matched) return;
    setSelectedScheduleKey(matched.keyId);
    setSelectedDate(dayjs(matched.date).toDate());
  }, [initialSelectedScheduleKeyId, schedules]);

  useEffect(() => {
    onSelectionChange?.({
      scheduleKeyId: selectedScheduleKey,
      isBookingDisabled,
    });
  }, [isBookingDisabled, onSelectionChange, selectedScheduleKey]);

  const calendarContent = (
    <>
      <p className={sectionTitleClass}>관람일</p>
      <Calendar
        mode="single"
        navLayout="around"
        selected={selectedDate}
        onSelect={handleSelectDate}
        defaultMonth={selectedDate ?? defaultDate}
        disabled={(date) =>
          dayjs(date).isBefore(dayjs(), DATE_UNIT.DAY) || !scheduleDatesSet.has(formatDateOnly(date))
        }
        className="[&_.rdp-weekday]:first-child:text-red-500 [&_.rdp-weekday]:last-child:text-muted-foreground rounded-lg border-0 p-0"
      />
    </>
  );

  const sessionContent = (
    <>
      <p className={sectionTitleClass}>회차</p>
      {schedulesOnSelectedDate.length > 0 ? (
        <>
          <ul className="grid grid-cols-2 gap-3">
            {schedulesOnSelectedDate.map((schedule) => {
              const isSelected = selectedScheduleKey === schedule.keyId;
              return (
                <li key={schedule.keyId}>
                  <button
                    type="button"
                    onClick={() => setSelectedScheduleKey(schedule.keyId)}
                    className={cn(
                      sessionButtonClass.base,
                      isSelected ? sessionButtonClass.selected : sessionButtonClass.default,
                    )}
                  >
                    {schedule.session}회 {schedule.time as string}
                  </button>
                </li>
              );
            })}
          </ul>
          {selectedScheduleKey != null && (
            <>
              <p className="text-muted-foreground mt-4 text-sm">{getRemainingSeatLabel(selectedDetail)}</p>
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
    </>
  );

  return (
    <div className="flex flex-col gap-4">
      {layout === 'split' ? (
        <div className="grid gap-4 lg:grid-cols-[minmax(0,340px)_minmax(0,1fr)]">
          <div className="bg-card rounded-lg border p-4">{calendarContent}</div>
          <div className="bg-card rounded-lg border p-4">{sessionContent}</div>
        </div>
      ) : (
        <div className="bg-card rounded-lg border p-4">
          {calendarContent}
          <hr className="border-border my-6" />
          {sessionContent}
        </div>
      )}

      {!hideBookingButton && (
        <Button className="w-full" disabled={isBookingDisabled} onClick={handleBooking}>
          {selectedDetail?.isSoldOut ? '매진' : bookingButtonLabel}
        </Button>
      )}
    </div>
  );
}
