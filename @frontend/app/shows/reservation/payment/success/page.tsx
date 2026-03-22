'use client';

import { HEADER_ROUTES, MY_PAGE_ROUTES, USER_ROUTES } from '@/constants/route/userRoutes';
import { completePayment, confirmPayment, type PaymentCompleteData } from '@/lib/api/payment';
import Link from 'next/link';
import { useRouter, useSearchParams } from 'next/navigation';
import { Suspense, useEffect, useRef, useState } from 'react';

function PaymentSuccessContent() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const confirmRequestedRef = useRef(false);
  const [confirmedData, setConfirmedData] = useState<PaymentCompleteData | null>(null);

  const orderId = searchParams.get('orderId');
  const amount = searchParams.get('amount');
  const paymentKey = searchParams.get('paymentKey');

  useEffect(() => {
    if (confirmRequestedRef.current) return;
    if (!orderId || !paymentKey || !amount) {
      router.replace(
        `/shows/reservation/payment/fail?message=${encodeURIComponent('결제 승인 파라미터가 올바르지 않습니다.')}&code=INVALID_CONFIRM_PARAM`,
      );
      return;
    }
    confirmRequestedRef.current = true;
    const safeOrderId = orderId;
    const safePaymentKey = paymentKey;
    const safeAmount = amount;

    async function confirm() {
      try {
        const result = await confirmPayment({
          orderId: safeOrderId,
          paymentKey: safePaymentKey,
          amount: Number(safeAmount),
        });

        if (!result.success || !result.data) {
          router.replace(
            `/shows/reservation/payment/fail?message=${encodeURIComponent(result.message ?? '결제 승인에 실패했습니다.')}&code=${encodeURIComponent(result.code ?? 'CONFIRM_FAILED')}&orderId=${encodeURIComponent(safeOrderId)}`,
          );
          return;
        }
        const completeResult = await completePayment(result.data.orderId, result.data.paymentKey);
        if (!completeResult.success || !completeResult.data) {
          router.replace(
            `/shows/reservation/payment/fail?message=${encodeURIComponent(completeResult.message ?? '결제 완료 정보 조회에 실패했습니다.')}&code=${encodeURIComponent(completeResult.code ?? 'COMPLETE_FAILED')}&orderId=${encodeURIComponent(result.data.orderId)}`,
          );
          return;
        }
        setConfirmedData(completeResult.data);
      } catch {
        router.replace(
          `/shows/reservation/payment/fail?message=${encodeURIComponent('결제 승인 처리 중 오류가 발생했습니다.')}&code=CONFIRM_ERROR&orderId=${encodeURIComponent(safeOrderId)}`,
        );
      }
    }

    void confirm();
  }, [amount, orderId, paymentKey, router]);

  if (!confirmedData) {
    return (
      <section className="min-h-screen bg-slate-50 px-5 py-8">
        <div className="mx-auto max-w-[760px] rounded-xl border border-slate-200 bg-white p-6 text-sm text-slate-600">
          결제 승인 정보를 확인하는 중입니다...
        </div>
      </section>
    );
  }
  const performance = confirmedData.performance;
  const seats = Array.isArray(confirmedData.seats) ? confirmedData.seats : [];
  const booker = confirmedData.booker;
  const payment = confirmedData.payment;

  return (
    <section className="min-h-screen bg-white">
      <header className="border-b border-slate-200 px-5 py-3">
        <div className="mx-auto flex max-w-[1400px] items-center justify-between">
          <Link href={USER_ROUTES.HOME} className="font-logo text-lg font-bold tracking-tight text-slate-900">
            MOA
          </Link>
          <div className="flex items-center gap-3 text-xs text-slate-500">
            <Link href={HEADER_ROUTES.MY_PAGE} className="hover:text-slate-800">
              마이페이지
            </Link>
            <span>|</span>
            <Link href={MY_PAGE_ROUTES.RESERVATIONS} className="hover:text-slate-800">
              예매내역/취소
            </Link>
          </div>
        </div>
      </header>

      <div className="px-5 py-8">
        <div className="mx-auto max-w-[560px] rounded-xl border border-slate-200 bg-white p-6 shadow-sm">
          <div className="mb-6 text-center">
            <div className="mx-auto mb-3 flex h-10 w-10 items-center justify-center rounded-full bg-blue-500 text-white">
              ✓
            </div>
            <h2 className="text-xl font-semibold text-slate-900">예매 완료</h2>
          </div>

          <dl className="space-y-3 text-sm">
            <div className="flex justify-between gap-4">
              <dt className="text-slate-500">예매번호</dt>
              <dd className="text-right font-medium text-slate-900">{confirmedData.reservationNumber}</dd>
            </div>
            <div className="flex justify-between gap-4">
              <dt className="text-slate-500">주문번호</dt>
              <dd className="text-right font-medium text-slate-900">{confirmedData.orderId}</dd>
            </div>
            <div className="flex justify-between gap-4">
              <dt className="text-slate-500">공연명</dt>
              <dd className="text-right font-medium text-slate-900">{performance?.title ?? '-'}</dd>
            </div>
            <div className="flex justify-between gap-4">
              <dt className="text-slate-500">공연일시</dt>
              <dd className="text-right font-medium text-slate-900">
                {performance?.date ?? '-'} {performance?.showTime ?? ''}
              </dd>
            </div>
            <div className="flex justify-between gap-4">
              <dt className="text-slate-500">좌석정보</dt>
              <dd className="text-right font-medium text-slate-900">
                {seats.length > 0
                  ? seats.map((seat) => `${seat.section}구역 ${seat.seatNumber}`).join(', ')
                  : '-'}
              </dd>
            </div>
            <div className="flex justify-between gap-4">
              <dt className="text-slate-500">예매자</dt>
              <dd className="text-right font-medium text-slate-900">
                {booker?.name ?? '-'} / {booker?.phone ?? '-'}
              </dd>
            </div>
            <div className="flex justify-between gap-4">
              <dt className="text-slate-500">결제수단</dt>
              <dd className="text-right font-medium text-slate-900">{payment?.method ?? '-'}</dd>
            </div>
            <div className="flex justify-between gap-4">
              <dt className="text-slate-500">결제금액</dt>
              <dd className="text-right font-semibold text-slate-900">
                {typeof payment?.amount === 'number' ? payment.amount.toLocaleString('ko-KR') : '0'}원
              </dd>
            </div>
          </dl>

          <div className="mt-6 flex gap-3">
            <Link
              href={USER_ROUTES.HOME}
              className="flex-1 rounded-md border border-slate-300 px-4 py-2 text-center text-sm text-slate-700"
            >
              상세내역
            </Link>
            <Link
              href={MY_PAGE_ROUTES.RESERVATIONS}
              className="flex-1 rounded-md border border-blue-500 px-4 py-2 text-center text-sm text-blue-600"
            >
              예매내역 확인
            </Link>
          </div>
        </div>
      </div>
    </section>
  );
}

export default function PaymentSuccessPage() {
  return (
    <Suspense fallback={<section className="min-h-screen bg-slate-50 px-5 py-8" />}>
      <PaymentSuccessContent />
    </Suspense>
  );
}
