'use client';

import {
  Button,
  Carousel,
  CarouselContent,
  CarouselItem,
  CarouselNext,
  CarouselPrevious,
} from '@/components/atoms';
import ShowListItemCard from '@/components/molecules/ShowListItemCard';
import { GENRE_OPTIONS } from '@/constants/common';
import { getShowCatalogList } from '@/lib/api/show';
import { useQuery } from '@tanstack/react-query';
import { Loader2 } from 'lucide-react';
import { useMemo, useState } from 'react';

const USE_MOCK_DATA = false;

const MOCK_SHOWS: ShowCatalog.List[] = [
  {
    id: 1,
    title: '뮤지컬 데스노트',
    genre: 'MUSICAL',
    status: 'ONGOING',
    saleStatus: 'ON_SALE',
    posterUrl: 'https://ticketimage.interpark.com/Play/image/large/26/26001820_p.gif',
    location: { region: 'SEOUL', venue: '디큐브 링크아트센터', hallName: '' },
    salePeriod: { startDate: '2025.5.10', endDate: '2026.5.10' },
    createdAt: '2026-02-21T00:00:00',
    viewCount: 12000,
    startDate: '2025.5.10',
    endDate: '2026.5.10',
    schedules: [],
  },
  {
    id: 2,
    title: '태양의서커스 <쿠자>',
    genre: 'MUSICAL',
    status: 'ONGOING',
    saleStatus: 'ON_SALE',
    posterUrl: 'https://ticketimage.interpark.com/Play/image/large/25/25005738_p.gif',
    location: { region: 'SEOUL', venue: '잠실종합운동장', hallName: '빅탑' },
    salePeriod: { startDate: '2025.10.11', endDate: '2025.12.28' },
    createdAt: '2026-02-21T00:00:00',
    viewCount: 11000,
    startDate: '2025.10.11',
    endDate: '2025.12.28',
    schedules: [],
  },
  {
    id: 3,
    title: '뮤지컬 <물랑루즈!>',
    genre: 'MUSICAL',
    status: 'ONGOING',
    saleStatus: 'ON_SALE',
    posterUrl: 'https://ticketimage.interpark.com/Play/image/large/25/25005103_p.gif',
    location: { region: 'SEOUL', venue: '블루스퀘어', hallName: '신한카드홀' },
    salePeriod: { startDate: '2025.11.27', endDate: '2026.2.22' },
    createdAt: '2026-02-21T00:00:00',
    viewCount: 10800,
    startDate: '2025.11.27',
    endDate: '2026.2.22',
    schedules: [],
  },
  {
    id: 4,
    title: '<라이프 오브 파이> 한국 초연',
    genre: 'THEATER',
    status: 'ONGOING',
    saleStatus: 'ON_SALE',
    posterUrl: 'https://ticketimage.interpark.com/Play/image/large/25/25006370_p.gif',
    location: { region: 'SEOUL', venue: 'GS아트센터', hallName: '' },
    salePeriod: { startDate: '2025.11.29', endDate: '2026.3.2' },
    createdAt: '2026-02-21T00:00:00',
    viewCount: 9600,
    startDate: '2025.11.29',
    endDate: '2026.3.2',
    schedules: [],
  },
  {
    id: 5,
    title: '뮤지컬 <EVITA>',
    genre: 'MUSICAL',
    status: 'ONGOING',
    saleStatus: 'ON_SALE',
    posterUrl: 'https://ticketimage.interpark.com/Play/image/large/25/25006774_p.gif',
    location: { region: 'SEOUL', venue: '광림아트센터', hallName: 'BBCH홀' },
    salePeriod: { startDate: '2025.11.7', endDate: '2026.1.11' },
    createdAt: '2026-02-21T00:00:00',
    viewCount: 9400,
    startDate: '2025.11.7',
    endDate: '2026.1.11',
    schedules: [],
  },
];

interface ShowListProps {
  showRank?: boolean;
}

export default function ShowList({ showRank = false }: ShowListProps) {
  const CONTENT_MIN_HEIGHT_CLASS = 'min-h-[560px]';
  const [selectedGenre, setSelectedGenre] = useState<ShowCatalog.Genre | 'ALL'>('ALL');

  const params = useMemo<ShowCatalog.ListParams>(
    () => ({
      page: 0,
      size: 20,
      ...(selectedGenre !== 'ALL' && { genre: selectedGenre }),
      // 메인에서는 인기순 정렬
      orderBy: 'viewCount',
      orderDirection: 'desc',
    }),
    [selectedGenre],
  );

  const { data, isFetching } = useQuery(getShowCatalogList(params));
  const shows = data?.content ?? [];
  const sourceShows = USE_MOCK_DATA ? MOCK_SHOWS : shows;
  const loading = !USE_MOCK_DATA && isFetching;
  const displayedShows =
    selectedGenre === 'ALL'
      ? sourceShows
      : sourceShows.filter((show) => show.genre === selectedGenre);

  return (
    <div className="space-y-6">
      <div className="flex flex-wrap gap-2">
        <Button
          type="button"
          size="sm"
          variant={selectedGenre === 'ALL' ? 'default' : 'outline'}
          onClick={() => setSelectedGenre('ALL')}
        >
          전체
        </Button>
        {GENRE_OPTIONS.map((genre) => (
          <Button
            key={genre.value}
            type="button"
            size="sm"
            variant={selectedGenre === genre.value ? 'default' : 'outline'}
            onClick={() => setSelectedGenre(genre.value)}
          >
            {genre.label}
          </Button>
        ))}
      </div>

      {loading ? (
        <div
          className={`${CONTENT_MIN_HEIGHT_CLASS} flex flex-col items-center justify-center gap-3 rounded-lg border bg-white`}
        >
          <Loader2 className="h-7 w-7 animate-spin text-slate-500" />
          <p className="text-sm text-slate-500">공연 목록을 불러오는 중입니다.</p>
        </div>
      ) : displayedShows.length === 0 ? (
        <div
          className={`${CONTENT_MIN_HEIGHT_CLASS} flex items-center justify-center rounded-lg border border-dashed p-10 text-center text-sm text-slate-500`}
        >
          선택한 장르의 공연이 없습니다.
        </div>
      ) : (
        <div className={`${CONTENT_MIN_HEIGHT_CLASS} space-y-3`}>
          <Carousel
            opts={{
              align: 'start',
              loop: false,
            }}
            className="w-full px-8"
          >
          <CarouselContent>
            {displayedShows.map((show, index) => (
              <CarouselItem key={show.id} className="basis-full md:basis-1/2 lg:basis-1/3 xl:basis-1/4">
                <ShowListItemCard
                  show={show}
                  href={`/shows/${show.id}`}
                  showRank={showRank}
                  rank={index + 1}
                />
              </CarouselItem>
            ))}
          </CarouselContent>
          <CarouselPrevious />
          <CarouselNext />
          </Carousel>
        </div>
      )}
    </div>
  );
}
