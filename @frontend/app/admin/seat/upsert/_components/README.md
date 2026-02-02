# 좌석 등록 기능

## 📋 개요

WIX 스타일의 좌석 배치도 에디터를 제공하며, **SeatGroup 중심 아키텍처**로 대량 좌석 처리 시에도 높은 성능을 보장합니다.

## 🏗️ 아키텍처

### 핵심 원칙

```typescript
// ❌ AS-IS: 개별 좌석 state (800석 = 800번 리렌더)
const [seats, setSeats] = useState<Seat[]>([])

// ✅ TO-BE: SeatGroup state (800석 = 10개 그룹만 관리)
const [seatGroups, setSeatGroups] = useState<SeatGroup[]>([])
```

### 1. 에디터 내부 구조 (SeatGroup 중심)

```typescript
interface SeatGroup {
  id: string;
  origin: { x: number; y: number };
  rows: number;          // 행 수
  columns: number;       // 열 수 (좌석 수)
  seatGap: number;       // 좌석 간격
  rowGap: number;        // 행 간격
  rotation: number;      // 회전
  curved: boolean;       // 곡선 여부
  curveAmount?: number;  // 곡선 정도
  rowLabelConfig: LabelConfig;
  seatLabelConfig: LabelConfig;
  seatRadius: number;
}
```

**장점:**
- 10행×10열 = 100석을 **단일 객체**로 관리
- 드래그/이동 시 **1번의 state 업데이트**만 발생
- 행 간격 조정 시 **useMemo**로 자동 재계산

### 2. 렌더링 (계산된 좌석)

```typescript
// useMemo로 좌석 계산 (state 아님!)
const calculatedSeats = useMemo(() => {
  return seatGroups.flatMap(group => 
    calculateSeatsForRendering(group, seatRadius)
  );
}, [seatGroups, seatRadius]);
```

**특징:**
- Canvas 기반 렌더링 (React 컴포넌트 0개)
- DOM 노드 생성 없음
- requestAnimationFrame으로 드래그 최적화

### 3. API 전송 (Flatten)

```typescript
// 저장 시 1회만 변환
const flattened = flattenSeatGroupsToSeats(
  editorData.seatGroups, 
  seatRadius
);

// API 요청
const request: Seat.CreateSeatRequest = {
  region: 'Seoul',
  venueName: '예술의전당',
  hallName: '콘서트홀',
  layoutData: {
    canvas: { width, height, seatRadius },
    seats: flattened, // SeatRequest[]
    totalSeats: flattened.length
  }
};
```

## 🎨 주요 기능

### 1. 좌석 그룹 추가
- **직선 그룹**: 일반적인 극장 좌석
- **곡선 그룹**: 원형 극장, 앰피씨어터

### 2. 그룹 속성 편집
- 행 수 / 열 수
- 좌석 간격 / 행 간격
- 곡선 정도 (0~1)
- 회전 (0~360도)
- 행 레이블 (A,B,C... / 1,2,3...)
- 좌석 번호 (1,2,3... / A,B,C...)

### 3. 사물 추가
- 무대 (Stage)
- 입구 (Entrance)

### 4. 드래그 & 이동
```typescript
// 드래그 중: offset만 저장 (setState 최소화)
const [dragOffset, setDragOffset] = useState({ x: 0, y: 0 });

// 렌더링 시: 임시 offset 적용
const renderX = group.origin.x + dragOffset.x;

// 마우스 up: 실제 위치 업데이트 (1회)
setSeatGroups(prev => prev.map(g => 
  g.id === selectedId 
    ? { ...g, origin: { x: g.origin.x + dragOffset.x, ... } }
    : g
));
```

## 📁 파일 구조

```
app/admin/seat/upsert/
├── page.tsx                      # 페이지 컨테이너
└── _components/
    ├── SeatUpsertForm.tsx        # 폼 + 저장 로직
    ├── SeatLayoutEditor.tsx      # Canvas 에디터 (핵심)
    ├── SeatPropertiesPanel.tsx   # 속성 패널
    └── README.md                 # 이 파일
```

```
lib/admin/
└── seat-calculator.ts
    ├── flattenSeatGroupsToSeats()        # API 변환 (저장 시 1회)
    ├── calculateSeatsForRendering()      # 렌더링용 계산
    ├── calculateRowLabelsForRendering()  # 행 레이블 계산
    └── calculateSeatGroupBounds()        # 바운딩 박스
```

```
types/admin/seat/
└── index.d.ts
    ├── SeatGroup              # 에디터 내부 state
    ├── EditorLayoutData       # 에디터 전체 상태
    ├── SeatRequest            # API 요청용
    ├── CalculatedSeat         # 렌더링용 (임시)
    └── CreateSeatRequest      # API 요청 전체
```

