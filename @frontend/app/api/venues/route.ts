import { BE_URL } from '@/constants/common/url';
import { NextRequest, NextResponse } from 'next/server';

export async function GET() {
  try {
    // 프로덕션 환경에서만 사용되므로 항상 실제 백엔드 API 호출
    const response = await fetch(`${BE_URL}/api/admin/venues`, {
      headers: {
        'Content-Type': 'application/json',
      },
    });

    if (!response.ok) {
      throw new Error('Failed to fetch venues');
    }

    const data = await response.json();
    return NextResponse.json(data);
  } catch (error) {
    console.error('Error fetching venues:', error);
    return NextResponse.json({ error: 'Failed to fetch venues' }, { status: 500 });
  }
}

export async function POST(request: NextRequest) {
  try {
    const body = await request.json();

    // 실제 백엔드 API 호출
    const response = await fetch(`${BE_URL}/api/admin/venues`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(body),
    });

    if (!response.ok) {
      throw new Error('Failed to create venue');
    }

    const data = await response.json();
    return NextResponse.json(data, { status: 201 });
  } catch (error) {
    console.error('Error creating venue:', error);
    return NextResponse.json({ error: 'Failed to create venue' }, { status: 500 });
  }
}
