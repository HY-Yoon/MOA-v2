import type { NextConfig } from 'next';

const nextConfig: NextConfig = {
  images: {
    remotePatterns: [
      // 로컬 개발 환경
      {
        protocol: 'http',
        hostname: 'localhost',
        port: '8080',
      },
      // 백엔드 서버 도메인 (환경 변수가 설정된 경우만)
      ...(process.env.NEXT_PUBLIC_BACKEND_URL
        ? [
            {
              protocol: 'https' as const,
              hostname: process.env.NEXT_PUBLIC_BACKEND_URL,
            },
          ]
        : []),
    ],
  },
};

export default nextConfig;
