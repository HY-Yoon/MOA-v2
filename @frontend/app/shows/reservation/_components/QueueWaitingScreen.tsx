'use client';

import { HEADER_ROUTES } from '@/constants/route/userRoutes';
import { enterQueue, getQueueStatus } from '@/lib/api/queue';
import { useAuth } from '@/lib/auth/AuthContext';
import { Loader2 } from 'lucide-react';
import { useRouter } from 'next/navigation';
import { useCallback, useEffect, useState } from 'react';

interface QueueWaitingScreenProps {
  scheduleId: number;
  title?: string | null;
  showDate?: string | null;
}

const formatDurationFromSeconds = (valueSeconds: number | null) => {
  if (valueSeconds === null || Number.isNaN(valueSeconds)) {
    return '-';
  }

  const totalSeconds = Math.max(0, Math.floor(valueSeconds));
  const hours = Math.floor(totalSeconds / 3600);
  const minutes = Math.floor((totalSeconds % 3600) / 60);
  const seconds = totalSeconds % 60;

  if (hours > 0) {
    return `${hours}시간 ${minutes}분 ${seconds}초`;
  }
  if (minutes > 0) {
    return `${minutes}분 ${seconds}초`;
  }
  return `${seconds}초`;
};

const formatNumber = (value: number | null) => {
  if (value === null || Number.isNaN(value)) {
    return '-';
  }
  return new Intl.NumberFormat('ko-KR').format(Math.max(0, Math.floor(value)));
};

