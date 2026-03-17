'use client';

import { ADMIN_ROUTES } from '@/constants/route/adminRoutes';
import { usePathname } from 'next/navigation';
import { Header } from './Header';

const HIDE_HEADER_PREFIXES = [ADMIN_ROUTES.DASHBOARD, '/shows/reservation'] as const;

export function AppHeader() {
  const pathname = usePathname();
  const hidden = HIDE_HEADER_PREFIXES.some((prefix) => pathname.startsWith(prefix));

  return <Header hidden={hidden} />;
}
