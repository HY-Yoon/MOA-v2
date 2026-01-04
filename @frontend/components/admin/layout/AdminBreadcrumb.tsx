"use client";

import * as React from "react";
import Link from "next/link";
import { usePathname } from "next/navigation";

import {
  Breadcrumb,
  BreadcrumbItem,
  BreadcrumbLink,
  BreadcrumbList,
  BreadcrumbPage,
  BreadcrumbSeparator,
} from "@/components/ui/breadcrumb";
import { ADMIN_ROUTE_LABELS, ADMIN_ROUTES } from "@/constants/adminRoutes";

export function AdminBreadcrumb() {
  const pathname = usePathname();
  const paths = pathname.split("/").filter(Boolean);
  const currentPath = "/" + paths.join("/");

  // 첫 번째에 '관리자' 고정 (링크는 /admin으로)
  const adminItem = {
    path: ADMIN_ROUTES.DASHBOARD,
    label: "관리자",
    isLast: false,
  };

  // 현재 경로에 대한 항목 추가
  const currentItem = {
    path: currentPath,
    label: ADMIN_ROUTE_LABELS[currentPath] || paths[paths.length - 1],
    isLast: true,
  };

  const breadcrumbItems = [adminItem, currentItem];

  return (
    <Breadcrumb>
      <BreadcrumbList>
        {breadcrumbItems.map((item, index) => (
          <React.Fragment key={`${item.path}-${index}`}>
            <BreadcrumbItem>
              {item.isLast ? (
                <BreadcrumbPage>{item.label}</BreadcrumbPage>
              ) : (
                <BreadcrumbLink asChild>
                  <Link href={item.path}>{item.label}</Link>
                </BreadcrumbLink>
              )}
            </BreadcrumbItem>
            {!item.isLast && <BreadcrumbSeparator />}
          </React.Fragment>
        ))}
      </BreadcrumbList>
    </Breadcrumb>
  );
}
