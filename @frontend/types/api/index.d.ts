namespace Api {
  interface Response<T> {
    success: boolean;
    data: T | null;
    message: string | null;
    code?: string | null;
  }

  interface ListResponse<T> {
    content: T[];
    page: number;
    size: number;
    totalElements: number;
    totalPages: number;
    first: boolean;
    last: boolean;
  }
}
