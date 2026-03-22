'use client';

import {
  getScheduleSeatMap,
  getScheduleSeats,
  type ScheduleSeatMapSeat,
  type ScheduleSeatMapSection,
} from '@/lib/api/reservation';
import { useQuery } from '@tanstack/react-query';
import { useCallback, useEffect, useMemo, useRef, useState } from 'react';

export interface SelectedSeatInfo {
  scheduleSeatId: number;
  seatId: string;
  sectionId: string;
  sectionName: string;
  row: string;
  number: number;
  price: number;
}

interface SeatMapCanvasProps {
  scheduleId: number;
  disabled?: boolean;
  selectedSeatIds?: number[];
  onSelectedSeatIdsChange?: (scheduleSeatIds: number[]) => void;
  onSelectedSeatsChange?: (seats: SelectedSeatInfo[]) => void;
  onSectionPriceMapChange?: (sectionPriceMap: Record<string, number>) => void;
}

function hexToRgba(hex: string, alpha = 1) {
  const cleaned = hex.replace('#', '');
  const full = cleaned.length === 3 ? cleaned.split('').map((char) => char + char).join('') : cleaned;
  const value = Number.parseInt(full, 16);
  const r = (value >> 16) & 255;
  const g = (value >> 8) & 255;
  const b = value & 255;
  return `rgba(${r}, ${g}, ${b}, ${alpha})`;
}

function normalizeSeatKey(value?: string | null) {
  if (!value) return '';
  return value.trim().toUpperCase();
}

function createScopedSeatKey(sectionId?: string, seatKey?: string) {
  const normalizedSection = normalizeSeatKey(sectionId);
  const normalizedSeatKey = normalizeSeatKey(seatKey);
  if (!normalizedSection || !normalizedSeatKey) return '';
  return `${normalizedSection}::${normalizedSeatKey}`;
}

function isSeatReservedStatus(status?: string) {
  const normalizedStatus = String(status ?? '').toUpperCase();
  return (
    normalizedStatus === 'LOCKED' ||
    normalizedStatus === 'RESERVED' ||
    normalizedStatus === 'BLOCKED' ||
    normalizedStatus === 'BOOKED' ||
    normalizedStatus === 'UNAVAILABLE' ||
    normalizedStatus === 'SOLD' ||
    normalizedStatus === 'SELECTED_BY_OTHER'
  );
}

function drawCheckMark(
  context: CanvasRenderingContext2D,
  centerX: number,
  centerY: number,
  radius: number,
  color: string,
) {
  context.beginPath();
  context.lineCap = 'round';
  context.lineJoin = 'round';
  context.strokeStyle = color;
  context.lineWidth = Math.max(1.8, radius * 0.28);
  context.moveTo(centerX - radius * 0.45, centerY + radius * 0.05);
  context.lineTo(centerX - radius * 0.1, centerY + radius * 0.4);
  context.lineTo(centerX + radius * 0.5, centerY - radius * 0.35);
  context.stroke();
}

function resolveSeatPositions(
  seatMap: Awaited<ReturnType<ReturnType<typeof getScheduleSeatMap>['queryFn']>>,
): ScheduleSeatMapSeat[] {
  return seatMap.seats ?? [];
}

