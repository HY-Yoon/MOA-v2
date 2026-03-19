'use client';

import InfoField from '@/components/molecules/InfoField';
import dayjs from '@/plugins/dayjs';
import { DATE_FORMAT } from '@/constants/common/dateFormat';
import SectionLayout from '@/components/molecules/SectionLayout';

interface Props {
  data: Reservation.Detail;
}

export default function ReservationBasicInfo({ data }: Props) {
  const { reservationNumber, show, booker, schedule, seats = [] } = data || {};

  const scheduleLabel = schedule.showDate
    ? [
        dayjs(schedule.showDate).format(DATE_FORMAT.DATE_KR),
        schedule.showTime
          ? dayjs(`${schedule.showDate}T${schedule.showTime}`).format(DATE_FORMAT.TIME_KR)
          : '',
      ]
        .filter(Boolean)
        .join(' ')
    : '-';

  return (
    <SectionLayout
      title={show.title}
      cols={2}
      gridColsClassName="grid-cols-[minmax(200px,280px)_1fr]"
      showDivider={true}
    >
      <div className="relative min-h-0">
        <div className="absolute top-0 left-1/2 aspect-2/3 h-full w-auto -translate-x-1/2">
          <img
            src={show.posterUrl}
            alt={show.title}
            className="h-full w-full rounded border object-cover"
          />
        </div>
      </div>

      <div className="border border-slate-200">
        <div className="divide-y divide-slate-200">
          <InfoField label="예매자" content={booker.name} />
          <InfoField label="예매번호" content={reservationNumber} />
          <InfoField label="공연일" content={scheduleLabel} />
          <InfoField
            label="장소"
            content={`${schedule.location.venue} ${schedule.location.hallName}`}
          />
          <InfoField
            label="좌석"
            content={seats?.map(
              (seat: Reservation.Seats) => `${seat.sectionName}석 ${seat.row}열 ${seat.number}`,
            )}
          />
        </div>
      </div>
    </SectionLayout>
  );
}
