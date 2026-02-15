'use client';

import Link from 'next/link';
import { usePathname } from 'next/navigation';
import { cn } from '@/lib/utils';
import { MY_PAGE_ROUTES, MY_PAGE_ROUTES_LABELS } from '@/constants/route/userRoutes';
import { User, Calendar, Bell } from 'lucide-react';
import { Card, CardContent } from '@/components/atoms/card';
import { Avatar, AvatarFallback, AvatarImage } from '@/components/atoms/avatar';
import { useAuth } from '@/lib/auth/AuthContext';

const MY_PAGE_LNB_ITEMS = [
  {
    href: MY_PAGE_ROUTES.RESERVATIONS,
    label: MY_PAGE_ROUTES_LABELS[MY_PAGE_ROUTES.RESERVATIONS],
    icon: Calendar,
  },
  {
    href: MY_PAGE_ROUTES.NOTIFICATIONS,
    label: MY_PAGE_ROUTES_LABELS[MY_PAGE_ROUTES.NOTIFICATIONS],
    icon: Bell,
  },
  {
    href: MY_PAGE_ROUTES.INFO,
    label: MY_PAGE_ROUTES_LABELS[MY_PAGE_ROUTES.INFO],
    icon: User,
  },
] as const;

export function MyPageSideBar() {
  const pathname = usePathname();
  const { user } = useAuth();

  return (
    <nav className="border-border bg-muted/30 flex w-52 shrink-0 flex-col border-r">
      <div className="px-3 py-5">
        <Card className="gap-0 py-5">
          <CardContent className="flex items-start gap-3 px-4 py-2">
            <Avatar className="h-9 w-9 shrink-0">
              <AvatarImage src={user?.picture} />
              <AvatarFallback className="text-xs">
                {(user?.name || 'User').slice(0, 1)}
              </AvatarFallback>
            </Avatar>
            <div className="min-w-0 flex-1">
              <p className="truncate text-sm font-medium">{user?.name || '회원'}님</p>
              <p className="text-muted-foreground truncate text-xs">{user?.email || '-'}</p>
            </div>
          </CardContent>
        </Card>
      </div>
      <ul className="space-y-1 px-3 pb-6">
        {MY_PAGE_LNB_ITEMS.map((item) => {
          const isActive = pathname === item.href;
          return (
            <li key={item.href}>
              <Link
                href={item.href}
                className={cn(
                  'flex items-center gap-3 rounded-md px-3 py-2 text-sm font-medium transition-colors',
                  isActive
                    ? 'bg-primary text-primary-foreground'
                    : 'text-muted-foreground hover:bg-muted hover:text-foreground',
                )}
              >
                <item.icon className="h-4 w-4 shrink-0" />
                {item.label}
              </Link>
            </li>
          );
        })}
      </ul>
    </nav>
  );
}
