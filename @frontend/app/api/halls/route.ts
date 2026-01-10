import { NextRequest, NextResponse } from 'next/server';

export async function GET() {
  try {
    // 프로덕션 환경에서만 사용되므로 항상 실제 백엔드 API 호출
    const backendUrl = process.env.BACKEND_URL || 'http://localhost:8080';
    const response = await fetch(`${backendUrl}/api/admin/halls`, {
      headers: {
        'Content-Type': 'application/json',
      },
    });

    if (!response.ok) {
      throw new Error('Failed to fetch halls');
    }

    const data = await response.json();
    return NextResponse.json(data);
  } catch (error) {
    console.error('Error fetching halls:', error);
    return NextResponse.json(
      { error: 'Failed to fetch halls' },
      { status: 500 }
    );
  }
}

export async function POST(request: NextRequest) {
  try {
    const body = await request.json();

    // 실제 백엔드 API 호출
    const backendUrl = process.env.BACKEND_URL || 'http://localhost:8080';
    const response = await fetch(`${backendUrl}/api/admin/halls`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(body),
    });

    if (!response.ok) {
      throw new Error('Failed to create hall');
    }

    const data = await response.json();
    return NextResponse.json(data, { status: 201 });
  } catch (error) {
    console.error('Error creating hall:', error);
    return NextResponse.json(
      { error: 'Failed to create hall' },
      { status: 500 }
    );
  }
}