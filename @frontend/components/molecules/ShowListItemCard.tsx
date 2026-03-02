import { Badge, Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/atoms';
import { GENRE_LABELS, REGION_LABELS } from '@/constants/common';
import Image from 'next/image';
import Link from 'next/link';

interface ShowListItemCardProps {
  show: ShowCatalog.List;
  href: string;
  showRank?: boolean;
  rank?: number;
}

export default function ShowListItemCard({
  show,
  href,
  showRank = false,
  rank,
}: ShowListItemCardProps) {
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
    <Link href={href} className="block h-full">
      <Card className="h-full gap-3 py-0 transition hover:shadow-md">
        <div className="relative aspect-3/4 w-full overflow-hidden rounded-t-xl bg-slate-100">
          {show.posterUrl ? (
            <>
              <Image src={show.posterUrl} alt={show.title} fill className="z-0 object-cover" />
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
          {showRank && typeof rank === 'number' && (
            <div className="pointer-events-none absolute bottom-2 left-3 z-10 text-6xl font-black leading-none text-white drop-shadow-[0_4px_4px_rgba(0,0,0,0.2)]">
              {rank}
            </div>
          )}
        </div>
        <CardHeader className="px-4 pt-4">
          <Badge variant="secondary" className="w-fit">
            {getGenreLabel(show.genre)}
          </Badge>
          <CardTitle className="line-clamp-1 text-base">{show.title}</CardTitle>
          <CardDescription className="line-clamp-2">
            {getRegionLabel(show.location.region)} · {show.location.venue} {show.location.hallName}
          </CardDescription>
        </CardHeader>
        <CardContent className="px-4 pb-4 pt-0 text-sm text-slate-600">
          공연기간: {show.startDate} ~ {show.endDate}
        </CardContent>
      </Card>
    </Link>
  );
}
