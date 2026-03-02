'use client';

import { Button } from '@/components/atoms';
import { useEffect, useState } from 'react';

interface ScrollToTopButtonProps {
  showAfter?: number;
}

export default function ScrollToTopButton({ showAfter = 300 }: ScrollToTopButtonProps) {
  const [visible, setVisible] = useState(false);

  useEffect(() => {
    const onScroll = () => {
      setVisible(window.scrollY > showAfter);
    };

    onScroll();
    window.addEventListener('scroll', onScroll, { passive: true });

    return () => {
      window.removeEventListener('scroll', onScroll);
    };
  }, [showAfter]);

  if (!visible) {
    return null;
  }

  return (
    <Button
      type="button"
      variant="default"
      className="fixed right-6 bottom-6 z-50 h-[40px] w-[40px] cursor-pointer rounded-full bg-black p-0 text-[14px] text-white shadow-[0_2px_8px_rgba(0,0,0,0.15)] hover:bg-black/90"
      onClick={() => window.scrollTo({ top: 0, behavior: 'smooth' })}
      aria-label="페이지 최상단 이동"
    >
      TOP
    </Button>
  );
}
