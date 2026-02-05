namespace User {
  type UserStatus = import('@shared/enums').UserStatus;
  type Gender = import('@shared/enums').Gender;

  // 회원 목록 조회 파라미터
  interface ListParams {
    page: number;
    size: number;
    sort?: string; // ex. 'createdAt, desc'
    searchType?: string; // 검색 컬럼 (이름,이메일,연락처), 없으면 전체 검색
    keyword?: string; // 검색어
    status?: UserStatus; // 회원 상태 필터
    gender?: Gender; // 성별 필터
    socialProvider?: string; // 소셜 필터
  }

  // 회원 목록
  interface List {
    id: number;
    name: string;
    email: string;
    phone: string | null;
    gender: Gender;
    socialProvider: string;
    status: UserStatus;
    isVerified: boolean;
    createdAt: string; // ISO YYYY-MM-DDTHH:mm:dd"
  }
}
