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
import { ArrowLeft, Upload } from 'lucide-react';
import { useRouter } from 'next/navigation';
import { useState } from 'react';
import { z } from 'zod';
import { Controller, useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { FormField } from '@/components/admin/show/upsert/FormField';

interface Props {
  id?: string;
}

const SHOW_FORM_FIELDS = {
  TITLE: 'title', // 제목
  GENRE: 'genre', // 장르
  REGION: 'region', // 지역
  VENUE_NAME: 'venueName', // 장소
  HALL_NAME: 'hallName', // 공연장
  RUNNING_TIME: 'runningTime', // 관람시간
  CAST: 'cast', // 출연진
  BOOKING_PERIOD: 'bookingPeriod', // 예매 일정
  START_DATE: 'startDate', // 예매 시작일
  END_DATE: 'endDate', // 예매 종료일
  SCHEDULES: 'schedules', // 공연 일정
  SHOW_DATE: 'showDate', // 공연 일자
  SHOW_TIME: 'showTime', // 공연 시간
  TICKET_OPEN_TIME: 'ticketOpenTime', // 티켓 오픈 시간
} as const;

const scheduleSchema = z.object({
  [SHOW_FORM_FIELDS.SHOW_DATE]: z
    .string()
    .min(1, '공연일을 입력하세요.')
    .refine(
      (str) => {
        if (!str) return false;
        const date = new Date(str);
        return !isNaN(date.getTime());
      },
      { message: '올바른 날짜 형식이 아닙니다.' },
    ),
  [SHOW_FORM_FIELDS.SHOW_TIME]: z.string().min(1, '공연 시간을 입력하세요.'),
  [SHOW_FORM_FIELDS.TICKET_OPEN_TIME]: z
    .string()
    .min(1, '티켓 오픈 시간을 입력하세요.')
    .refine(
      (str) => {
        if (!str) return false;
        const date = new Date(str);
        return !isNaN(date.getTime());
      },
      { message: '올바른 날짜 형식이 아닙니다.' },
    ),
});
const showFormSchema = z.object({
  [SHOW_FORM_FIELDS.TITLE]: z
    .string()
    .min(1, '제목을 입력하세요.')
    .max(100, '제목은 100자 이하여야 합니다'),
  [SHOW_FORM_FIELDS.GENRE]: z.string().min(1, '장르를 선택하세요.'),
  [SHOW_FORM_FIELDS.REGION]: z.string().min(1, '지역을 선택하세요.'),
  [SHOW_FORM_FIELDS.VENUE_NAME]: z.string().min(1, '장소를 선택하세요.'),
  [SHOW_FORM_FIELDS.HALL_NAME]: z.string().min(1, '공연장을 선택하세요.'),
  [SHOW_FORM_FIELDS.RUNNING_TIME]: z.number().min(1, '관람 시간은 1분 이상이어야 합니다.'),
  [SHOW_FORM_FIELDS.CAST]: z
    .string()
    .min(1, '출연진을 입력하세요.')
    .max(500, '출연진은 500자 이내로 입력하세요.'),
  [SHOW_FORM_FIELDS.START_DATE]: z
    .string()
    .min(1, '예매 시작일을 입력하세요.')
    .refine(
      (str) => {
        if (!str) return false;
        const date = new Date(str);
        return !isNaN(date.getTime());
      },
      { message: '올바른 날짜 형식이 아닙니다.' },
    ),
  [SHOW_FORM_FIELDS.SCHEDULES]: z.array(scheduleSchema),
  // poster: z
  //   .instanceof(FileList)
  //   .refine((files) => files.length > 0, '포스터 이미지는 필수입니다')
  //   .refine((files) => files[0]?.size <= 10 * 1024 * 1024, '파일 크기는 10MB 이하여야 합니다'),
});
type ShowFormData = z.infer<typeof showFormSchema>;

export default function PerformanceRegistrationForm(props: Props) {
  const router = useRouter();

  const isUpdate = !!props.id;
  const flag = isUpdate ? '수정' : '등록';

  const [imagePreview, setImagePreview] = useState(null);

  const {
    register,
    handleSubmit,
    control,
    formState: { errors, isSubmitting },
  } = useForm<ShowFormData>({
    resolver: zodResolver(showFormSchema),
  });

  async function onSubmit(data: ShowFormData) {
    alert('새로운 공연이 등록되었습니다.');
  }

  return (
    <Card className="shadow-lg">
      <CardHeader>
        <div className="flex items-center">
          <Button type="button" variant="ghost" onClick={() => router.back()}>
            <ArrowLeft className="!size-6" />
          </Button>
          <CardTitle className="text-2xl font-bold">공연 {flag}</CardTitle>
        </div>
      </CardHeader>

      <CardContent>
        <form onSubmit={handleSubmit(onSubmit)}>
          <div className="space-y-6">
            <FormField
              label="제목"
              htmlFor={SHOW_FORM_FIELDS.TITLE}
              error={errors.title?.message}
              required={true}
            >
              <Input
                id={SHOW_FORM_FIELDS.TITLE}
                {...register(SHOW_FORM_FIELDS.TITLE)}
                className={errors.title ? 'border-red-500' : ''}
                placeholder="제목을 입력하세요."
              />
            </FormField>

            <FormField
              label="장르"
              htmlFor={SHOW_FORM_FIELDS.GENRE}
              error={errors.genre?.message}
              required={true}
            >
              <Controller
                name={SHOW_FORM_FIELDS.GENRE}
                control={control}
                defaultValue=""
                render={({ field }) => (
                  <Select value={field.value || ''} onValueChange={field.onChange}>
                    <SelectTrigger id={SHOW_FORM_FIELDS.GENRE} className="h-11">
                      <SelectValue placeholder="장르를 선택하세요." />
                    </SelectTrigger>
                    <SelectContent>
                      <SelectItem value="musical">뮤지컬</SelectItem>
                      <SelectItem value="concert">콘서트</SelectItem>
                    </SelectContent>
                  </Select>
                )}
              />
            </FormField>

            <FormField
              label="지역"
              htmlFor={SHOW_FORM_FIELDS.REGION}
              error={errors.region?.message}
              required={true}
            >
              <Select>
                <SelectTrigger id={SHOW_FORM_FIELDS.REGION} className="h-11">
                  <SelectValue placeholder="지역을 선택하세요." />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="seoul">서울</SelectItem>
                  <SelectItem value="busan">부산</SelectItem>
                </SelectContent>
              </Select>
            </FormField>

            <div className="grid grid-cols-2 gap-4">
              <FormField
                label="장소"
                htmlFor={SHOW_FORM_FIELDS.VENUE_NAME}
                error={errors.venueName?.message}
                required={true}
              >
                <Select>
                  <SelectTrigger id={SHOW_FORM_FIELDS.VENUE_NAME} className="h-11">
                    <SelectValue placeholder="장소를 선택하세요." />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value="a">예술의 전당</SelectItem>
                    <SelectItem value="b">오페라하우스</SelectItem>
                  </SelectContent>
                </Select>
              </FormField>

              <FormField
                label="공연장"
                htmlFor={SHOW_FORM_FIELDS.HALL_NAME}
                error={errors.hallName?.message}
                required={true}
              >
                <Select>
                  <SelectTrigger id={SHOW_FORM_FIELDS.HALL_NAME} className="h-11">
                    <SelectValue placeholder="공연장을 선택하세요." />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value="a">A홀</SelectItem>
                    <SelectItem value="b">B홀</SelectItem>
                  </SelectContent>
                </Select>
              </FormField>
            </div>

            <FormField
              label="관람 시간"
              htmlFor={SHOW_FORM_FIELDS.RUNNING_TIME}
              error={errors.runningTime?.message}
              required={true}
            >
              <Input
                id={SHOW_FORM_FIELDS.RUNNING_TIME}
                {...register(SHOW_FORM_FIELDS.RUNNING_TIME)}
                className={errors.runningTime ? 'border-red-500' : ''}
                placeholder="관람 시간을 입력하세요."
              />
            </FormField>

            <FormField
              label="출연진"
              htmlFor={SHOW_FORM_FIELDS.CAST}
              error={errors.cast?.message}
              required={true}
            >
              <Input
                id={SHOW_FORM_FIELDS.CAST}
                {...register(SHOW_FORM_FIELDS.CAST)}
                className={errors.cast ? 'border-red-500' : ''}
                placeholder="출연진을 입력하세요."
              />
            </FormField>

            <div className="grid grid-cols-[200px_1fr] items-center gap-4">
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
              <Button type="button" variant="outline" size="lg" className="h-12 flex-1 text-base">
                이전
              </Button>
              <Button
                type="submit"
                size="lg"
                className="h-12 flex-1 text-base"
                disabled={isSubmitting}
              >
                등록
              </Button>
            </div>
          </div>
        </form>
      </CardContent>
    </Card>
  );
}
