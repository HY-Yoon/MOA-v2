/**
 * BaseTable 컴포넌트 관련 타입 정의
 */

export interface BaseTableColumn<T> {
  /** 데이터 객체의 key */
  key: string;
  /** 테이블 헤더에 표시될 라벨 */
  label: string;
  /** 커스텀 렌더링 함수 */
  render?: (item: T) => React.ReactNode;
  /** 컬럼에 적용할 CSS 클래스 */
  className?: string;
  /** 검색 컬럼 여부 */
  search?: boolean;
  /** 정렬 컬럼 여부 */
  sorter?: boolean;
  /** 필터 컬럼 여부 */
  filter?: boolean;
  /** 필터 옵션 목록 */
  filterOptions?: { value: string; label: string }[];
}

export interface BaseTableProps<T> {
  /** 표시할 데이터 배열 */
  data: T[];
  /** 테이블 컬럼 정의 */
  columns: BaseTableColumn<T>[];

  // 검색 기능
  showSearch?: boolean;
  searchPlaceholder?: string;
  onSearch?: (keyword: string, searchColumn: string) => void;

  // 정렬 기능
  defaultSortColumn?: string;
  defaultSortOrder?: "asc" | "desc";
  onSort?: (column?: string, order?: "asc" | "desc") => void;

  // 필터 기능
  filterValues?: Record<string, string[]>;
  onFilter?: (columnKey: string, selectedValues: string[]) => void;

  // 페이지네이션
  page: number;
  totalPages: number;
  pageSize: number;
  onPageChange: (page: number) => void;
  onPageSizeChange: (size: number) => void;
  pageSizeOptions?: number[];

  // 헤더 액션 영역
  headerActions?: React.ReactNode;

  // 로딩 상태
  isLoading?: boolean;

  // 커스터마이징
  className?: string;
  emptyMessage?: string;
}

/**
 * 하위 호환 alias는 제거했습니다.
 * (프로젝트 내 admin-table 용어 제거 요청 반영)
 */

