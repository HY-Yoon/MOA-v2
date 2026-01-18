import { NextRequest, NextResponse } from 'next/server';

// 좌석 등록 API
export async function POST(request: NextRequest) {
  try {
    const body: Seat.CreateSeatRequest = await request.json();
    const { region, venueName, hallName, layoutData } = body;

    // TODO: 실제 DB 저장 로직 구현
    // 현재는 임시로 성공 반환
    const seatId = Math.floor(Math.random() * 1000);

    return NextResponse.json({
      success: true,
      data: { seatId },
      message: '좌석이 성공적으로 등록되었습니다.',
    });
  } catch (error) {
    console.error('좌석 등록 에러:', error);
    return NextResponse.json(
      {
        success: false,
        message: '좌석 등록 중 오류가 발생했습니다.',
      },
      { status: 500 },
    );
  }
}

// 좌석 목록 조회 API
export async function GET(request: NextRequest) {
  try {
    const searchParams = request.nextUrl.searchParams;
    const region = searchParams.get('region');
    const venueName = searchParams.get('venueName');
    const hallName = searchParams.get('hallName');

    // TODO: 실제 DB 조회 로직 구현
    const seats: Seat.UpsertFormData[] = [];

    return NextResponse.json({
      success: true,
      data: seats,
    });
  } catch (error) {
    console.error('좌석 목록 조회 에러:', error);
    return NextResponse.json(
      {
        success: false,
        message: '좌석 목록 조회 중 오류가 발생했습니다.',
      },
      { status: 500 },
    );
  }
}
