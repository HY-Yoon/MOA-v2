import dayjs from '@/plugins/dayjs';
import { DATE_FORMAT } from '@/constants/common/dateFormat';

/**
 * 날짜 문자열을 Date 객체로 변환 (API 요청용)
 * @param dateString - 날짜 문자열 (예: "2026-01-10")
 * @returns Date 객체
 */
export function stringToDate(dateString: string): Date {
  return dayjs(dateString).toDate();
}

/**
 * Date 객체를 날짜 문자열로 변환 (API 응답용)
 * @param date - Date 객체 또는 날짜 문자열
 * @returns 날짜 문자열 (예: "2026-01-10")
 */
export function dateToString(date: Date | string): string {
  return dayjs(date).format(DATE_FORMAT.DATE_ONLY);
}
