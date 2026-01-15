import { BE_URL } from '@/constants/common/url';
import { NextRequest, NextResponse } from 'next/server';

export async function GET(request: NextRequest, { params }: { params: { id: string } }) {
  try {
    const { id } = params;

    // 프로덕션 환경에서만 사용되므로 항상 실제 백엔드 API 호출
    const response = await fetch(`${BE_URL}/api/admin/shows/${id}`, {
      headers: {
        'Content-Type': 'application/json',
      },
    });

    if (!response.ok) {
      if (response.status === 404) {
        return NextResponse.json({ error: 'Show not found' }, { status: 404 });
      }
      throw new Error('Failed to fetch show');
    }

    const data = await response.json();
    return NextResponse.json(data);
  } catch (error) {
    console.error('Error fetching show:', error);
    return NextResponse.json({ error: 'Failed to fetch show' }, { status: 500 });
  }
}

export async function PUT(request: NextRequest, { params }: { params: { id: string } }) {
  try {
    const { id } = params;
    const body = await request.json();

    // 실제 백엔드 API 호출
    const response = await fetch(`${BE_URL}/api/admin/shows/${id}`, {
      method: 'PUT',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(body),
    });

    if (!response.ok) {
      if (response.status === 404) {
        return NextResponse.json({ error: 'Show not found' }, { status: 404 });
      }
      throw new Error('Failed to update show');
    }

    const data = await response.json();
    return NextResponse.json(data);
  } catch (error) {
    console.error('Error updating show:', error);
    return NextResponse.json({ error: 'Failed to update show' }, { status: 500 });
  }
}

export async function DELETE(request: NextRequest, { params }: { params: { id: string } }) {
  try {
    const { id } = params;

    // 실제 백엔드 API 호출
    const response = await fetch(`${BE_URL}/api/admin/shows/${id}`, {
      method: 'DELETE',
      headers: {
        'Content-Type': 'application/json',
      },
    });

    if (!response.ok) {
      if (response.status === 404) {
        return NextResponse.json({ error: 'Show not found' }, { status: 404 });
      }
      throw new Error('Failed to delete show');
    }

    return NextResponse.json({ message: 'Show deleted successfully' });
  } catch (error) {
    console.error('Error deleting show:', error);
    return NextResponse.json({ error: 'Failed to delete show' }, { status: 500 });
  }
}
