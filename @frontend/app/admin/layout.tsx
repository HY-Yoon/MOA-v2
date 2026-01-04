import { SidebarProvider } from "@/components/ui/sidebar";
import { AdminSidebar } from "@/components/admin/layout/AdminSidebar";
import { AdminBreadcrumb } from "@/components/admin/layout/AdminBreadcrumb";

export default function AdminLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <SidebarProvider style={{ "--sidebar-width": "12rem" }}>
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
