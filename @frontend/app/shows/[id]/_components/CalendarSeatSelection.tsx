'use client';

import { Button } from '@/components/atoms';
import { Calendar, MapPin } from 'lucide-react';

interface ScheduleOption {
  keyId: number;
  date: string;
  time: string;
  session?: number;
}

interface CalendarSeatSelectionProps {
  schedules?: ScheduleOption[];
  onSelectSchedule?: (keyId: number) => void;
  selectedScheduleKeyId?: number | null;
}

/**
 * 달력 및 좌석 선택 영역 (예매 오픈 후 날짜·회차 선택 → 좌석 맵)
 * 현재는 날짜/회차 목록 + 좌석 선택 플레이스홀더
 */
export default function CalendarSeatSelection({
  schedules = [],
  onSelectSchedule,
  selectedScheduleKeyId = null,
}: CalendarSeatSelectionProps) {
  const formatDate = (dateStr: string) => {
    try {
      const d = new Date(dateStr);
      return d.toLocaleDateString('ko-KR', { month: 'long', day: 'numeric', weekday: 'short' });
    } catch {
      return dateStr;
    }
  };

  return (
    <div className="flex flex-col gap-4">
      <div className="flex items-center gap-2 font-medium">
        <Calendar className="h-4 w-4" />
        <span>날짜·회차 선택</span>
      </div>
      {schedules.length > 0 ? (
        <ul className="grid gap-2">
          {schedules.map((s) => (
            <li key={s.keyId}>
              <button
                type="button"
                onClick={() => onSelectSchedule?.(s.keyId)}
                className={`w-full rounded-lg border px-3 py-2 text-left text-sm transition ${
                  selectedScheduleKeyId === s.keyId
                    ? 'border-primary bg-primary/10 text-primary'
                    : 'border-border hover:bg-muted/50'
                }`}
              >
                {formatDate(s.date)} {s.time}
                {s.session != null ? ` (${s.session}회차)` : ''}
              </button>
            </li>
          ))}
        </ul>
      ) : (
        <p className="text-muted-foreground rounded-lg border border-dashed p-4 text-center text-sm">
          등록된 회차가 없습니다.
        </p>
      )}

      <div className="flex items-center gap-2 font-medium">
        <MapPin className="h-4 w-4" />
        <span>좌석 선택</span>
      </div>
      <div className="min-h-[120px] rounded-lg border border-dashed bg-muted/30 p-6 text-center text-sm text-muted-foreground">
        좌석 배치도 영역 (추후 구현)
      </div>
      <Button className="w-full" disabled={selectedScheduleKeyId == null}>
        선택 완료
      </Button>
    </div>
  );
}
