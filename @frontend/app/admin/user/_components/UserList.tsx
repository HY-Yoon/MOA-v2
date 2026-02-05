'use client';

import { Skeleton } from '@/components/atoms';
import { useAlert } from '@/components/molecules/AlertContext';
import { ToggleDropdown } from '@/components/molecules/ToggleDropdown';
import { PageCard } from '@/components/molecules/PageCard';
import { AdminTable, AdminTableColumn } from '@/components/organisms';
import { DATE_FORMAT } from '@/constants/common/dateFormat';
import dayjs from '@/plugins/dayjs';
import { changeUserStatus, getUserList } from '@/lib/api/admin/user';
import { useMutation, useQuery } from '@tanstack/react-query';
import { useEffect, useMemo, useState } from 'react';
import { Gender, UserStatus } from '@shared/enums';
import StatusBadge from '@/components/molecules/StatusBadge';
import { VERIFY_LABELS } from '@/constants/admin/user';

export default function UserList() {
  const { confirmWithInput } = useAlert();

  const [mounted, setMounted] = useState(false);

  const [page, setPage] = useState(0);
  const [pageSize, setPageSize] = useState(10);

  const [sortColumn, setSortColumn] = useState<string | undefined>(undefined);
  const [sortOrder, setSortOrder] = useState<'asc' | 'desc' | undefined>(undefined);

  const [keyword, setKeyword] = useState('');
  const [searchColumn, setSearchColumn] = useState('all');

  const params: User.ListParams = useMemo(
    () => ({
      page,
      size: pageSize,
      ...(sortColumn && sortOrder && { sort: `${sortColumn},${sortOrder}` }),
      ...(keyword.trim() && { keyword: keyword.trim() }),
      ...(keyword.trim() && searchColumn !== 'all' && { searchType: searchColumn }),
    }),
    [page, pageSize, sortColumn, sortOrder, keyword, searchColumn],
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

  const columns: AdminTableColumn<User.List>[] = [
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
    },
    {
      key: 'gender',
      label: '성별',
      render: (user) => <StatusBadge type="gender" status={user.gender as Gender} />,
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
    },
    {
      key: 'isVerified',
      label: '인증 여부',
      render: (user) => (user.isVerified ? VERIFY_LABELS.VERIFY : VERIFY_LABELS.NO_VERIFY),
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
          <AdminTable
            data={userList}
            columns={columns}
            onSearch={handleSearch}
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
