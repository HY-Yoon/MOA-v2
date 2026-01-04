"use client"

import Link from "next/link"
import { usePathname } from "next/navigation"
import {
  Sidebar,
  SidebarContent,
  SidebarGroup,
  SidebarGroupContent,
  SidebarGroupLabel,
  SidebarMenu,
  SidebarMenuButton,
  SidebarMenuItem,
  SidebarFooter,
  SidebarHeader,
} from "@/components/ui/sidebar"
import {
  Avatar,
  AvatarFallback,
  AvatarImage,
} from "@/components/ui/avatar"
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu"

import {
  LayoutDashboard,
  Theater,
  Armchair,
  Users,
} from "lucide-react"

const items = [
  {
    title: "대시보드",
    href: "/admin",
    icon: LayoutDashboard,
  },
  {
    title: "공연관리",
    href: "/admin/show",
    icon: Theater,
  },
  {
    title: "좌석관리",
    href: "/admin/seat",
    icon: Armchair,
  },
  {
    title: "회원관리",
    href: "/admin/user",
    icon: Users,
  },
]

// 🔥 임시 관리자 정보 (나중에 auth로 교체)
const adminUser = {
  name: "관리자",
  email: "admin@moa.com",
  image: "/avatar.png", // 없으면 fallback 사용됨
}

export function AdminSidebar() {
  const pathname = usePathname()

  return (
    <Sidebar className="dark text-muted-foreground">
      <SidebarHeader>
        <Link
          href="/admin"
          className="flex h-14 items-center text-center justify-center text-[1.5rem] font-logo "
        >
          MOA Place
        </Link>
      </SidebarHeader>
      <SidebarContent>
        <SidebarGroup>
          <SidebarGroupLabel>Admin</SidebarGroupLabel>

          <SidebarGroupContent>
            <SidebarMenu>
              {items.map((item) => (
                <SidebarMenuItem key={item.href}>
                  <SidebarMenuButton
                    asChild
                    isActive={pathname === item.href}
                  >
                    <Link href={item.href} className="flex items-center gap-3">
                      <item.icon className="h-4 w-4" />
                      <span>{item.title}</span>
                    </Link>
                  </SidebarMenuButton>
                </SidebarMenuItem>
              ))}
            </SidebarMenu>
          </SidebarGroupContent>
        </SidebarGroup>
      </SidebarContent>

      {/* 🔽 Sidebar Footer */}
      <SidebarFooter>
        <DropdownMenu>
          <DropdownMenuTrigger asChild>
            <button className="flex w-full items-center gap-3 rounded-md px-2 py-2 text-sm hover:bg-muted">
              <Avatar className="h-8 w-8">
                <AvatarImage src={adminUser.image} />
                <AvatarFallback>
                  {adminUser.name.slice(0, 1)}
                </AvatarFallback>
              </Avatar>

              <div className="flex flex-col text-left leading-tight">
                <span className="font-medium">{adminUser.name}</span>
                <span className="text-xs text-muted-foreground">
                  {adminUser.email}
                </span>
              </div>
            </button>
          </DropdownMenuTrigger>

          <DropdownMenuContent side="top" align="start">
            <DropdownMenuItem>로그아웃</DropdownMenuItem>
          </DropdownMenuContent>
        </DropdownMenu>
      </SidebarFooter>
    </Sidebar>
  )
}
