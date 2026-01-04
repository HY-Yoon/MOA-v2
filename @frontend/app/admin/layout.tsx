import { SidebarProvider } from "@/components/ui/sidebar"
import { AdminSidebar } from "@/components/admin/admin-sidebar/AdminSidebarComponent"

export default function AdminLayout({
  children,
}: {
  children: React.ReactNode
}) {
  return (
    <SidebarProvider style={{'--sidebar-width': "12rem"}}>
      <div className="flex min-h-screen w-full ">
        <AdminSidebar />
        <main className="flex-1 bg-muted/40 p-6">
          {children}
        </main>
      </div>
    </SidebarProvider>
  )
}
