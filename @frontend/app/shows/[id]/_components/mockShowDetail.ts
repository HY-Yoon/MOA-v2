import { GENRE_OPTIONS, REGION_OPTIONS } from '@/constants/common';

/**
 * API 미구현 시 사용할 공연 상세 목업
 */
export function createMockShowDetail(id: number): ShowCatalog.Detail {
  const now = new Date();
  const startDate = new Date(now);
  startDate.setDate(startDate.getDate() + 1);
  const endDate = new Date(startDate);
  endDate.setDate(endDate.getDate() + 14);

  return {
    id,
    title: `목업 공연 상세 ${id}`,
    genre: GENRE_OPTIONS[0]?.value ?? 'CONCERT',
    status: 'ON_SALE',
    posterUrl: 'https://ticketimage.interpark.com/Play/image/large/26/26001820_p.gif',
    location: {
      region: REGION_OPTIONS[0]?.value ?? 'SEOUL',
      venue: '고양종합운동장',
      hallName: '주경기장',
    },
    salePeriod: {
      startDate: startDate.toISOString().slice(0, 10),
      endDate: endDate.toISOString().slice(0, 10),
    },
    startDate: startDate.toISOString().slice(0, 10),
    endDate: endDate.toISOString().slice(0, 10),
    schedules: [
      {
        keyId: 1,
        date: startDate.toISOString().slice(0, 10),
        time: { hour: 19, minute: 0, second: 0, nano: 0 },
        session: 1,
      },
      {
        keyId: 2,
        date: startDate.toISOString().slice(0, 10),
        time: { hour: 14, minute: 0, second: 0, nano: 0 },
        session: 2,
      },
    ],
    runningTime: '120분',
    cast: '김뫄뫄, 윤나나, 박롸라 외',
    detailImageUrls: [
      'https://ticketimage.interpark.com/Play/image/large/26/26001820_p.gif',
      'https://ticketimage.interpark.com/Play/image/large/26/26001820_p.gif',
    ],
    saleStatus: 'ALLOWED',
    createdAt: '',
    viewCount: 0,
  };
}
