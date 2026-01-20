// [dayjs] date format 관리
export const DATE_FORMAT = {
  FULL: 'YYYY-MM-DD HH:mm:ss',
  FULL_TIMEZONE: 'YYYY-MM-DDTHH:mm',
  FULL_DOT: 'YYYY.MM.DD HH:mm:ss',
  FULL_FLAT: 'YYYYMMDDHHmmss',
  YY_FLAT: 'YYMMDDHHmmss',
  DATE_ONLY: 'YYYY-MM-DD',
  TIME_ONLY: 'HH:mm:ss',
  HH_MM_ONLY: 'HH:mm',
  YEAR_MONTH: 'YYYY-MM',
  COMPACT_TIMESTAMP: 'YYMMDD-HHmmss',
} as const;
