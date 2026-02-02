import { DATE_FORMAT } from '@/constants/common/dateFormat';
import dayjs from '@/plugins/dayjs';

/**
 * 날짜 문자열을 ISO 문자열로 변환 (API 요청용)
 * 타임존 정보 없이 로컬 시간 그대로 전송 (YYYY-MM-DDTHH:mm:ss)
 *
 * @param dateStr - 날짜 문자열
 * @returns ISO 문자열 (타임존 없음)
 */
export function stringToDate(dateStr: string): string {
  let fullDateString: string;

  if (dateStr.includes('T')) {
    // 이미 시간 정보가 있는 경우
    const hasSeconds = dateStr.split('T')[1].split(':').length === 3;
    fullDateString = hasSeconds ? dateStr : `${dateStr}:00`;
  } else {
    // 날짜만 있는 경우 자정(00:00:00)으로 설정
    fullDateString = `${dateStr}T00:00:00`;
  }

  // ISO 문자열 그대로 반환 (타임존 변환 없음)
  return dayjs(fullDateString).format(DATE_FORMAT.FULL_TIMEZONE);
}
