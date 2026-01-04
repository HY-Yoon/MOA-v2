import type { Metadata } from "next";
import { Inter, Noto_Sans_KR, Sansation } from "next/font/google";
import "./globals.css";

const inter = Inter({
  subsets: ["latin"],
  variable: "--font-inter",
})

const notoSansKr = Noto_Sans_KR({
  subsets: ["latin"],
  variable: "--font-noto-kr",
})

const sansation = Sansation({
  subsets: ["latin"],
  weight: "700",          
  variable: "--font-sansation",
})

export default function RootLayout({
  children,
}: {
  children: React.ReactNode
}) {
  return (
    <html lang="ko">
      <body
        className={`${inter.variable} ${notoSansKr.variable} ${sansation.variable} font-sans`}
      >
        {children}
      </body>
    </html>
  )
}