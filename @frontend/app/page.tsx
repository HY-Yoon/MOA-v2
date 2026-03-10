'use client';

import { useReservationPopup } from '@/hooks/useReservationPopup';
import Image from 'next/image';
import ShowList from "./(home)/_components/ShowList";

// 메인 홈
export default function Home() {
  const { openReservationPopup } = useReservationPopup();

  const handleOpenReservationPopup = async () => {
    await openReservationPopup({
      showId: 16,
      scheduleId: 19,
      title: '예매 테스트 공연',
      showDate: '2026-01-15',
    });
  };

  return (
    <div className="min-h-screen">
      {/* Banner Section */}
      <section>
        <div className="container mx-auto px-4">
          <div className="border-border relative h-64 overflow-hidden rounded-sm border sm:h-80 lg:h-96">
            <Image
              src="https://ticketimage.interpark.com/TCMS3.0/NMain/BbannerPC/2602/260224093155_26001991.gif"
              alt="메인 배너"
              fill
              priority
              className="object-cover"
            />
          </div>
        </div>
      </section>

      {/* Main Content */}
      <section className="py-16">
        <div className="container mx-auto px-4">
          {/* Featured Shows */}
          <div className="mb-16">
            <h2 className="mb-8 text-2xl font-bold">장르별 인기공연</h2>

            <div className="mb-4">
              <button
                type="button"
                onClick={handleOpenReservationPopup}
                className="inline-flex items-center rounded-md bg-black px-4 py-2 text-sm font-medium text-white hover:bg-black/90"
              >
                예매 팝업 화면 테스트
              </button>
            </div>

            <ShowList showRank />
          </div>
        </div>
      </section>
    </div>
  );
}
