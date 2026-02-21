// 좌석 관리 타입 정의 (API 구조 기반)

declare namespace Seat {
  // ============================================
  // 에디터 내부 상태 (SeatGroup 중심)
  // ============================================
  
  // 좌석 그룹 (행×열 기반 관리)
  interface SeatGroup {
    id: string;
    sectionId?: string;
    origin: { x: number; y: number };
    // ✅ anchor는 state가 아님 - 선택 시마다 bounding box 기준으로 재계산
    rows: number;
    columns: number;
    seatGap: number;
    rowGap: number;
    rotation: number;
    flipHorizontal?: boolean;
    flipVertical?: boolean;
    curved?: boolean;
    curveAmount?: number;
    rowLabelConfig: LabelConfig;
    seatLabelConfig: LabelConfig;
    seatRadius: number;
  }

  // 라벨 설정
  interface LabelConfig {
    format: '1,2,3,4...' | 'A,B,C,D...';
    startValue: number | string;
    direction: 'ltr' | 'rtl' | 'ttb' | 'btt';
    position?: 'left' | 'right' | 'top' | 'bottom';
    visible: boolean;
  }

  // 구역 정보
  interface Section {
    id: string;
    name: string;
    color: string;
    price?: number;
  }

  // 사물 요소 (무대, 입구 등)
  interface ObjectElement {
    id: string;
    type: 'stage' | 'entrance';
    label: string;
    x: number;
    y: number;
    width: number;
    height: number;
    color?: string;
  }

  // 에디터 레이아웃 데이터 (내부 상태)
  interface EditorLayoutData {
    canvas: {
      width: number;
      height: number;
      seatRadius: number;
    };
    sections: Section[];
    seatGroups: SeatGroup[];
    objects: ObjectElement[];
  }

  // ============================================
  // API 요청/응답 타입 (변환 후)
  // ============================================

  // API 좌석 요청 (flatten된 개별 좌석)
  interface SeatRequest {
    seatId: string;        // 예: "A-A1", "B-B5"
    sectionId?: string;    // 구역 ID
    row: string;           // 행 레이블 (A, B, C...)
    number: number;        // 좌석 번호 (1, 2, 3...)
    x: number;             // X 좌표
    y: number;             // Y 좌표
  }

  // 캔버스 요청
  interface CanvasRequest {
    width: number;
    height: number;
    seatRadius: number;
  }

  // 구역 요청
  interface SectionRequest {
    sectionId: string;
    name: string;
    color: string;
    price?: number;
  }

  // 좌석 배치도 생성 요청 (API 전송용)
  interface CreateSeatRequest {
    region: string;
    venueName: string;
    hallName: string;
    layoutData: {
      canvas: {
        width: number;
        height: number;
        seatRadius: number;
      };
      sections: SectionRequest[];
      seats: SeatRequest[];
      totalSeats: number;
    };
  }

  // 좌석 중복 확인
  interface CheckDuplicateRequest {
    region: string;
    venueName: string;
    hallName: string;
  }

  interface CheckDuplicateResponse {
    isDuplicate: boolean;
    message?: string;
  }

  interface ListParams {
    page: number;
    size: number;
    region?: string;
    venueName?: string;
    hallName?: string;
  }

  interface List {
    seatMapId: string;
    region: string;
    venueName: string;
    hallName: string;
    createdAt: string;
    updatedAt: string;
  }

  // 좌석 등록 폼 데이터
  interface UpsertFormData {
    region: string;
    venueName: string;
    hallName: string;
    layoutData: EditorLayoutData;
  }

  // ============================================
  // 렌더링용 계산 타입 (화면 표시만)
  // ============================================

  // 계산된 좌석 (렌더링 시에만 생성, state 아님)
  interface CalculatedSeat {
    groupId: string;
    sectionId?: string;
    row: string;
    number: number;
    x: number;
    y: number;
    radius: number;
    status?: 'available' | 'reserved' | 'disabled';
  }

  // 계산된 행 레이블 (렌더링 시에만 생성)
  interface CalculatedRowLabel {
    groupId: string;
    label: string;
    x: number;
    y: number;
  }
}