export default function QueueWaitingScreen({ scheduleId, title, showDate }: QueueWaitingScreenProps) {
  const router = useRouter();
  const { user, isLoading } = useAuth();
  const [queueToken, setQueueToken] = useState<string | null>(null);
  const [position, setPosition] = useState<number | null>(null);
  const [estimatedWaitTimeSeconds, setEstimatedWaitTimeSeconds] = useState<number | null>(null);
  const [phase, setPhase] = useState<'IDLE' | 'ENTERING' | 'WAITING' | 'READY' | 'EXPIRED' | 'ERROR'>(
    'IDLE',
  );
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [queueMessage, setQueueMessage] = useState<string | null>(null);
  const [retryAfterSeconds, setRetryAfterSeconds] = useState<number>(1);

  const handleEnterQueue = useCallback(
    async () => {
      if (!scheduleId || Number.isNaN(scheduleId)) {
        setPhase('ERROR');
        setErrorMessage('스케줄 식별자가 없습니다.');
        return;
      }
      if (!user?.providerId) {
        router.replace(HEADER_ROUTES.LOGIN);
        return;
      }

      try {
        setErrorMessage(null);
        setPhase('ENTERING');
        const queueData = await enterQueue({
          scheduleId,
        });

        setQueueMessage(queueData.message);
        setPosition(queueData.position);
        setEstimatedWaitTimeSeconds(queueData.estimatedWaitTimeSeconds);
        setRetryAfterSeconds(1);

        if (queueData.token) {
          setQueueToken(queueData.token);
          setPhase('READY');
          return;
        }

        setPhase('WAITING');
      } catch {
        setPhase('ERROR');
        setErrorMessage('대기열 진입에 실패했습니다. 잠시 후 다시 시도해주세요.');
      }
    },
    [router, scheduleId, user?.providerId],
  );

  useEffect(() => {
    if (isLoading) return;
    if (!user) {
      router.replace(HEADER_ROUTES.LOGIN);
      return;
    }
    void handleEnterQueue();
  }, [handleEnterQueue, isLoading, router, user]);

  useEffect(() => {
    if (phase !== 'WAITING') return;

    let cancelled = false;
    let timeoutId: number | null = null;

    const pollQueueStatus = async () => {
      try {
        const status = await getQueueStatus(scheduleId);
        if (cancelled) return;

        setQueueMessage(status.message);

        if (status.status === 'WAITING') {
          setPosition(status.position);
          setEstimatedWaitTimeSeconds(status.estimatedWaitTimeSeconds);
          setRetryAfterSeconds(status.retryAfterSeconds);
          timeoutId = window.setTimeout(
            () => {
              void pollQueueStatus();
            },
            Math.max(1, status.retryAfterSeconds) * 1000,
          );
          return;
        }

        if (status.status === 'READY') {
          setPosition(status.position);
          setEstimatedWaitTimeSeconds(status.estimatedWaitTimeSeconds);
          setQueueToken(status.token);
          setPhase('READY');
          return;
        }

        setQueueToken(null);
        setPhase('EXPIRED');
      } catch {
        if (!cancelled) {
          setPhase('ERROR');
          setErrorMessage('대기열 상태 조회에 실패했습니다. 잠시 후 다시 시도해주세요.');
        }
      }
    };

    void pollQueueStatus();

    return () => {
      cancelled = true;
      if (timeoutId) {
        window.clearTimeout(timeoutId);
      }
    };
  }, [phase, scheduleId]);

  if (phase === 'IDLE' || phase === 'ENTERING') {
    return (
      <section className="flex min-h-screen items-center justify-center bg-[#f3f3f3] p-6">
        <div className="flex flex-col items-center gap-6 text-center">
          <Loader2 className="h-16 w-16 animate-spin text-slate-500" />
          <div className="space-y-2">
            <p className="text-2xl font-bold text-black">예매 화면을 불러오는 중입니다.</p>
            <p className="text-2xl font-bold text-violet-600">조금만 기다려주세요.</p>
          </div>
        </div>
      </section>
    );
  }

  if (phase === 'WAITING') {
    return (
      <section className="min-h-screen m-auto flex flex-col justify-center w-full max-w-3xl p-8">
        <div className="space-y-2">
          <p className="text-2xl font-bold text-black">접속 인원이 많아 대기 중입니다.</p>
          <p className="text-2xl font-bold text-violet-600">조금만 기다려주세요.</p>
          <p className="pt-2 text-slate-700">
            {title ?? '공연'}
            {showDate ? ` · ${showDate}` : ''}
          </p>
        </div>

        <div className="mt-8 rounded-xl border border-slate-200 bg-white p-8 shadow-sm">
          <p className="text-center font-semibold text-black">나의 대기순서</p>
          <p className="mt-4 text-center text-2xl font-black tracking-tight text-black">
            {formatNumber(position)}
          </p>

          <div className="mt-6 h-4 w-full overflow-hidden rounded-full bg-slate-100">
            <div className="h-full w-10 rounded-full bg-violet-500" />
          </div>

          <div className="mt-7 border-t border-slate-200 pt-5">
            <div className="flex items-center justify-between">
              <span className="text-slate-700">현재 대기인원</span>
              <span className="font-bold text-black">{formatNumber(position)}명</span>
            </div>
            <p className="mt-3 text-right text-sm text-slate-500">
              예상 대기 시간 {formatDurationFromSeconds(estimatedWaitTimeSeconds)} · {retryAfterSeconds}초
              간격 갱신
            </p>
          </div>
        </div>
      </section>
    );
  }

  if (phase === 'EXPIRED') {
    return (
      <section className="min-h-screen m-auto flex flex-col justify-center max-w-3xl space-y-4 p-8">
        <div className="rounded-xl border border-slate-200 bg-white p-8 shadow-sm">
          <p className="text-2xl font-semibold text-violet-600">대기열이 만료되었습니다.</p>
          <p className="mt-2 text-sm text-slate-600">
            {queueMessage ?? '대기 시간이 만료되었습니다. 다시 대기열에 진입해주세요.'}
          </p>
          <div className="mt-5 flex items-center gap-2">
            <button
              type="button"
              onClick={() => window.close()}
              className="mt-5 inline-flex items-center rounded-md bg-black px-3 py-2 text-xs font-medium text-white hover:bg-black/90"
            >
              현재 창 종료
            </button>
          </div>
        </div>
      </section>
    );
  }

  

  return null;
}
