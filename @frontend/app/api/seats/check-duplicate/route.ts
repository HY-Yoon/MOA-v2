import { NextRequest, NextResponse } from 'next/server';

// 좌석 중복 확인 API
export async function POST(request: NextRequest) {
  try {
    const body = await request.json();
    const { region, venueName, hallName } = body;

    // TODO: 실제 DB 조회 로직 구현
    // 현재는 임시로 중복 없음 반환
    const isDuplicate = false;

    return NextResponse.json({
      success: true,
      data: {
        isDuplicate,
        message: isDuplicate ? '이미 등록된 홀입니다.' : '등록 가능한 홀입니다.',
      },
    });
  } catch (error) {
    console.error('좌석 중복 확인 에러:', error);
    return NextResponse.json(
      {
        success: false,
        message: '중복 확인 중 오류가 발생했습니다.',
      },
      { status: 500 },
    );
  }
}
