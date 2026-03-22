'use client';

import { Button } from '@/components/atoms';
import { ShowScheduleSelection } from '@/components/molecules';
import { X } from 'lucide-react';
import { useMemo, useState } from 'react';
import type { SelectedSeatInfo } from './SeatMapCanvas';

interface ReservationScheduleSidePanelProps {
  showId: number;
  schedules: ShowCatalog.Schedule[];
  mode: 'schedule' | 'seat';
  selectedSeats: SelectedSeatInfo[];
  sectionPriceMap: Record<string, number>;
  initialSelectedScheduleKeyId?: number;
  onConfirmSchedule: (scheduleKeyId: number) => void;
  onConfirmSeatSelection: () => void;
  onCloseSchedulePanel: () => void;
  onRemoveSeat: (scheduleSeatId: number) => void;
  onClearSeats: () => void;
  isSubmittingSeatConfirm?: boolean;
}

export default function ReservationScheduleSidePanel({
  showId,
  schedules,
  mode,
  selectedSeats,
  sectionPriceMap,
  initialSelectedScheduleKeyId,
  onConfirmSchedule,
  onConfirmSeatSelection,
  onCloseSchedulePanel,
  onRemoveSeat,
  onClearSeats,
  isSubmittingSeatConfirm = false,
}: ReservationScheduleSidePanelProps) {
  const [selectedScheduleKeyId, setSelectedScheduleKeyId] = useState<number | null>(
    initialSelectedScheduleKeyId ?? null,
  );
  const [isScheduleSelectionDisabled, setIsScheduleSelectionDisabled] = useState(true);
  const hasSelectedSeats = selectedSeats.length > 0;
  const numberFormatter = new Intl.NumberFormat('ko-KR');
  const sectionPriceById = useMemo(() => new Map(Object.entries(sectionPriceMap)), [sectionPriceMap]);

  const isCompleteDisabled = mode === 'schedule' ? isScheduleSelectionDisabled : !hasSelectedSeats;

  return (
    <aside className="flex h-full min-h-0 flex-col border-l border-slate-200 bg-white">
      <div className="shrink-0 border-b border-slate-200 px-5 py-4">
        <div className="flex items-center justify-between">
          <p className="text-xl font-semibold text-slate-900">
            {mode === 'schedule' ? '일정 선택' : `선택 좌석 ${selectedSeats.length}`}
          </p>
          {mode === 'seat' ? (
            <button
              type="button"
              onClick={onClearSeats}
              className="text-sm text-slate-400 transition-colors hover:text-slate-600 cursor-pointer"
            >
              전체삭제
            </button>
          ) : (
            <button
              type="button"
              aria-label="좌석 선택으로 돌아가기"
              onClick={onCloseSchedulePanel}
              className="rounded-md p-1 text-slate-500 transition-colors hover:bg-slate-100 hover:text-slate-800"
            >
              <X className="h-5 w-5" />
            </button>
          )}
        </div>
      </div>

      <div className="min-h-0 flex-1 overflow-y-auto p-5">
        {mode === 'schedule' ? (
          <ShowScheduleSelection
            showId={showId}
            schedules={schedules}
            hideBookingButton
            initialSelectedScheduleKeyId={initialSelectedScheduleKeyId}
            onSelectionChange={({ scheduleKeyId, isBookingDisabled }) => {
              setSelectedScheduleKeyId(scheduleKeyId);
              setIsScheduleSelectionDisabled(isBookingDisabled);
            }}
          />
        ) : (
          <div className="space-y-4">
            {hasSelectedSeats ? (
              <ul className="divide-y divide-slate-200">
                {selectedSeats.map((seat) => (
                  <li key={seat.scheduleSeatId} className="flex items-center justify-between py-4">
                    <div>
                      <p className="text-base font-semibold text-slate-900">{seat.sectionName}구역</p>
                      <p className="mt-1 text-sm text-slate-500">
                        {seat.row}열 {seat.number}번
                      </p>
                    </div>
                    <div className="ml-3 flex items-center gap-3">
                      <p className="text-xl font-semibold text-slate-900">
                        {numberFormatter.format(sectionPriceById.get(seat.sectionId) ?? 0)}원
                      </p>
                      <button
                        type="button"
                        onClick={() => onRemoveSeat(seat.scheduleSeatId)}
                        className="text-slate-400 transition-colors hover:text-slate-700"
                      >
                        <X className="h-4 w-4" />
                      </button>
                    </div>
                  </li>
                ))}
              </ul>
            ) : (
              <p className="mt-1 text-sm text-slate-500">선택한 좌석이 없습니다.</p>
            )}
          </div>
        )}
      </div>

      <div className="shrink-0 border-t border-slate-200 p-5">
        <Button
          className="w-full"
          disabled={isCompleteDisabled || isSubmittingSeatConfirm}
          onClick={() => {
            if (mode === 'schedule') {
              if (!selectedScheduleKeyId) return;
              onConfirmSchedule(selectedScheduleKeyId);
              return;
            }
            onConfirmSeatSelection();
          }}
        >
          {isSubmittingSeatConfirm ? '처리중...' : '선택완료'}
        </Button>
      </div>
    </aside>
  );
}
