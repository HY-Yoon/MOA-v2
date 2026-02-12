import { Inter, Noto_Sans_KR } from 'next/font/google';
import './globals.css';
import { Providers } from '@/lib/client-providers';
import { AuthProvider } from '@/lib/auth/AuthContext';
import { AlertProvider } from '@/components/molecules/AlertContext';
import { Header } from '@/components/organisms/Header';

const inter = Inter({
  subsets: ['latin'],
  variable: '--font-inter',
});

const notoSansKr = Noto_Sans_KR({
  subsets: ['latin'],
  variable: '--font-noto-kr',
});

export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="ko">
      <head>
        <link rel="preconnect" href="https://fonts.googleapis.com" />
        <link rel="preconnect" href="https://fonts.gstatic.com" crossOrigin="anonymous" />
        <link
          href="https://fonts.googleapis.com/css2?family=Sansation:wght@700&display=swap"
          rel="stylesheet"
        />
      </head>
      <body className={`${inter.variable} ${notoSansKr.variable} font-sans`}>
        <Providers>
          <AuthProvider>
            <AlertProvider>
              <Header />
              <main>{children}</main>
            </AlertProvider>
          </AuthProvider>
        </Providers>
      </body>
    </html>
  );
}
