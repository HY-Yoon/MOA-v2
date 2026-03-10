import { axiosInstance } from '@/lib/api-client';

const QUEUE_ENTER_URL = '/api/v2/queue/enter' as const;
const QUEUE_STATUS_URL = '/api/v2/queue/status' as const;

export interface EnterQueueRequest {
  scheduleId: number;
}

export interface EnterQueueResponse {
  success: boolean;
  position: number;
  estimatedWaitTimeSeconds: number;
  token: string | null;
  message: string;
}

export interface WaitingQueueStatusResponse {
  status: 'WAITING';
  position: number;
  estimatedWaitTimeSeconds: number;
  retryAfterSeconds: number;
  token: null;
  message: string;
}

export interface ReadyQueueStatusResponse {
  status: 'READY';
  position: number;
  estimatedWaitTimeSeconds: number;
  token: string;
  message: string;
}

export interface ExpiredQueueStatusResponse {
  status: 'EXPIRED';
  message: string;
}

export type QueueStatusResponse =
  | WaitingQueueStatusResponse
  | ReadyQueueStatusResponse
  | ExpiredQueueStatusResponse;

export const enterQueue = async (request: EnterQueueRequest): Promise<EnterQueueResponse> => {
  const response = await axiosInstance.post<Api.Response<EnterQueueResponse>>(QUEUE_ENTER_URL, request, {
    withCredentials: true,
    headers: {
      'Cache-Control': 'no-store, no-cache, must-revalidate',
      Pragma: 'no-cache',
      Expires: '0',
    },
  });
  const payload = response?.data?.data;
  if (!payload) {
    throw new Error('대기열 진입 응답 데이터가 없습니다.');
  }
  return payload;
};

export const getQueueStatus = async (scheduleId: number): Promise<QueueStatusResponse> => {
  const response = await axiosInstance.get<Api.Response<QueueStatusResponse>>(QUEUE_STATUS_URL, {
    params: { scheduleId },
    withCredentials: true,
    headers: {
      'Cache-Control': 'no-store, no-cache, must-revalidate',
      Pragma: 'no-cache',
      Expires: '0',
    },
  });
  const payload = response?.data?.data;
  if (!payload) {
    throw new Error('대기열 상태 응답 데이터가 없습니다.');
  }
  return payload;
};
