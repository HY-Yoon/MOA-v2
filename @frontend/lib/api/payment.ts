import { axiosInstance } from '@/lib/api-client';

export interface PaymentRequestPayload {
  scheduleId: number;
  seatIds: number[];
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
): Promise<PaymentApiResponse> => {
  const response = await axiosInstance.post<PaymentApiResponse>(
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
