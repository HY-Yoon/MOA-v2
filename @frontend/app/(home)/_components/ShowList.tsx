'use client';

import {
  Badge,
  Button,
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
  Carousel,
  CarouselContent,
  CarouselItem,
  CarouselNext,
  CarouselPrevious,
} from '@/components/atoms';
import { GENRE_LABELS, GENRE_OPTIONS, REGION_LABELS } from '@/constants/common';
import { getShowCatalogList } from '@/lib/api/show';
import { useQuery } from '@tanstack/react-query';
import { Loader2 } from 'lucide-react';
import Image from 'next/image';
import { useMemo, useState } from 'react';

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
  const displayedShows =
    selectedGenre === 'ALL'
      ? shows
      : shows.filter((show) => show.genre === selectedGenre);

  const getRegionLabel = (region: ShowCatalog.List['location']['region']) => {
    if (typeof region === 'string' && region in REGION_LABELS) {
      return REGION_LABELS[region as keyof typeof REGION_LABELS];
    }
    return String(region);
  };
  const getGenreLabel = (genre: ShowCatalog.List['genre']) => {
    if (genre in GENRE_LABELS) {
      return GENRE_LABELS[genre as keyof typeof GENRE_LABELS];
    }
    return String(genre);
  };

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

      {isFetching ? (
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
                <Card className="h-full gap-3 py-0">
                  <div className="relative aspect-3/4 w-full overflow-hidden rounded-t-xl bg-slate-100">
                    {show.posterUrl ? (
                      <>
                        <Image
                          src={show.posterUrl}
                          alt={show.title}
                          fill
                          className="z-0 object-cover"
                        />
                        {showRank && (
                          <div
                            className="pointer-events-none absolute inset-0 z-1"
                            style={{
                              background:
                                'linear-gradient(0deg, rgba(0, 0, 0, .04), rgba(0, 0, 0, .04)), linear-gradient(180deg, transparent 69.07%, rgba(0, 0, 0, .36))',
                            }}
                          />
                        )}
                      </>
                    ) : (
                      <div className="flex h-full items-center justify-center text-sm text-slate-400">
                        포스터 없음
                      </div>
                    )}
                    {showRank && (
                      <div className="pointer-events-none absolute bottom-2 left-3 z-10 text-6xl font-black leading-none text-white drop-shadow-[0_4px_4px_rgba(0,0,0,0.2)]">
                        {index + 1}
                      </div>
                    )}
                  </div>
                  <CardHeader className="px-4 pt-4">
                    <Badge variant="secondary" className="w-fit">
                      {getGenreLabel(show.genre)}
                    </Badge>
                    <CardTitle className="line-clamp-1 text-base">{show.title}</CardTitle>
                    <CardDescription className="line-clamp-2">
                      {getRegionLabel(show.location.region)} · {show.location.venue}{' '}
                      {show.location.hallName}
                    </CardDescription>
                  </CardHeader>
                  <CardContent className="px-4 pb-4 pt-0 text-sm text-slate-600">
                    공연기간: {show.startDate} ~ {show.endDate}
                  </CardContent>
                </Card>
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
