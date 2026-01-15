'use client';

import { Button, Card, CardContent, CardHeader, CardTitle, Skeleton } from '@/components/atoms';
import {
  FormField,
  FormFileField,
  FormInputField,
  FormScheduleTableField,
  FormSelectField,
} from '@/components/molecules';
import { ERROR_MESSAGES, SHOW_FORM_FIELDS } from '@/constants/admin/show';
import { GENRE_OPTIONS, REGION_OPTIONS } from '@/constants/common';
import { DATE_FORMAT } from '@/constants/common/dateFormat';
import { ADMIN_ROUTES } from '@/constants/route/adminRoutes';
import { getFirstShowDate } from '@/lib/admin/show';
import { createShow, getShow } from '@/lib/api/admin/show';
import { stringToDate } from '@/lib/common/date';
import dayjs from '@/plugins/dayjs';
import { zodResolver } from '@hookform/resolvers/zod';
import { Genre, Region } from '@shared/enums';
import { useMutation, useQuery } from '@tanstack/react-query';
import { ArrowLeft } from 'lucide-react';
import { useRouter } from 'next/navigation';
import { useEffect, useMemo, useState } from 'react';
import { FieldErrors, useFieldArray, useForm, useWatch } from 'react-hook-form';
import { z } from 'zod';

interface Props {
  id?: string;
}
type ActionType = '입력' | '선택';

const requiredStringSchema = (field: string, action: ActionType) =>
  z.string().min(1, `${field}을(를) ${action}하세요.`);

const dateTimeSchema = (field: string) =>
  requiredStringSchema(field, '입력').refine((str) => dayjs(str).isValid(), {
    message: ERROR_MESSAGES.NOT_VALID_FORMAT,
  });

const timeSchema = (field: string) =>
  requiredStringSchema(field, '입력').regex(/^([01]\d|2[0-3]):([0-5]\d)$/, {
    message: ERROR_MESSAGES.NOT_VALID_FORMAT,
  });

const scheduleSchema = z
  .object({
    [SHOW_FORM_FIELDS.SHOW_DATE]: dateTimeSchema('공연일'),
    [SHOW_FORM_FIELDS.SHOW_TIME]: timeSchema('공연 시간'),
    [SHOW_FORM_FIELDS.TICKET_OPEN_TIME]: dateTimeSchema('티켓 오픈일'),
  })
  .refine(
    (s) => {
      // 공연일과 공연시간이 모두 입력되어야 비교 가능
      if (!s.showDate || !s.showTime || !s.ticketOpenTime) {
        return true; // 다른 필드 입력시 처리
      }

      // 공연일 + 공연시간 조합
      const showDateTime = dayjs(`${s.showDate}T${s.showTime}`);
      const ticketOpenDateTime = dayjs(s.ticketOpenTime);

      // 티켓 오픈일이 공연일+시간보다 이전이어야 함
      return ticketOpenDateTime.isBefore(showDateTime);
    },
    {
      message: '티켓 오픈일은 공연일보다 이전이어야 합니다.',
      path: [SHOW_FORM_FIELDS.TICKET_OPEN_TIME],
    },
  );

const showFormSchema = z
  .object({
    [SHOW_FORM_FIELDS.TITLE]: requiredStringSchema('제목', '입력').max(
      100,
      '제목은 100자 이내로 입력하세요.',
    ),
    [SHOW_FORM_FIELDS.GENRE]: requiredStringSchema('장르', '선택'),
    [SHOW_FORM_FIELDS.REGION]: requiredStringSchema('지역', '선택'),
    [SHOW_FORM_FIELDS.VENUE_NAME]: requiredStringSchema('장소', '선택'),
    [SHOW_FORM_FIELDS.HALL_NAME]: requiredStringSchema('공연장', '선택'),
    [SHOW_FORM_FIELDS.RUNNING_TIME]: requiredStringSchema('관람 시간', '입력').regex(
      /^\d+분$/,
      '분 단위로 입력하세요. (ex. 100분)',
    ),
    [SHOW_FORM_FIELDS.CAST]: requiredStringSchema('출연진', '입력').max(
      500,
      '출연진은 500자 이내로 입력하세요.',
    ),
    [SHOW_FORM_FIELDS.START_DATE]: dateTimeSchema('예매 시작일'),
    [SHOW_FORM_FIELDS.END_DATE]: z.string(),
    [SHOW_FORM_FIELDS.SCHEDULES]: z.array(scheduleSchema),
  })
  .refine(
    (form) => {
      // 예매 시작일과 일정이 모두 입력되어야 비교 가능
      if (!form.startDate || !form.schedules || form.schedules.length === 0) {
        return true; // 다른 필드 입력시 처리
      }

      const firstShowDate = getFirstShowDate(form.schedules);
      if (!firstShowDate) {
        return true; // 공연일이 입력되지 않았으면 우선 리턴
      }

      const startDateTime = dayjs(form.startDate);

      // 예매 시작일이 첫 번째 공연일보다 이전이어야 함
      return startDateTime.isBefore(firstShowDate);
    },
    {
      message: ERROR_MESSAGES.START_DATE_EARLY,
      path: [SHOW_FORM_FIELDS.START_DATE],
    },
  );
