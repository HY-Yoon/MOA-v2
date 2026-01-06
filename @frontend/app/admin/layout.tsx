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
      <div className="flex min-h-screen w-full ">
        <AdminSidebar />
        <main className="flex-1 bg-muted/40 p-6">
          <div className="mb-4 pb-4 border-b">
            <AdminBreadcrumb />
          </div>
          {children}
        </main>
      </div>
    </SidebarProvider>
  );
}
