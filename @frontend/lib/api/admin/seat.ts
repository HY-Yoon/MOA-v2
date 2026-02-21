import { axiosInstance } from '@/lib/api-client';

// 임시: 로컬 백엔드 대신 배포 도메인 직접 호출
const SEAT_API_ORIGIN = 'https://registered-rozalin-moa-v2-65e6ffe7.koyeb.app';
const BASE_URL = `${SEAT_API_ORIGIN}/api/v1/admin/seat-maps`;

// 좌석 배치도 목록 조회
export const getSeatMapList = (params: Seat.ListParams) => ({
  queryKey: ['admin', 'seat', 'list', params],
  queryFn: async () => {
    const response = await axiosInstance.get<Api.Response<Api.ListResponse<Seat.List>>>(BASE_URL, { params });
    return response?.data.data;
  },
});

// 좌석 중복 확인
export async function checkDuplicateSeat(request: Seat.CheckDuplicateRequest) {
  const response = await axiosInstance.get<Api.Response<Seat.CheckDuplicateResponse>>(
    `${BASE_URL}/duplicate`,
    { params: request },
  );
  return response?.data;
}

// 좌석 등록
export async function createSeat(request: Seat.CreateSeatRequest) {
  const normalizedSections = request.layoutData.sections.map((section) => ({
    sectionId: section.sectionId.trim(),
    name: section.name.trim(),
    color: section.color,
    price: Math.max(0, Math.round(section.price ?? 0)),
  }));

  const payload = {
    region: request.region.trim(),
    venueName: request.venueName.trim(),
    hallName: request.hallName.trim(),
    canvas: {
      ...request.layoutData.canvas,
      rowGap: 20,
      columnGap: 20,
    },
    sections: normalizedSections,
    seats: request.layoutData.seats.map((seat) => ({
      seatId: seat.seatId.trim(),
      sectionId: (seat.sectionId ?? '').trim(),
      row: seat.row.trim(),
      number: Math.round(seat.number),
      x: Math.round(seat.x),
      y: Math.round(seat.y),
    })),
  };

  const response = await axiosInstance.post<Api.Response<{ seatMapId: string }>>(
    BASE_URL,
    payload,
    {
      timeout: 60000,
    },
  );
  return response?.data;
}

// TODO: seat-map 상세/수정/삭제 API 스펙 확정 후 정리
// 기존 seat API
export async function getSeats(params?: {
  region?: string;
  venueName?: string;
  hallName?: string;
}) {
  const response = await axiosInstance.get<Api.Response<Seat.UpsertFormData[]>>('/api/seats', {
    params,
  });
  return response.data;
}

// 좌석 상세 조회
export async function getSeat(id: number) {
  const response = await axiosInstance.get<Api.Response<Seat.UpsertFormData>>(`/api/seats/${id}`);
  return response.data;
}

// 좌석 수정
export async function updateSeat(id: number, request: Seat.CreateSeatRequest) {
  const response = await axiosInstance.put<Api.Response<{ seatId: number }>>(
    `/api/seats/${id}`,
    request,
  );
  return response.data;
}

// 좌석 삭제
export async function deleteSeat(id: number) {
  const response = await axiosInstance.delete<Api.Response<void>>(`/api/seats/${id}`);
  return response.data;
}
