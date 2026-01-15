import { BE_URL } from '@/constants/common/url';

/**
 * 이미지 URL 처리 유틸리티
 */

/**
 * 백엔드 baseURL 가져오기
 */
function getBackendBaseUrl(): string {
  // 서버 사이드
  if (typeof window === 'undefined') return BE_URL;

  // 클라이언트 사이드
  // 1. 프로덕션 환경: 상대 경로 (Next.js API Routes 통해)
  if (process.env.NODE_ENV === 'production') return '';

  // 2. 개발 환경: 백엔드 URL
  return BE_URL;
}

/**
 * 이미지 URL을 절대 경로로 변환
 * 상대 경로인 경우 백엔드 baseURL과 결합
 */
export function getAbsoluteImageUrl(url: string | undefined | null): string | null {
  if (!url) return null;

  // 이미 절대 경로인 경우 (http:// 또는 https://로 시작)
  if (url.startsWith('http://') || url.startsWith('https://')) {
    return url;
  }

  // 상대 경로인 경우 백엔드 baseURL
  const backendBaseUrl = getBackendBaseUrl();

  // baseURL이 비어있으면 상대 경로(프로덕션 환경)
  if (!backendBaseUrl) {
    return url.startsWith('/') ? url : `/${url}`;
  }

  // baseURL과 결합
  const baseUrl = backendBaseUrl.endsWith('/') ? backendBaseUrl.slice(0, -1) : backendBaseUrl;
  const imagePath = url.startsWith('/') ? url : `/${url}`;

  return `${baseUrl}${imagePath}`;
}

/**
 * 이미지 URL 배열을 절대 경로로 변환
 */
export function getAbsoluteImageUrls(urls: string | string[] | undefined | null): string[] {
  if (!urls) return [];

  const urlArray = Array.isArray(urls) ? urls : [urls];
  return urlArray
    .map((url) => getAbsoluteImageUrl(url))
    .filter((url): url is string => url !== null);
}
