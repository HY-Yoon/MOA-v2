'use client';

import Link from 'next/link';
import { useAuth, useConfirmLogout } from '@/lib/auth/AuthContext';
import { Button } from '@/components/atoms/button';
import {
  HEADER_ROUTES,
  HEADER_ROUTES_LABELS,
  USER_ROUTES,
  USER_ROUTES_LABELS,
} from '@/constants/route/userRoutes';
import { usePathname } from 'next/navigation';
import { ADMIN_ROUTES } from '@/constants/route/adminRoutes';

// 네비게이션 메뉴 설정
const NAV_ITEMS = [
  {
    label: USER_ROUTES_LABELS[USER_ROUTES.SHOW],
    href: USER_ROUTES.SHOW,
  },
  {
    label: USER_ROUTES_LABELS[USER_ROUTES.COMUNITY],
    href: USER_ROUTES.COMUNITY,
  },
];

// 로그인 안했을 때 헤더 유틸 메뉴
const UNAUTHENTICATED_ITEMS = [
  {
    label: HEADER_ROUTES_LABELS.unauthenticated[HEADER_ROUTES.LOGIN],
    href: HEADER_ROUTES.LOGIN,
    isLogout: false,
  },
  {
    label: HEADER_ROUTES_LABELS.unauthenticated[HEADER_ROUTES.JOIN],
    href: HEADER_ROUTES.JOIN,
    isLogout: false,
  },
];

// 로그인 했을 때 헤더 유틸 메뉴
const AUTHENTICATED_DROPDOWN_ITEMS = [
  {
    label: HEADER_ROUTES_LABELS.authenticated[HEADER_ROUTES.MY_PAGE],
    href: HEADER_ROUTES.MY_PAGE,
    isLogout: false,
  },
  {
    label: HEADER_ROUTES_LABELS.authenticated[HEADER_ROUTES.LOGOUT],
    href: HEADER_ROUTES.LOGOUT,
    isLogout: true,
  },
];

export function Header() {
  const { isLoggedIn, isLoading, user } = useAuth();
  const handleLogout = useConfirmLogout();
  const pathname = usePathname();

  // admin 경로는 GNB 표시 안 함
  if (pathname.startsWith(ADMIN_ROUTES.DASHBOARD)) {
    return null;
  }

  if (isLoading) {
    return null;
  }

  return (
    <header className="bg-background sticky top-0 z-40 shadow-sm">
      <div className="container mx-auto px-6">
        <div className="flex h-15 items-center justify-between">
          <div className="flex items-center gap-16">
            {/* Logo */}
            <div className="shrink-0">
              <Link href={USER_ROUTES.HOME} className="text-lg font-bold tracking-tight">
                MOA
              </Link>
            </div>

            {/*  Navigation */}
            <nav className="hidden gap-8 md:flex">
              {NAV_ITEMS.map((item) => (
                <Link
                  key={item.href}
                  href={item.href}
                  className="text-foreground hover:text-primary text-sm font-medium transition-colors"
                >
                  {item.label}
                </Link>
              ))}
            </nav>
          </div>

          {/* UTILS */}
          <div className="flex items-center gap-4">
            {/* 로그인 여부에 따른 헤더 유틸 메뉴 */}
            <div className="flex items-center gap-2 text-sm">
              {isLoggedIn && (
                <span className="text-foreground text-xs font-medium">{user?.name}님</span>
              )}

              {(isLoggedIn ? AUTHENTICATED_DROPDOWN_ITEMS : UNAUTHENTICATED_ITEMS).map(
                (item, index) => (
                  <div key={item.href} className="flex items-center gap-1">
                    {item.isLogout ? (
                      <Button
                        variant="ghost"
                        size="sm"
                        className="h-auto px-2 py-1 text-xs"
                        onClick={handleLogout}
                      >
                        {item.label}
                      </Button>
                    ) : (
                      <Button
                        asChild
                        variant="ghost"
                        size="sm"
                        className="h-auto px-2 py-1 text-xs"
                      >
                        <Link href={item.href}>{item.label}</Link>
                      </Button>
                    )}
                    {index <
                      (isLoggedIn ? AUTHENTICATED_DROPDOWN_ITEMS : UNAUTHENTICATED_ITEMS).length -
                        1 && <span className="text-slate-300">|</span>}
                  </div>
                ),
              )}
            </div>
          </div>
        </div>
      </div>
    </header>
  );
}
