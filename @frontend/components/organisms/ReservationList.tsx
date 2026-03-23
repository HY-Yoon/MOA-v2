'use client';

import { useState, useMemo, useEffect } from 'react';
import { BaseTable, type BaseTableColumn, type BaseTableFilterOption } from './BaseTable';
import { useQuery } from '@tanstack/react-query';
import dayjs from '@/plugins/dayjs';
import { DATE_FORMAT, DATE_UNIT } from '@/constants/common/dateFormat';
import {
  Button,
  Input,
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/atoms';
import { PAYMENT_STATUS_LABELS } from '@/constants/common/paymentStatus';
import { deriveFilterOptions } from '@/lib/admin/table-filter';
import { PaymentStatus, ReservationStatus } from '@shared/enums';
import { RESERVATION_STATUS_LABELS } from '@/constants/common/reservationStatus';
import Link from 'next/link';

type RangePreset = '7d' | '1m' | '3m' | '6m';

/** `YYYY-MM-DD` → 해당 일 00:00:00.000 (로컬) 기준 ISO8601 */
function toIso8601StartOfDay(dateOnly: string) {
  return dayjs(dateOnly).startOf(DATE_UNIT.DAY).toISOString();
}

/** `YYYY-MM-DD` → 해당 일 23:59:59.999 (로컬) 기준 ISO8601 */
function toIso8601EndOfDay(dateOnly: string) {
  return dayjs(dateOnly).endOf(DATE_UNIT.DAY).toISOString();
}

function ReservationListHeaderFilters({
  draftPreset,
  onChangePreset,
  draftDateType,
  onChangeDateType,
  draftStartDate,
  onChangeStartDate,
  draftEndDate,
  onChangeEndDate,
  onSubmit,
}: {
  draftPreset: RangePreset;
  onChangePreset: (preset: RangePreset) => void;
  draftDateType: Reservation.DateType;
  onChangeDateType: (dateType: Reservation.DateType) => void;
  draftStartDate: string;
  onChangeStartDate: (startDate: string) => void;
  draftEndDate: string;
  onChangeEndDate: (endDate: string) => void;
  onSubmit: () => void;
}) {
  const presetOptions: { value: RangePreset; label: string }[] = [
    { value: '7d', label: '1주일' },
    { value: '1m', label: '1개월' },
    { value: '3m', label: '3개월' },
    { value: '6m', label: '6개월' },
  ];

  return (
    <div className="flex flex-wrap items-center gap-2">
      <span className="text-muted-foreground/60 text-sm font-medium">기간별</span>
      <div
        className="mr-2 flex flex-wrap items-center gap-2 px-1 py-1"
        role="radiogroup"
        aria-label="기간별"
      >
        {presetOptions.map((opt) => (
          <button
            key={opt.value}
            type="button"
            role="radio"
            aria-checked={draftPreset === opt.value}
            onClick={() => onChangePreset(opt.value)}
            className={[
              'h-8 rounded-md border px-2 text-xs transition-colors',
              'focus-visible:ring-ring focus-visible:ring-2 focus-visible:ring-offset-2 focus-visible:outline-none',
              draftPreset === opt.value
                ? 'border-primary bg-primary text-primary-foreground'
                : 'border-input bg-background hover:bg-accent hover:text-accent-foreground',
            ].join(' ')}
          >
            {opt.label}
          </button>
        ))}
      </div>

      <Select
        value={draftDateType}
        onValueChange={(v) => onChangeDateType(v as Reservation.DateType)}
      >
        <SelectTrigger className="w-32">
          <SelectValue placeholder="날짜 기준" />
        </SelectTrigger>
        <SelectContent>
          <SelectItem value="RESERVATION">예매일</SelectItem>
          <SelectItem value="SHOW">공연일</SelectItem>
        </SelectContent>
      </Select>

      <div className="flex items-center gap-3">
        <Input
          type="date"
          value={draftStartDate}
          onChange={(e) => onChangeStartDate(e.target.value)}
          className="w-40"
        />
        <span className="text-muted-foreground text-sm">~</span>
        <Input
          type="date"
          value={draftEndDate}
          onChange={(e) => onChangeEndDate(e.target.value)}
          className="w-40"
        />
      </div>

      <Button size="sm" onClick={onSubmit}>
        조회
      </Button>
    </div>
  );
}

/** 목록 API 응답 (user/admin 공통 형식) */
export type ReservationListQueryResult = {
  content: Reservation.List[];
  totalPages: number;
};

/** useQuery에 넘길 옵션 (queryKey, queryFn 등) */
export type ReservationListQueryOptions = {
  queryKey: unknown[];
  queryFn: () => Promise<ReservationListQueryResult | null>;
};

/** 목록 조회 파라미터 (공통 + extraParams) */
export type ReservationListQueryParams = Reservation.ListParams &
  Record<string, string | number | undefined>;

export interface ReservationListProps {
  /** 상세 페이지 링크를 반환하는 함수 (예: (r) => `/my-page/reservations/${r.reservationId}`) */
  getDetailLink: (reservation: Reservation.List) => string;
  /** 목록 조회용 쿼리 옵션을 반환하는 함수 (user API / admin API 등 호출부에서 주입) */
  getListQuery: (params: ReservationListQueryParams) => ReservationListQueryOptions;
  /** API 요청 시 추가할 쿼리 파라미터 (예: 어드민에서 showId 전달) */
  extraParams?: Record<string, string | number | undefined>;
  /** 관리자 / 사용자 화면 여부 */
  variant?: 'user' | 'admin';
}

export default function ReservationList({
  getDetailLink,
  getListQuery,
  extraParams = {},
  variant,
}: ReservationListProps) {
  const isAdmin = variant === 'admin';

  // 페이지네이션
  const [page, setPage] = useState(0);
  const [pageSize, setPageSize] = useState(10);

  // 필터 (예매 상태 / 결제 상태)
  const [initialFilterOptions, setInitialFilterOptions] = useState<
    Record<string, BaseTableFilterOption[]>
  >({});
  const [filterValues, setFilterValues] = useState<Record<string, string[]>>({});

  // 임시 조회 필터 -> [조회] 버튼 눌렀을 때 실제 적용
  const [draftPreset, setDraftPreset] = useState<RangePreset>('7d');
  const [draftDateType, setDraftDateType] = useState<Reservation.DateType>('RESERVATION');
  const [draftStartDate, setDraftStartDate] = useState<string>(() =>
    dayjs().subtract(7, DATE_UNIT.DAY).format(DATE_FORMAT.DATE_ONLY),
  );
  const [draftEndDate, setDraftEndDate] = useState<string>(() =>
    dayjs().format(DATE_FORMAT.DATE_ONLY),
  );

  const [appliedDateType, setAppliedDateType] = useState<Reservation.DateType>(draftDateType);
  const [appliedStartDate, setAppliedStartDate] = useState<string>(draftStartDate);
  const [appliedEndDate, setAppliedEndDate] = useState<string>(draftEndDate);

  const params = useMemo(
    () => ({
      page,
      size: pageSize,
      ...(filterValues.status?.length && { status: filterValues.status[0] }),
      ...(filterValues.paymentStatus?.length && { paymentStatus: filterValues.paymentStatus[0] }),
      ...(appliedStartDate && {
        startDate: isAdmin ? toIso8601StartOfDay(appliedStartDate) : appliedStartDate,
      }),
      ...(appliedEndDate && {
        endDate: isAdmin ? toIso8601EndOfDay(appliedEndDate) : appliedEndDate,
      }),
      dateType: appliedDateType,
      ...extraParams,
    }),
    [
      page,
      pageSize,
      filterValues.status,
      filterValues.paymentStatus,
      appliedStartDate,
      isAdmin,
      appliedEndDate,
      appliedDateType,
      extraParams,
    ],
  );

  const { data, isFetching } = useQuery(getListQuery(params));

  const reservationList = data?.content ?? [];
  const totalPages = data?.totalPages ?? 0;

  // 필터 옵션 목록
  useEffect(() => {
    // 최초 전체 리스트 기준으로 한 번만 계산
    const hasNoFilter = Object.values(filterValues).every((arr) => !arr?.length);
    const hasOptions = Object.keys(initialFilterOptions).length > 0;

    if (hasNoFilter && reservationList.length > 0 && !hasOptions) {
      setInitialFilterOptions(
        deriveFilterOptions<Reservation.List>(reservationList, [
          {
            key: 'status',
            getValue: (s) => s.reservationStatus,
            getLabel: (v) => RESERVATION_STATUS_LABELS[v as ReservationStatus],
          },
          {
            key: 'paymentStatus',
            getValue: (s) => s.paymentStatus,
            getLabel: (v) => PAYMENT_STATUS_LABELS[v as PaymentStatus],
          },
        ]),
      );
    }
  }, [reservationList, filterValues, initialFilterOptions]);

  const columns: BaseTableColumn<Reservation.List>[] = [
    {
      key: 'reservationDate',
      label: '예매일',
      render: (r) => dayjs(r.reservationDate).format(DATE_FORMAT.DATE_ONLY),
    },
    {
      key: 'reservationNumber',
      label: '예매번호',
      render: (r) => (
        <Link
          href={getDetailLink(r)}
          className="cursor-pointer font-medium text-blue-600 underline"
        >
          {r.reservationNumber}
        </Link>
      ),
    },
    {
      key: 'showTitle',
      label: '공연명',
      render: (r) => (isAdmin ? r.showTitle : r.show.title),
    },
    {
      key: 'showDateTime',
      label: '공연일시',
      render: (r) => `${r.schedule.showDate} ${r.schedule.showTime}`,
    },
    {
      key: 'status',
      label: '예매상태',
      render: (r) => RESERVATION_STATUS_LABELS[r.reservationStatus],
      filter: true,
      filterOptions: initialFilterOptions.status,
    },
    {
      key: 'paymentStatus',
      label: '결제상태',
      render: (r) => PAYMENT_STATUS_LABELS[r.paymentStatus],
      filter: true,
      filterOptions: initialFilterOptions.paymentStatus,
    },
    {
      key: 'canCancel',
      label: '취소 가능',
      render: (r) => (r.canCancel ? '가능' : '불가'),
    },
  ];

  function handleSearch(searchKeyword: string, column: string) {
    void searchKeyword;
    void column;
  }

  function handleFilter(columnKey: string, selectedValues: string[]) {
    setFilterValues((prev) => ({ ...prev, [columnKey]: selectedValues }));
    setPage(0);
  }

  function applyPreset(preset: RangePreset) {
    setDraftPreset(preset);

    const now = dayjs();
    const start =
      preset === '7d'
        ? now.subtract(7, DATE_UNIT.DAY)
        : preset === '1m'
          ? now.subtract(1, DATE_UNIT.MONTH)
          : preset === '3m'
            ? now.subtract(3, DATE_UNIT.MONTH)
            : now.subtract(6, DATE_UNIT.MONTH);

    setDraftStartDate(start.format(DATE_FORMAT.DATE_ONLY));
    setDraftEndDate(now.format(DATE_FORMAT.DATE_ONLY));
  }

  function handleQueryChange() {
    setAppliedDateType(draftDateType);
    setAppliedStartDate(draftStartDate);
    setAppliedEndDate(draftEndDate);
    setPage(0);
  }

  function handlePageSizeChange(size: number) {
    setPageSize(size);
    setPage(0);
  }

  return (
    <BaseTable
      data={reservationList}
      columns={columns}
      showSearch={false}
      onSearch={handleSearch}
      filterValues={filterValues}
      onFilter={handleFilter}
      page={page}
      totalPages={totalPages}
      pageSize={pageSize}
      onPageChange={setPage}
      onPageSizeChange={handlePageSizeChange}
      isLoading={isFetching}
      emptyMessage="예매내역이 없습니다."
      headerActions={
        <ReservationListHeaderFilters
          draftPreset={draftPreset}
          onChangePreset={applyPreset}
          draftDateType={draftDateType}
          onChangeDateType={setDraftDateType}
          draftStartDate={draftStartDate}
          onChangeStartDate={(v) => setDraftStartDate(v)}
          draftEndDate={draftEndDate}
          onChangeEndDate={(v) => setDraftEndDate(v)}
          onSubmit={handleQueryChange}
        />
      }
    />
  );
}
