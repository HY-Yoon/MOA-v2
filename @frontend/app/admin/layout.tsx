'use client';

import { SidebarProvider } from '@/components/atoms/sidebar';
import { AdminSidebar } from '@/components/organisms/AdminSidebar';
import { AdminBreadcrumb } from '@/components/organisms/AdminBreadcrumb';
import React, { useEffect, useRef } from 'react';
import { Loader2 } from 'lucide-react';
import { useAlert } from '@/components/molecules/AlertContext';
import { setGlobalAlertHandler, setGlobalRouter } from '@/lib/api-client';
import { LOGIN_PATH, useAuth } from '@/lib/auth/AuthContext';
import { useRouter } from 'next/navigation';
import { UserRole } from '@shared/enums';

export default function AdminLayout({ children }: { children: React.ReactNode }) {
  const router = useRouter();
  const { alert } = useAlert();
  const { isLoggedIn, isLoading, user } = useAuth();

  // 중복 모달 방지
  const alertShown = useRef(false);

  const isAdmin = (role?: UserRole) => role === 'ADMIN';

  useEffect(() => {
    setGlobalAlertHandler(alert);
    setGlobalRouter(router);
  }, [alert, router]);

  // TODO: 관리자 권한 체크 예정
  // useEffect(() => {
  //   if (isLoading) return;
  //
  //   if (!isLoggedIn) {
  //     router.replace(LOGIN_PATH);
  //     return;
  //   }
  //
  //   if (isLoggedIn && !isAdmin(user?.role)) {
  //     if (!alertShown.current) {
  //       alertShown.current = true;
  //       alert({
  //         title: '접근 불가',
  //         description: '권한이 없습니다. 관리자만 접근 가능합니다.',
  //       }).then(() => router.replace('/'));
  //     }
  //   }
  // }, [isLoggedIn, isLoading, router, user?.role, alert]);
  //
  // const allowedAdmin = !isLoading && isLoggedIn && isAdmin(user?.role);
  //
  // if (!allowedAdmin) {
  //   return (
  //     <div className="bg-muted/30 flex min-h-screen w-full items-center justify-center">
  //       <div className="flex flex-col items-center gap-3">
  //         <Loader2 className="text-muted-foreground h-8 w-8 animate-spin" />
  //         <span className="text-muted-foreground text-sm">로딩 중...</span>
  //       </div>
  //     </div>
  //   );
  // }

  return (
    <SidebarProvider style={{ '--sidebar-width': '12rem' } as React.CSSProperties}>
      <div className="flex min-h-screen w-full">
        <AdminSidebar />
        <main className="bg-muted/40 flex-1">
          <div className="bg-background border-b px-6 py-4">
            <AdminBreadcrumb />
          </div>
          <div className="in-h-screen bg-slate-50 py-8">
            <div className="mr-8 ml-8">{children}</div>
          </div>
        </main>
      </div>
    </SidebarProvider>
  );
}
