'use client';

import { Button, Input, Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/atoms';
import { ShowListItemCard } from '@/components/molecules';
import { GENRE_OPTIONS, REGION_OPTIONS } from '@/constants/common';
import { fetchShowCatalogList } from '@/lib/api/show';
import { useInfiniteQuery } from '@tanstack/react-query';
import { Loader2, Search } from 'lucide-react';
import { useEffect, useMemo, useRef, useState } from 'react';

const PAGE_SIZE = 20;
const USE_MOCK_DATA = true;
const DEFAULT_SORT: SortLabel = '인기순';

const SORT_OPTIONS = [
  { label: '인기순', orderBy: 'viewCount', orderDirection: 'desc' as const },
  { label: '공연임박순', orderBy: 'startDate', orderDirection: 'asc' as const },
  { label: '최신순', orderBy: 'createdAt', orderDirection: 'desc' as const },
];

type SortLabel = (typeof SORT_OPTIONS)[number]['label'];

const MOCK_POSTERS = [
  'https://ticketimage.interpark.com/Play/image/large/26/26001820_p.gif',
  'https://ticketimage.interpark.com/Play/image/large/25/25005738_p.gif',
  'https://ticketimage.interpark.com/Play/image/large/25/25005103_p.gif',
  'https://ticketimage.interpark.com/Play/image/large/25/25006370_p.gif',
  'https://ticketimage.interpark.com/Play/image/large/25/25006774_p.gif',
];

const toComparableDate = (value: string) => new Date(value.replaceAll('.', '-')).getTime();

const createMockShows = (count: number): ShowCatalog.List[] => {
  const genres = GENRE_OPTIONS.map((option) => option.value);
  const regions = REGION_OPTIONS.map((option) => option.value);

  return Array.from({ length: count }, (_, index) => {
    const start = new Date(2025, 0, 1 + index * 2);
    const end = new Date(start);
    end.setDate(start.getDate() + 45);

    const startDate = start.toISOString().slice(0, 10);
    const endDate = end.toISOString().slice(0, 10);

    return {
      id: index + 1,
      title: `목업 공연 ${index + 1}`,
      genre: genres[index % genres.length],
      status: 'ONGOING',
      saleStatus: 'ON_SALE',
      posterUrl: MOCK_POSTERS[index % MOCK_POSTERS.length] ?? '',
      location: {
        region: regions[index % regions.length],
        venue: `목업 공연장 ${index % 12}`,
        hallName: `${(index % 4) + 1}관`,
      },
      salePeriod: { startDate, endDate },
      createdAt: new Date(2026, 0, 1 + index).toISOString(),
      viewCount: 5000 - index * 15,
      startDate,
      endDate,
      schedules: [],
    };
  });
};

const getMockShowCatalogPage = async (
  params: ShowCatalog.ListParams,
  source: ShowCatalog.List[],
): Promise<Api.ListResponse<ShowCatalog.List>> => {
  const page = params.page ?? 0;
  const size = params.size ?? PAGE_SIZE;
  const keyword = params.keyword?.trim().toLowerCase() ?? '';

  const filtered = source
    .filter((show) => (params.genre ? show.genre === params.genre : true))
    .filter((show) => (params.region ? show.location.region === params.region : true))
    .filter((show) => {
      if (!keyword) return true;
      const searchable = `${show.title} ${show.location.venue} ${show.location.hallName}`.toLowerCase();
      return searchable.includes(keyword);
    })
    .filter((show) => {
      const showStart = toComparableDate(show.startDate);
      const showEnd = toComparableDate(show.endDate);
      const filterStart = params.startDate ? toComparableDate(params.startDate) : null;
      const filterEnd = params.endDate ? toComparableDate(params.endDate) : null;
      if (filterStart && showEnd < filterStart) return false;
      if (filterEnd && showStart > filterEnd) return false;
      return true;
    });

  const sorted = [...filtered].sort((a, b) => {
    const direction = params.orderDirection === 'asc' ? 1 : -1;
    switch (params.orderBy) {
      case 'startDate':
        return (toComparableDate(a.startDate) - toComparableDate(b.startDate)) * direction;
      case 'createdAt':
        return (toComparableDate(a.createdAt) - toComparableDate(b.createdAt)) * direction;
      case 'viewCount':
      default:
        return (a.viewCount - b.viewCount) * direction;
    }
  });

  const totalElements = sorted.length;
  const totalPages = Math.ceil(totalElements / size);
  const startIndex = page * size;
  const content = sorted.slice(startIndex, startIndex + size);

  await new Promise((resolve) => setTimeout(resolve, 250));

  return {
    content,
    page,
    size,
    totalElements,
    totalPages,
    first: page === 0,
    last: page + 1 >= totalPages,
  };
};

const getTodayDateString = () => {
  const today = new Date();
  const timezoneOffsetMs = today.getTimezoneOffset() * 60 * 1000;
  return new Date(today.getTime() - timezoneOffsetMs).toISOString().slice(0, 10);
};

export default function ShowCatalogList() {
  const [searchInput, setSearchInput] = useState('');
  const [keyword, setKeyword] = useState('');
  const [selectedRegion, setSelectedRegion] = useState<ShowCatalog.Region | 'ALL'>('ALL');
  const [selectedGenre, setSelectedGenre] = useState<ShowCatalog.Genre | 'ALL'>('ALL');
  const [startDate, setStartDate] = useState('');
  const [endDate, setEndDate] = useState('');
  const [selectedSort, setSelectedSort] = useState<SortLabel>(DEFAULT_SORT);
  const sentinelRef = useRef<HTMLDivElement | null>(null);
  const mockShows = useMemo(() => createMockShows(120), []);
  const todayDate = useMemo(() => getTodayDateString(), []);

  const sortConfig = useMemo(
    () => SORT_OPTIONS.find((option) => option.label === selectedSort) ?? SORT_OPTIONS[0],
    [selectedSort],
  );

  const baseParams = useMemo<Omit<ShowCatalog.ListParams, 'page' | 'size'>>(
    () => ({
      ...(keyword.trim() && { keyword: keyword.trim() }),
      ...(selectedRegion !== 'ALL' && { region: selectedRegion }),
      ...(selectedGenre !== 'ALL' && { genre: selectedGenre }),
      ...(startDate && { startDate }),
      ...(endDate && { endDate }),
      orderBy: sortConfig.orderBy,
      orderDirection: sortConfig.orderDirection,
    }),
    [keyword, selectedRegion, selectedGenre, startDate, endDate, sortConfig],
  );

  const { data, isFetching, isFetchingNextPage, hasNextPage, fetchNextPage, refetch } = useInfiniteQuery({
    queryKey: ['show', 'catalog', 'infinite', baseParams],
    initialPageParam: 0,
    queryFn: async ({ pageParam }) => {
      const requestParams = {
        ...baseParams,
        page: pageParam,
        size: PAGE_SIZE,
      };
      const apiResponse = await fetchShowCatalogList(requestParams).catch(() => undefined);
      if (USE_MOCK_DATA) {
        return getMockShowCatalogPage(requestParams, mockShows);
      }
      return apiResponse;
    },
    getNextPageParam: (lastPage) => {
      if (!lastPage || lastPage.last) {
        return undefined;
      }
      return lastPage.page + 1;
    },
  });

  const shows = useMemo(() => data?.pages.flatMap((page) => page?.content ?? []) ?? [], [data]);
  const isInitialLoading = isFetching && shows.length === 0;

  useEffect(() => {
    const target = sentinelRef.current;
    if (!target || !hasNextPage || isFetchingNextPage) {
      return;
    }

    const observer = new IntersectionObserver(
      (entries) => {
        if (entries[0]?.isIntersecting) {
          fetchNextPage();
        }
      },
      { rootMargin: '200px' },
    );

    observer.observe(target);
    return () => observer.disconnect();
  }, [hasNextPage, isFetchingNextPage, fetchNextPage]);

  const onSubmitSearch = () => setKeyword(searchInput);

  return (
    <div className="space-y-6">
      <div className="space-y-4 rounded-xl border bg-white p-4">
        <div className="relative">
          <Input
            value={searchInput}
            onChange={(e) => setSearchInput(e.target.value)}
            onKeyDown={(e) => {
              if (e.key === 'Enter') {
                onSubmitSearch();
              }
            }}
            placeholder="공연·전시명 또는 지역명을 입력하세요"
            className="pr-10"
          />
          <button
            type="button"
            onClick={onSubmitSearch}
            className="text-muted-foreground hover:text-foreground absolute right-3 top-1/2 -translate-y-1/2"
          >
            <Search className="h-4 w-4" />
          </button>
        </div>

        <div className="flex flex-wrap items-center gap-2">
          <Select value={selectedRegion} onValueChange={(value) => setSelectedRegion(value as ShowCatalog.Region | 'ALL')}>
            <SelectTrigger className="w-[150px]">
              <SelectValue placeholder="지역" />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="ALL">전체 지역</SelectItem>
              {REGION_OPTIONS.map((option) => (
                <SelectItem key={option.value} value={option.value}>
                  {option.label}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>

          <Select value={selectedGenre} onValueChange={(value) => setSelectedGenre(value as ShowCatalog.Genre | 'ALL')}>
            <SelectTrigger className="w-[150px]">
              <SelectValue placeholder="장르" />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="ALL">전체 장르</SelectItem>
              {GENRE_OPTIONS.map((option) => (
                <SelectItem key={option.value} value={option.value}>
                  {option.label}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>

          <Input
            type="date"
            min={todayDate}
            value={startDate}
            onChange={(e) => setStartDate(e.target.value)}
            className="w-[160px]"
          />
          <span className="text-muted-foreground text-sm">~</span>
          <Input
            type="date"
            min={todayDate}
            value={endDate}
            onChange={(e) => setEndDate(e.target.value)}
            className="w-[160px]"
          />

          <Select value={selectedSort} onValueChange={(value) => setSelectedSort(value as SortLabel)}>
            <SelectTrigger className="ml-auto w-[140px]">
              <SelectValue />
            </SelectTrigger>
            <SelectContent>
              {SORT_OPTIONS.map((option) => (
                <SelectItem key={option.label} value={option.label}>
                  {option.label}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>

          <Button
            type="button"
            variant="outline"
            onClick={() => {
              const isAlreadyDefault =
                searchInput === '' &&
                keyword === '' &&
                selectedRegion === 'ALL' &&
                selectedGenre === 'ALL' &&
                startDate === '' &&
                endDate === '' &&
                selectedSort === DEFAULT_SORT;

              setSearchInput('');
              setKeyword('');
              setSelectedRegion('ALL');
              setSelectedGenre('ALL');
              setStartDate('');
              setEndDate('');
              setSelectedSort(DEFAULT_SORT);

              // 이미 기본값이면 queryKey 변화가 없으므로 강제 재요청
              if (isAlreadyDefault) {
                void refetch();
              }
            }}
          >
            초기화
          </Button>
        </div>
      </div>

      {isInitialLoading ? (
        <div className="flex min-h-[380px] items-center justify-center rounded-lg border">
          <Loader2 className="text-muted-foreground h-8 w-8 animate-spin" />
        </div>
      ) : shows.length === 0 ? (
        <div className="text-muted-foreground flex min-h-[380px] items-center justify-center rounded-lg border border-dashed text-sm">
          조건에 맞는 공연이 없습니다.
        </div>
      ) : (
        <div className="space-y-6">
          <div className="grid grid-cols-2 gap-4 md:grid-cols-3 lg:grid-cols-5">
            {shows.map((show) => (
              <ShowListItemCard key={show.id} show={show} href={`/show/detail/${show.id}`} />
            ))}
          </div>

          <div ref={sentinelRef} className="flex h-12 items-center justify-center">
            {isFetchingNextPage ? <Loader2 className="text-muted-foreground h-5 w-5 animate-spin" /> : null}
            {!hasNextPage && shows.length > 0 ? (
              <p className="text-muted-foreground text-sm">마지막 공연입니다.</p>
            ) : null}
          </div>
        </div>
      )}
    </div>
  );
}
