'use client';

import * as React from 'react';
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuCheckboxItem,
  DropdownMenuTrigger,
  Skeleton,
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/atoms';
import { SearchBar, SearchColumn } from '@/components/molecules/SearchBar';
import { Pagination } from '@/components/molecules/Pagination';
import type { AdminTableFilterOption } from '@/lib/admin/table-filter';
import { cn } from '@/lib/utils';
import { ArrowDown, ArrowUp, ChevronsUpDown, ListFilter, FunnelX } from 'lucide-react';

export type { AdminTableFilterOption };

export interface AdminTableColumn<T> {
  key: string;
  label: string;
  render?: (item: T) => React.ReactNode;
  className?: string;
  search?: boolean; // 검색 컬럼 여부
  sorter?: boolean; // 정렬 컬럼 여부
  filter?: boolean; // 필터 컬럼 여부
  filterOptions?: AdminTableFilterOption[]; // 필터  옵션 목록
}

export interface AdminTableProps<T> {
  // 데이터
  data: T[];
  columns: AdminTableColumn<T>[];

  // 검색 기능
  searchPlaceholder?: string;
  onSearch?: (keyword: string, searchColumn: string) => void;

  // 정렬 기능
  defaultSortColumn?: string;
  defaultSortOrder?: 'asc' | 'desc';
  onSort?: (column?: string, order?: 'asc' | 'desc') => void;

  // 필터 기능 (컬럼별 선택값)
  filterValues?: Record<string, string[]>;
  onFilter?: (columnKey: string, selectedValues: string[]) => void;

  // 페이지네이션
  page: number;
  totalPages: number;
  pageSize: number;
  onPageChange: (page: number) => void;
  onPageSizeChange: (size: number) => void;
  pageSizeOptions?: number[];

  // 헤더 액션 영역 (우측 버튼 영역)
  headerActions?: React.ReactNode;

  // 로딩 상태
  isLoading?: boolean;

  // 커스터마이징
  className?: string;
  emptyMessage?: string;
}

