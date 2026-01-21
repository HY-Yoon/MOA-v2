import { NextRequest, NextResponse } from 'next/server';

type Props = { params: Promise<{ id: string }> };

// 좌석 상세 조회 API
export async function GET(request: NextRequest, { params }: Props) {
  try {
    const { id } = await params;

    // TODO: 실제 DB 조회 로직 구현
    const seat: Seat.UpsertFormData | null = null;

    if (!seat) {
      return NextResponse.json(
        {
          success: false,
          message: '좌석을 찾을 수 없습니다.',
        },
        { status: 404 },
      );
    }

    return NextResponse.json({
      success: true,
      data: seat,
    });
  } catch (error) {
    console.error('좌석 조회 에러:', error);
    return NextResponse.json(
      {
        success: false,
        message: '좌석 조회 중 오류가 발생했습니다.',
      },
      { status: 500 },
    );
  }
}

// 좌석 수정 API
export async function PUT(request: NextRequest, { params }: Props) {
  try {
    const { id } = await params;
    const body = await request.json();

    // TODO: 실제 DB 업데이트 로직 구현

    return NextResponse.json({
      success: true,
      data: { seatId: id },
    });
  } catch (error) {
    console.error('좌석 수정 에러:', error);
    return NextResponse.json(
      {
        success: false,
        message: '좌석 수정 중 오류가 발생했습니다.',
      },
      { status: 500 },
    );
  }
}

// 좌석 삭제 API
export async function DELETE(request: NextRequest, { params }: Props) {
  try {
    const { id } = await params;

    // TODO: 실제 DB 삭제 로직 구현

    return NextResponse.json({
      success: true,
      data: null,
    });
  } catch (error) {
    console.error('좌석 삭제 에러:', error);
    return NextResponse.json(
      {
        success: false,
        message: '좌석 삭제 중 오류가 발생했습니다.',
      },
      { status: 500 },
    );
  }
}
