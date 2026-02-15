'use client';

import { useEffect } from 'react';
import { useRouter } from 'next/navigation';
import { Loader2 } from 'lucide-react';
import { LOGIN_PATH, useAuth } from '@/lib/auth/AuthContext';
import { MyPageSideBar } from '@/components/organisms/MyPageSideBar';

export default function MyPageLayout({ children }: { children: React.ReactNode }) {
  const router = useRouter();
  const { isLoggedIn, isLoading } = useAuth();

  useEffect(() => {
    if (isLoading) return;
    if (!isLoggedIn) {
      router.replace(LOGIN_PATH);
    }
  }, [isLoggedIn, isLoading, router]);

  if (isLoading || !isLoggedIn) {
    return (
      <div className="bg-muted/30 flex min-h-screen w-full items-center justify-center">
        <div className="flex flex-col items-center gap-3">
          <Loader2 className="text-muted-foreground h-8 w-8 animate-spin" />
          <span className="text-muted-foreground text-sm">로딩 중...</span>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen">
      <div className="container mx-auto px-4 py-8">
        <div className="border-border flex overflow-hidden rounded-lg border">
          <MyPageSideBar />
          <main className="min-w-0 flex-1 p-8">{children}</main>
        </div>
      </div>
    </div>
  );
}
