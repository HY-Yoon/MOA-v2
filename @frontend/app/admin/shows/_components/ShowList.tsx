'use client';

import { Button, Skeleton } from '@/components/atoms';
import { useAlert } from '@/components/molecules/AlertContext';
import { changeSaleStatus, deleteShow, getShowList } from '@/lib/api/admin/show';
import { useMutation, useQuery } from '@tanstack/react-query';
import { useRouter } from 'next/navigation';
import React, { useEffect, useMemo, useState } from 'react';
import { PageCard } from '@/components/molecules/PageCard';
import {
  AdminTable,
  type AdminTableColumn,
  type AdminTableFilterOption,
} from '@/components/organisms';
import { ADMIN_ROUTES } from '@/constants/route/adminRoutes';
import StatusBadge from '@/components/molecules/StatusBadge';
import { GENRE_LABELS, SALE_STATUS_LABELS, SHOW_STATUS_LABELS } from '@/constants/common';
import dayjs from '@/plugins/dayjs';
import { DATE_FORMAT } from '@/constants/common/dateFormat';
import Link from 'next/link';
import { ToggleDropdown, ToggleItem } from '@/components/molecules/ToggleDropdown';
import { Genre, SaleStatus, ShowStatus } from '@shared/enums';
import { deriveFilterOptions } from '@/lib/admin/table-filter';

