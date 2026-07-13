'use client';

import useEmblaCarousel from 'embla-carousel-react';
import Autoplay from 'embla-carousel-autoplay';
import Image from 'next/image';

const banners = [
  {
    src: 'https://ticketimage.interpark.com/TCMS3.0/NMain/BbannerPC/2607/260713090055_26009919.gif',
    alt: 'khalid',
    bgColor: '#000000',
  },
  {
    src: 'https://ticketimage.interpark.com/TCMS3.0/NMain/BbannerPC/2607/260713090709_22000354.gif',
    alt: 'nol',
    bgColor: '#E15E3B',
  },
  {
    src: 'https://ticketimage.interpark.com/TCMS3.0/NMain/BbannerPC/2606/260630093733_26009314.gif',
    alt: 'elisabeth',
    bgColor: '#4D4266',
  },
  {
    src: 'https://ticketimage.interpark.com/TCMS3.0/NMain/BbannerPC/2606/260615085250_26007895.gif',
    alt: 'busan',
    bgColor: '#6CC4EF',
  },
  {
    src: 'https://ticketimage.interpark.com/TCMS3.0/NMain/BbannerPC/2605/260527093711_26007442.gif',
    alt: 'dear',
    bgColor: '#2E2B30',
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
