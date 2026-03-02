import { AUTH_COOKIE_KEYS } from '@/constants/common/auth';
import { NextResponse } from 'next/server';
import { ResponseCookie } from 'next/dist/compiled/@edge-runtime/cookies';

/**
 * 로그아웃: 프론트 도메인의 토큰 쿠키 삭제
 */
export async function POST() {
  const response = NextResponse.json({ success: true });

  const body = {
    httpOnly: true,
    secure: process.env.NODE_ENV === 'production',
    sameSite: 'lax',
    path: '/',
    maxAge: 0,
  } as Partial<ResponseCookie>;

  response.cookies.set(AUTH_COOKIE_KEYS.ACCESS_TOKEN, '', body);
  response.cookies.set(AUTH_COOKIE_KEYS.REFRESH_TOKEN, '', body);

  return response;
}
