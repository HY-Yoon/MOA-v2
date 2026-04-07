'use client';

import useEmblaCarousel from 'embla-carousel-react';
import Autoplay from 'embla-carousel-autoplay';
import Image from 'next/image';

const banners = [
  {
    src: 'https://ticketimage.interpark.com/TCMS3.0/NMain/BbannerPC/2603/260312093256_26003451.gif',
    alt: '메인 1',
  },
  {
    src: 'https://ticketimage.interpark.com/TCMS3.0/NMain/BbannerPC/2603/260303083142_26002980.gif',
    alt: '메인 2',
  },
  {
    src: 'https://ticketimage.interpark.com/TCMS3.0/NMain/BbannerPC/2601/260129081837_26001001.gif',
    alt: '메인 3',
  },
  {
    src: 'https://ticketimage.interpark.com/TCMS3.0/NMain/BbannerPC/2604/260407082558_26002992.gif',
    alt: '메인 4',
  },
];

export default function MainBanner() {
  const [emblaRef] = useEmblaCarousel({ loop: true }, [Autoplay({ delay: 4000 })]);

  return (
    <div className="overflow-hidden" ref={emblaRef}>
      <div className="flex">
        {banners.map((banner, index) => (
          <div key={index} className="relative h-72 min-w-full sm:h-96 lg:h-[480px]">
            <Image
              src={banner.src}
              alt={banner.alt}
              fill
              priority={index === 0}
              className="object-cover object-top"
            />
          </div>
        ))}
      </div>
    </div>
  );
}
