'use client';

import Link from 'next/link';
import { usePathname } from 'next/navigation';
import {
  Sidebar,
  SidebarContent,
  SidebarFooter,
  SidebarGroup,
  SidebarGroupContent,
  SidebarGroupLabel,
  SidebarHeader,
  SidebarMenu,
  SidebarMenuButton,
  SidebarMenuItem,
} from '@/components/atoms/sidebar';
import { Avatar, AvatarFallback, AvatarImage } from '@/components/atoms/avatar';
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from '@/components/atoms/dropdown-menu';
import { Armchair, LayoutDashboard, Theater, Users } from 'lucide-react';
import { ADMIN_ROUTE_LABELS, ADMIN_ROUTES } from '@/constants/route/adminRoutes';
import { useEffect, useState } from 'react';
import { useAuth, useConfirmLogout } from '@/lib/auth/AuthContext';

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

export function AdminSidebar() {
  const pathname = usePathname();
  const { user } = useAuth();
  const handleLogout = useConfirmLogout();

  // Next.js 하이드레이션 콘솔 에러로 mounted 상태 체크 추가
  const [mounted, setMounted] = useState(false);
  useEffect(() => setMounted(true), []);

  return (
    <Sidebar className="dark text-muted-foreground">
      <SidebarHeader>
        <Link
          href={ADMIN_ROUTE_ITEMS[0].href}
          className="font-logo flex h-14 items-center justify-center text-center text-[1.5rem]"
        >
          MOA Place
        </Link>
      </SidebarHeader>
      <SidebarContent>
        <SidebarGroup>
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

      <SidebarFooter>
        {mounted && (
          <DropdownMenu>
            <DropdownMenuTrigger asChild>
              <button className="hover:bg-muted flex w-full items-center gap-3 rounded-md px-2 py-2 text-sm">
                <Avatar className="h-8 w-8">
                  <AvatarImage src={user?.picture} />
                  <AvatarFallback>{(user?.name || '관리자').slice(0, 1)}</AvatarFallback>
                </Avatar>
                <div className="flex flex-col text-left leading-tight">
                  <span className="font-medium">{user?.name || '관리자'}</span>
                  <span className="text-muted-foreground text-xs">{user?.email || '-'}</span>
                </div>
              </button>
            </DropdownMenuTrigger>
            <DropdownMenuContent side="top" align="start">
              <DropdownMenuItem onClick={handleLogout}>로그아웃</DropdownMenuItem>
            </DropdownMenuContent>
          </DropdownMenu>
        )}
      </SidebarFooter>
    </Sidebar>
  );
}
