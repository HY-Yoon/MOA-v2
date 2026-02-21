'use client';

import { Button } from '@/components/atoms';
import { DEFAULT_CANVAS_CONFIG, DEFAULT_SEAT_GROUP_CONFIG, EDITOR_MODES, LABEL_FORMATS } from '@/constants/admin/seat';
import {
  calculateRotationAnchor,
  calculateRowLabelsForRendering,
  calculateSeatGroupBounds,
  calculateSeatsForRendering,
} from '@/lib/admin/seat-calculator';
import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import SeatPropertiesPanel from './SeatPropertiesPanel';
import SectionManager from './SectionManager';

interface Props {
  onLayoutChange: (layoutData: Seat.EditorLayoutData) => void;
  initialData: Seat.EditorLayoutData | null;
}

type ResizeHandle = 'nw' | 'ne' | 'sw' | 'se' | 'n' | 's' | 'e' | 'w';

export default function SeatLayoutEditor({ onLayoutChange, initialData }: Props) {
  const canvasRef = useRef<HTMLCanvasElement>(null);
  const containerRef = useRef<HTMLDivElement>(null);
  
  const [canvasSize] = useState({
    width: DEFAULT_CANVAS_CONFIG.WIDTH,
    height: DEFAULT_CANVAS_CONFIG.HEIGHT,
  });
  const [seatRadius] = useState(DEFAULT_CANVAS_CONFIG.SEAT_RADIUS);
  const [zoom, setZoom] = useState(1);
  const [mode, setMode] = useState<string>(EDITOR_MODES.SELECT);

  // ============================================
  // ✅ SeatGroup 중심 상태
  // ============================================
  const [seatGroups, setSeatGroups] = useState<Seat.SeatGroup[]>([]);
  const [objects, setObjects] = useState<Seat.ObjectElement[]>([]);
  const [sections, setSections] = useState<Seat.Section[]>([]);
  const [selectedId, setSelectedId] = useState<string | null>(null);

  // ============================================
  // ✅ 입력 모드 상태
  // ============================================
  type InputMode = 'IDLE' | 'DRAGGING' | 'ROTATING' | 'RESIZING' | 'CREATING';
  const [inputMode, setInputMode] = useState<InputMode>('IDLE');
  const [resizeHandle, setResizeHandle] = useState<ResizeHandle | null>(null);
  const [dragStart, setDragStart] = useState<{ x: number; y: number } | null>(null);
  const [dragOffset, setDragOffset] = useState<{ x: number; y: number }>({ x: 0, y: 0 });

  // ✅ 회전 anchor (회전 모드에서만 유효)
  const [rotationAnchor, setRotationAnchor] = useState<{ x: number; y: number } | null>(null);

  // Animation frame
  const animationFrameRef = useRef<number | null>(null);

  // ============================================
  // 렌더링용 좌석 계산
  // ============================================
  const calculatedSeats = useMemo(() => {
    return seatGroups.flatMap((group) => {
      // ✅ 회전 모드이고 선택된 그룹인 경우 rotationAnchor 사용
      const anchor = (inputMode === 'ROTATING' && selectedId === group.id && rotationAnchor) 
        ? rotationAnchor 
        : undefined;
      return calculateSeatsForRendering(group, seatRadius, anchor);
    });
  }, [seatGroups, seatRadius, inputMode, selectedId, rotationAnchor]);


  // 선택된 요소 가져오기
  const selectedElement = useMemo(() => {
    if (!selectedId) return null;
    const group = seatGroups.find(g => g.id === selectedId);
    if (group) return group;
    return objects.find(o => o.id === selectedId) || null;
  }, [selectedId, seatGroups, objects]);

  // ============================================
  // 초기 데이터 로드 (한 번만)
  // ============================================
  const isInitializedRef = useRef(false);
  
  useEffect(() => {
    // ✅ 한 번만 초기화 (무한 루프 방지)
    if (initialData && !isInitializedRef.current) {
      setSeatGroups(initialData.seatGroups);
      setObjects(initialData.objects);
      setSections(initialData.sections);
      isInitializedRef.current = true;
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []); // ✅ 빈 배열: 마운트 시 한 번만 실행 (의도적으로 initialData 제외)

  // ============================================
  // 핸들러 함수 정의
  // ============================================
  const handleDelete = useCallback(() => {
    if (!selectedId) return;

    setSeatGroups((prev) => prev.filter((g) => g.id !== selectedId));
    setObjects((prev) => prev.filter((o) => o.id !== selectedId));
    setSelectedId(null);
  }, [selectedId]);

  const handleUpdateElement = useCallback((updates: Partial<Seat.SeatGroup> | Partial<Seat.ObjectElement>) => {
    // ✅ selectedId를 클로저에 직접 캡처하지 않고 함수형 업데이트 사용
    setSeatGroups(prev => prev.map(g => {
      // 선택된 그룹인지 확인 (함수 내부에서 판단)
      const isSelected = selectedId !== null && g.id === selectedId;
      return isSelected ? { ...g, ...updates } as Seat.SeatGroup : g;
    }));

    setObjects(prev => prev.map(o => {
      const isSelected = selectedId !== null && o.id === selectedId;
      return isSelected ? { ...o, ...updates } as Seat.ObjectElement : o;
    }));
  }, [selectedId]); 

  // ============================================
  // 레이아웃 데이터 변경 알림
  // ============================================
  const onLayoutChangeRef = useRef(onLayoutChange);
  
  useEffect(() => {
    onLayoutChangeRef.current = onLayoutChange;
  }, [onLayoutChange]);

  useEffect(() => {
    const layoutData: Seat.EditorLayoutData = {
      canvas: {
        width: canvasSize.width,
        height: canvasSize.height,
        seatRadius,
      },
      sections,
      seatGroups,
      objects,
    };
    onLayoutChangeRef.current(layoutData);
  }, [seatGroups, objects, sections, canvasSize, seatRadius]);

  // ============================================
  // 키보드 단축키
  // ============================================
  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      const target = e.target as HTMLElement | null;
      const isEditableTarget =
        target?.tagName === 'INPUT' ||
        target?.tagName === 'TEXTAREA' ||
        target?.tagName === 'SELECT' ||
        target?.isContentEditable;

      // 입력 필드 포커스 중에는 에디터 단축키를 무시
      if (isEditableTarget) return;

      // Cmd/Ctrl + Z: 실행 취소 (TODO: Undo/Redo 구현 시)
      if ((e.metaKey || e.ctrlKey) && e.key === 'z' && !e.shiftKey) {
        e.preventDefault();
        // TODO: Undo
        console.log('Undo');
      }
      
      // Cmd/Ctrl + Shift + Z 또는 Cmd/Ctrl + Y: 다시 실행
      if (((e.metaKey || e.ctrlKey) && e.shiftKey && e.key === 'z') || ((e.metaKey || e.ctrlKey) && e.key === 'y')) {
        e.preventDefault();
        // TODO: Redo
        console.log('Redo');
      }

      // Delete 또는 Backspace: 선택 항목 삭제
      if ((e.key === 'Delete' || e.key === 'Backspace') && selectedId) {
        e.preventDefault();
        handleDelete();
      }

      // Escape: 선택 해제 또는 모드 초기화
      if (e.key === 'Escape') {
        e.preventDefault();
        setSelectedId(null);
        setMode(EDITOR_MODES.SELECT);
      }

      // V: 선택 모드
      if (e.key === 'v' || e.key === 'V') {
        e.preventDefault();
        setMode(EDITOR_MODES.SELECT);
      }

      // R: 직선 행 추가
      if (e.key === 'r' || e.key === 'R') {
        e.preventDefault();
        setMode(EDITOR_MODES.ADD_SEAT_GROUP);
      }

      // C: 곡선 행 추가
      if (e.key === 'c' || e.key === 'C') {
        e.preventDefault();
        setMode(EDITOR_MODES.ADD_CURVED_GROUP);
      }

      // S: 무대 추가
      if (e.key === 's' || e.key === 'S') {
        e.preventDefault();
        setMode(EDITOR_MODES.ADD_STAGE);
      }

      // E: 입구 추가
      if (e.key === 'e' || e.key === 'E') {
        e.preventDefault();
        setMode(EDITOR_MODES.ADD_ENTRANCE);
      }

      // +: Zoom In
      if (e.key === '+' || e.key === '=') {
        e.preventDefault();
        setZoom((z) => Math.min(2, z + 0.1));
      }

      // -: Zoom Out
      if (e.key === '-' || e.key === '_') {
        e.preventDefault();
        setZoom((z) => Math.max(0.5, z - 0.1));
      }

      // 0: Zoom Reset
      if (e.key === '0') {
        e.preventDefault();
        setZoom(1);
      }
    };

    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [selectedId, handleDelete]);

  // ============================================
  // 회전 핸들 감지
  // ============================================
  const getRotationHandle = useCallback((x: number, y: number, element: Seat.SeatGroup): { x: number; y: number } | null => {
    const handleSize = 10;
    // ✅ 현재 bounding box 기준으로 anchor 계산
    const anchor = calculateRotationAnchor(element, seatRadius, 30);
    
    // 회전 핸들 클릭 감지
    const distance = Math.sqrt(Math.pow(x - anchor.x, 2) + Math.pow(y - anchor.y, 2));
    return distance < handleSize ? anchor : null;
  }, [seatRadius]);

  // ============================================
  // 리사이즈 핸들 감지
  // ============================================
  const getResizeHandle = useCallback((x: number, y: number, element: Seat.SeatGroup | Seat.ObjectElement): ResizeHandle | null => {
    const handleSize = 8;
    
    if ('width' in element && 'height' in element) {
      // Object (무대, 입구)
      const { x: ex, y: ey, width, height } = element;
      
      // 코너
      if (Math.abs(x - ex) < handleSize && Math.abs(y - ey) < handleSize) return 'nw';
      if (Math.abs(x - (ex + width)) < handleSize && Math.abs(y - ey) < handleSize) return 'ne';
      if (Math.abs(x - ex) < handleSize && Math.abs(y - (ey + height)) < handleSize) return 'sw';
      if (Math.abs(x - (ex + width)) < handleSize && Math.abs(y - (ey + height)) < handleSize) return 'se';
      
      // 변
      if (Math.abs(y - ey) < handleSize && x > ex && x < ex + width) return 'n';
      if (Math.abs(y - (ey + height)) < handleSize && x > ex && x < ex + width) return 's';
      if (Math.abs(x - ex) < handleSize && y > ey && y < ey + height) return 'w';
      if (Math.abs(x - (ex + width)) < handleSize && y > ey && y < ey + height) return 'e';
    } else if ('origin' in element) {
      // ✅ SeatGroup - 회전된 좌표계 고려
      const localGroup = { ...element, rotation: 0 };
      const localBounds = calculateSeatGroupBounds(localGroup, seatRadius);
      const width = localBounds.maxX - localBounds.minX;
      const height = localBounds.maxY - localBounds.minY;
      
      // 로컬 바운딩 박스 중심 (origin 기준)
      const localCenterX = (localBounds.minX + localBounds.maxX) / 2 - element.origin.x;
      const localCenterY = (localBounds.minY + localBounds.maxY) / 2 - element.origin.y;
      
      // 클릭 좌표를 로컬 좌표계로 변환 (역회전)
      const centerX = element.origin.x;
      const centerY = element.origin.y;
      const rad = -(element.rotation * Math.PI) / 180; // 역회전
      const dx = x - centerX;
      const dy = y - centerY;
      const localX = dx * Math.cos(rad) - dy * Math.sin(rad);
      const localY = dx * Math.sin(rad) + dy * Math.cos(rad);
      
      // 로컬 좌표계에서 핸들 위치
      const eastX = localCenterX + width / 2;
      const eastY = localCenterY;
      const southX = localCenterX;
      const southY = localCenterY + height / 2;
      
      // 동쪽 (열 추가)
      if (Math.abs(localX - eastX) < handleSize && Math.abs(localY - eastY) < handleSize) {
        return 'e';
      }
      
      // 남쪽 (행 추가)
      if (Math.abs(localX - southX) < handleSize && Math.abs(localY - southY) < handleSize) {
        return 's';
      }
    }
    
    return null;
  }, [seatRadius]);

  // ============================================
  // Canvas 렌더링
  // ============================================
  useEffect(() => {
    const canvas = canvasRef.current;
    if (!canvas) return;

    const ctx = canvas.getContext('2d');
    if (!ctx) return;

    // ✅ 1. 먼저 transform 초기화하고 전체 Canvas 지우기
    ctx.setTransform(1, 0, 0, 1, 0, 0);
    ctx.clearRect(0, 0, canvas.width, canvas.height);

    // ✅ 2. 스케일 적용
    const scale = zoom;
    ctx.setTransform(scale, 0, 0, scale, 0, 0);

    // 격자 배경
    ctx.strokeStyle = '#e5e7eb';
    ctx.lineWidth = 1 / scale;
    for (let x = 0; x <= canvasSize.width; x += 50) {
      ctx.beginPath();
      ctx.moveTo(x, 0);
      ctx.lineTo(x, canvasSize.height);
      ctx.stroke();
    }
    for (let y = 0; y <= canvasSize.height; y += 50) {
      ctx.beginPath();
      ctx.moveTo(0, y);
      ctx.lineTo(canvasSize.width, y);
      ctx.stroke();
    }

    // 생성 중인 객체 렌더링 (드래그 중)
    if (inputMode === 'CREATING' && dragStart && (mode === EDITOR_MODES.ADD_STAGE || mode === EDITOR_MODES.ADD_ENTRANCE)) {
      const x = Math.min(dragStart.x, dragStart.x + dragOffset.x);
      const y = Math.min(dragStart.y, dragStart.y + dragOffset.y);
      const width = Math.abs(dragOffset.x);
      const height = Math.abs(dragOffset.y);

      ctx.fillStyle = mode === EDITOR_MODES.ADD_STAGE ? 'rgba(255, 215, 0, 0.3)' : 'rgba(76, 175, 80, 0.3)';
      ctx.strokeStyle = mode === EDITOR_MODES.ADD_STAGE ? '#FFD700' : '#4CAF50';
      ctx.lineWidth = 2 / scale;
      ctx.setLineDash([5 / scale, 5 / scale]);
      ctx.fillRect(x, y, width, height);
      ctx.strokeRect(x, y, width, height);
      ctx.setLineDash([]);
    }

    // 드래그 오프셋 적용
    const getPosition = (x: number, y: number, id: string) => {
      if (inputMode === 'DRAGGING' && selectedId === id) {
        return { x: x + dragOffset.x, y: y + dragOffset.y };
      }
      return { x, y };
    };

    const getObjectSize = (obj: Seat.ObjectElement) => {
      if (inputMode === 'RESIZING' && selectedId === obj.id) {
        let { width, height, x, y } = obj;
        
        if (resizeHandle?.includes('e')) width += dragOffset.x;
        if (resizeHandle?.includes('w')) {
          width -= dragOffset.x;
          x += dragOffset.x;
        }
        if (resizeHandle?.includes('s')) height += dragOffset.y;
        if (resizeHandle?.includes('n')) {
          height -= dragOffset.y;
          y += dragOffset.y;
        }
        
        return { x, y, width: Math.max(20, width), height: Math.max(20, height) };
      }
      return obj;
    };

    // 사물 렌더링
    objects.forEach((obj) => {
      const sized = getObjectSize(obj);
      const pos = getPosition(sized.x, sized.y, obj.id);
      const isSelected = selectedId === obj.id;

      ctx.fillStyle = obj.color || '#FFD700';
      ctx.strokeStyle = isSelected ? '#2563eb' : '#64748b';
      ctx.lineWidth = (isSelected ? 3 : 1) / scale;
      ctx.fillRect(pos.x, pos.y, sized.width, sized.height);
      ctx.strokeRect(pos.x, pos.y, sized.width, sized.height);

      // 리사이즈 핸들 (선택 시)
      if (isSelected) {
        ctx.fillStyle = '#2563eb';
        const handleSize = 6 / scale;
        const handles = [
          { x: pos.x, y: pos.y }, // nw
          { x: pos.x + sized.width, y: pos.y }, // ne
          { x: pos.x, y: pos.y + sized.height }, // sw
          { x: pos.x + sized.width, y: pos.y + sized.height }, // se
        ];
        handles.forEach(h => {
          ctx.fillRect(h.x - handleSize / 2, h.y - handleSize / 2, handleSize, handleSize);
        });
      }

      // 레이블
      ctx.fillStyle = 'white';
      ctx.font = `bold ${14 / scale}px sans-serif`;
      ctx.textAlign = 'center';
      ctx.textBaseline = 'middle';
      ctx.fillText(obj.label, pos.x + sized.width / 2, pos.y + sized.height / 2);
    });

    // 좌석 그룹 렌더링
    seatGroups.forEach((group) => {
      const isSelected = selectedId === group.id;
      
      // 리사이즈로 행/열 변경 중
      let displayGroup = group;
      if (inputMode === 'RESIZING' && selectedId === group.id) {
        const seatWidth = (seatRadius * 2 + group.seatGap);
        const rowHeight = (seatRadius * 2 + group.rowGap);
        
        let newColumns = group.columns;
        let newRows = group.rows;
        
        if (resizeHandle === 'e') {
          const deltaColumns = Math.round(dragOffset.x / seatWidth);
          newColumns = Math.max(1, group.columns + deltaColumns);
        } else if (resizeHandle === 's') {
          const deltaRows = Math.round(dragOffset.y / rowHeight);
          newRows = Math.max(1, group.rows + deltaRows);
        }
        
        displayGroup = { ...group, rows: newRows, columns: newColumns };
      }
      
      // ✅ 회전 모드인 경우 rotationAnchor 전달
      const anchor = (inputMode === 'ROTATING' && selectedId === group.id && rotationAnchor)
        ? rotationAnchor
        : undefined;
      const seats = calculateSeatsForRendering(displayGroup, seatRadius, anchor);

      // 바운딩 박스 (선택 시)
      if (isSelected) {
        // ✅ rotation = 0 기준 로컬 바운딩 박스
        const localGroup = { ...displayGroup, rotation: 0 };
        const localBounds = calculateSeatGroupBounds(localGroup, seatRadius);
        const width = localBounds.maxX - localBounds.minX;
        const height = localBounds.maxY - localBounds.minY;
        
        // ✅ 회전 중심
        const centerX = inputMode === 'ROTATING' && rotationAnchor ? rotationAnchor.x : displayGroup.origin.x;
        const centerY = inputMode === 'ROTATING' && rotationAnchor ? rotationAnchor.y : displayGroup.origin.y;
        
        // ✅ 드래그 오프셋 적용
        const offsetX = inputMode === 'DRAGGING' && selectedId === group.id ? dragOffset.x : 0;
        const offsetY = inputMode === 'DRAGGING' && selectedId === group.id ? dragOffset.y : 0;
        
        ctx.save();
        ctx.translate(centerX + offsetX, centerY + offsetY);
        ctx.rotate((displayGroup.rotation * Math.PI) / 180);
        
        // 로컬 좌표계에서 사각형 그리기 (중심 기준)
        const localCenterX = (localBounds.minX + localBounds.maxX) / 2 - displayGroup.origin.x;
        const localCenterY = (localBounds.minY + localBounds.maxY) / 2 - displayGroup.origin.y;
        
        ctx.strokeStyle = '#2563eb';
        ctx.lineWidth = 2 / scale;
        ctx.setLineDash([5 / scale, 5 / scale]);
        ctx.strokeRect(
          localCenterX - width / 2 - 5,
          localCenterY - height / 2 - 5,
          width + 10,
          height + 10
        );
        ctx.setLineDash([]);
        
        // 리사이즈 핸들 (로컬 좌표계)
        ctx.fillStyle = '#2563eb';
        const handleSize = 8 / scale;
        // 동쪽 (열 추가)
        ctx.fillRect(
          localCenterX + width / 2 - handleSize / 2,
          localCenterY - handleSize / 2,
          handleSize,
          handleSize
        );
        // 남쪽 (행 추가)
        ctx.fillRect(
          localCenterX - handleSize / 2,
          localCenterY + height / 2 - handleSize / 2,
          handleSize,
          handleSize
        );
        
        ctx.restore();
        
        // ✅ 회전 핸들 (월드 좌표, 항상 바운딩 박스 상단 중앙 + offset)
        const currentAnchor = calculateRotationAnchor(displayGroup, seatRadius, 30);
        
        // 회전 핸들 (원형)
        ctx.fillStyle = '#ff0000';
        ctx.strokeStyle = '#ffffff';
        ctx.lineWidth = 2 / scale;
        ctx.beginPath();
        ctx.arc(currentAnchor.x + offsetX, currentAnchor.y + offsetY, 8 / scale, 0, Math.PI * 2);
        ctx.fill();
        ctx.stroke();
        
        // anchor 아이콘
        ctx.fillStyle = '#ffffff';
        ctx.font = `bold ${10 / scale}px sans-serif`;
        ctx.textAlign = 'center';
        ctx.textBaseline = 'middle';
        ctx.fillText('⟳', currentAnchor.x + offsetX, currentAnchor.y + offsetY);
      }

      // 좌석 렌더링
      seats.forEach((seat) => {
        const pos = getPosition(seat.x, seat.y, group.id);

        // 구역 색상 가져오기
        const sectionColor = group.sectionId 
          ? sections.find(s => s.id === group.sectionId)?.color 
          : undefined;

        ctx.fillStyle = sectionColor || '#3b82f6';
        ctx.strokeStyle = isSelected ? '#2563eb' : (sectionColor ? '#1e293b' : '#1e3a8a');
        ctx.lineWidth = (isSelected ? 2 : 1) / scale;
        ctx.beginPath();
        ctx.arc(pos.x, pos.y, seat.radius, 0, Math.PI * 2);
        ctx.fill();
        ctx.stroke();

        // 좌석 번호
        ctx.fillStyle = 'white';
        ctx.font = `bold ${10 / scale}px sans-serif`;
        ctx.textAlign = 'center';
        ctx.textBaseline = 'middle';
        ctx.fillText(String(seat.number), pos.x, pos.y);
      });

      // 행 레이블
      const rowLabels = calculateRowLabelsForRendering(displayGroup, seatRadius, anchor);
      rowLabels.forEach((label) => {
        const pos = getPosition(label.x, label.y, group.id);

        ctx.save(); // ✅ 현재 상태 저장
        
        // ✅ 레이블 위치로 이동
        ctx.translate(pos.x, pos.y);
        
        // ✅ 회전 적용
        if (displayGroup.rotation !== 0) {
          const rad = (displayGroup.rotation * Math.PI) / 180;
          ctx.rotate(rad);
        }
        
        ctx.fillStyle = '#64748b';
        ctx.font = `bold ${12 / scale}px sans-serif`;
        ctx.textAlign = displayGroup.rowLabelConfig.position === 'left' ? 'end' : 'start';
        ctx.textBaseline = 'middle';
        ctx.fillText(label.label, 0, 0); // ✅ 원점에 그림 (이미 translate됨)
        
        ctx.restore(); // ✅ 상태 복원
      });
    });
  }, [
    seatGroups,
    objects,
    sections,
    selectedId,
    inputMode,
    rotationAnchor,
    resizeHandle,
    dragOffset,
    dragStart,
    canvasSize,
    seatRadius,
    zoom,
    mode,
  ]);

  // ============================================
  // 이벤트 핸들러
  // ============================================
  const handleCanvasMouseDown = useCallback(
    (e: React.MouseEvent<HTMLCanvasElement>) => {
      const rect = canvasRef.current?.getBoundingClientRect();
      if (!rect) return;

      const x = (e.clientX - rect.left) / zoom;
      const y = (e.clientY - rect.top) / zoom;

      // 생성 모드
      if (mode === EDITOR_MODES.ADD_STAGE || mode === EDITOR_MODES.ADD_ENTRANCE) {
        setInputMode('CREATING');
        setDragStart({ x, y });
        setDragOffset({ x: 0, y: 0 });
        return;
      }

      // 좌석 그룹 추가 모드
      if (mode === EDITOR_MODES.ADD_SEAT_GROUP || mode === EDITOR_MODES.ADD_CURVED_GROUP) {
        const newGroup: Seat.SeatGroup = {
          id: `group-${Date.now()}`,
          origin: { x, y },
          // ✅ anchor는 state가 아님 - 선택 시 재계산
          rows: DEFAULT_SEAT_GROUP_CONFIG.ROWS,
          columns: DEFAULT_SEAT_GROUP_CONFIG.COLUMNS,
          seatGap: DEFAULT_SEAT_GROUP_CONFIG.SEAT_GAP,
          rowGap: DEFAULT_SEAT_GROUP_CONFIG.ROW_GAP,
          rotation: DEFAULT_SEAT_GROUP_CONFIG.ROTATION,
          curved: mode === EDITOR_MODES.ADD_CURVED_GROUP,
          curveAmount: mode === EDITOR_MODES.ADD_CURVED_GROUP ? 0.2 : undefined,
          rowLabelConfig: {
            format: LABEL_FORMATS.ALPHABET,
            startValue: 'A',
            direction: 'ttb',
            position: 'left',
            visible: true,
          },
          seatLabelConfig: {
            format: LABEL_FORMATS.NUMBER,
            startValue: 1,
            direction: 'ltr',
            visible: false,
          },
          seatRadius,
        };

        setSeatGroups((prev) => [...prev, newGroup]);
        setSelectedId(newGroup.id);
        setMode(EDITOR_MODES.SELECT);
        return;
      }

      // 선택 모드
      if (mode === EDITOR_MODES.SELECT) {
        // ✅ 1. 회전 핸들 체크 (최우선)
        if (selectedId) {
          const group = seatGroups.find(g => g.id === selectedId);
          if (group) {
            const anchor = getRotationHandle(x, y, group);
            if (anchor) {
              // ✅ 회전 모드 시작
              setInputMode('ROTATING');
              setRotationAnchor(anchor); // anchor 고정
              setDragStart({ x, y });
              return;
            }
          }
        }
        
        // ✅ 2. 리사이즈 핸들 체크
        if (selectedId) {
          const element = seatGroups.find(g => g.id === selectedId) || objects.find(o => o.id === selectedId);
          if (element) {
            const handle = getResizeHandle(x, y, element);
            if (handle) {
              setInputMode('RESIZING');
              setResizeHandle(handle);
              setDragStart({ x, y });
              setDragOffset({ x: 0, y: 0 });
              return;
            }
          }
        }

        // ✅ 3. 사물 선택 및 드래그 시작
        for (const obj of objects) {
          if (x >= obj.x && x <= obj.x + obj.width && y >= obj.y && y <= obj.y + obj.height) {
            setSelectedId(obj.id);
            setInputMode('DRAGGING'); // ✅ 이동 모드
            setDragStart({ x, y });
            setDragOffset({ x: 0, y: 0 });
            return;
          }
        }

        // ✅ 4. 좌석 그룹 선택 및 드래그 시작 (OBB 기반)
        for (const group of seatGroups) {
          const localBounds = calculateSeatGroupBounds(group, seatRadius);
          const width = localBounds.maxX - localBounds.minX;
          const height = localBounds.maxY - localBounds.minY;
          
          // 로컬 중심
          const localCenterX = (localBounds.minX + localBounds.maxX) / 2 - group.origin.x;
          const localCenterY = (localBounds.minY + localBounds.maxY) / 2 - group.origin.y;
          
          // 클릭 좌표를 로컬 좌표계로 변환 (역회전)
          const centerX = group.origin.x;
          const centerY = group.origin.y;
          const rad = -(group.rotation * Math.PI) / 180;
          const dx = x - centerX;
          const dy = y - centerY;
          const localX = dx * Math.cos(rad) - dy * Math.sin(rad);
          const localY = dx * Math.sin(rad) + dy * Math.cos(rad);
          
          // 로컬 바운딩 박스 내부인지 확인
          if (
            localX >= localCenterX - width / 2 &&
            localX <= localCenterX + width / 2 &&
            localY >= localCenterY - height / 2 &&
            localY <= localCenterY + height / 2
          ) {
            setSelectedId(group.id);
            setInputMode('DRAGGING'); // ✅ 이동 모드
            setDragStart({ x, y });
            setDragOffset({ x: 0, y: 0 });
            return;
          }
        }

        // 빈 공간 클릭 - 선택 해제
        setSelectedId(null);
        setInputMode('IDLE');
      }
    },
    [mode, zoom, objects, seatGroups, seatRadius, selectedId, getResizeHandle, getRotationHandle]
  );

  const handleCanvasMouseMove = useCallback(
    (e: React.MouseEvent<HTMLCanvasElement>) => {
      if (!dragStart) return;

      if (animationFrameRef.current) return;

      animationFrameRef.current = requestAnimationFrame(() => {
        const rect = canvasRef.current?.getBoundingClientRect();
        if (!rect) return;

        const x = (e.clientX - rect.left) / zoom;
        const y = (e.clientY - rect.top) / zoom;

        // ✅ 회전 모드 (rotation만 변경, position 변경 금지)
        if (inputMode === 'ROTATING' && selectedId && rotationAnchor) {
          const anchorX = rotationAnchor.x;
          const anchorY = rotationAnchor.y;
          
          // 회전 각도 계산
          const angle = Math.atan2(y - anchorY, x - anchorX) * (180 / Math.PI);
          // 상단을 기준으로 (90도 오프셋)
          const rawRotation = angle + 90;
          // 15도 단위로 스냅
          const rotation = Math.round(rawRotation / 15) * 15;
          
          // ✅ rotation만 업데이트
          setSeatGroups(prev => prev.map(g => 
            g.id === selectedId ? { ...g, rotation } : g
          ));
          
          animationFrameRef.current = null;
          return;
        }

        setDragOffset({
          x: x - dragStart.x,
          y: y - dragStart.y,
        });

        animationFrameRef.current = null;
      });
    },
    [dragStart, zoom, inputMode, selectedId, rotationAnchor]
  );

  const handleCanvasMouseUp = useCallback(() => {
    // ✅ 회전 완료 → IDLE
    if (inputMode === 'ROTATING') {
      setInputMode('IDLE');
      setRotationAnchor(null); // anchor 폐기
      setDragStart(null);
      return;
    }

    // ✅ 생성 완료
    if (inputMode === 'CREATING' && dragStart) {
      const x = Math.min(dragStart.x, dragStart.x + dragOffset.x);
      const y = Math.min(dragStart.y, dragStart.y + dragOffset.y);
      const width = Math.abs(dragOffset.x);
      const height = Math.abs(dragOffset.y);

      if (width > 20 && height > 20) {
        if (mode === EDITOR_MODES.ADD_STAGE) {
          const newStage: Seat.ObjectElement = {
            id: `stage-${Date.now()}`,
            type: 'stage',
            label: '무대',
            x,
            y,
            width,
            height,
            color: '#FFD700',
          };
          setObjects((prev) => [...prev, newStage]);
          setSelectedId(newStage.id);
        } else if (mode === EDITOR_MODES.ADD_ENTRANCE) {
          const newEntrance: Seat.ObjectElement = {
            id: `entrance-${Date.now()}`,
            type: 'entrance',
            label: '입구',
            x,
            y,
            width,
            height,
            color: '#4CAF50',
          };
          setObjects((prev) => [...prev, newEntrance]);
          setSelectedId(newEntrance.id);
        }
      }
      
      setMode(EDITOR_MODES.SELECT);
      setInputMode('IDLE');
    }

    // ✅ 이동 완료 - 위치 업데이트
    if (inputMode === 'DRAGGING' && selectedId) {
      setSeatGroups((prev) =>
        prev.map((group) =>
          group.id === selectedId
            ? {
                ...group,
                origin: {
                  x: group.origin.x + dragOffset.x,
                  y: group.origin.y + dragOffset.y,
                },
              }
            : group
        )
      );

      setObjects((prev) =>
        prev.map((obj) =>
          obj.id === selectedId
            ? {
                ...obj,
                x: obj.x + dragOffset.x,
                y: obj.y + dragOffset.y,
              }
            : obj
        )
      );

      setInputMode('IDLE');
    }

    // ✅ 리사이즈 완료
    if (inputMode === 'RESIZING' && selectedId) {
      // 좌석 그룹 리사이즈 (행/열 추가)
      const group = seatGroups.find(g => g.id === selectedId);
      if (group) {
        const seatWidth = (seatRadius * 2 + group.seatGap);
        const rowHeight = (seatRadius * 2 + group.rowGap);
        
        setSeatGroups(prev => prev.map(g => {
          if (g.id !== selectedId) return g;
          
          let newColumns = g.columns;
          let newRows = g.rows;
          
          if (resizeHandle === 'e') {
            const deltaColumns = Math.round(dragOffset.x / seatWidth);
            newColumns = Math.max(1, g.columns + deltaColumns);
          } else if (resizeHandle === 's') {
            const deltaRows = Math.round(dragOffset.y / rowHeight);
            newRows = Math.max(1, g.rows + deltaRows);
          }
          
          return { ...g, rows: newRows, columns: newColumns };
        }));
      }

      // 사물 리사이즈
      const obj = objects.find(o => o.id === selectedId);
      if (obj) {
        setObjects(prev => prev.map(o => {
          if (o.id !== selectedId) return o;
          
          let { width, height, x, y } = o;
          
          if (resizeHandle?.includes('e')) width += dragOffset.x;
          if (resizeHandle?.includes('w')) {
            width -= dragOffset.x;
            x += dragOffset.x;
          }
          if (resizeHandle?.includes('s')) height += dragOffset.y;
          if (resizeHandle?.includes('n')) {
            height -= dragOffset.y;
            y += dragOffset.y;
          }
          
          return { ...o, x, y, width: Math.max(20, width), height: Math.max(20, height) };
        }));
      }

      setInputMode('IDLE');
      setResizeHandle(null);
    }

    // ✅ 항상 IDLE로 복귀
    setDragStart(null);
    setDragOffset({ x: 0, y: 0 });
  }, [inputMode, selectedId, dragStart, dragOffset, mode, resizeHandle, seatGroups, objects, seatRadius]);

  return (
    <div className="flex gap-4">
      {/* 좌측: 구역 관리 */}
      <div className="w-80">
        <SectionManager sections={sections} onSectionsChange={setSections} />
      </div>

      {/* 메인 에디터 */}
      <div className="flex flex-1 flex-col gap-4">
        {/* 도구 모음 */}
        <div className="flex items-center gap-2 rounded-lg border bg-white p-4">
          <Button
            type="button"
            variant={mode === EDITOR_MODES.SELECT ? 'default' : 'outline'}
            size="sm"
            onClick={() => setMode(EDITOR_MODES.SELECT)}
            title="선택 (V)"
          >
            선택
          </Button>
          <Button
            type="button"
            variant={mode === EDITOR_MODES.ADD_SEAT_GROUP ? 'default' : 'outline'}
            size="sm"
            onClick={() => setMode(EDITOR_MODES.ADD_SEAT_GROUP)}
            title="직선 행 (R)"
          >
            직선 행
          </Button>
          <Button
            type="button"
            variant={mode === EDITOR_MODES.ADD_CURVED_GROUP ? 'default' : 'outline'}
            size="sm"
            onClick={() => setMode(EDITOR_MODES.ADD_CURVED_GROUP)}
            title="곡선 행 (C)"
          >
            곡선 행
          </Button>
          <Button
            type="button"
            variant={mode === EDITOR_MODES.ADD_STAGE ? 'default' : 'outline'}
            size="sm"
            onClick={() => setMode(EDITOR_MODES.ADD_STAGE)}
            title="무대 (S)"
          >
            무대
          </Button>
          <Button
            type="button"
            variant={mode === EDITOR_MODES.ADD_ENTRANCE ? 'default' : 'outline'}
            size="sm"
            onClick={() => setMode(EDITOR_MODES.ADD_ENTRANCE)}
            title="입구 (E)"
          >
            입구
          </Button>

          <div className="ml-auto flex items-center gap-2">
            <Button 
              type="button" 
              variant="outline" 
              size="sm" 
              onClick={() => setZoom((z) => Math.max(0.5, z - 0.1))}
              title="축소 (-)"
            >
              -
            </Button>
            <span className="min-w-[60px] text-center text-sm">{Math.round(zoom * 100)}%</span>
            <Button 
              type="button" 
              variant="outline" 
              size="sm" 
              onClick={() => setZoom((z) => Math.min(2, z + 0.1))}
              title="확대 (+)"
            >
              +
            </Button>

            {selectedId && (
              <Button 
                type="button" 
                variant="destructive" 
                size="sm" 
                onClick={handleDelete}
                title="삭제 (Del)"
              >
                삭제
              </Button>
            )}
          </div>
        </div>

        {/* Canvas */}
        <div ref={containerRef} className="overflow-auto rounded-lg border bg-slate-50">
          <canvas
            ref={canvasRef}
            width={canvasSize.width}
            height={canvasSize.height}
            style={{ 
              width: canvasSize.width * zoom, 
              height: canvasSize.height * zoom,
              display: 'block',
            }}
            onMouseDown={handleCanvasMouseDown}
            onMouseMove={handleCanvasMouseMove}
            onMouseUp={handleCanvasMouseUp}
            onMouseLeave={handleCanvasMouseUp}
            className={
              mode === EDITOR_MODES.SELECT 
                ? 'cursor-move' 
                : mode === EDITOR_MODES.ADD_SEAT_GROUP || mode === EDITOR_MODES.ADD_CURVED_GROUP
                ? 'cursor-pointer'
                : 'cursor-crosshair'
            }
          />
        </div>

        {/* 정보 표시 */}
        <div className="rounded-lg border bg-white p-4">
          <div className="text-sm text-slate-600">
            총 좌석 그룹: {seatGroups.length}개 | 총 좌석 수: {calculatedSeats.length}석
            {selectedId && ` | 선택됨: ${selectedElement && 'type' in selectedElement ? selectedElement.label : '좌석 그룹'}`}
          </div>
        </div>
      </div>

      {/* 우측 속성 패널 */}
      <div className="w-80">
        <SeatPropertiesPanel
          element={selectedElement}
          sections={sections}
          onUpdate={handleUpdateElement}
          onDelete={handleDelete}
        />
      </div>
    </div>
  );
}

