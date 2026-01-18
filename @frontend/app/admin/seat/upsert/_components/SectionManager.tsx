'use client';

import { Button } from '@/components/atoms';
import { Input } from '@/components/atoms/input';
import { Label } from '@/components/atoms/label';
import { Check, Plus, Trash2 } from 'lucide-react';
import { useState } from 'react';

interface Props {
  sections: Seat.Section[];
  onSectionsChange: (sections: Seat.Section[]) => void;
}

export default function SectionManager({ sections, onSectionsChange }: Props) {
  const [newSectionName, setNewSectionName] = useState('');
  const [newSectionPrice, setNewSectionPrice] = useState('');
  const [editingColorId, setEditingColorId] = useState<string | null>(null);
  const [tempColor, setTempColor] = useState('');

  const handleAddSection = () => {
    if (!newSectionName.trim()) return;

    const newSection: Seat.Section = {
      id: `section-${Date.now()}`,
      name: newSectionName.trim(),
      color: `#${Math.floor(Math.random() * 16777215).toString(16).padStart(6, '0')}`,
      price: newSectionPrice ? Number(newSectionPrice) : undefined,
    };

    onSectionsChange([...sections, newSection]);
    setNewSectionName('');
    setNewSectionPrice('');
  };

  const handleDeleteSection = (id: string) => {
    onSectionsChange(sections.filter((s) => s.id !== id));
  };

  const handleUpdateSection = (id: string, updates: Partial<Seat.Section>) => {
    onSectionsChange(sections.map((s) => (s.id === id ? { ...s, ...updates } : s)));
  };

  const handleStartColorEdit = (id: string, currentColor: string) => {
    setEditingColorId(id);
    setTempColor(currentColor);
  };

  const handleApplyColor = (id: string) => {
    handleUpdateSection(id, { color: tempColor });
    setEditingColorId(null);
    setTempColor('');
  };

  const handleCancelColorEdit = () => {
    setEditingColorId(null);
    setTempColor('');
  };

  return (
    <div className="space-y-4 rounded-lg border bg-white p-4">
      <div className="flex items-center justify-between">
        <h3 className="font-semibold">구역 관리</h3>
      </div>

      {/* 구역 목록 */}
      <div className="space-y-2">
        {sections.map((section) => (
          <div key={section.id} className="flex items-center gap-2 rounded border p-2">
            {/* 색상 */}
            <div className="relative">
              <div
                className="h-8 w-8 cursor-pointer rounded border"
                style={{ backgroundColor: editingColorId === section.id ? tempColor : section.color }}
                onClick={() => handleStartColorEdit(section.id, section.color)}
                title="클릭하여 색상 변경"
              />
              {editingColorId === section.id && (
                <div className="absolute left-0 top-10 z-50 rounded-lg border bg-white p-2 shadow-lg">
                  <div className="flex flex-col gap-2">
                    <input
                      type="color"
                      value={tempColor}
                      onChange={(e) => setTempColor(e.target.value)}
                      className="h-20 w-20 cursor-pointer"
                    />
                    <div className="flex gap-1">
                      <Button
                        type="button"
                        size="sm"
                        onClick={() => handleApplyColor(section.id)}
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

            <Input
              value={section.name}
              onChange={(e) => handleUpdateSection(section.id, { name: e.target.value })}
              className="flex-1"
              placeholder="구역명"
            />
            <Input
              type="number"
              value={section.price || ''}
              onChange={(e) =>
                handleUpdateSection(section.id, {
                  price: e.target.value ? Number(e.target.value) : undefined,
                })
              }
              className="w-24"
              placeholder="가격"
            />
            <Button
              type="button"
              size="sm"
              variant="ghost"
              onClick={() => handleDeleteSection(section.id)}
            >
              <Trash2 className="h-4 w-4 text-red-500" />
            </Button>
          </div>
        ))}
      </div>

      {/* 새 구역 추가 */}
      <div className="space-y-2 border-t pt-4">
        <Label className="text-xs font-semibold">새 구역 추가</Label>
        <div className="flex gap-2">
          <Input
            value={newSectionName}
            onChange={(e) => setNewSectionName(e.target.value)}
            placeholder="구역명 (예: VIP석, R석)"
            className="flex-1"
            onKeyDown={(e) => {
              if (e.key === 'Enter') handleAddSection();
            }}
          />
          <Input
            type="number"
            value={newSectionPrice}
            onChange={(e) => setNewSectionPrice(e.target.value)}
            placeholder="가격"
            className="w-24"
            onKeyDown={(e) => {
              if (e.key === 'Enter') handleAddSection();
            }}
          />
          <Button type="button" size="sm" onClick={handleAddSection}>
            <Plus className="h-4 w-4" />
          </Button>
        </div>
      </div>
    </div>
  );
}