## 🚀 성능 최적화

### 1. State 최소화
- ❌ 800개 좌석 → 800번 리렌더
- ✅ 10개 그룹 → 10번만 관리

### 2. Canvas 렌더링
- ❌ SVG 800개 DOM 노드
- ✅ Canvas 1개, 직접 그리기

### 3. Throttle/Debounce
```typescript
const animationFrameRef = useRef<number | null>(null);

const handleMouseMove = useCallback((e) => {
  if (animationFrameRef.current) return;
  
  animationFrameRef.current = requestAnimationFrame(() => {
    setDragOffset({ x: deltaX, y: deltaY });
    animationFrameRef.current = null;
  });
}, []);
```

### 4. useMemo 활용
```typescript
// 좌석 계산은 seatGroups 변경 시에만
const calculatedSeats = useMemo(() => 
  seatGroups.flatMap(g => calculateSeatsForRendering(g, radius)),
  [seatGroups, radius]
);
```

## 📊 예상 성능

| 좌석 수 | 그룹 수 | State 크기 | 드래그 응답 |
|--------|--------|-----------|-----------|
| 100석  | 2개    | ~2KB      | 60fps     |
| 500석  | 5개    | ~5KB      | 60fps     |
| 1000석 | 10개   | ~10KB     | 50fps+    |
| 2000석 | 20개   | ~20KB     | 40fps+    |

## 🔧 사용 방법

### 기본 사용
```typescript
<SeatLayoutEditor
  onLayoutChange={(data: EditorLayoutData) => {
    console.log('Groups:', data.seatGroups.length);
    console.log('Total seats:', calculateTotal(data));
  }}
  initialData={null}
/>
```

### 저장 시 변환
```typescript
const handleSave = () => {
  const flattened = flattenSeatGroupsToSeats(
    editorData.seatGroups,
    editorData.canvas.seatRadius
  );
  
  await createSeat({
    region: 'Seoul',
    venueName: '예술의전당',
    hallName: '콘서트홀',
    layoutData: {
      canvas: editorData.canvas,
      seats: flattened,
      totalSeats: flattened.length
    }
  });
};
```

## 🎯 향후 개선 방향

### 1. Undo/Redo
```typescript
const [history, setHistory] = useState<EditorLayoutData[]>([]);
const [currentIndex, setCurrentIndex] = useState(0);
```

### 2. 키보드 단축키
- `Ctrl+Z`: Undo
- `Ctrl+Y`: Redo
- `Delete`: 선택 삭제
- `Ctrl+D`: 복제

### 3. 구역(Section) 관리
```typescript
interface Section {
  id: string;
  name: string;      // VIP, R석, S석
  color: string;
  price: number;
}

// 그룹에 구역 할당
seatGroup.sectionId = 'section-vip';
```

### 4. 다중 선택
```typescript
const [selectedIds, setSelectedIds] = useState<string[]>([]);

// Shift+Click으로 다중 선택
// 일괄 이동/삭제/속성 변경
```

### 5. 스냅 그리드
```typescript
const snapToGrid = (x: number, gridSize = 10) => {
  return Math.round(x / gridSize) * gridSize;
};
```

## 🐛 알려진 이슈

1. ~~곡선 그룹 회전 시 좌표 오차~~ ✅ 해결
2. ~~대량 좌석 드래그 시 브라우저 다운~~ ✅ 해결 (requestAnimationFrame)
3. ~~행 수 조정 시 무한 리렌더~~ ✅ 해결 (SeatGroup 구조 변경)

## 📝 변경 이력

- **2026-01-17**: SeatGroup 중심 아키텍처로 완전 재작성
  - Canvas 렌더링 적용
  - State 최소화 (개별 좌석 → 그룹)
  - 성능 최적화 (requestAnimationFrame, useMemo)
  - 타입 정의 정리 (LayoutSettings 제거)
  - SeatToolbar 제거 (에디터에 통합)

## 📚 참고 자료

- [WIX 좌석 배치도 가이드](https://support.wix.com/ko/article/wix-%EC%9D%B4%EB%B2%A4%ED%8A%B8-%EC%A2%8C%EC%84%9D-%EB%B0%B0%EC%B9%98%EB%8F%84-%EC%83%9D%EC%84%B1%ED%95%98%EA%B8%B0)
- [Canvas API](https://developer.mozilla.org/en-US/docs/Web/API/Canvas_API)
- [React useMemo](https://react.dev/reference/react/useMemo)