export type ShowFormData = z.infer<typeof showFormSchema>;

export default function ShowUpsertForm(props: Props) {
  const router = useRouter();

  const showId = Number(props.id) || -1;
  const isUpdate = !!showId && showId > 0;
  const flag = isUpdate ? '수정' : '등록';

  const { data, isLoading } = useQuery(getShow(showId));
  const createShowMutation = useMutation(createShow());

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
  const [posterFile, setPosterFile] = useState<File | null>(null);
  const [detailFiles, setDetailFiles] = useState<File[]>([]);

  function createEmptySchedule() {
    return {
      [SHOW_FORM_FIELDS.SHOW_DATE]: '',
      [SHOW_FORM_FIELDS.SHOW_TIME]: '',
      [SHOW_FORM_FIELDS.TICKET_OPEN_TIME]: '',
    };
  }

  function getDefaultValues(): ShowFormData {
    return {
      [SHOW_FORM_FIELDS.TITLE]: '',
      [SHOW_FORM_FIELDS.GENRE]: '',
      [SHOW_FORM_FIELDS.REGION]: '',
      [SHOW_FORM_FIELDS.VENUE_NAME]: '',
      [SHOW_FORM_FIELDS.HALL_NAME]: '',
      [SHOW_FORM_FIELDS.RUNNING_TIME]: '',
      [SHOW_FORM_FIELDS.CAST]: '',
      [SHOW_FORM_FIELDS.START_DATE]: '',
      [SHOW_FORM_FIELDS.END_DATE]: '',
      [SHOW_FORM_FIELDS.SCHEDULES]: [createEmptySchedule()],
    };
  }

  const {
    register,
    handleSubmit,
    control,
    setValue,
    setError,
    clearErrors,
    reset,
    formState: { errors, isSubmitting },
  } = useForm<ShowFormData>({
    resolver: zodResolver(showFormSchema),
    defaultValues: getDefaultValues(),
    mode: 'onChange', // 필드 변경 시 자동 검증
  });

  const { fields, append, remove } = useFieldArray({
    control,
    name: SHOW_FORM_FIELDS.SCHEDULES,
  });

  // 수정인 경우 데이터 로드 후 폼 초기화
  useEffect(() => {
    if (!isUpdate || !data) return;

    const formSchedules =
      data.schedules.length > 0
        ? data.schedules.map((schedule: Show.Schedules) => ({
            [SHOW_FORM_FIELDS.SHOW_DATE]: schedule.showDate,
            [SHOW_FORM_FIELDS.SHOW_TIME]: schedule.showTime,
            [SHOW_FORM_FIELDS.TICKET_OPEN_TIME]: dayjs(schedule.ticketOpenTime).format(
              DATE_FORMAT.FULL_NO_SEC,
            ),
          }))
        : [createEmptySchedule()];

    const formValues = {
      [SHOW_FORM_FIELDS.TITLE]: data.title,
      [SHOW_FORM_FIELDS.GENRE]: data.genre,
      [SHOW_FORM_FIELDS.REGION]: data.region,
      [SHOW_FORM_FIELDS.VENUE_NAME]: data.venueName,
      [SHOW_FORM_FIELDS.HALL_NAME]: data.hallName,
      [SHOW_FORM_FIELDS.RUNNING_TIME]: data.runningTime,
      [SHOW_FORM_FIELDS.CAST]: data.cast,
      [SHOW_FORM_FIELDS.START_DATE]: dayjs(data.saleStartDate).format(DATE_FORMAT.DATE_ONLY),
      [SHOW_FORM_FIELDS.SCHEDULES]: formSchedules,
    };

    // Controller가 마운트된 후 reset 실행
    requestAnimationFrame(() => {
      reset(formValues, {
        keepDefaultValues: false,
        keepDirty: false,
        keepErrors: false,
      });
    });
  }, [isUpdate, data, reset]);

  // 일정 및 예매 시작일 변경 감지
  const [schedules, startDate] = useWatch({
    control,
    name: [SHOW_FORM_FIELDS.SCHEDULES, SHOW_FORM_FIELDS.START_DATE],
  });

  // 1. 공연 일정 중복 검증
  const hasDuplicateSchedule = useMemo(() => {
    if (!schedules || schedules.length === 0) return false;

    const seen = new Set<string>();
    for (const schedule of schedules) {
      if (!schedule.showDate || !schedule.showTime) continue;
      const key = `${schedule.showDate}_${schedule.showTime}`;
      if (seen.has(key)) return true;
      seen.add(key);
    }
    return false;
  }, [schedules]);

  useEffect(() => {
    if (hasDuplicateSchedule) {
      setError(SHOW_FORM_FIELDS.SCHEDULES, {
        type: 'manual',
        message: '중복된 공연 일정이 있습니다.',
      });
    } else {
      clearErrors(SHOW_FORM_FIELDS.SCHEDULES);
    }
  }, [hasDuplicateSchedule, setError, clearErrors]);

  // 2. 예매 시작일 또는 첫 번째 공연일이 변경될 때 재검증
  const firstShowDateStr = useMemo(() => {
    if (!schedules || schedules.length === 0) return null;
    const firstShowDate = getFirstShowDate(schedules);
    return firstShowDate ? firstShowDate.format(DATE_FORMAT.DATE_ONLY) : null;
  }, [schedules]);

  useEffect(() => {
    if (!startDate || !firstShowDateStr) {
      clearErrors(SHOW_FORM_FIELDS.START_DATE);
      return;
    }

    // 예매 시작일 재검증
    const startDateTime = dayjs(startDate);
    const firstShowDateTime = dayjs(firstShowDateStr);
    const isValid = startDateTime.isBefore(firstShowDateTime);

    if (!isValid) {
      setError(SHOW_FORM_FIELDS.START_DATE, {
        type: 'manual',
        message: ERROR_MESSAGES.START_DATE_EARLY,
      });
    } else {
      clearErrors(SHOW_FORM_FIELDS.START_DATE);
    }
  }, [startDate, firstShowDateStr, setError, clearErrors]);

  // 3. 일정 공연일이 변경될 때 마지막 공연일을 endDate 설정
  useEffect(() => {
    // 유효한 공연일 체크
    const validDates =
      schedules
        ?.map((schedule) => schedule.showDate)
        .filter((date) => date && dayjs(date).isValid())
        .map((date) => dayjs(date)) ?? [];

    if (validDates.length === 0) {
      setValue(SHOW_FORM_FIELDS.END_DATE, '');
      return;
    }

    // 마지막 날짜 찾기
    const lastDate = validDates.reduce((latest, current) => {
      return current.isAfter(latest) ? current : latest;
    }, validDates[0]);

    setValue(SHOW_FORM_FIELDS.END_DATE, lastDate.format(DATE_FORMAT.DATE_ONLY));
  }, [schedules, setValue]);

  // 메인 포스터 파일 변경 핸들러
  const handlePosterChange = (files: File[]) => {
    setPosterFile(files.length > 0 ? files[0] : null);

    // 파일이 첨부되면 에러 메시지 제거
    if (files.length > 0) {
      clearErrors('root.poster');
    }
  };

  // 상세 이미지 파일 변경 핸들러
  const handleDetailImagesChange = (files: File[]) => {
    setDetailFiles(files);

    // 파일이 첨부되면 에러 메시지 제거
    if (files.length > 0) {
      clearErrors('root.details');
    }
  };

  // 이미지 파일 유효성 검사
  function validateImageFiles(): boolean {
    const errors: Array<{ field: string; message: string }> = [];

    // 메인 포스터
    if (!posterFile) {
      errors.push({ field: 'poster', message: '메인 포스터를 첨부하세요.' });
    }
    // 상세 이미지
    if (detailFiles.length === 0) {
      errors.push({ field: 'details', message: '상세 이미지를 하나 이상 첨부하세요.' });
    }

    // 에러가 있으면 모두 설정하고 false 반환
    if (errors.length > 0) {
      errors.forEach(({ field, message }) => {
        setError(`root.${field}`, { type: 'manual', message });
      });
      return false;
    }
    return true;
  }

  // formData 생성
  function createFormData(request: ShowUpsertType.ShowForm): FormData {
    const formData = new FormData();

    // data 필드를 JSON 문자열로 추가
    formData.append('data', JSON.stringify(request));

    // 포스터 파일 추가
    if (posterFile) {
      formData.append('poster', posterFile);
    }

    // 상세 이미지 파일들 추가
    detailFiles.forEach((file) => {
      formData.append('detailImages', file);
    });

    return formData;
  }

  // api request 설정
  async function onSubmit(formData: ShowFormData) {
    // 이미지 파일 유효성 검사
    if (!validateImageFiles()) return;

    const {
      title,
      genre,
      region,
      venueName,
      hallName,
      runningTime,
      cast,
      startDate,
      endDate,
      schedules,
    } = formData;

    const request: ShowUpsertType.ShowForm = {
      title,
      genre: genre as Genre,
      location: {
        region: region as Region,
        venueName,
        hallName,
      },
      runningTime,
      cast,
      salePeriod: {
        startDate: stringToDate(startDate),
        endDate: stringToDate(endDate),
      },
      schedules: schedules.map((schedule) => ({
        showDate: stringToDate(schedule.showDate), // YYYY-MM-DD -> Date
        showTime: schedule.showTime, // HH:mm -> string
        ticketOpenTime: stringToDate(schedule.ticketOpenTime), // YYYY-MM-DDTHH:mm -> Date
      })),
    };

    // TODO: 수정인 경우
    // if (isUpdate) {
    //   await updateShow(props.id, formDataToSend);
    // } else {
    //   await createShow(formDataToSend);
    // }
    const requestFormData = createFormData(request);
    const response = await createShowMutation.mutateAsync(requestFormData);

    // 등록 성공시 목록 화면으로 이동
    if (response.success && response.data.showId > 0) {
      router.push(ADMIN_ROUTES.SHOW);
    }
  }

  return (
    <Card className="shadow-lg">
      {/*header*/}
      <CardHeader>
        <div className="flex items-center">
          <Button type="button" variant="ghost" onClick={router.back}>
            <ArrowLeft className="!size-6" />
          </Button>
          <CardTitle className="text-2xl font-bold">공연 {flag}</CardTitle>
        </div>
      </CardHeader>

      {/*content*/}
      <CardContent>
        <form onSubmit={handleSubmit(onSubmit, (errors) => console.log('통과안됨', errors))}>
          <div className="space-y-6">
            {/* 1. 제목 */}
            <FormInputField
              isLoading={isLoading}
              name={SHOW_FORM_FIELDS.TITLE}
              label="제목"
              htmlFor={SHOW_FORM_FIELDS.TITLE}
              register={register}
              errors={errors}
              placeholder="제목을 입력하세요. (100자 이내)"
              required={true}
            />

            {/* 2. 장르 */}
            <FormSelectField
              isLoading={isLoading}
              name={SHOW_FORM_FIELDS.GENRE}
              label="장르"
              htmlFor={SHOW_FORM_FIELDS.GENRE}
              control={control}
              errors={errors}
              options={GENRE_OPTIONS}
              placeholder="장르를 선택하세요."
              required={true}
            />

            {/* 3. 지역 */}
            <FormSelectField
              isLoading={isLoading}
              name={SHOW_FORM_FIELDS.REGION}
              label="지역"
              htmlFor={SHOW_FORM_FIELDS.REGION}
              control={control}
              errors={errors}
              options={REGION_OPTIONS}
              placeholder="지역을 선택하세요."
              required={true}
            />

            {/* 4. 장소 */}
            <FormSelectField
              isLoading={isLoading}
              name={SHOW_FORM_FIELDS.VENUE_NAME}
              label="장소"
              htmlFor={SHOW_FORM_FIELDS.VENUE_NAME}
              control={control}
              errors={errors}
              options={venueOptions}
              placeholder="장소를 선택하세요."
              required={true}
              className="xl:col-span-1"
            />

            {/* 5. 공연장 */}
            <FormSelectField
              isLoading={isLoading}
              name={SHOW_FORM_FIELDS.HALL_NAME}
              label="공연장"
              htmlFor={SHOW_FORM_FIELDS.HALL_NAME}
              control={control}
              errors={errors}
              options={hallOptions}
              placeholder="공연장을 선택하세요."
              required={true}
              className="xl:col-span-1"
            />

            {/* 6. 관람시간 */}
            <FormInputField
              isLoading={isLoading}
              name={SHOW_FORM_FIELDS.RUNNING_TIME}
              label="관람시간"
              htmlFor={SHOW_FORM_FIELDS.RUNNING_TIME}
              register={register}
              errors={errors}
              placeholder="관람시간을 입력하세요. (분 단위, ex. 100분)"
              required={true}
            />

            {/* 7. 출연진 */}
            <FormInputField
              isLoading={isLoading}
              name={SHOW_FORM_FIELDS.CAST}
              label="출연진"
              htmlFor={SHOW_FORM_FIELDS.CAST}
              register={register}
              errors={errors}
              placeholder="출연진을 입력하세요. (500자 이내)"
              required={true}
            />

            {/* 8. 일정(테이블) */}
            <FormField
              isLoading={isLoading}
              label="일정"
              htmlFor={SHOW_FORM_FIELDS.SCHEDULES}
              required={true}
              error={errors?.schedules?.message}
            >
              <FormScheduleTableField<ShowFormData>
                fields={fields}
                register={register}
                scheduleErrors={errors?.schedules as FieldErrors<ShowUpsertType.Schedule[]>}
                removeSchedule={remove}
                addSchedule={() => append(createEmptySchedule())}
              />
            </FormField>

            {/* 9. 예매 시작일 */}
            <FormInputField
              isLoading={isLoading}
              name={SHOW_FORM_FIELDS.START_DATE}
              label="예매 시작일"
              htmlFor={SHOW_FORM_FIELDS.START_DATE}
              type="date"
              min={dayjs().format(DATE_FORMAT.DATE_ONLY)}
              register={register}
              errors={errors}
              placeholder="예매 시작일을 입력하세요."
              required={true}
              description="예매 종료일은 마지막 공연일로 자동 설정됩니다."
            />

            {/* 10. 메인 포스터 */}
            <FormFileField
              isLoading={isLoading}
              label="메인 포스터"
              htmlFor="poster"
              required={true}
              accept="image/*"
              maxSize={10}
              onFileChange={handlePosterChange}
              error={errors.root?.poster?.message}
              previewImages={data?.posterUrl ? [data.posterUrl] : []}
            />

            {/* 11. 상세 이미지 */}
            <FormFileField
              isLoading={isLoading}
              label="상세 이미지"
              htmlFor="detail-images"
              accept="image/*"
              required={true}
              multiple={true}
              maxSize={10}
              onFileChange={handleDetailImagesChange}
              error={errors.root?.details?.message}
              previewImages={data?.detailImageUrls}
            />

            {/*footer*/}
            <div className="flex gap-4 border-t pt-6">
              {isLoading ? (
                <>
                  <Skeleton className="h-12 flex-1" />
                  <Skeleton className="h-12 flex-1" />
                </>
              ) : (
                <>
                  <Button
                    type="button"
                    variant="outline"
                    size="lg"
                    className="h-12 flex-1 text-base"
                    onClick={router.back}
                  >
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
                </>
              )}
            </div>
          </div>
        </form>
      </CardContent>
    </Card>
  );
}
