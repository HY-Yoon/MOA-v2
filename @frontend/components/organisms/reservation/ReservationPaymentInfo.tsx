'use client';

import InfoField from '@/components/molecules/InfoField';
import HorizontalTable from '@/components/molecules/HorizontalTable';
import dayjs, { Dayjs } from 'dayjs';
import { DATE_FORMAT } from '@/constants/common/dateFormat';
import SectionLayout from '@/components/molecules/SectionLayout';
import { RESERVATION_STATUS_LABELS } from '@/constants/common/reservationStatus';
import { PAYMENT_STATUS_LABELS } from '@/constants/common/paymentStatus';
import { Button } from '@/components/atoms';
import { PAYMENT_METHOD_LABELS } from '@/constants/common/paymentMethod';

interface Props {
  data: Reservation.Detail;
  cancelDeadline: Dayjs;
  canCancel: boolean;
  onCancel: () => void;
}

/** 취소 수수료 */
const cancelFeeRows = [
  {
    label: '관람일 10일 전',
    fee: '뮤지컬/콘서트/클래식 장당 4,000원\n연극/전시 등 장당 2,000원\n(단, 최대 티켓금액의 10% 한도)',
  },
  { label: '관람일 7~9일 전', fee: '10%' },
  { label: '관람일 6~3일 전', fee: '20%' },
  { label: '관람일 2~1일 전', fee: '30%' },
] as const;

export default function ReservationPaymentInfo({
  data,
  cancelDeadline,
  canCancel,
  onCancel,
}: Props) {
  const { reservationNumber, reservationDate, reservationStatus, payment, seats } = data || {};

  return (
    <>
      <SectionLayout title="결제내역" showDivider={true}>
        <div className="border border-slate-200">
          <div className="grid grid-cols-2 divide-x divide-y divide-slate-200">
            <InfoField
              label="예매일"
              content={dayjs(reservationDate).format(DATE_FORMAT.DATE_KR)}
            />
            <InfoField label="예매상태" content={RESERVATION_STATUS_LABELS[reservationStatus]} />
            <InfoField label="결제수단" content={PAYMENT_METHOD_LABELS[payment.paymentMethod]} />
            <InfoField label="결제상태" content={PAYMENT_STATUS_LABELS[payment.paymentStatus]} />
            <InfoField
              label="총 결제금액"
              content={`${payment.totalAmount.toLocaleString()}원`}
              contentBold={true}
            />
          </div>
        </div>

        <HorizontalTable
          columns={['예매번호', '좌석 등급', '좌석 번호', '좌석 금액', '취소 가능 여부']}
          rows={
            seats?.map((seat: Reservation.Seats) => [
              reservationNumber,
              `${seat.sectionName}석`,
              `${seat.row}열 ${seat.number}`,
              `${seat.price.toLocaleString()}원`,
              canCancel ? '취소가능' : '취소불가',
            ]) ?? []
          }
        />
      </SectionLayout>

      <SectionLayout title="예매취소 유의사항" showDivider={true}>
        <div className="border border-slate-200">
          <div className="divide-y divide-slate-200">
            <InfoField
              label="취소 가능일"
              content={dayjs(cancelDeadline).format(DATE_FORMAT.DATE_KR)}
              contentBold={true}
              contentColor="text-red-600"
            />
            <InfoField label="취소 수수료">
              <div className="space-y-3">
                <p className="text-sm">취소 일자에 따라 취소 수수료가 달라집니다.</p>
                <HorizontalTable
                  columns={['구분', '취소 수수료']}
                  rows={cancelFeeRows.map((row) => [row.label, row.fee])}
                />
              </div>
            </InfoField>
            <InfoField label="취소 및 환불">
              {canCancel ? (
                <Button variant="destructive" onClick={onCancel}>
                  예매취소
                </Button>
              ) : (
                '취소/환불 불가'
              )}
            </InfoField>
          </div>
        </div>
      </SectionLayout>
    </>
  );
}
