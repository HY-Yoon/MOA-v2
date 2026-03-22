import { axiosInstance } from '@/lib/api-client';

export interface PaymentRequestPayload {
  scheduleId: number;
  scheduleSeatIds: number[];
  bookerName: string;
  bookerPhone: string;
  bookerEmail: string;
}

export interface PaymentRequestSuccessData {
  orderId: string;
  amount: number;
  orderName: string;
  booker: {
    name: string;
    email: string;
    phone: string;
  };
  successUrl: string;
  failUrl: string;
}

export interface PaymentRequestResponse {
  success: boolean;
  data?: PaymentRequestSuccessData;
  message: string | null;
  code?: string | null;
}

export interface PaymentConfirmPayload {
  paymentKey: string;
  orderId: string;
  amount: number;
}

export interface PaymentFailPayload {
  orderId?: string;
  code: string;
  message: string;
}

export interface PaymentApiResponse {
  success: boolean;
  data?: unknown;
  message: string | null;
  code?: string | null;
}

export interface PaymentConfirmSuccessData {
  reservationId: number;
  reservationNumber: string;
  orderId: string;
  paymentKey: string;
  amount: number;
  method: string;
  orderName: string;
  approvedAt: string;
}

export interface PaymentCompleteData {
  reservationNumber: string;
  orderId: string;
  performance: {
    title: string;
    date: string;
    showTime: string;
    session: number;
  };
  seats: Array<{
    section: string;
    seatNumber: string;
  }>;
  booker: {
    name: string;
    phone: string;
    email: string;
  };
  payment: {
    method: string;
    amount: number;
    paidAt: string;
  };
}

export interface PaymentConfirmResponse {
  success: boolean;
  data?: PaymentConfirmSuccessData;
  message: string | null;
  code?: string | null;
}

export interface PaymentCompleteResponse {
  success: boolean;
  data?: PaymentCompleteData;
  message: string | null;
  code?: string | null;
}

export const requestPayment = async (
  payload: PaymentRequestPayload,
): Promise<PaymentRequestResponse> => {
  const response = await axiosInstance.post<PaymentRequestResponse>(
    '/api/v1/payment/request',
    payload,
    {
      withCredentials: true,
    },
  );

  if (!response?.data) {
    throw new Error('결제 요청 응답 데이터가 없습니다.');
  }

  return response.data;
};

export const confirmPayment = async (
  payload: PaymentConfirmPayload,
): Promise<PaymentConfirmResponse> => {
  const response = await axiosInstance.post<PaymentConfirmResponse>(
    '/api/v1/payment/confirm',
    payload,
    {
      withCredentials: true,
    },
  );

  if (!response?.data) {
    throw new Error('결제 승인 응답 데이터가 없습니다.');
  }

  return response.data;
};

export const completePayment = async (
  orderId: string,
  paymentKey: string,
): Promise<PaymentCompleteResponse> => {
  const response = await axiosInstance.get<PaymentCompleteResponse>('/api/v1/payment/complete', {
    params: {
      orderId,
      paymentKey,
    },
    withCredentials: true,
  });

  if (!response?.data) {
    throw new Error('결제 완료 상세 응답 데이터가 없습니다.');
  }

  return response.data;
};

export const reportPaymentFail = async (
  payload: PaymentFailPayload,
): Promise<PaymentApiResponse> => {
  const response = await axiosInstance.post<PaymentApiResponse>(
    '/api/v1/payment/fail',
    payload,
    {
      withCredentials: true,
    },
  );

  if (!response?.data) {
    throw new Error('결제 실패 처리 응답 데이터가 없습니다.');
  }

  return response.data;
};
