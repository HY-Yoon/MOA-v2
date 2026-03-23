'use client';

import {
  Button,
  Skeleton,
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/atoms';
import { FormField } from '@/components/molecules';
import StatusBadge from '@/components/molecules/StatusBadge';
import { GENRE_LABELS, REGION_LABELS } from '@/constants/common';
import { PanelRightOpen } from 'lucide-react';
import { useMemo, useState } from 'react';
import { DATE_FORMAT } from '@/constants/common/dateFormat';
import dayjs from 'dayjs';
import ShowSeatStatusPanel from './ShowSeatStatusPanel';

interface Props {
  data?: Show.Detail;
  loading?: boolean;
}

export default function ShowDetailBasicInfo({ data, loading }: Props) {
  const [panelOpen, setPanelOpen] = useState(false);
  const [panelTarget, setPanelTarget] = useState<{
    scheduleId: number;
    showDate: string;
  } | null>(null);

  const genreLabel = useMemo(
    () => (data?.genre ? (GENRE_LABELS[data.genre] ?? data.genre) : '-'),
    [data],
  );
  const regionLabel = useMemo(
    () => (data?.region ? (REGION_LABELS[data.region] ?? data.region) : '-'),
    [data],
  );

  function handlePanelOpen(schedule: Show.Schedules) {
    setPanelTarget({ scheduleId: schedule.scheduleId, showDate: schedule.showDate });
    setPanelOpen(true);
  }

  function handlePanelOpenChange(next: boolean) {
    setPanelOpen(next);
    if (!next) setPanelTarget(null);
  }

  if (loading) {
    return (
      <div className="space-y-6">
        <Skeleton className="h-10 w-full" />
        <Skeleton className="h-10 w-full" />
      </div>
    );
  }

  if (!data) {
    return <p className="text-slate-500">데이터를 불러올 수 없습니다.</p>;
  }

  return (
    <div className="space-y-6">
      <div className="grid grid-cols-2 gap-6">
        <div className="flex justify-center">
          <img
            src={data.posterUrl}
            alt={data.title}
            className="h-80 w-auto min-w-72 rounded border object-cover sm:h-96 sm:w-80 sm:min-w-80"
          />
        </div>

        <div className="space-y-6">
          <FormField label="상태" htmlFor="status">
            <div className="flex gap-2">
              {data.status && <StatusBadge type="show" status={data.status} />}
            </div>
          </FormField>

          <FormField label="판매 허용" htmlFor="saleStatus">
            <div className="flex gap-2">
              {data.saleStatus && <StatusBadge type="sale" status={data.saleStatus} />}
            </div>
          </FormField>

          <FormField label="제목" htmlFor="title">
            <p className="text-slate-800">{data.title}</p>
          </FormField>

          <FormField label="장르" htmlFor="genre">
            <p className="text-slate-800">{genreLabel}</p>
          </FormField>

          <FormField label="지역" htmlFor="region">
            <p className="text-slate-800">{regionLabel}</p>
          </FormField>

          <FormField label="장소" htmlFor="venueName">
            <p className="text-slate-800">{data.venueName}</p>
          </FormField>

          <FormField label="공연장" htmlFor="hallName">
            <p className="text-slate-800">{data.hallName}</p>
          </FormField>

          <FormField label="관람시간" htmlFor="runningTime">
            <p className="text-slate-800">{data.runningTime}</p>
          </FormField>

          <FormField label="출연진" htmlFor="cast">
            <p className="whitespace-pre-wrap text-slate-800">{data.cast}</p>
          </FormField>
        </div>
      </div>

      <FormField label="예매 일정" htmlFor="saleStartDate">
        <p className="text-slate-800">{`${dayjs(data.saleStartDate).format(DATE_FORMAT.DATE_ONLY)} ~ ${dayjs(data.saleEndDate).format(DATE_FORMAT.DATE_ONLY)}`}</p>
      </FormField>

      <FormField label="공연 일정" htmlFor="schedules">
        <div className="overflow-hidden rounded-lg border border-slate-200">
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead className="text-center">공연일</TableHead>
                <TableHead className="text-center">공연 시간</TableHead>
                <TableHead className="text-center">티켓 오픈일</TableHead>
                <TableHead className="text-center">잔여석/총 좌석</TableHead>
                <TableHead className="w-14 pr-4 text-center">좌석 현황</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {data.schedules?.map((schedule: Show.Schedules) => (
                <TableRow key={schedule.scheduleId}>
                  <TableCell className="text-center">{schedule.showDate}</TableCell>
                  <TableCell className="text-center">{schedule.showTime}</TableCell>
                  <TableCell className="text-center">
                    {dayjs(schedule.ticketOpenTime).format(DATE_FORMAT.FULL)}
                  </TableCell>
                  <TableCell className="text-center">
                    {schedule.remainingSeats} / {schedule.totalSeats}
                  </TableCell>
                  <TableCell className="pr-4 text-center">
                    <Button
                      type="button"
                      variant="ghost"
                      size="icon"
                      onClick={() => handlePanelOpen(schedule)}
                      aria-label="좌석 현황 보기"
                    >
                      <PanelRightOpen className="h-4 w-4" />
                    </Button>
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </div>
      </FormField>

      <FormField label="상세 이미지" htmlFor="detailImages">
        {data.detailImages?.length ? (
          <div className="flex flex-col gap-4">
            {data.detailImages.map((image: Show.DetailImages) => (
              <img
                key={image.id}
                src={image.url}
                alt={`상세 ${image.id}`}
                className="h-auto max-w-[60%] rounded border object-contain"
              />
            ))}
          </div>
        ) : (
          <span className="text-slate-400">-</span>
        )}
      </FormField>

      <ShowSeatStatusPanel
        showId={data.id}
        scheduleId={panelTarget?.scheduleId ?? null}
        showDate={panelTarget?.showDate ?? null}
        open={panelOpen}
        onOpenChange={handlePanelOpenChange}
      />
    </div>
  );
}
