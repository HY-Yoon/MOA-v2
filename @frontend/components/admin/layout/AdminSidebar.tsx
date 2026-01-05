'use client';

import Link from 'next/link';
import { usePathname } from 'next/navigation';
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
} from '@/components/ui/sidebar';
import { Avatar, AvatarFallback, AvatarImage } from '@/components/ui/avatar';
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu';
import { LayoutDashboard, Theater, Armchair, Users, type LucideIcon } from 'lucide-react';
import { ADMIN_ROUTE_LABELS, ADMIN_ROUTES } from '@/constants/adminRoutes';
import { useEffect, useState } from 'react';

const ADMIN_ROUTE_ITEMS = [
  {
    title: ADMIN_ROUTE_LABELS[ADMIN_ROUTES.DASHBOARD],
    href: ADMIN_ROUTES.DASHBOARD,
    icon: LayoutDashboard,
  },
  {
    title: ADMIN_ROUTE_LABELS[ADMIN_ROUTES.SHOW],
    href: ADMIN_ROUTES.SHOW,
    icon: Theater,
  },
  {
    title: ADMIN_ROUTE_LABELS[ADMIN_ROUTES.SEAT],
    href: ADMIN_ROUTES.SEAT,
    icon: Armchair,
  },
  {
    title: ADMIN_ROUTE_LABELS[ADMIN_ROUTES.USER],
    href: ADMIN_ROUTES.USER,
    icon: Users,
  },
];

// 🔥 임시 관리자 정보 (나중에 auth로 교체)
const adminUser = {
  name: '관리자',
  email: 'admin@moa.com',
  image: '/avatar.png', // 없으면 fallback 사용됨
};

export function AdminSidebar() {
  const pathname = usePathname();

  // Next.js 하이드레이션 콘솔 에러로 mounted 상태 체크 추가
  const [mounted, setMounted] = useState(false);

  useEffect(() => setMounted(true), []);

  return (
    <Sidebar className="dark text-muted-foreground">
      <SidebarHeader>
        <Link
          href={ADMIN_ROUTE_ITEMS[0].href}
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
              {ADMIN_ROUTE_ITEMS.map((item) => (
                <SidebarMenuItem key={item.href}>
                  <SidebarMenuButton asChild isActive={pathname === item.href}>
                    <Link href={item.href} className="flex items-center gap-3">
                      <item.icon className="h-4 w-4" />
                      <span>{item.title as string}</span>
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
        {mounted && (
          <DropdownMenu>
            <DropdownMenuTrigger asChild>
              <button className="flex w-full items-center gap-3 rounded-md px-2 py-2 text-sm hover:bg-muted">
                <Avatar className="h-8 w-8">
                  <AvatarImage src={adminUser.image} />
                  <AvatarFallback>{adminUser.name.slice(0, 1)}</AvatarFallback>
                </Avatar>

                <div className="flex flex-col text-left leading-tight">
                  <span className="font-medium">{adminUser.name}</span>
                  <span className="text-xs text-muted-foreground">{adminUser.email}</span>
                </div>
              </button>
            </DropdownMenuTrigger>

            <DropdownMenuContent side="top" align="start">
              <DropdownMenuItem>로그아웃</DropdownMenuItem>
            </DropdownMenuContent>
          </DropdownMenu>
        )}
      </SidebarFooter>
    </Sidebar>
  );
}