export default function SeatMapCanvas({
  scheduleId,
  disabled = false,
  selectedSeatIds,
  onSelectedSeatIdsChange,
  onSelectedSeatsChange,
  onSectionPriceMapChange,
}: SeatMapCanvasProps) {
  const wrapperRef = useRef<HTMLDivElement | null>(null);
  const canvasRef = useRef<HTMLCanvasElement | null>(null);
  const [zoom, setZoom] = useState(1);
  const [internalSelectedSeatIds, setInternalSelectedSeatIds] = useState<number[]>([]);
  const [isHoverSelectableSeat, setIsHoverSelectableSeat] = useState(false);
  const isControlled = selectedSeatIds !== undefined;
  const effectiveSelectedSeatIds = isControlled ? selectedSeatIds : internalSelectedSeatIds;

  const updateSelectedSeatIds = useCallback((nextSeatIds: number[]) => {
    if (!isControlled) {
      setInternalSelectedSeatIds(nextSeatIds);
    }
    onSelectedSeatIdsChange?.(nextSeatIds);
  }, [isControlled, onSelectedSeatIdsChange]);

  const { data: seatMapData, isLoading, isError } = useQuery(getScheduleSeatMap(scheduleId));
  const { data: seatStatusData } = useQuery(getScheduleSeats(scheduleId));

  const seats = useMemo(() => {
    if (!seatMapData) return [];
    const layoutSeats = resolveSeatPositions(seatMapData);
    if (!seatStatusData?.seats?.length) return layoutSeats;

    const detailedSeats = seatStatusData.seats.flatMap((seat) => {
      const value = seat as Record<string, unknown>;
      const seatId = typeof value.seatId === 'string' ? value.seatId : undefined;
      const sectionId =
        typeof value.sectionId === 'string'
          ? value.sectionId
          : typeof value.section === 'string'
            ? value.section
            : undefined;
      const row = typeof value.row === 'string' ? value.row : undefined;
      const number = typeof value.number === 'number' ? value.number : undefined;
      const x = typeof value.x === 'number' ? value.x : undefined;
      const y = typeof value.y === 'number' ? value.y : undefined;
      const status = typeof value.status === 'string' ? value.status : undefined;

      if (typeof x !== 'number' || typeof y !== 'number') return [];
      if (!seatId || !sectionId || !row || typeof number !== 'number') return [];

      const parsed: ScheduleSeatMapSeat = {
        scheduleSeatId: typeof value.scheduleSeatId === 'number' ? value.scheduleSeatId : undefined,
        seatId,
        sectionId,
        row,
        number,
        x,
        y,
        status,
      };
      return [parsed];
    });

    if (detailedSeats.length > 0) {
      return detailedSeats;
    }

    const detailedSeatByKey = new Map<string, Partial<ScheduleSeatMapSeat>>();
    const unavailableSeatKeySet = new Set<string>();

    seatStatusData.seats.forEach((seat) => {
      const value = seat as Record<string, unknown>;
      const seatId = typeof value.seatId === 'string' ? value.seatId : undefined;
      const seatNumber = typeof value.seatNumber === 'string' ? value.seatNumber : undefined;
      const sectionId =
        typeof value.sectionId === 'string'
          ? value.sectionId
          : typeof value.section === 'string'
            ? value.section
            : undefined;
      const row = typeof value.row === 'string' ? value.row : undefined;
      const number = typeof value.number === 'number' ? value.number : undefined;
      const status = typeof value.status === 'string' ? value.status : undefined;
      const x = typeof value.x === 'number' ? value.x : undefined;
      const y = typeof value.y === 'number' ? value.y : undefined;
      const rowNumber = row && typeof number === 'number' ? `${row}-${number}` : '';

      const keys = [
        createScopedSeatKey(sectionId, seatId),
        createScopedSeatKey(sectionId, seatNumber),
        createScopedSeatKey(sectionId, rowNumber),
        normalizeSeatKey(seatId),
        normalizeSeatKey(seatNumber),
        normalizeSeatKey(rowNumber),
      ].filter(Boolean);

      keys.forEach((key) => {
        if (!key) return;
        if (status) {
          const detailed: Partial<ScheduleSeatMapSeat> = { status };
          if (sectionId) detailed.sectionId = sectionId;
          if (row) detailed.row = row;
          if (typeof number === 'number') detailed.number = number;
          if (typeof x === 'number') detailed.x = x;
          if (typeof y === 'number') detailed.y = y;
          detailedSeatByKey.set(key, detailed);
          return;
        }
        // section + seatNumber 응답은 예약 좌석 목록으로 간주
        unavailableSeatKeySet.add(key);
      });
    });

    console.log('seatStatusData.seats',seatStatusData.seats);

    return layoutSeats.map((layoutSeat) => {
      const seatIdKey = normalizeSeatKey(layoutSeat.seatId);
      const rowNumberKey = normalizeSeatKey(`${layoutSeat.row}-${layoutSeat.number}`);
      const scopedSeatIdKey = createScopedSeatKey(layoutSeat.sectionId, layoutSeat.seatId);
      const scopedRowNumberKey = createScopedSeatKey(
        layoutSeat.sectionId,
        `${layoutSeat.row}-${layoutSeat.number}`,
      );

      const detailedSeat =
        detailedSeatByKey.get(scopedSeatIdKey) ??
        detailedSeatByKey.get(scopedRowNumberKey) ??
        detailedSeatByKey.get(seatIdKey) ??
        detailedSeatByKey.get(rowNumberKey);

      const isUnavailable =
        unavailableSeatKeySet.has(scopedSeatIdKey) ||
        unavailableSeatKeySet.has(scopedRowNumberKey) ||
        unavailableSeatKeySet.has(seatIdKey) ||
        unavailableSeatKeySet.has(rowNumberKey);

      return {
        ...layoutSeat,
        ...detailedSeat,
        status: detailedSeat?.status ?? (isUnavailable ? 'RESERVED' : layoutSeat.status),
      };
    });
  }, [seatMapData, seatStatusData]);
  const maxSelectable = seatStatusData?.maxSelectable ?? 6;
  const selectedScheduleSeatIdSet = useMemo(
    () => new Set(effectiveSelectedSeatIds),
    [effectiveSelectedSeatIds],
  );

  const sectionMap = useMemo(() => {
    return new Map((seatMapData?.sections ?? []).map((section) => [section.sectionId, section]));
  }, [seatMapData?.sections]);
  const sectionByAnyKey = useMemo(() => {
    const map = new Map<string, ScheduleSeatMapSection>();
    (seatMapData?.sections ?? []).forEach((section) => {
      map.set(normalizeSeatKey(section.sectionId), section);
      map.set(normalizeSeatKey(section.name), section);
    });
    return map;
  }, [seatMapData?.sections]);

  useEffect(() => {
    const sectionPriceMapEntries = (seatMapData?.sections ?? []).flatMap((section) => [
      [section.sectionId, section.price ?? 0] as const,
      [section.name, section.price ?? 0] as const,
    ]);
    const sectionPriceMap = Object.fromEntries(sectionPriceMapEntries);
    onSectionPriceMapChange?.(sectionPriceMap);
  }, [onSectionPriceMapChange, seatMapData?.sections]);

  useEffect(() => {
    updateSelectedSeatIds([]);
  }, [scheduleId, updateSelectedSeatIds]);

  useEffect(() => {
    const seatByScheduleSeatId = new Map(
      seats
        .filter((seat): seat is ScheduleSeatMapSeat & { scheduleSeatId: number } => typeof seat.scheduleSeatId === 'number')
        .map((seat) => [seat.scheduleSeatId, seat]),
    );

    const infos: SelectedSeatInfo[] = effectiveSelectedSeatIds.flatMap((scheduleSeatId) => {
      const seat = seatByScheduleSeatId.get(scheduleSeatId);
      if (!seat) return [];
      const section =
        sectionByAnyKey.get(normalizeSeatKey(seat.sectionId)) ?? sectionMap.get(seat.sectionId);
      return [
        {
          scheduleSeatId: seat.scheduleSeatId,
          seatId: seat.seatId,
          sectionId: seat.sectionId,
          sectionName: section?.name ?? seat.sectionId,
          row: seat.row,
          number: seat.number,
          price: section?.price ?? 0,
        },
      ];
    });
    onSelectedSeatsChange?.(infos);
  }, [effectiveSelectedSeatIds, onSelectedSeatsChange, sectionByAnyKey, sectionMap, seats]);

  useEffect(() => {
    const canvas = canvasRef.current;
    const wrapper = wrapperRef.current;
    if (!canvas || !wrapper || !seatMapData) return;

    const dpr = window.devicePixelRatio || 1;
    const width = wrapper.clientWidth;
    const height = wrapper.clientHeight;
    canvas.width = width * dpr;
    canvas.height = height * dpr;
    canvas.style.width = `${width}px`;
    canvas.style.height = `${height}px`;

    const context = canvas.getContext('2d');
    if (!context) return;

    context.setTransform(dpr, 0, 0, dpr, 0, 0);
    context.clearRect(0, 0, width, height);

    const mapWidth = seatMapData.canvas.width;
    const mapHeight = seatMapData.canvas.height;
    const baseScale = Math.min(width / mapWidth, height / mapHeight) * 0.92;
    const scale = baseScale * zoom;
    const offsetX = (width - mapWidth * scale) / 2;
    const offsetY = (height - mapHeight * scale) / 2;

    context.fillStyle = '#f1f5f9';
    context.fillRect(0, 0, width, height);

    const stageWidth = mapWidth * 0.28 * scale;
    const stageHeight = 64 * scale;
    const stageX = offsetX + mapWidth * scale * 0.36;
    const stageY = offsetY + 32 * scale;
    context.fillStyle = '#c2c8d3';
    context.beginPath();
    context.roundRect(stageX, stageY, stageWidth, stageHeight, 10);
    context.fill();
    context.fillStyle = '#ffffff';
    context.font = `${Math.max(12, 18 * scale)}px sans-serif`;
    context.textAlign = 'center';
    context.textBaseline = 'middle';
    context.fillText('STAGE', stageX + stageWidth / 2, stageY + stageHeight / 2);

    const radius = Math.max(4, seatMapData.canvas.seatRadius * scale);
    const firstSeatByRow = new Map<string, ScheduleSeatMapSeat>();
    seats.forEach((seat) => {
      const key = `${seat.sectionId}::${seat.row}`;
      const current = firstSeatByRow.get(key);
      if (!current || seat.x < current.x) {
        firstSeatByRow.set(key, seat);
      }
    });

    context.fillStyle = '#64748b';
    context.font = `${Math.max(10, 14 * scale)}px sans-serif`;
    context.textAlign = 'right';
    context.textBaseline = 'middle';
    firstSeatByRow.forEach((seat) => {
      const x = offsetX + seat.x * scale - radius * 1.8;
      const y = offsetY + seat.y * scale;
      context.fillText(seat.row, x, y);
    });

    seats.forEach((seat) => {
      const section =
        sectionByAnyKey.get(normalizeSeatKey(seat.sectionId)) ?? sectionMap.get(seat.sectionId);
      const defaultColor = '#7c6cf3';
      const baseColor = section?.color ?? defaultColor;
      const isSelected =
        typeof seat.scheduleSeatId === 'number' && selectedScheduleSeatIdSet.has(seat.scheduleSeatId);
      const normalizedStatus = String(seat.status ?? '').toUpperCase();
      const isReserved =
        normalizedStatus === 'LOCKED' ||
        normalizedStatus === 'RESERVED' ||
        normalizedStatus === 'BLOCKED' ||
        normalizedStatus === 'BOOKED' ||
        normalizedStatus === 'UNAVAILABLE' ||
        normalizedStatus === 'SOLD' ||
        normalizedStatus === 'SELECTED_BY_OTHER';
      const x = offsetX + seat.x * scale;
      const y = offsetY + seat.y * scale;

      if (isReserved) {
        context.beginPath();
        context.arc(x, y, radius, 0, Math.PI * 2);
        context.fillStyle = 'rgba(203, 213, 225, 0.55)';
        context.fill();
        context.strokeStyle = 'rgba(148, 163, 184, 0.35)';
        context.lineWidth = Math.max(1, radius * 0.14);
        context.stroke();
        return;
      }

      if (isSelected) {
        // 선택 상태: 외곽 글로우 + 내부 채움 + 체크 아이콘
        context.beginPath();
        context.arc(x, y, radius * 1.45, 0, Math.PI * 2);
        context.fillStyle = hexToRgba(baseColor, 0.24);
        context.fill();

        context.beginPath();
        context.arc(x, y, radius * 1.15, 0, Math.PI * 2);
        context.fillStyle = hexToRgba(baseColor, 0.38);
        context.fill();

        context.beginPath();
        context.arc(x, y, radius, 0, Math.PI * 2);
        context.fillStyle = hexToRgba(baseColor, 0.92);
        context.fill();
        context.strokeStyle = hexToRgba(baseColor, 1);
        context.lineWidth = Math.max(1.2, radius * 0.16);
        context.stroke();

        drawCheckMark(context, x, y, radius * 0.68, '#ffffff');
        return;
      }

      // 기본 상태: 흰 배경 + 섹션 컬러 외곽선
      context.beginPath();
      context.arc(x, y, radius, 0, Math.PI * 2);
      context.fillStyle = 'rgba(255, 255, 255, 0.92)';
      context.fill();
      context.strokeStyle = hexToRgba(baseColor, 0.95);
      context.lineWidth = Math.max(1.2, radius * 0.16);
      context.stroke();
    });
  }, [
    effectiveSelectedSeatIds,
    seatMapData,
    sectionByAnyKey,
    sectionMap,
    seats,
    selectedScheduleSeatIdSet,
    zoom,
  ]);

  const getSelectableSeatAtPoint = (pointX: number, pointY: number) => {
    if (!seatMapData || !wrapperRef.current) return undefined;

    const rect = wrapperRef.current.getBoundingClientRect();
    const mapWidth = seatMapData.canvas.width;
    const mapHeight = seatMapData.canvas.height;
    const baseScale = Math.min(rect.width / mapWidth, rect.height / mapHeight) * 0.92;
    const scale = baseScale * zoom;
    const offsetX = (rect.width - mapWidth * scale) / 2;
    const offsetY = (rect.height - mapHeight * scale) / 2;
    const radius = Math.max(4, seatMapData.canvas.seatRadius * scale);

    return seats.find((seat) => {
      if (typeof seat.scheduleSeatId !== 'number') return false;
      if (isSeatReservedStatus(seat.status)) return false;
      const x = offsetX + seat.x * scale;
      const y = offsetY + seat.y * scale;
      const dx = x - pointX;
      const dy = y - pointY;
      return Math.sqrt(dx * dx + dy * dy) <= radius * 1.2;
    });
  };

  const handleCanvasMouseMove = (event: React.MouseEvent<HTMLCanvasElement>) => {
    if (disabled || !wrapperRef.current) {
      if (isHoverSelectableSeat) setIsHoverSelectableSeat(false);
      return;
    }
    const rect = wrapperRef.current.getBoundingClientRect();
    const hoverX = event.clientX - rect.left;
    const hoverY = event.clientY - rect.top;
    const isSelectable = !!getSelectableSeatAtPoint(hoverX, hoverY);
    if (isSelectable !== isHoverSelectableSeat) {
      setIsHoverSelectableSeat(isSelectable);
    }
  };

  const handleCanvasMouseLeave = () => {
    if (isHoverSelectableSeat) {
      setIsHoverSelectableSeat(false);
    }
  };

  const handleCanvasClick = (event: React.MouseEvent<HTMLCanvasElement>) => {
    if (disabled || !seatMapData || !wrapperRef.current) return;

    const rect = wrapperRef.current.getBoundingClientRect();
    const clickX = event.clientX - rect.left;
    const clickY = event.clientY - rect.top;
    const targetSeat = getSelectableSeatAtPoint(clickX, clickY);

    if (!targetSeat) return;

    if (typeof targetSeat.scheduleSeatId !== 'number') return;
    const isAlreadySelected = selectedScheduleSeatIdSet.has(targetSeat.scheduleSeatId);

    const nextSeatIds = isAlreadySelected
      ? effectiveSelectedSeatIds.filter((selectedId) => selectedId !== targetSeat.scheduleSeatId)
      : effectiveSelectedSeatIds.length >= maxSelectable
        ? effectiveSelectedSeatIds
        : [...effectiveSelectedSeatIds, targetSeat.scheduleSeatId];

    updateSelectedSeatIds(nextSeatIds);
  };

  if (isLoading) {
    return <div className="flex h-full items-center justify-center text-sm text-slate-500">좌석 정보를 불러오는 중입니다...</div>;
  }

  if (isError || !seatMapData) {
    return <div className="flex h-full items-center justify-center text-sm text-red-500">좌석 정보를 불러오지 못했습니다.</div>;
  }

  return (
    <>
     <div ref={wrapperRef} className="h-full w-full">
        <canvas
          ref={canvasRef}
          className={`h-full w-full ${isHoverSelectableSeat ? 'cursor-pointer' : 'cursor-default'}`}
          onClick={handleCanvasClick}
          onMouseMove={handleCanvasMouseMove}
          onMouseLeave={handleCanvasMouseLeave}
        />
      </div>
      <div className="absolute bottom-4 right-4 flex flex-col overflow-hidden rounded-md border border-slate-300 bg-white shadow-sm">
        <button
          type="button"
          className="h-10 w-10 text-lg text-slate-700 hover:bg-slate-50"
          onClick={() => setZoom((prev) => Math.min(1.8, Number((prev + 0.1).toFixed(2))))}
        >
          +
        </button>
        <button
          type="button"
          className="h-10 w-10 border-t border-slate-300 text-lg text-slate-700 hover:bg-slate-50"
          onClick={() => setZoom((prev) => Math.max(0.7, Number((prev - 0.1).toFixed(2))))}
        >
          -
        </button>
      </div>
      {disabled && <div className="pointer-events-none absolute inset-0 bg-black/12" />} 
    </>
  );
}
