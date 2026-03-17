'use client';

import { Button, Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/atoms';
import { useAlert } from '@/components/molecules/AlertContext';
import { ADMIN_ROUTES } from '@/constants/route/adminRoutes';
import { changeSaleStatus, deleteShow, getShow } from '@/lib/api/admin/show';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { TrashIcon } from 'lucide-react';
import { useRouter } from 'next/navigation';
import { useCallback, useMemo } from 'react';
import ShowDetailBasicInfo from './ShowDetailBasicInfo';
import ShowDetailReservations from './ShowDetailReservations';
import { ButtonGroup } from '@/components/atoms/button-group';
import { PageCard } from '@/components/molecules/PageCard';

interface Props {
  id: string;
}

export default function ShowDetail({ id }: Props) {
  const router = useRouter();
  const showId = Number(id) || 0;

  const { confirm } = useAlert();
  const queryClient = useQueryClient();

  const { data, isFetching, refetch } = useQuery(getShow(showId));
  const { mutateAsync: onDeleteShow, isPending: isDeletePending } = useMutation(deleteShow());
  const { mutateAsync: onChangeSaleStatus, isPending: isSaleStatusPending } =
    useMutation(changeSaleStatus());

  const loading = isFetching || isDeletePending || isSaleStatusPending;
  const canDelete = data?.status === 'WAITING';

  const handleSaleStatusChange = useCallback(async () => {
    if (!data) return;

    const isCurrentAllowed = data.saleStatus === 'ALLOWED';
    const requestStatus = isCurrentAllowed ? ('SUSPENDED' as const) : ('ALLOWED' as const);
    const requestLabel = isCurrentAllowed ? '중지' : '허용';

    const confirmed = await confirm({
      title: '판매 설정',
      description: `${data.title} 공연을 [판매 ${requestLabel}] 설정하시겠습니까?`,
      confirmText: '설정',
    });
    if (!confirmed) return;

    await onChangeSaleStatus({ id: showId, saleStatus: requestStatus });
    refetch();
  }, [data, confirm, onChangeSaleStatus, showId, refetch]);

  const handleDelete = useCallback(async () => {
    if (!data || !canDelete) return;

    const confirmed = await confirm({
      title: '공연 삭제',
      description: `${data.title} 공연을 삭제하시겠습니까?`,
      confirmText: '삭제',
    });
    if (!confirmed) return;

    await onDeleteShow(data.id);
    queryClient.invalidateQueries({ queryKey: ['admin', 'show', 'list'] });
    router.push(ADMIN_ROUTES.SHOW);
  }, [data, canDelete, confirm, onDeleteShow, queryClient, router]);

  const buttonItems = useMemo(
    () => [
      {
        label: '판매설정',
        onClick: handleSaleStatusChange,
      },
      {
        label: '수정',
        onClick: () => router.push(`${ADMIN_ROUTES.SHOW_UPSERT}/${showId}`),
      },
      {
        label: '삭제',
        onClick: handleDelete,
        variant: 'destructive' as const,
        disabled: !canDelete,
      },
    ],
    [router, showId, canDelete, handleDelete, handleSaleStatusChange],
  );

  if (!showId) return;

  return (
    <PageCard>
      <PageCard.Title
        useRouteBack={true}
        buttonGroups={
          <ButtonGroup>
            {buttonItems?.map((item) => (
              <Button key={item.label} variant={item.variant || 'outline'} onClick={item.onClick}>
                {item.label === '삭제' && <TrashIcon />}
                {item.label}
              </Button>
            ))}
          </ButtonGroup>
        }
      >
        {data?.title || '공연 상세'}
      </PageCard.Title>

      <PageCard.Content>
        <Tabs defaultValue="basic" className="w-full">
          <TabsList variant="line" className="mb-6 w-fit">
            <TabsTrigger value="basic">기본정보</TabsTrigger>
            <TabsTrigger value="reservations">예매내역</TabsTrigger>
          </TabsList>

          {/*기본정보*/}
          <TabsContent value="basic">
            <ShowDetailBasicInfo data={data} loading={loading} />
          </TabsContent>

          {/*TODO: 예매내역*/}
          <TabsContent value="reservations">
            <ShowDetailReservations showId={showId} />
          </TabsContent>
        </Tabs>
      </PageCard.Content>
    </PageCard>
  );
}
