// [dayjs] date format 관리
export const DATE_FORMAT = {
  FULL: 'YYYY-MM-DD HH:mm',
  FULL_TIMEZONE: 'YYYY-MM-DDTHH:mm',
  FULL_DOT: 'YYYY.MM.DD HH:mm:ss',
  FULL_FLAT: 'YYYYMMDDHHmmss',
  YY_FLAT: 'YYMMDDHHmmss',
  DATE_ONLY: 'YYYY-MM-DD',
  TIME_ONLY: 'HH:mm:ss',
  HH_MM_ONLY: 'HH:mm',
  YEAR_MONTH: 'YYYY-MM',
  COMPACT_TIMESTAMP: 'YYMMDD-HHmmss',
  DATE_KR: 'YYYY년 MM월 DD일',
  TIME_KR: 'HH시 mm분',
} as const;

export const DATE_UNIT = {
  MILLISECOND: 'millisecond',
  SECOND: 'second',
  MINUTE: 'minute',
  HOUR: 'hour',
  DAY: 'day',
  MONTH: 'month',
  YEAR: 'year',
  DATE: 'date',
} as const;
