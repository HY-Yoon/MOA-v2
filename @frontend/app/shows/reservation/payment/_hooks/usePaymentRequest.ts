'use client';

import { requestPayment, type PaymentRequestResponse } from '@/lib/api/payment';
import { useMutation } from '@tanstack/react-query';
import { useCallback, useMemo } from 'react';

interface UsePaymentRequestParams {
  scheduleId: number;
  scheduleSeatIds: string[];
  deliveryMethod: string;
  bookerName: string;
  bookerEmail: string;
  bookerPhone: string;
  agreedPersonalInfo: boolean;
  agreedRefundPolicy: boolean;
}

export function usePaymentRequest({
  scheduleId,
  scheduleSeatIds,
  deliveryMethod,
  bookerName,
  bookerEmail,
  bookerPhone,
  agreedPersonalInfo,
  agreedRefundPolicy,
}: UsePaymentRequestParams) {
  const parsedSeatIds = useMemo(
    () =>
      scheduleSeatIds
        .map((id) => Number(id))
        .filter((id) => Number.isFinite(id) && id > 0),
    [scheduleSeatIds],
  );

  const normalizedName = bookerName.trim();
  const normalizedEmail = bookerEmail.trim();
  const normalizedPhone = bookerPhone.trim();

  const isValidName = normalizedName.length > 0;
  const isValidEmail =
    normalizedEmail.includes('@') &&
    normalizedEmail.includes('.') &&
    /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(normalizedEmail);
  const isValidPhone = /^\d{3}-\d{4}-\d{4}$/.test(normalizedPhone);
  const isDeliverySelected = deliveryMethod.length > 0;
  const isTermsAgreed = agreedPersonalInfo && agreedRefundPolicy;

  const missingItems = useMemo(() => {
    const items: string[] = [];
    if (!isDeliverySelected) items.push('티켓 수령 방법을 선택해주세요.');
    if (!isValidName) items.push('예약자 이름을 입력해주세요.');
    if (!isValidEmail) items.push('예약자 이메일 형식을 확인해주세요. (@, . 포함)');
    if (!isValidPhone) items.push('예약자 전화번호 형식을 확인해주세요. (000-0000-0000)');
    if (!isTermsAgreed) items.push('약관 동의를 모두 완료해주세요.');
    if (!scheduleId || scheduleId <= 0 || parsedSeatIds.length === 0) {
      items.push('좌석 정보가 올바르지 않습니다. 다시 예매를 진행해주세요.');
    }
    return items;
  }, [
    isDeliverySelected,
    isValidName,
    isValidEmail,
    isValidPhone,
    isTermsAgreed,
    scheduleId,
    parsedSeatIds.length,
  ]);

  const isPaymentEnabled = missingItems.length === 0;

  const paymentMutation = useMutation({
    mutationFn: () =>
      requestPayment({
        scheduleId,
        scheduleSeatIds: parsedSeatIds,
        bookerName: normalizedName,
        bookerPhone: normalizedPhone,
        bookerEmail: normalizedEmail,
      }),
  });

  const submitPayment = useCallback(async (): Promise<PaymentRequestResponse | null> => {
    if (!isPaymentEnabled) return null;
    return await paymentMutation.mutateAsync();
  }, [isPaymentEnabled, paymentMutation]);

  return {
    missingItems,
    isPaymentEnabled,
    isValidEmail,
    isValidPhone,
    isSubmitting: paymentMutation.isPending,
    submitPayment,
  };
}
