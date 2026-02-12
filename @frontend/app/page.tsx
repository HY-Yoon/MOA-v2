'use client';

import Link from 'next/link';
import { ADMIN_ROUTES } from '@/constants/route/adminRoutes';
import { Button } from '@/components/atoms';
import { useAuth, useConfirmLogout } from '@/lib/auth/AuthContext';

// 메인 홈 
export default function Home() {
  const { isLoggedIn, isLoading, user } = useAuth();
  const handleLogout = useConfirmLogout();

  return (
    <div className="min-h-screen">
      {/* Banner Section */}
      <section className="bg-muted">
        <div className="container mx-auto px-4">
          <div className="h-64 sm:h-80 lg:h-96 flex items-center justify-center border border-border rounded-sm">
            <div className="flex flex-col items-center gap-2">
              
              <p className="text-sm text-muted-foreground">배너 이미지 영역</p>
            </div>
          </div>
        </div>
      </section>

      {/* Main Content */}
      <section className="py-16">
        <div className="container mx-auto px-4">
          {/* Featured Shows */}
          <div className="mb-16">
            <h2 className="text-2xl font-bold mb-8">타이틀</h2>
            
          </div>

          
        </div>
      </section>
    </div>
  );
}
