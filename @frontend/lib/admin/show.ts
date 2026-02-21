import dayjs from 'dayjs';

/**
 * 공연 일정 중에서 첫 번째 공연일을 찾는 함수
 * @param schedules - 공연 일정 객체 (Schedules: { showDate, showTime, openTicketTime })
 * @returns 날짜 문자열 (예: "2026-01-10")
 */
export function getFirstShowDate(schedules: Array<{ showDate: string }>): dayjs.Dayjs | null {
  if (!schedules || schedules.length === 0) return null;

  const validDates = schedules
    .map((schedule) => schedule.showDate)
    .filter((date) => date && dayjs(date).isValid())
    .map((date) => dayjs(date));

  if (validDates.length === 0) return null;

  return validDates.reduce((earliest, current) => {
    return current.isBefore(earliest) ? current : earliest;
  }, validDates[0]);
}
