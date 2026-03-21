'use client';

import { useEffect, useMemo, useRef, useState } from 'react';

interface PaymentCountdownProps {
  expiresAt: number;
  fallbackRemainingSeconds?: number;
}

function formatCountdown(totalSeconds: number) {
  const safe = Math.max(0, totalSeconds);
  const minutes = Math.floor(safe / 60);
  const seconds = safe % 60;
  return `${minutes}:${String(seconds).padStart(2, '0')}`;
}

export default function PaymentCountdown({
  expiresAt,
  fallbackRemainingSeconds = 0,
}: PaymentCountdownProps) {
  const initialRemaining = useMemo(() => {
    if (expiresAt > 0) {
      return Math.max(0, Math.floor((expiresAt - Date.now()) / 1000));
    }
    return Math.max(0, fallbackRemainingSeconds);
  }, [expiresAt, fallbackRemainingSeconds]);

  const [remainingSeconds, setRemainingSeconds] = useState(initialRemaining);
  const isExpiredHandledRef = useRef(false);

  useEffect(() => {
    setRemainingSeconds(initialRemaining);
  }, [initialRemaining]);

  useEffect(() => {
    const timer = window.setInterval(() => {
      setRemainingSeconds((prev) => Math.max(0, prev - 1));
    }, 1000);
    return () => window.clearInterval(timer);
  }, []);

  useEffect(() => {
    if (remainingSeconds > 0 || isExpiredHandledRef.current) return;
    isExpiredHandledRef.current = true;
    window.alert('결제 가능 시간이 종료되었습니다.');
    window.close();
  }, [remainingSeconds]);

  return (
    <p className="text-sm font-semibold text-slate-800">
      결제 가능 시간 <span className="text-primary">{formatCountdown(remainingSeconds)}</span>
    </p>
  );
}
