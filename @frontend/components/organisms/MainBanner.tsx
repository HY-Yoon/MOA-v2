'use client';

import useEmblaCarousel from 'embla-carousel-react';
import Autoplay from 'embla-carousel-autoplay';
import Image from 'next/image';

const banners = [
  {
    src: 'https://ticketimage.interpark.com/TCMS3.0/NMain/BbannerPC/2603/260312093256_26003451.gif',
    alt: 'verdy',
    bgColor: '#E7E8E7',
  },
  {
    src: 'https://ticketimage.interpark.com/TCMS3.0/NMain/BbannerPC/2603/260303083142_26002980.gif',
    alt: '마리 로랑생',
    bgColor: '#98A0A2',
  },
  {
    src: 'https://ticketimage.interpark.com/TCMS3.0/NMain/BbannerPC/2601/260129081837_26001001.gif',
    alt: '빌리 엘리어트',
    bgColor: '#F7F7F7',
  },
  {
    src: 'https://ticketimage.interpark.com/TCMS3.0/NMain/BbannerPC/2604/260407082558_26002992.gif',
    alt: '엔하이픈',
    bgColor: '#214552',
  },
  {
    src: 'https://ticketimage.interpark.com/TCMS3.0/NMain/BbannerPC/2604/260402012900_P0004610.gif',
    alt: '디즈니',
    bgColor: '#000000',
  },
];

export default function MainBanner() {
  const [emblaRef] = useEmblaCarousel({ loop: true }, [Autoplay({ delay: 4000 })]);

  return (
    <div className="overflow-hidden" ref={emblaRef}>
      <div className="flex">
        {banners.map((banner, index) => (
          <div
            key={index}
            className="flex h-120 min-w-full items-center justify-center overflow-hidden"
            style={{ backgroundColor: banner.bgColor }}
          >
            <Image
              src={banner.src}
              alt={banner.alt}
              width={2000}
              height={560}
              priority={index === 0}
              className="h-120 w-auto max-w-none object-contain object-center"
            />
          </div>
        ))}
      </div>
    </div>
  );
}
