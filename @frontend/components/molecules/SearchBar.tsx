'use client';

import * as React from 'react';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/atoms';
import { Input } from '@/components/atoms';
import { Search } from 'lucide-react';
import { cn } from '@/lib/utils';

export interface SearchColumn {
  key: string;
  label: string;
}

export interface SearchBarProps {
  /** 검색 가능한 컬럼 목록 */
  searchColumns?: SearchColumn[];
  /** 검색창 placeholder */
  placeholder?: string;
  /** 검색 키워드 */
  value: string;
  /** 선택된 검색 컬럼 */
  selectedColumn: string;
  /** 검색 키워드 변경 핸들러 */
  onValueChange: (value: string) => void;
  /** 검색 컬럼 변경 핸들러 */
  onColumnChange: (column: string) => void;
  /** 검색 실행 핸들러 */
  onSearch: () => void;
  /** 추가 CSS 클래스 */
  className?: string;
}

export function SearchBar({
  searchColumns = [],
  placeholder = '검색어를 입력하세요',
  value,
  selectedColumn,
  onValueChange,
  onColumnChange,
  onSearch,
  className,
}: SearchBarProps) {
  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    onSearch();
  };

  return (
    <form onSubmit={handleSubmit} className={cn('flex items-center gap-2', className)}>
      {/* 검색 컬럼 셀렉트 */}
      {searchColumns.length > 0 && (
        <Select value={selectedColumn} onValueChange={onColumnChange}>
          <SelectTrigger className="w-35">
            <SelectValue placeholder="전체" />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value="all">전체</SelectItem>
            {searchColumns.map((column) => (
              <SelectItem key={column.key} value={column.key}>
                {column.label}
              </SelectItem>
            ))}
          </SelectContent>
        </Select>
      )}

      {/* 검색 입력창 */}
      <div className="relative">
        <Input
          type="text"
          placeholder={placeholder}
          value={value}
          onChange={(e) => onValueChange(e.target.value)}
          className="w-80 pr-10"
        />
        <button
          type="submit"
          className="text-muted-foreground hover:text-foreground absolute top-1/2 right-2 -translate-y-1/2 transition-colors"
        >
          <Search className="size-4" />
        </button>
      </div>
    </form>
  );
}
