'use client';

// 메인 홈
export default function Home() {
  return (
    <div className="min-h-screen">
      {/* Banner Section */}
      <section className="bg-muted">
        <div className="container mx-auto px-4">
          <div className="border-border flex h-64 items-center justify-center rounded-sm border sm:h-80 lg:h-96">
            <div className="flex flex-col items-center gap-2">
              <p className="text-muted-foreground text-sm">배너 이미지 영역</p>
            </div>
          </div>
        </div>
      </section>

      {/* Main Content */}
      <section className="py-16">
        <div className="container mx-auto px-4">
          {/* Featured Shows */}
          <div className="mb-16">
            <h2 className="mb-8 text-2xl font-bold">타이틀</h2>
          </div>
        </div>
      </section>
    </div>
  );
}