export default function ShowList() {
  const router = useRouter();
  const { confirm, alert } = useAlert();

  const [mounted, setMounted] = useState(false);

  // 페이지네이션
  const [page, setPage] = useState(0);
  const [pageSize, setPageSize] = useState(10);

  // 정렬
  const [sortColumn, setSortColumn] = useState<string | undefined>(undefined);
  const [sortOrder, setSortOrder] = useState<'asc' | 'desc' | undefined>(undefined);

  // 검색
  const [keyword, setKeyword] = useState('');

  // 필터
  const [initialFilterOptions, setInitialFilterOptions] = useState<
    Record<string, AdminTableFilterOption[]>
  >({});
  const [filterValues, setFilterValues] = useState<Record<string, string[]>>({});

  // query params
  const params: Show.ListParams = useMemo(
    () => ({
      page,
      size: pageSize,
      ...(sortColumn && sortOrder && { sort: `${sortColumn}, ${sortOrder}` }),
      ...(keyword.trim() && { keyword: keyword.trim() }),
      ...(filterValues.status?.length && { showStatus: filterValues.status[0] as ShowStatus }),
      ...(filterValues.saleStatus?.length && {
        saleStatus: filterValues.saleStatus[0] as SaleStatus,
      }),
      ...(filterValues.genre?.length && { genre: filterValues.genre[0] as Genre }),
    }),
    [page, pageSize, sortColumn, sortOrder, keyword, filterValues],
  );

  const { data, isFetching, refetch } = useQuery(getShowList(params));
  const { mutateAsync: onDeleteShow, isPending: isDeletePending } = useMutation(deleteShow());
  const { mutateAsync: onChangeSaleStatus, isPending: isSaleStatusPending } =
    useMutation(changeSaleStatus());

  const loading = isFetching || isDeletePending || isSaleStatusPending;
  const showList = data?.content ?? [];
  const totalPages = data?.totalPages ?? 0;

  // 리액트 하이드레이션 무한 루프 방지용 마운트 플래그
  useEffect(() => {
    setMounted(true);
  }, []);

  // 필터 옵션 목록
  useEffect(() => {
    // 최초 전체 리스트 기준으로 한 번만 계산
    const hasNoFilter = Object.values(filterValues).every((arr) => !arr?.length);
    const hasOptions = Object.keys(initialFilterOptions).length > 0;

    if (hasNoFilter && showList.length > 0 && !hasOptions) {
      setInitialFilterOptions(
        deriveFilterOptions<Show.List>(showList, [
          {
            key: 'status',
            getValue: (s) => s.status,
            getLabel: (v) => SHOW_STATUS_LABELS[v as ShowStatus],
          },
          {
            key: 'saleStatus',
            getValue: (s) => s.saleStatus,
            getLabel: (v) => SALE_STATUS_LABELS[v as SaleStatus],
          },
          {
            key: 'genre',
            getValue: (s) => s.genre,
            getLabel: (v) => GENRE_LABELS[v as Genre],
          },
        ]),
      );
    }
  }, [showList, filterValues, initialFilterOptions]);

  // 컬럼 정의
  const columns: AdminTableColumn<Show.List>[] = [
    { key: 'id', label: '번호', sorter: true },
    {
      key: 'title',
      label: '제목',
      render: (show) => (
        <Link
          href={`${ADMIN_ROUTES.SHOW}/${show.id}`}
          className="cursor-pointer font-medium text-gray-900 hover:text-blue-600 hover:underline"
        >
          {show.title}
        </Link>
      ),
      search: true,
      sorter: true,
    },
    {
      key: 'status',
      label: '상태',
      render: (show) => <StatusBadge type="show" status={show.status} />,
      filter: true,
      filterOptions: initialFilterOptions.status,
    },
    {
      key: 'saleStatus',
      label: '판매허용',
      render: (show) => <StatusBadge type="sale" status={show.saleStatus} />,
      filter: true,
      filterOptions: initialFilterOptions.saleStatus,
    },
    {
      key: 'genre',
      label: '장르',
      render: (show) => GENRE_LABELS[show.genre],
      filter: true,
      filterOptions: initialFilterOptions.genre,
    },
    {
      key: 'schedule',
      label: '일정',
      sorter: true,
      render: (show) => renderSchedules(show),
    },
    {
      key: 'salePeriod',
      label: '예매 일정',
      sorter: true,
      render: (show) => renderSalePeriod(show),
    },
    {
      key: 'toggle',
      label: '',
      render: (show) => {
        const toggleItems: ToggleItem[] = [
          {
            label: '판매 설정',
            onClick: () => handleSaleStatusChange(show),
          },
          {
            label: '수정',
            onClick: () => router.push(`${ADMIN_ROUTES.SHOW_UPSERT}/${show.id}`),
          },
          {
            label: '삭제',
            onClick: () => handleDelete(show),
            variant: 'destructive',
          },
        ];

        return <ToggleDropdown items={toggleItems} />;
      },
    },
  ];

  // 일정 포맷
  function renderSchedules(show: Show.List) {
    if (!show.schedules || show.schedules.length === 0) {
      return '-';
    }

    const first = show.schedules[0] as Show.SchedulesList;
    const count = show.schedules.length - 1;
    const additional = count > 0 ? `외 ${count}건` : '';

    return `${first.date} ${first.time} ${additional}`;
  }

  // 판매 일정 포맷
  function renderSalePeriod(show: Show.List) {
    if (!show.salePeriod || !show.salePeriod.startDate || !show.salePeriod.endDate) {
      return '-';
    }

    const start = dayjs(show.salePeriod.startDate).format(DATE_FORMAT.DATE_ONLY);
    const end = dayjs(show.salePeriod.endDate).format(DATE_FORMAT.DATE_ONLY);
    return `${start} ~ ${end}`;
  }

  // 판매 설정 핸들러
  async function handleSaleStatusChange(show: Show.List) {
    const isCurrentAllowed = show.saleStatus === 'ALLOWED';
    const requestStatus = isCurrentAllowed ? ('SUSPENDED' as const) : ('ALLOWED' as const);
    const requestLabel = isCurrentAllowed ? '중지' : '허용';

    const confirmed = await confirm({
      title: '판매 설정',
      description: `${show.title} 공연을 [판매 ${requestLabel}] 설정하시겠습니까?`,
      confirmText: '설정',
    });
    if (confirmed) {
      await onChangeSaleStatus({ id: show.id, saleStatus: requestStatus });
      refetch();
    }
  }

  // 삭제 핸들러
  async function handleDelete(show: Show.List) {
    if (show.status !== 'WAITING') {
      await alert({
        title: '삭제 불가',
        description: `'예매 대기' 상태인 공연만 삭제할 수 있습니다.`,
      });
      return;
    }

    const confirmed = await confirm({
      title: '공연 삭제',
      description: `${show.title} 공연을 삭제하시겠습니까?`,
      confirmText: '삭제',
    });
    if (confirmed) {
      await onDeleteShow(show.id);
      refetch();
    }
  }

  // 검색 핸들러 (ListParams 수정시 useQuery 자동 재요청)
  function handleSearch(searchKeyword: string) {
    setKeyword(searchKeyword);
    setPage(0);
  }

  // 정렬 핸들러
  const handleSortChange = (changedColumn?: string, changedOrder?: 'asc' | 'desc') => {
    setSortColumn(changedColumn);
    setSortOrder(changedOrder);
  };

  // 필터 핸들러
  function handleFilter(columnKey: string, selectedValues: string[]) {
    setFilterValues((prev) => ({ ...prev, [columnKey]: selectedValues }));
    setPage(0);
  }

  // 페이지 사이즈 변경 핸들러
  function handlePageSizeChange(size: number) {
    setPageSize(size);
    setPage(0); // 페이지 사이즈 변경 시 첫 페이지로 이동
  }

  return (
    <PageCard>
      <PageCard.Title useRouteBack={false}>공연 목록</PageCard.Title>
      <PageCard.Content>
        {!mounted ? (
          // 서버 사이드에서 렌더링 중에 로딩 화면 표시
          <div className="space-y-6">
            <Skeleton className="h-10 w-full" />
          </div>
        ) : (
          <AdminTable
            data={showList}
            columns={columns}
            onSearch={handleSearch}
            filterValues={filterValues}
            onFilter={handleFilter}
            page={page}
            defaultSortColumn={sortColumn}
            defaultSortOrder={sortOrder}
            onSort={handleSortChange}
            totalPages={totalPages}
            pageSize={pageSize}
            onPageChange={setPage}
            onPageSizeChange={handlePageSizeChange}
            headerActions={
              <Button onClick={() => router.push(ADMIN_ROUTES.SHOW_UPSERT)} size="sm">
                공연 등록
              </Button>
            }
            isLoading={loading}
            emptyMessage="등록된 공연이 없습니다."
          />
        )}
      </PageCard.Content>
    </PageCard>
  );
}
