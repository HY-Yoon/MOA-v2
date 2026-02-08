'use client';

import Link from 'next/link';
import { useEffect, useState } from 'react';
import { ADMIN_ROUTES } from '@/constants/route/adminRoutes';
import { Button } from '@/components/atoms';
import { useMutation, useQuery } from '@tanstack/react-query';
import { getUserInfo, logout } from '@/lib/api/login/auth';
import { useAlert } from '@/components/molecules/AlertContext';
import { queryClient } from '@/lib/query-client';

export default function Home() {
  const [isLoggedIn, setIsLoggedIn] = useState<boolean | null>(null);

  const { data } = useQuery(getUserInfo());
  const { mutateAsync: onLogout } = useMutation(logout());

  const { confirm } = useAlert();

  useEffect(() => {
    if (!data) return;

    // console.log('userInfo', data);
    setIsLoggedIn(!!data);
  }, [data]);

  async function handleLogout() {
    const result = await confirm({
      title: '로그아웃',
      description: '로그아웃 하시겠습니까?',
    });

    if (result) {
      await onLogout();
      // 회원정보 캐시 제거
      queryClient.removeQueries({ queryKey: ['auth', 'user'] });
      setIsLoggedIn(false);
    }
  }

  return (
    <div className="bg-muted/30 flex min-h-screen flex-col items-center justify-center px-4 py-12">
      <h1 className="text-foreground text-2xl font-semibold">임시 메인화면</h1>
      {isLoggedIn === true && <p className="text-muted-foreground mt-2 text-sm">현재 로그인중</p>}
      <div className="mt-8 flex gap-3">
        {isLoggedIn ? (
          <Button size="lg" asChild onClick={handleLogout}>
            <Link href="#">로그아웃</Link>
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
