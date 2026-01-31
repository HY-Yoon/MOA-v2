namespace User {
  type UserStatus = import('@shared/enums').UserStatus;

  // 회원 목록 조회 파라미터
  interface ListParams {
    keyword?: string; // 이름, 이메일
    sort?: string; // ex. 'createdAt, desc'
    page: number;
    size: number;
  }

  // 회원 목록
  interface List {
    id: number;
    name: string;
    email: string;
    phone: string | null;
    gender: string | null;
    socialProvider: string;
    status: UserStatus;
    isVerified: boolean;
    createdAt: string; // ISO YYYY-MM-DDTHH:mm:dd"
  }
}
