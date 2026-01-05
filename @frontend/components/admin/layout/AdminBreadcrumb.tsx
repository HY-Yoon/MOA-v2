'use client';

import * as React from 'react';
import Link from 'next/link';
import { usePathname } from 'next/navigation';

import {
  Breadcrumb,
  BreadcrumbItem,
  BreadcrumbLink,
  BreadcrumbList,
  BreadcrumbPage,
  BreadcrumbSeparator,
} from '@/components/ui/breadcrumb';
import { ADMIN_ROUTE_LABELS, ADMIN_ROUTES } from '@/constants/adminRoutes';

export function AdminBreadcrumb() {
  function makeBreadcrumbItems(pathname: string) {
    const segments = pathname.split('/').filter(Boolean);

    const items: { path: string; label: string }[] = [];

    // 1. 최상단 관리자 고정
    items.push({ path: ADMIN_ROUTES.DASHBOARD, label: '관리자' });

    // 2. /admin 단독 → 대시보드
    if (segments.length === 1) {
      items.push({
        path: ADMIN_ROUTES.DASHBOARD,
        label: ADMIN_ROUTE_LABELS[ADMIN_ROUTES.DASHBOARD] as string,
      });

      return items;
    }

    // 3. 목록 페이지
    if (segments.length >= 2) {
      const domainPath = `/${segments.slice(0, 2).join('/')}`;
      const domainLabel = ADMIN_ROUTE_LABELS[domainPath];

      if (typeof domainLabel === 'string') {
        items.push({
          path: domainPath,
          label: domainLabel,
        });
      }
    }

    // 4. 하위 페이지 (upsert, detail)
    if (segments.length >= 3) {
      const actionPath = `/${segments.slice(0, 3).join('/')}`;
      const labelConfig = ADMIN_ROUTE_LABELS[actionPath];

      if (labelConfig) {
        // upsert
        if (typeof labelConfig === 'object') {
          const isEdit = segments.length >= 4; // id 존재하면 수정
          items.push({
            path: actionPath,
            label: isEdit ? labelConfig.EDIT : labelConfig.CREATE,
          });
        } else {
          items.push({
            path: actionPath,
            label: labelConfig,
          });
        }
      }
    }

    return items;
  }

  const pathname = usePathname();
  const items = makeBreadcrumbItems(pathname);

  return (
    <Breadcrumb>
      <BreadcrumbList>
        {items.map((item, index) => {
          const isLast = index === items.length - 1;

          return (
            <React.Fragment key={item.path}>
              <BreadcrumbItem>
                {isLast ? (
                  <BreadcrumbPage>{item.label}</BreadcrumbPage>
                ) : (
                  <BreadcrumbLink asChild>
                    <Link href={item.path}>{item.label}</Link>
                  </BreadcrumbLink>
                )}
              </BreadcrumbItem>
              {!isLast && <BreadcrumbSeparator />}
            </React.Fragment>
          );
        })}
      </BreadcrumbList>
    </Breadcrumb>
  );
}
