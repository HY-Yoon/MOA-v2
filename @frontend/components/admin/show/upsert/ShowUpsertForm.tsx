'use client';

import { Button } from '@/components/ui/button';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select';
import { ArrowLeft, Calendar, Upload, X } from 'lucide-react';
import { useRouter } from 'next/navigation';
import { useState } from 'react';

interface Props {
  id?: string;
}

export default function PerformanceRegistrationForm(props: Props) {
  const router = useRouter();

  const isUpdate = !!props.id;
  const flag = isUpdate ? '수정' : '등록';

  const [performers, setPerformers] = useState(['']);
  const [imagePreview, setImagePreview] = useState(null);

  const handleSubmit = () => {
    alert('공연 정보가 등록되었습니다!');
  };

  return (
    <Card className="shadow-lg">
      <CardHeader className="border-b !pb-4">
        <div className="flex items-center">
          <Button type="button" variant="ghost" onClick={() => router.back()}>
            <ArrowLeft className="!size-6" />
          </Button>
          <CardTitle className="text-2xl font-bold">공연 {flag}</CardTitle>
        </div>
      </CardHeader>

      <CardContent>
        <div className="space-y-8">
          {/* 기본 정보 섹션 */}
          <div className="space-y-6">
            <div className="space-y-2">
              <Label htmlFor="title" className="text-sm font-medium">
                공연 제목 <span className="text-red-500">*</span>
              </Label>
              <Input id="title" placeholder="공연 제목을 입력하세요" className="h-11" />
            </div>

            <div className="grid grid-cols-2 gap-6">
              <div className="space-y-2">
                <Label htmlFor="category" className="text-sm font-medium">
                  공연 카테고리 <span className="text-red-500">*</span>
                </Label>
                <Select>
                  <SelectTrigger id="category" className="h-11">
                    <SelectValue placeholder="카테고리 선택" />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value="musical">뮤지컬</SelectItem>
                    <SelectItem value="play">연극</SelectItem>
                    <SelectItem value="concert">콘서트</SelectItem>
                    <SelectItem value="opera">오페라</SelectItem>
                    <SelectItem value="dance">무용</SelectItem>
                    <SelectItem value="exhibition">전시</SelectItem>
                  </SelectContent>
                </Select>
              </div>

              <div className="space-y-2">
                <Label htmlFor="venue" className="text-sm font-medium">
                  공연 장소 <span className="text-red-500">*</span>
                </Label>
                <Input id="venue" placeholder="예: 세종문화회관 대극장" className="h-11" />
              </div>
            </div>

            <div className="grid grid-cols-2 gap-6">
              <div className="space-y-2">
                <Label htmlFor="startDate" className="text-sm font-medium">
                  시작일 <span className="text-red-500">*</span>
                </Label>
                <div className="relative">
                  <Input id="startDate" type="date" className="h-11" />
                  <Calendar className="pointer-events-none absolute top-3 right-3 h-5 w-5 text-slate-400" />
                </div>
              </div>

              <div className="space-y-2">
                <Label htmlFor="endDate" className="text-sm font-medium">
                  종료일 <span className="text-red-500">*</span>
                </Label>
                <div className="relative">
                  <Input id="endDate" type="date" className="h-11" />
                  <Calendar className="pointer-events-none absolute top-3 right-3 h-5 w-5 text-slate-400" />
                </div>
              </div>
            </div>

            <div className="space-y-2">
              <Label htmlFor="runtime" className="text-sm font-medium">
                공연 시간
              </Label>
              <Input
                id="runtime"
                placeholder="예: 2시간 30분 (인터미션 15분 포함)"
                className="h-11"
              />
            </div>

            {performers.map((performer, index) => (
              <div key={index} className="flex gap-3">
                <Input
                  value={performer}
                  placeholder={`출연진 ${index + 1} (예: 홍길동 - 주인공 역)`}
                  className="h-11"
                />
                {performers.length > 1 && (
                  <Button
                    type="button"
                    variant="ghost"
                    size="icon"
                    className="h-11 w-11 flex-shrink-0"
                  >
                    <X className="h-4 w-4" />
                  </Button>
                )}
              </div>
            ))}
          </div>

          {/* 포스터 이미지 */}
          <div className="space-y-4">
            <Label htmlFor="poster" className="text-sm font-medium">
              메인 포스터 <span className="text-red-500">*</span>
            </Label>

            <div className="rounded-lg border-2 border-dashed border-slate-300 p-8 text-center transition-colors hover:border-slate-400">
              {imagePreview ? (
                <div className="space-y-4">
                  <img
                    src={imagePreview}
                    alt="포스터 미리보기"
                    className="mx-auto max-h-96 rounded-lg shadow-md"
                  />
                  <Button type="button" variant="outline" onClick={() => setImagePreview(null)}>
                    이미지 변경
                  </Button>
                </div>
              ) : (
                <label htmlFor="poster" className="block cursor-pointer">
                  <Upload className="mx-auto mb-4 h-12 w-12 text-slate-400" />
                  <p className="mb-2 text-sm text-slate-600">클릭하여 이미지를 업로드하세요</p>
                  <p className="text-xs text-slate-400">JPG, PNG 파일 (최대 10MB)</p>
                  <Input id="poster" type="file" accept="image/*" className="hidden" />
                </label>
              )}
            </div>
          </div>

          {/* 제출 버튼 */}
          <div className="flex gap-4 border-t pt-6">
            <Button onClick={handleSubmit} size="lg" className="h-12 flex-1 text-base">
              공연 등록하기
            </Button>
            <Button type="button" variant="outline" size="lg" className="h-12 flex-1 text-base">
              취소
            </Button>
          </div>
        </div>
      </CardContent>
    </Card>
  );
}
