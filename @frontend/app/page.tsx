'use client';

import Link from 'next/link';
import { ADMIN_ROUTES } from '@/constants/route/adminRoutes';
import { Button } from '@/components/atoms';
import { useAuth, useConfirmLogout } from '@/lib/auth/AuthContext';

export default function Home() {
  const { isLoggedIn, isLoading, user } = useAuth();
  const handleLogout = useConfirmLogout();

  return (
    <div className="bg-muted/30 flex min-h-screen flex-col items-center justify-center px-4 py-12">
      <h1 className="text-foreground text-2xl font-semibold">임시 메인화면</h1>
      {!isLoading && isLoggedIn && (
        <>
          <p className="text-muted-foreground mt-2 text-sm">현재 로그인 중</p>
          <p className="text-muted-foreground mt-2 text-sm">이름: {user?.name}</p>
          <p className="text-muted-foreground text-sm">이메일: {user?.email}</p>
          <p className="text-muted-foreground text-sm">소셜: {user?.provider}</p>
          <p className="text-muted-foreground text-sm">권한: {user?.role}</p>
        </>
      )}
      <div className="mt-8 flex gap-3">
        {!isLoading && isLoggedIn ? (
          <Button size="lg" onClick={handleLogout}>
            로그아웃
          </Button>
        ) : (
          <Button variant="outline" size="lg" asChild>
            <Link href="/login">로그인</Link>
          </Button>
        )}
        <Button variant="outline" size="lg" asChild>
          <Link href={ADMIN_ROUTES.DASHBOARD}>관리자 페이지</Link>
        </Button>
      </div>
    </div>
  );
}
