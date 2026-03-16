'use client';

import { Button, Skeleton } from '@/components/atoms';
import { PageCard } from '@/components/molecules/PageCard';
import { BaseTable, type BaseTableColumn, type BaseTableFilterOption } from '@/components/organisms';
import { REGION_LABELS } from '@/constants/common';
import { ADMIN_ROUTES } from '@/constants/route/adminRoutes';
import { deriveFilterOptions } from '@/lib/admin/table-filter';
import { getSeatMapList } from '@/lib/api/admin/seat';
import { useQuery } from '@tanstack/react-query';
import dayjs from '@/plugins/dayjs';
import { DATE_FORMAT } from '@/constants/common/dateFormat';
import { useRouter } from 'next/navigation';
import { useEffect, useMemo, useState } from 'react';

export default function SeatList() {
  const router = useRouter();
  const [mounted, setMounted] = useState(false);
  const [page, setPage] = useState(0);
  const [pageSize, setPageSize] = useState(10);
  const [keyword, setKeyword] = useState('');
  const [searchColumn, setSearchColumn] = useState('all');
  const [initialFilterOptions, setInitialFilterOptions] = useState<
    Record<string, BaseTableFilterOption[]>
  >({});
  const [filterValues, setFilterValues] = useState<Record<string, string[]>>({});

  const params: Seat.ListParams = useMemo(
    () => ({
      page,
      size: pageSize,
      ...(keyword.trim() && searchColumn === 'hallName' && { hallName: keyword.trim() }),
      ...(keyword.trim() &&
        (searchColumn === 'all' || searchColumn === 'venueName') && { venueName: keyword.trim() }),
      ...(filterValues.region?.length && { region: filterValues.region[0] }),
    }),
    [page, pageSize, keyword, searchColumn, filterValues],
  );

  const { data, isFetching } = useQuery(getSeatMapList(params));
  const seatList = data?.content ?? [];
  const totalPages = data?.totalPages ?? 0;

  useEffect(() => {
    setMounted(true);
  }, []);

  useEffect(() => {
    const hasNoFilter = Object.values(filterValues).every((arr) => !arr?.length);
    const hasOptions = Object.keys(initialFilterOptions).length > 0;

    if (hasNoFilter && seatList.length > 0 && !hasOptions) {
      setInitialFilterOptions(
        deriveFilterOptions<Seat.List>(seatList, [
          {
            key: 'region',
            getValue: (seat) => seat.region,
            getLabel: (value) => REGION_LABELS[value as keyof typeof REGION_LABELS] ?? value,
          },
        ]),
      );
    }
  }, [seatList, filterValues, initialFilterOptions]);

  const columns: BaseTableColumn<Seat.List>[] = [
    { key: 'seatMapId', label: '번호' },
    {
      key: 'region',
      label: '지역',
      render: (seat) => REGION_LABELS[seat.region as keyof typeof REGION_LABELS] ?? seat.region,
      filter: true,
      filterOptions: initialFilterOptions.region,
    },
    { key: 'venueName', label: '공연장명', search: true },
    { key: 'hallName', label: '홀명', search: true },
    {
      key: 'createdAt',
      label: '등록일',
      render: (seat) => dayjs(seat.createdAt).format(DATE_FORMAT.DATE_ONLY),
    },
    {
      key: 'updatedAt',
      label: '수정일',
      render: (seat) => dayjs(seat.updatedAt).format(DATE_FORMAT.DATE_ONLY),
    },
  ];

  function handleSearch(searchKeyword: string, column: string) {
    setKeyword(searchKeyword);
    setSearchColumn(column);
    setPage(0);
  }

  function handleFilter(columnKey: string, selectedValues: string[]) {
    setFilterValues((prev) => ({ ...prev, [columnKey]: selectedValues }));
    setPage(0);
  }

  function handlePageSizeChange(size: number) {
    setPageSize(size);
    setPage(0);
  }

  return (
    <PageCard>
      <PageCard.Title useRouteBack={false}>좌석 목록</PageCard.Title>
      <PageCard.Content>
        {!mounted ? (
          <div className="space-y-6">
            <Skeleton className="h-10 w-full" />
          </div>
        ) : (
          <BaseTable
            data={seatList}
            columns={columns}
            onSearch={handleSearch}
            filterValues={filterValues}
            onFilter={handleFilter}
            page={page}
            totalPages={totalPages}
            pageSize={pageSize}
            onPageChange={setPage}
            onPageSizeChange={handlePageSizeChange}
            isLoading={isFetching}
            headerActions={
              <Button onClick={() => router.push(ADMIN_ROUTES.SEAT + '/upsert')} size="sm">
                좌석 등록
              </Button>
            }
            emptyMessage="등록된 좌석 배치도가 없습니다."
          />
        )}
      </PageCard.Content>
    </PageCard>
  );
}
