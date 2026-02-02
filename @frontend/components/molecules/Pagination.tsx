'use client';

import * as React from 'react';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/atoms';
import { Button } from '@/components/atoms';
import { ChevronLeft, ChevronRight } from 'lucide-react';
import { cn } from '@/lib/utils';

export interface PaginationProps {
  /** 현재 페이지 (0부터 시작) */
  page: number;
  /** 전체 페이지 수 */
  totalPages: number;
  /** 페이지당 데이터 개수 */
  pageSize: number;
  /** 페이지 변경 핸들러 */
  onPageChange: (page: number) => void;
  /** 페이지 사이즈 변경 핸들러 */
  onPageSizeChange: (size: number) => void;
  /** 페이지 사이즈 옵션 */
  pageSizeOptions?: number[];
  /** 비활성화 여부 */
  disabled?: boolean;
  /** 추가 CSS 클래스 */
  className?: string;
}

export function Pagination({
  page,
  totalPages,
  pageSize,
  onPageChange,
  onPageSizeChange,
  pageSizeOptions = [5, 10, 20, 50],
  disabled = false,
  className,
}: PaginationProps) {
  return (
    <div className={cn('flex items-center justify-end', className)}>
      {/* 페이지 네비게이션 */}
      <div className="flex items-center gap-2">
        <Select
          value={pageSize.toString()}
          onValueChange={(value) => onPageSizeChange(Number(value))}
          disabled={disabled}
        >
          <SelectTrigger className="w-30">
            <SelectValue />
          </SelectTrigger>
          <SelectContent>
            {pageSizeOptions.map((size) => (
              <SelectItem key={size} value={size.toString()}>
                {size}개
              </SelectItem>
            ))}
          </SelectContent>
        </Select>

        <Button
          variant="outline"
          size="icon-sm"
          onClick={() => onPageChange(page - 1)}
          disabled={page === 0 || disabled}
        >
          <ChevronLeft className="size-4" />
        </Button>

        <span className="min-w-25 text-center text-sm font-medium">
          {totalPages > 0 ? `${page + 1} / ${totalPages}` : '0 / 0'}
        </span>

        <Button
          variant="outline"
          size="icon-sm"
          onClick={() => onPageChange(page + 1)}
          disabled={page >= totalPages - 1 || disabled}
        >
          <ChevronRight className="size-4" />
        </Button>
      </div>
    </div>
  );
}
