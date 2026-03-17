'use client';

import { Skeleton } from '@/components/atoms';
import { useAlert } from '@/components/molecules/AlertContext';
import { ToggleDropdown } from '@/components/molecules/ToggleDropdown';
import { PageCard } from '@/components/molecules/PageCard';
import {
  BaseTable,
  type BaseTableColumn,
  type BaseTableFilterOption,
} from '@/components/organisms';
import { DATE_FORMAT } from '@/constants/common/dateFormat';
import dayjs from '@/plugins/dayjs';
import { changeUserStatus, getUserList } from '@/lib/api/admin/user';
import { useMutation, useQuery } from '@tanstack/react-query';
import React, { useEffect, useMemo, useState } from 'react';
import { Gender, UserStatus } from '@shared/enums';
import StatusBadge from '@/components/molecules/StatusBadge';
import { USER_STATUS_LABELS } from '@/constants/common/userStatus';
import { GENDER_LABELS } from '@/constants/common/gender';
import { deriveFilterOptions } from '@/lib/admin/table-filter';
import { ROLE_LABELS } from '@/constants/common/role';

export default function UserList() {
  const { confirmWithInput } = useAlert();

  const [mounted, setMounted] = useState(false);

  // 페이지네이션
  const [page, setPage] = useState(0);
  const [pageSize, setPageSize] = useState(10);

  // 정렬
  const [sortColumn, setSortColumn] = useState<string | undefined>(undefined);
  const [sortOrder, setSortOrder] = useState<'asc' | 'desc' | undefined>(undefined);

  // 검색
  const [keyword, setKeyword] = useState('');
  const [searchColumn, setSearchColumn] = useState('all');

  // 필터
  const [initialFilterOptions, setInitialFilterOptions] = useState<
    Record<string, BaseTableFilterOption[]>
  >({});
  const [filterValues, setFilterValues] = useState<Record<string, string[]>>({});

  const params: User.ListParams = useMemo(
    () => ({
      page,
      size: pageSize,
      ...(sortColumn && sortOrder && { sort: `${sortColumn},${sortOrder}` }),
      ...(keyword.trim() && { keyword: keyword.trim() }), // 전체 검색
      ...(keyword.trim() && searchColumn !== 'all' && { searchType: searchColumn }), // 컬럼 검색
      ...(filterValues.status?.length && { status: filterValues.status[0] as UserStatus }),
      ...(filterValues.gender?.length && { gender: filterValues.gender[0] as Gender }),
      ...(filterValues.socialProvider?.length && {
        socialProvider: filterValues.socialProvider[0],
      }),
    }),
    [
      page,
      pageSize,
      sortColumn,
      sortOrder,
      keyword,
      searchColumn,
      filterValues.status,
      filterValues.gender,
      filterValues.socialProvider,
    ],
  );

  const { data, isFetching, refetch } = useQuery(getUserList(params));
  const { mutateAsync: onChangeUserStatus, isPending: changePending } =
    useMutation(changeUserStatus());

  const loading = isFetching || changePending;
  const userList = data?.content ?? [];
  const totalPages = data?.totalPages ?? 0;

  useEffect(() => {
    setMounted(true);
  }, []);

  // 필터 옵션 목록
  useEffect(() => {
    // 최초 전체 리스트 기준으로 한 번만 계산
    const hasNoFilter = Object.values(filterValues).every((arr) => !arr?.length);
    const hasOptions = Object.keys(initialFilterOptions).length > 0;

    if (hasNoFilter && userList.length > 0 && !hasOptions) {
      setInitialFilterOptions(
        deriveFilterOptions<User.List>(userList, [
          {
            key: 'status',
            getValue: (u) => u.status,
            getLabel: (v) => USER_STATUS_LABELS[v as UserStatus],
          },
          {
            key: 'gender',
            getValue: (u) => u.gender,
            getLabel: (v) => GENDER_LABELS[v as Gender],
          },
          {
            key: 'socialProvider',
            getValue: (u) => u.socialProvider,
          },
        ]),
      );
    }
  }, [userList, filterValues, initialFilterOptions]);

  const columns: BaseTableColumn<User.List>[] = [
    {
      key: 'id',
      label: '번호',
      render: (user) => user.id || '-',
      sorter: true,
    },
    {
      key: 'name',
      label: '이름',
      render: (user) => user.name || '-',
      search: true,
      sorter: true,
    },
    {
      key: 'status',
      label: '상태',
      render: (user) => <StatusBadge type="user" status={user.status as UserStatus} />,
      filter: true,
      filterOptions: initialFilterOptions.status,
    },
    {
      key: 'gender',
      label: '성별',
      render: (user) => <StatusBadge type="gender" status={user.gender as Gender} />,
      filter: true,
      filterOptions: initialFilterOptions.gender,
    },
    {
      key: 'email',
      label: '이메일',
      render: (user) => user.email || '-',
      search: true,
      sorter: true,
    },
    {
      key: 'phone',
      label: '연락처',
      render: (user) => user.phone || '-',
      search: true,
    },
    {
      key: 'socialProvider',
      label: '소셜 연동',
      render: (user) => user.socialProvider || '-',
      filter: true,
      filterOptions: initialFilterOptions.socialProvider,
    },
    {
      key: 'role',
      label: '권한',
      render: (user) => ROLE_LABELS[user.role],
    },
    {
      key: 'createdAt',
      label: '가입일',
      sorter: true,
      render: (user) =>
        user.createdAt ? dayjs(user.createdAt).format(DATE_FORMAT.DATE_ONLY) : '-',
    },
    {
      key: 'toggle',
      label: '',
      render: (user) => {
        const isActive = user.status === 'ACTIVE';
        const requestLabel = isActive ? '강제 탈퇴' : '회원 복구';

        return (
          <ToggleDropdown
            items={[
              {
                label: requestLabel,
                onClick: () => handleUserStatusChange(user.id, user.name, isActive, requestLabel),
                disabled: user.status === 'DELETED', // 이미 삭제된 경우,
                ...(isActive && { variant: 'destructive' }),
              },
            ]}
          />
        );
      },
    },
  ];

  async function handleUserStatusChange(
    id: number,
    name: string,
    isActive: boolean,
    label: string,
  ) {
    const requestStatus = isActive ? 'SUSPENDED' : 'ACTIVE';

    const result = await confirmWithInput({
      title: '회원 설정',
      description: `${name} 회원을 [${label}] 처리하시겠습니까?`,
      confirmText: '설정',
      input: {
        label: '사유',
        placeholder: '변경 사유를 입력하세요.',
        required: true,
      },
    });
    if (result.confirmed) {
      await onChangeUserStatus({ id, status: requestStatus, reason: result.value });
      refetch();
    }
  }

  function handleSearch(searchKeyword: string, column: string) {
    setKeyword(searchKeyword);
    setSearchColumn(column);
    setPage(0);
  }

  const handleSortChange = (changedColumn?: string, changedOrder?: 'asc' | 'desc') => {
    setSortColumn(changedColumn);
    setSortOrder(changedOrder);
  };

  // 필터 핸들러
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
      <PageCard.Title useRouteBack={false}>회원 목록</PageCard.Title>
      <PageCard.Content>
        {!mounted ? (
          <div className="space-y-6">
            <Skeleton className="h-10 w-full" />
          </div>
        ) : (
          <BaseTable
            data={userList}
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
            isLoading={loading}
            emptyMessage="등록된 회원이 없습니다."
          />
        )}
      </PageCard.Content>
    </PageCard>
  );
}
