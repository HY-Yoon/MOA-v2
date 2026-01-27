/**
 * AdminTable 컴포넌트 관련 타입 정의
 */

export interface AdminTableColumn<T> {
  /** 데이터 객체의 key */
  key: string;
  /** 테이블 헤더에 표시될 라벨 */
  label: string;
  /** 커스텀 렌더링 함수 */
  render?: (item: T) => React.ReactNode;
  /** 컬럼에 적용할 CSS 클래스 */
  className?: string;
}

/**
 * SearchBar 컴포넌트 검색 컬럼 타입
 * @deprecated Use SearchColumn from @/components/molecules/SearchBar instead
 */
export interface AdminTableSearchColumn {
  /** 검색할 컬럼의 key */
  key: string;
  /** 셀렉트박스에 표시될 라벨 */
  label: string;
}

/**
 * SearchBar 컴포넌트 검색 컬럼 타입
 */
export interface SearchColumn {
  /** 검색할 컬럼의 key */
  key: string;
  /** 셀렉트박스에 표시될 라벨 */
  label: string;
}

/**
 * Pagination 컴포넌트 Props 타입
 */
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

/**
 * SearchBar 컴포넌트 Props 타입
 */
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

export interface AdminTableProps<T> {
  // 데이터
  /** 표시할 데이터 배열 */
  data: T[];
  /** 테이블 컬럼 정의 */
  columns: AdminTableColumn<T>[];

  // 검색 기능
  /** 검색 가능한 컬럼 목록 */
  searchColumns?: SearchColumn[];
  /** 검색창 placeholder */
  searchPlaceholder?: string;
  /** 검색 실행 시 호출되는 콜백 */
  onSearch?: (keyword: string, searchColumn: string) => void;

  // 페이지네이션
  /** 현재 페이지 번호 (0부터 시작) */
  page: number;
  /** 전체 페이지 수 */
  totalPages: number;
  /** 페이지당 데이터 개수 */
  pageSize: number;
  /** 페이지 변경 시 호출되는 콜백 */
  onPageChange: (page: number) => void;
  /** 페이지 사이즈 변경 시 호출되는 콜백 */
  onPageSizeChange: (size: number) => void;
  /** 페이지 사이즈 옵션 */
  pageSizeOptions?: number[];

  // 등록하기 버튼 (optional)
  /** 등록 버튼 클릭 시 호출되는 콜백 */
  onRegister?: () => void;
  /** 등록 버튼 텍스트 */
  registerButtonText?: string;

  // 로딩 상태
  /** 로딩 상태 */
  isLoading?: boolean;

  // 커스터마이징
  /** 추가 CSS 클래스 */
  className?: string;
  /** 빈 데이터 메시지 */
  emptyMessage?: string;
}

/**
 * API 쿼리 파라미터 타입
 */
export interface AdminTableQueryParams {
  /** 검색 키워드 */
  keyword?: string;
  /** 페이지 번호 (0부터 시작) */
  page: number;
  /** 페이지 사이즈 */
  size: number;
  /** 검색할 컬럼 (optional) */
  searchColumn?: string;
}

/**
 * 페이지네이션 API 응답 타입 (Spring Data 기준)
 */
export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
  empty: boolean;
}
