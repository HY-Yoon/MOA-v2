'use client';

import Link from 'next/link';
import { usePathname } from 'next/navigation';
import { cn } from '@/lib/utils';

interface NavItem {
  label: string;
  href: string;
}

export const NAV_ITEMS: NavItem[] = [
  {
    label: '공연',
    href: '/shows',
  },
  {
    label: '예매',
    href: '/booking',
  },
  {
    label: '마이페이지',
    href: '/my',
  },
];

export function GlobalNavBar() {
  const pathname = usePathname();

  // admin 경로는 GNB 표시 안 함
  if (pathname.startsWith('/admin')) {
    return null;
  }

  return (
    <nav className="border-b border-border bg-background">
      <div className="container mx-auto px-4">
        <ul className="flex items-center gap-8">
          {NAV_ITEMS.map((item) => (
            <li key={item.href}>
              <Link
                href={item.href}
                className={cn(
                  'py-4 text-sm font-medium transition-colors hover:text-foreground/80',
                  pathname?.startsWith(item.href)
                    ? 'border-b-2 border-primary text-foreground'
                    : 'text-muted-foreground'
                )}
              >
                {item.label}
              </Link>
            </li>
          ))}
        </ul>
      </div>
    </nav>
  );
}
