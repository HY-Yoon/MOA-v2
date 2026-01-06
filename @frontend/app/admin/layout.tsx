import { SidebarProvider } from "@/components/ui/sidebar";
import { AdminSidebar } from "@/components/admin/layout/AdminSidebar";
import { AdminBreadcrumb } from "@/components/admin/layout/AdminBreadcrumb";
import React from "react";

export default function AdminLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <SidebarProvider style={{ "--sidebar-width": "12rem" } as React.CSSProperties}>
      <div className="flex min-h-screen w-full">
        <AdminSidebar />
        <main className="flex-1 bg-muted/40">
          {/* Breadcrumb */}
          <div className="border-b bg-background px-6 py-4">
            <AdminBreadcrumb />
          </div>

          {/* 메인 콘텐츠 영역 */}
          <div className="p-6">
            {children}
          </div>
        </main>
      </div>
    </SidebarProvider>
  );
}
