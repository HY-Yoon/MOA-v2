import { Gender } from '@shared/enums';

export const GENDER_LABELS: Record<Gender, string> = {
  MALE: '남',
  FEMALE: '여',
  OTHER: '기타',
};

export const GENDER_COLORS: Record<Gender, string> = {
  MALE: 'bg-blue-100 text-blue-800 border-blue-100',
  FEMALE: 'bg-pink-100 text-pink-800 border-pink-100',
  OTHER: 'bg-gray-100 text-gray-800 border-gray-100',
};

export const GENDER_OPTIONS = (Object.keys(GENDER_LABELS) as Gender[]).map((key) => ({
  label: GENDER_LABELS[key],
  value: key,
}));
