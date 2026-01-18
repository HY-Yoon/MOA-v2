import { axiosInstance } from '@/lib/api-client';

// 좌석 중복 확인
export async function checkDuplicateSeat(request: Seat.CheckDuplicateRequest) {
  const response = await axiosInstance.post<ApiResponse<Seat.CheckDuplicateResponse>>(
    '/api/seats/check-duplicate',
    request,
  );
  return response.data;
}

// 좌석 등록
export async function createSeat(request: Seat.CreateSeatRequest) {
  const response = await axiosInstance.post<ApiResponse<{ seatId: number }>>(
    '/api/seats',
    request,
  );
  return response.data;
}

// 좌석 목록 조회
export async function getSeats(params?: {
  region?: string;
  venueName?: string;
  hallName?: string;
}) {
  const response = await axiosInstance.get<ApiResponse<Seat.UpsertFormData[]>>('/api/seats', {
    params,
  });
  return response.data;
}

// 좌석 상세 조회
export async function getSeat(id: number) {
  const response = await axiosInstance.get<ApiResponse<Seat.UpsertFormData>>(`/api/seats/${id}`);
  return response.data;
}

// 좌석 수정
export async function updateSeat(id: number, request: Seat.CreateSeatRequest) {
  const response = await axiosInstance.put<ApiResponse<{ seatId: number }>>(
    `/api/seats/${id}`,
    request,
  );
  return response.data;
}

// 좌석 삭제
export async function deleteSeat(id: number) {
  const response = await axiosInstance.delete<ApiResponse<void>>(`/api/seats/${id}`);
  return response.data;
}
