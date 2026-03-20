'use client';

import Image from 'next/image';
import ShowList from "./(home)/_components/ShowList";

// 메인 홈
export default function Home() {

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

            <ShowList showRank />
          </div>
        </div>
      </section>
    </div>
  );
}
