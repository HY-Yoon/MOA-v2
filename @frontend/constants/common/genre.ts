import { Genre } from '@shared/enums';

export const GENRE_LABELS: Record<Genre, string> = {
  MUSICAL: '뮤지컬',
  CONCERT: '콘서트',
  THEATER: '연극',
  CLASSIC: '클래식',
  DANCE: '무용',
};

export const GENRE_OPTIONS = (Object.keys(GENRE_LABELS) as Genre[]).map((key) => ({
  label: GENRE_LABELS[key],
  value: key,
}));
