'use client';

import { reportPaymentFail } from '@/lib/api/payment';
import { useSearchParams } from 'next/navigation';
import { useEffect, useRef } from 'react';

export default function PaymentFailPage() {
  const searchParams = useSearchParams();
  const failReportedRef = useRef(false);
  const orderId = searchParams.get('orderId');
  const code = searchParams.get('code');
  const message = searchParams.get('message');

  useEffect(() => {
    if (failReportedRef.current) return;
    if (!code || !message) return;
    failReportedRef.current = true;

    void reportPaymentFail({
      orderId: orderId ?? undefined,
      code,
      message,
    }).catch(() => {
      // 실패 보고 API 오류는 화면 표시 흐름을 막지 않음
    });
  }, [code, message, orderId]);

  return (
    <section className="min-h-screen bg-slate-50 px-5 py-8">
      <div className="mx-auto max-w-[760px] rounded-xl border border-slate-200 bg-white p-6">
        <h2 className="text-xl font-semibold text-slate-900">결제 실패</h2>
        <p className="mt-3 text-sm text-slate-700">에러 코드: {code}</p>
        <p className="mt-1 text-sm text-slate-700">실패 사유: {message}</p>
      </div>
    </section>
  );
}
