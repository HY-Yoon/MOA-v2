'use client';

import { SidebarProvider } from '@/components/atoms/sidebar';
import { AdminSidebar } from '@/components/organisms/AdminSidebar';
import { AdminBreadcrumb } from '@/components/organisms/AdminBreadcrumb';
import React from 'react';

export default function AdminLayout({ children }: { children: React.ReactNode }) {
  return (
    <SidebarProvider style={{ '--sidebar-width': '12rem' } as React.CSSProperties}>
      <div className="flex min-h-screen w-full">
        <AdminSidebar />
        <main className="bg-muted/40 flex-1">
          {/* Breadcrumb */}
          <div className="bg-background border-b px-6 py-4">
            <AdminBreadcrumb />
          </div>

          {/* 메인 콘텐츠 영역 */}
          <div className="in-h-screen bg-slate-50 py-8">
            <div className="mr-8 ml-8">{children}</div>
          </div>
        </main>
      </div>
    </SidebarProvider>
  );
}
