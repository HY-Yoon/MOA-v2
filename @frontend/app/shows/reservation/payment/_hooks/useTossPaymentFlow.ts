'use client';

import { useCallback, useState } from 'react';
import type { PaymentRequestSuccessData } from '@/lib/api/payment';
import { useRouter } from 'next/navigation';

export function useTossPaymentFlow() {
  const router = useRouter();
  const [isLaunching, setIsLaunching] = useState(false);

  const launch = useCallback(async (data: PaymentRequestSuccessData) => {
    setIsLaunching(true);
    try {
      const params = new URLSearchParams({
        orderId: data.orderId,
        amount: String(data.amount),
        orderName: data.orderName,
        customerName: data.booker?.name ?? '',
        customerEmail: data.booker?.email ?? '',
        customerMobilePhone: data.booker?.phone ?? '',
        successUrl: data.successUrl,
        failUrl: data.failUrl,
      });
      router.push(`/shows/reservation/payment/checkout?${params.toString()}`);
      return;
    } finally {
      setIsLaunching(false);
    }
  }, [router]);

  return {
    launch,
    isLaunching,
  };
}
