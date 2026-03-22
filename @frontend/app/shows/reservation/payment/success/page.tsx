'use client';

import { confirmPayment } from '@/lib/api/payment';
import { useRouter, useSearchParams } from 'next/navigation';
import { Suspense, useEffect, useRef } from 'react';

function PaymentSuccessContent() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const confirmRequestedRef = useRef(false);

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

        if (!result.success) {
          router.replace(
            `/shows/reservation/payment/fail?message=${encodeURIComponent(result.message ?? '결제 승인에 실패했습니다.')}&code=${encodeURIComponent(result.code ?? 'CONFIRM_FAILED')}&orderId=${encodeURIComponent(safeOrderId)}`,
          );
        }
      } catch {
        router.replace(
          `/shows/reservation/payment/fail?message=${encodeURIComponent('결제 승인 처리 중 오류가 발생했습니다.')}&code=CONFIRM_ERROR&orderId=${encodeURIComponent(safeOrderId)}`,
        );
      }
    }

    void confirm();
  }, [amount, orderId, paymentKey, router]);

  return (
    <section className="min-h-screen bg-slate-50 px-5 py-8">
      <div className="mx-auto max-w-[760px] rounded-xl border border-slate-200 bg-white p-6">
        <h2 className="text-xl font-semibold text-slate-900">결제 성공</h2>
        <p className="mt-3 text-sm text-slate-700">주문번호: {orderId}</p>
        <p className="mt-1 text-sm text-slate-700">
          결제 금액: {Number(amount ?? 0).toLocaleString('ko-KR')}원
        </p>
        <p className="mt-1 text-sm text-slate-700">paymentKey: {paymentKey}</p>
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
