import { BE_URL } from '@/constants/common/url';
import { NextRequest, NextResponse } from 'next/server';

export async function GET() {
  try {
    // 프로덕션 환경에서만 사용되므로 항상 실제 백엔드 API 호출
    const response = await fetch(`${BE_URL}/api/admin/shows`, {
      headers: {
        'Content-Type': 'application/json',
      },
    });

    if (!response.ok) {
      throw new Error('Failed to fetch shows');
    }

    const data = await response.json();
    return NextResponse.json(data);
  } catch (error) {
    console.error('Error fetching shows:', error);
    return NextResponse.json({ error: 'Failed to fetch shows' }, { status: 500 });
  }
}

export async function POST(request: NextRequest) {
  try {
    const body = await request.json();

    // 실제 백엔드 API 호출
    const response = await fetch(`${BE_URL}/api/admin/shows`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(body),
    });

    if (!response.ok) {
      throw new Error('Failed to create show');
    }

    const data = await response.json();
    return NextResponse.json(data, { status: 201 });
  } catch (error) {
    console.error('Error creating show:', error);
    return NextResponse.json({ error: 'Failed to create show' }, { status: 500 });
  }
}
