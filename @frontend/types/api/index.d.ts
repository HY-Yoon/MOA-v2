namespace Api {
  interface Response<T> {
    success: boolean;
    data: T;
    message: string;
  }
}
