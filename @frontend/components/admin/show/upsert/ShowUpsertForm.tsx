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
import { SHOW_FORM_FIELDS } from '@/constants/admin/show';
import { GENRE_OPTIONS, REGION_OPTIONS } from '@/constants/common';

interface Props {
  id?: string;
}

const requiredStringSchema = (fieldName: string, action: '입력' | '선택' = '입력') =>
  z.string().min(1, `${fieldName}을(를) ${action}하세요.`);

const dateSchema = (fieldName: string) =>
  z
    .string()
    .min(1, `${fieldName}을(를) 입력하세요.`)
    .refine((str) => !isNaN(new Date(str).getTime()), {
      message: '올바른 날짜 형식이 아닙니다.',
    });

const scheduleSchema = z.object({
  [SHOW_FORM_FIELDS.SHOW_DATE]: dateSchema('공연일'),
  [SHOW_FORM_FIELDS.SHOW_TIME]: requiredStringSchema('공연 시간'),
  [SHOW_FORM_FIELDS.TICKET_OPEN_TIME]: dateSchema('티켓 오픈 시간'),
});
const showFormSchema = z.object({
  [SHOW_FORM_FIELDS.TITLE]: requiredStringSchema('제목').max(100, '제목은 100자 이하여야 합니다'),
  [SHOW_FORM_FIELDS.GENRE]: requiredStringSchema('장르', '선택'),
  [SHOW_FORM_FIELDS.REGION]: requiredStringSchema('지역', '선택'),
  [SHOW_FORM_FIELDS.VENUE_NAME]: requiredStringSchema('장소', '선택'),
  [SHOW_FORM_FIELDS.HALL_NAME]: requiredStringSchema('공연장', '선택'),
  [SHOW_FORM_FIELDS.RUNNING_TIME]: requiredStringSchema('관람 시간').regex(
    /^\d+분$/,
    '분 단위로 입력하세요. (ex. 100분)',
  ),
  [SHOW_FORM_FIELDS.CAST]: requiredStringSchema('출연진').max(
    500,
    '출연진은 500자 이내로 입력하세요.',
  ),
  [SHOW_FORM_FIELDS.START_DATE]: dateSchema('예매 시작일'),
  // [SHOW_FORM_FIELDS.SCHEDULES]: z.array(scheduleSchema),
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

  // TODO: api 적용 예정
  const [venueOptions, setVenueOptions] = useState([
    { label: '예술의 전당', value: 'aa' },
    { label: '오페라 하우스', value: 'bb' },
  ]);
  // TODO: api 적용 예정
  const [hallOptions, setHallOptions] = useState([
    { label: '큰홀', value: 'big' },
    { label: '작은홀', value: 'small' },
  ]);
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
                placeholder="제목을 입력하세요. (100자 이내)"
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
                    <SelectTrigger id={SHOW_FORM_FIELDS.GENRE} className="w-64">
                      <SelectValue placeholder="장르를 선택하세요." />
                    </SelectTrigger>
                    <SelectContent>
                      {GENRE_OPTIONS.map((gerne) => (
                        <SelectItem key={gerne.value} value={gerne.value}>
                          {gerne.label}
                        </SelectItem>
                      ))}
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
              <Controller
                name={SHOW_FORM_FIELDS.REGION}
                control={control}
                defaultValue=""
                render={({ field }) => (
                  <Select value={field.value || ''} onValueChange={field.onChange}>
                    <SelectTrigger id={SHOW_FORM_FIELDS.REGION} className="w-64">
                      <SelectValue placeholder="지역을 선택하세요." />
                    </SelectTrigger>
                    <SelectContent>
                      {REGION_OPTIONS.map((region) => (
                        <SelectItem key={region.value} value={region.value}>
                          {region.label}
                        </SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                )}
              />
            </FormField>

            <div className="grid grid-cols-1 gap-x-6 gap-y-4 xl:grid-cols-2">
              <FormField
                label="장소"
                htmlFor={SHOW_FORM_FIELDS.VENUE_NAME}
                error={errors.venueName?.message}
                required={true}
              >
                <Controller
                  name={SHOW_FORM_FIELDS.VENUE_NAME}
                  control={control}
                  defaultValue=""
                  render={({ field }) => (
                    <Select value={field.value || ''} onValueChange={field.onChange}>
                      <SelectTrigger id={SHOW_FORM_FIELDS.VENUE_NAME} className="w-64">
                        <SelectValue placeholder="장소를 선택하세요." />
                      </SelectTrigger>
                      <SelectContent>
                        {venueOptions.map((venue) => (
                          <SelectItem key={venue.value} value={venue.value}>
                            {venue.label}
                          </SelectItem>
                        ))}
                      </SelectContent>
                    </Select>
                  )}
                />
              </FormField>

              <FormField
                label="공연장"
                htmlFor={SHOW_FORM_FIELDS.HALL_NAME}
                error={errors.hallName?.message}
                required={true}
              >
                <Controller
                  name={SHOW_FORM_FIELDS.HALL_NAME}
                  control={control}
                  defaultValue=""
                  render={({ field }) => (
                    <Select value={field.value || ''} onValueChange={field.onChange}>
                      <SelectTrigger id={SHOW_FORM_FIELDS.HALL_NAME} className="w-64">
                        <SelectValue placeholder="공연장을 선택하세요." />
                      </SelectTrigger>
                      <SelectContent>
                        {hallOptions.map((hall) => (
                          <SelectItem key={hall.value} value={hall.value}>
                            {hall.label}
                          </SelectItem>
                        ))}
                      </SelectContent>
                    </Select>
                  )}
                />
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
                placeholder="관람 시간을 입력하세요. (분 단위, ex. 100분)"
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
                placeholder="출연진을 입력하세요. (500자 이내)"
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