export function AdminTable<T extends Record<string, any>>({
  data,
  columns,
  searchPlaceholder = '검색어를 입력하세요',
  onSearch,
  defaultSortColumn,
  defaultSortOrder,
  onSort,
  filterValues,
  onFilter,
  page,
  totalPages,
  pageSize,
  onPageChange,
  onPageSizeChange,
  pageSizeOptions = [5, 10, 20, 50],
  headerActions,
  isLoading = false,
  className,
  emptyMessage = '데이터가 없습니다.',
}: AdminTableProps<T>) {
  const [sortColumn, setSortColumn] = React.useState<string | undefined>(defaultSortColumn);
  const [sortOrder, setSortOrder] = React.useState<'asc' | 'desc' | undefined>(defaultSortOrder);

  const [searchKeyword, setSearchKeyword] = React.useState('');
  const [selectedSearchColumn, setSelectedSearchColumn] = React.useState('all');

  // columns에서 search: true만 필터링
  const searchColumns: SearchColumn[] = React.useMemo(() => {
    return columns.filter((col) => col.search).map((col) => ({ key: col.key, label: col.label }));
  }, [columns]);

  // 검색 처리
  function handleSearch() {
    if (onSearch) {
      onSearch(searchKeyword, selectedSearchColumn);
    }
  }

  // 정렬 처리
  function handleSort(key: string) {
    let newSortColumn: string | undefined;
    let newSortOrder: 'asc' | 'desc' | undefined;

    if (sortColumn === key) {
      if (sortOrder === 'asc') {
        newSortColumn = key;
        newSortOrder = 'desc';
      } else if (sortOrder === 'desc') {
        // 정렬 취소
        newSortColumn = undefined;
        newSortOrder = undefined;
      }
    } else {
      newSortColumn = key;
      newSortOrder = 'asc';
    }

    setSortColumn(newSortColumn);
    setSortOrder(newSortOrder);

    // 부모에게 정렬 변경 알림
    onSort?.(newSortColumn, newSortOrder);
  }

  function handleFilterChange(columnKey: string, selectedVal: string, checked: boolean) {
    const current = filterValues?.[columnKey] ?? [];
    // TODO: 필터 다중 선택
    // const next = checked ? [...current, selectedVal] : current.filter((v) => v !== selectedVal);
    const next = checked ? [selectedVal] : current.filter((v) => v !== selectedVal);

    onFilter?.(columnKey, next);
  }

  // 정렬 아이콘 렌더링
  function renderSortIcon(columnKey: string) {
    if (sortColumn !== columnKey) {
      return <ChevronsUpDown className="ml-2 h-4 w-4" />;
    }

    return sortOrder === 'asc' ? (
      <ArrowUp className="ml-2 h-4 w-4" />
    ) : (
      <ArrowDown className="ml-2 h-4 w-4" />
    );
  }

  // 필터 아이콘 + 체크박스 드롭다운 렌더링
  function renderFilter(column: AdminTableColumn<T>) {
    if (!column.filter) return null;

    const options = column.filterOptions ?? [];
    const selectedValues = filterValues?.[column.key] ?? [];
    const isActive = selectedValues.length > 0;

    return (
      <DropdownMenu>
        <DropdownMenuTrigger asChild>
          <button
            type="button"
            className={cn(
              'focus-visible:ring-ring ml-2 inline-flex rounded transition-colors hover:text-gray-900 focus:outline-none focus-visible:ring-2 focus-visible:ring-offset-2',
              isActive && 'text-blue-600',
            )}
            aria-label={`${column.label} 필터`}
            aria-pressed={isActive}
          >
            {isActive ? (
              <FunnelX className="h-4 w-4" />
            ) : (
              <ListFilter className="h-4 w-4" />
            )}
          </button>
        </DropdownMenuTrigger>
        <DropdownMenuContent align="start" className="min-w-40">
          {options.length === 0 ? (
            <DropdownMenuCheckboxItem
              disabled
              checked={false}
              className="cursor-default px-3 py-2 text-sm text-muted-foreground"
            >
              데이터가 없습니다.
            </DropdownMenuCheckboxItem>
          ) : (
            options.map((opt) => (
              <DropdownMenuCheckboxItem
                key={opt.value}
                checked={(filterValues?.[column.key] ?? []).includes(opt.value)}
                onCheckedChange={(checked) => handleFilterChange(column.key, opt.value, checked)}
              >
                {opt.label}
              </DropdownMenuCheckboxItem>
            ))
          )}
        </DropdownMenuContent>
      </DropdownMenu>
    );
  }

  return (
    <div className={cn('w-full space-y-4', className)}>
      {/* 상단 컨트롤 영역 */}
      <div className="flex items-center justify-between gap-4">
        {/* 좌측: 검색 영역 */}
        <SearchBar
          searchColumns={searchColumns}
          placeholder={searchPlaceholder}
          value={searchKeyword}
          selectedColumn={selectedSearchColumn}
          onValueChange={setSearchKeyword}
          onColumnChange={setSelectedSearchColumn}
          onSearch={handleSearch}
        />

        {/* 우측: 헤더 액션 영역 */}
        {headerActions && <div className="flex items-center gap-2">{headerActions}</div>}
      </div>

      {/* 테이블 */}
      <div className="rounded-md border">
        <Table>
          <TableHeader>
            <TableRow>
              {columns.map((column) => (
                <TableHead key={column.key} className={column.className}>
                  <div className="flex items-center">
                    {/* 컬럼명 */}
                    {column.label}
                    {/* 정렬 아이콘 */}
                    {column.sorter && (
                      <button
                        onClick={() => handleSort(column.key)}
                        className="transition-colors hover:text-gray-900"
                      >
                        {renderSortIcon(column.key)}
                      </button>
                    )}
                    {/* 필터 아이콘 + 체크박스 드롭다운 */}
                    {renderFilter(column)}
                  </div>
                </TableHead>
              ))}
            </TableRow>
          </TableHeader>
          <TableBody>
            {isLoading ? (
              <TableRow>
                <TableCell colSpan={columns.length} className="h-24 text-center">
                  <Skeleton className="h-10 w-full" />
                </TableCell>
              </TableRow>
            ) : data.length === 0 ? (
              <TableRow>
                <TableCell colSpan={columns.length} className="h-24 text-center">
                  {emptyMessage}
                </TableCell>
              </TableRow>
            ) : (
              data.map((item, index) => (
                <TableRow key={index}>
                  {columns.map((column) => (
                    <TableCell key={column.key} className={column.className}>
                      {column.render ? column.render(item) : item[column.key] || '-'}
                    </TableCell>
                  ))}
                </TableRow>
              ))
            )}
          </TableBody>
        </Table>
      </div>

      {/* 하단: 페이지네이션 */}
      <Pagination
        page={page}
        totalPages={totalPages}
        pageSize={pageSize}
        onPageChange={onPageChange}
        onPageSizeChange={onPageSizeChange}
        pageSizeOptions={pageSizeOptions}
        disabled={isLoading}
      />
    </div>
  );
}
