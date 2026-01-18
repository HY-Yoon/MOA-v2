// ============================================
// 폼 필드 및 에러 메시지
// ============================================
export const SEAT_FORM_FIELDS = {
  REGION: 'region',
  VENUE_NAME: 'venueName',
  HALL_NAME: 'hallName',
  LAYOUT_DATA: 'layoutData',
} as const;

export const SEAT_ERROR_MESSAGES = {
  DUPLICATE_HALL: '이미 등록된 홀입니다.',
  NOT_VALIDATED: '중복 확인을 먼저 진행해주세요.',
  NO_SEATS: '좌석 그룹을 최소 1개 이상 배치해주세요.',
} as const;

// ============================================
// 에디터 모드
// ============================================
export const EDITOR_MODES = {
  SELECT: 'select',
  ADD_SEAT_GROUP: 'addSeatGroup',
  ADD_CURVED_GROUP: 'addCurvedGroup',
  ADD_STAGE: 'addStage',
  ADD_ENTRANCE: 'addEntrance',
} as const;

// ============================================
// 사물 타입
// ============================================
export const OBJECT_TYPES = {
  STAGE: 'stage',
  ENTRANCE: 'entrance',
} as const;

// ============================================
// 라벨 설정
// ============================================
export const LABEL_FORMATS = {
  NUMBER: '1,2,3,4...',
  ALPHABET: 'A,B,C,D...',
} as const;

export const LABEL_DIRECTIONS = {
  LTR: 'ltr',
  RTL: 'rtl',
  TTB: 'ttb',
  BTT: 'btt',
} as const;

export const LABEL_POSITIONS = {
  LEFT: 'left',
  RIGHT: 'right',
  TOP: 'top',
  BOTTOM: 'bottom',
} as const;

// ============================================
// 기본 설정값
// ============================================
export const DEFAULT_CANVAS_CONFIG = {
  WIDTH: 1200,
  HEIGHT: 800,
  SEAT_RADIUS: 15,
} as const;

export const DEFAULT_SEAT_GROUP_CONFIG = {
  ROWS: 10,
  COLUMNS: 10,
  SEAT_GAP: 5,
  ROW_GAP: 10,
  ROTATION: 0,
  SEAT_RADIUS: 15,
} as const;
