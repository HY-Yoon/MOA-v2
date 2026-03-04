import { GENRE_LABELS, REGION_LABELS } from '@/constants/common';

export function getGenreLabel(genre: ShowCatalog.List['genre']) {
  if (genre in GENRE_LABELS) {
    return GENRE_LABELS[genre as keyof typeof GENRE_LABELS];
  }
  return String(genre);
}

export const getRegionLabel = (region: ShowCatalog.List['location']['region']) => {
  if (typeof region === 'string' && region in REGION_LABELS) {
    return REGION_LABELS[region as keyof typeof REGION_LABELS];
  }
  return String(region);
};
