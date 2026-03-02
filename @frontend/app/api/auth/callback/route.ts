import { AUTH_COOKIE_KEYS } from '@/constants/common/auth';
import { BE_URL } from '@/constants/common/url';
import { HEADER_ROUTES, USER_ROUTES } from '@/constants/route/userRoutes';
import { NextRequest, NextResponse } from 'next/server';
import { ResponseCookie } from 'next/dist/compiled/@edge-runtime/cookies';

interface TokenResponse {
  success?: boolean;
  data?: {
    accessToken?: string;
    refreshToken?: string;
    accessTokenExpiresIn?: number;
    refreshTokenExpiresIn?: number;
    email?: string;
  };
  message?: string;
  code?: string;
}

/**
 * OAuth 성공 후 백엔드가 리다이렉트하는 콜백
 * - 1. code로 백엔드 토큰 교환 API 호출
 * - 2. 응답 토큰을 프론트 도메인 쿠키에 저장
 */
export async function GET(request: NextRequest) {
  const { searchParams } = new URL(request.url);
  const code = searchParams.get('code');

  if (!code || code.trim() === '') {
    return redirectToLogin(request, 'invalid');
  }

  try {
    const tokenResponse = await fetch(`${BE_URL}/api/auth/exchange-code`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ code }),
    });

    if (!tokenResponse.ok) {
      const message =
        tokenResponse.status === 400 || tokenResponse.status === 401 ? 'invalid' : 'failed';
      return redirectToLogin(request, message);
    }

    const body = (await tokenResponse.json()) as TokenResponse;

    const data = body.data;
    const accessToken = data?.accessToken;
    const refreshToken = data?.refreshToken;
    const accessTokenExpiresInMs = data?.accessTokenExpiresIn;
    const refreshTokenExpiresInMs = data?.refreshTokenExpiresIn;

    if (!accessToken) {
      return redirectToLogin(request, 'failed');
    }

    const successUrl = new URL(USER_ROUTES.HOME, request.url);
    const response = NextResponse.redirect(successUrl);

    // maxAge는 초 단위. API는 밀리초
    const accessMaxAge =
      accessTokenExpiresInMs != null ? Math.floor(accessTokenExpiresInMs / 1000) : 60 * 15;

    const base = {
      httpOnly: true,
      secure: process.env.NODE_ENV === 'production',
      sameSite: 'lax',
      path: '/',
    } as ResponseCookie;

    response.cookies.set(AUTH_COOKIE_KEYS.ACCESS_TOKEN, accessToken, {
      ...base,
      maxAge: accessMaxAge,
    });

    if (refreshToken) {
      const refreshMaxAge =
        refreshTokenExpiresInMs != null
          ? Math.floor(refreshTokenExpiresInMs / 1000)
          : 60 * 60 * 24 * 7;

      response.cookies.set(AUTH_COOKIE_KEYS.REFRESH_TOKEN, refreshToken, {
        ...base,
        maxAge: refreshMaxAge,
      });
    }

    return response;
  } catch {
    return redirectToLogin(request, 'failed');
  }
}

function redirectToLogin(request: NextRequest, message: string): NextResponse {
  const origin = new URL(request.url).origin;
  const url = new URL(HEADER_ROUTES.LOGIN, origin);
  url.searchParams.set('message', message);
  return NextResponse.redirect(url);
}
