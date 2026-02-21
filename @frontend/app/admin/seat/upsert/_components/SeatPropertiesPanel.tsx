'use client';

import { Button } from '@/components/atoms';
import { Input } from '@/components/atoms/input';
import { Label } from '@/components/atoms/label';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/atoms/select';
import { Check, Trash2 } from 'lucide-react';
import { useCallback, useEffect, useRef, useState } from 'react';

interface Props {
  element: Seat.SeatGroup | Seat.ObjectElement | null;
  sections: Seat.Section[];
  onUpdate: (updates: Partial<Seat.SeatGroup> | Partial<Seat.ObjectElement>) => void;
  onDelete: () => void;
}

export default function SeatPropertiesPanel({ element, sections, onUpdate, onDelete }: Props) {
  // ✅ onUpdate를 ref로 안정화
  const onUpdateRef = useRef(onUpdate);
  
  useEffect(() => {
    onUpdateRef.current = onUpdate;
  }, [onUpdate]);

  // Throttle input changes
  const updateTimerRef = useRef<NodeJS.Timeout | null>(null);
  
  // 색상 편집 상태
  const [editingColor, setEditingColor] = useState(false);
  const [tempColor, setTempColor] = useState('');
  
  const handleUpdate = useCallback((updates: Partial<Seat.SeatGroup> | Partial<Seat.ObjectElement>) => {
    if (updateTimerRef.current) {
      clearTimeout(updateTimerRef.current);
    }
    
    updateTimerRef.current = setTimeout(() => {
      onUpdateRef.current(updates);
    }, 100); // 100ms debounce
  }, []); // ✅ 의존성 제거

  // Select 등 즉시 적용되어야 하는 항목용 (debounce 없음)
  const handleImmediateUpdate = useCallback((updates: Partial<Seat.SeatGroup> | Partial<Seat.ObjectElement>) => {
    onUpdateRef.current(updates);
  }, []); // ✅ 의존성 제거

  const handleStartColorEdit = () => {
    if (element && 'color' in element && element.color) {
      setTempColor(element.color);
      setEditingColor(true);
    }
  };

  const handleApplyColor = () => {
    handleImmediateUpdate({ color: tempColor });
    setEditingColor(false);
  };

  const handleCancelColorEdit = () => {
    setEditingColor(false);
    setTempColor('');
  };

  if (!element) return null;

  const isSeatGroup = 'rows' in element && 'columns' in element;
  const isObject = 'type' in element && (element.type === 'stage' || element.type === 'entrance');

  return (
    <div className="space-y-4 rounded-lg border bg-white p-4">
      <div className="flex items-center justify-between">
        <h3 className="font-semibold">{isSeatGroup ? '좌석 그룹 속성' : '사물 속성'}</h3>
        <Button type="button" size="sm" variant="ghost" onClick={onDelete}>
          <Trash2 className="h-4 w-4 text-red-500" />
        </Button>
      </div>

      {/* 좌석 그룹 속성 */}
      {isSeatGroup && (
        <div className="space-y-3">
          {/* 구역 선택 */}
          <div>
            <Label className="mb-1 text-xs">구역</Label>
            <Select
              value={element.sectionId || 'none'}
              onValueChange={(value) => handleImmediateUpdate({ sectionId: value === 'none' ? undefined : value })}
            >
              <SelectTrigger className="w-full">
                <SelectValue placeholder="구역 없음" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="none">구역 없음</SelectItem>
                {sections.map((section) => (
                  <SelectItem key={section.id} value={section.id}>
                    <div className="flex items-center gap-2">
                      <div
                        className="h-3 w-3 rounded"
                        style={{ backgroundColor: section.color }}
                      />
                      {section.name}
                      {section.price != null && ` (${section.price.toLocaleString()}원)`}
                    </div>
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>

          {/* 행 수 */}
          <div>
            <Label className="mb-1 text-xs">행 수</Label>
            <Input
              type="number"
              value={element.rows}
              onChange={(e) => handleUpdate({ rows: Math.max(1, Number(e.target.value)) })}
              className="w-full"
              min="1"
              max="50"
            />
          </div>

          {/* 열 수 */}
          <div>
            <Label className="mb-1 text-xs">열 수 (좌석 수)</Label>
            <Input
              type="number"
              value={element.columns}
              onChange={(e) => handleUpdate({ columns: Math.max(1, Number(e.target.value)) })}
              className="w-full"
              min="1"
              max="50"
            />
          </div>

          {/* 좌석 간격 */}
          <div>
            <Label className="mb-1 text-xs">좌석 간격 (px)</Label>
            <Input
              type="number"
              value={element.seatGap}
              onChange={(e) => handleUpdate({ seatGap: Number(e.target.value) })}
              className="w-full"
              min="0"
              max="50"
            />
          </div>

          {/* 행 간격 */}
          <div>
            <Label className="mb-1 text-xs">행 간격 (px)</Label>
            <Input
              type="number"
              value={element.rowGap}
              onChange={(e) => handleUpdate({ rowGap: Number(e.target.value) })}
              className="w-full"
              min="0"
              max="50"
            />
          </div>

          {/* 곡선 설정 */}
          <div>
            <Label className="mb-1 text-xs">곡선 행</Label>
            <Select
              value={element.curved ? 'true' : 'false'}
              onValueChange={(value) => handleImmediateUpdate({ curved: value === 'true' })}
            >
              <SelectTrigger className="w-full">
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="false">직선</SelectItem>
                <SelectItem value="true">곡선</SelectItem>
              </SelectContent>
            </Select>
          </div>

          {/* 곡선 정도 */}
          {element.curved && (
            <div>
              <Label className="mb-1 text-xs">곡선 정도</Label>
              <Input
                type="number"
                value={element.curveAmount || 0.2}
                onChange={(e) => handleUpdate({ curveAmount: Number(e.target.value) })}
                className="w-full"
                min="0"
                max="1"
                step="0.05"
              />
              <p className="mt-1 text-xs text-slate-500">0: 직선, 1: 완전한 곡선</p>
            </div>
          )}

          {/* 회전 */}
          <div>
            <Label className="mb-1 text-xs">회전 (도)</Label>
            <Input
              type="number"
              value={element.rotation}
              onChange={(e) => handleUpdate({ rotation: Number(e.target.value) })}
              className="w-full"
              min="0"
              max="360"
            />
          </div>

          {/* 행 레이블 형식 */}
          <div>
            <Label className="mb-1 text-xs">행 레이블 형식</Label>
            <Select
              value={element.rowLabelConfig.format}
              onValueChange={(value) =>
                handleImmediateUpdate({
                  rowLabelConfig: {
                    ...element.rowLabelConfig,
                    format: value as '1,2,3,4...' | 'A,B,C,D...',
                  },
                })
              }
            >
              <SelectTrigger className="w-full">
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="1,2,3,4...">1,2,3,4...</SelectItem>
                <SelectItem value="A,B,C,D...">A,B,C,D...</SelectItem>
              </SelectContent>
            </Select>
          </div>

          {/* 위치 */}
          <div className="grid grid-cols-2 gap-2">
            <div>
              <Label className="mb-1 text-xs">X 좌표</Label>
              <Input
                type="number"
                value={Math.round(element.origin.x)}
                onChange={(e) =>
                  handleUpdate({
                    origin: { ...element.origin, x: Number(e.target.value) },
                  })
                }
                className="w-full"
              />
            </div>
            <div>
              <Label className="mb-1 text-xs">Y 좌표</Label>
              <Input
                type="number"
                value={Math.round(element.origin.y)}
                onChange={(e) =>
                  handleUpdate({
                    origin: { ...element.origin, y: Number(e.target.value) },
                  })
                }
                className="w-full"
              />
            </div>
          </div>
        </div>
      )}

      {/* 사물 속성 */}
      {isObject && (
        <div className="space-y-3">
          {/* 레이블 */}
          <div>
            <Label className="mb-1 text-xs">이름</Label>
            <Input
              type="text"
              value={element.label}
              onChange={(e) => handleUpdate({ label: e.target.value })}
              className="w-full"
            />
          </div>

          {/* 위치 */}
          <div className="grid grid-cols-2 gap-2">
            <div>
              <Label className="mb-1 text-xs">X 좌표</Label>
              <Input
                type="number"
                value={Math.round(element.x)}
                onChange={(e) => handleUpdate({ x: Number(e.target.value) })}
                className="w-full"
              />
            </div>
            <div>
              <Label className="mb-1 text-xs">Y 좌표</Label>
              <Input
                type="number"
                value={Math.round(element.y)}
                onChange={(e) => handleUpdate({ y: Number(e.target.value) })}
                className="w-full"
              />
            </div>
          </div>

          {/* 크기 */}
          <div className="grid grid-cols-2 gap-2">
            <div>
              <Label className="mb-1 text-xs">너비</Label>
              <Input
                type="number"
                value={element.width}
                onChange={(e) => handleUpdate({ width: Number(e.target.value) })}
                className="w-full"
              />
            </div>
            <div>
              <Label className="mb-1 text-xs">높이</Label>
              <Input
                type="number"
                value={element.height}
                onChange={(e) => handleUpdate({ height: Number(e.target.value) })}
                className="w-full"
              />
            </div>
          </div>

          {/* 색상 */}
          {element.color && (
            <div>
              <Label className="mb-1 text-xs">색상</Label>
              <div className="relative">
                <div
                  className="h-10 w-full cursor-pointer rounded border"
                  style={{ backgroundColor: editingColor ? tempColor : element.color }}
                  onClick={handleStartColorEdit}
                  title="클릭하여 색상 변경"
                />
                {editingColor && (
                  <div className="absolute left-0 top-12 z-50 rounded-lg border bg-white p-2 shadow-lg">
                    <div className="flex flex-col gap-2">
                      <input
                        type="color"
                        value={tempColor}
                        onChange={(e) => setTempColor(e.target.value)}
                        className="h-20 w-full cursor-pointer"
                      />
                      <div className="flex gap-1">
                        <Button
                          type="button"
                          size="sm"
                          onClick={handleApplyColor}
                          className="flex-1"
                        >
                          <Check className="h-4 w-4" />
                        </Button>
                        <Button
                          type="button"
                          size="sm"
                          variant="outline"
                          onClick={handleCancelColorEdit}
                          className="flex-1"
                        >
                          취소
                        </Button>
                      </div>
                    </div>
                  </div>
                )}
              </div>
            </div>
          )}
        </div>
      )}
    </div>
  );
}
