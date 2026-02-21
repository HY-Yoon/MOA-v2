'use client';

import { Button } from '@/components/atoms';
import { FormInputField, FormSelectField } from '@/components/molecules';
import { useAlert } from '@/components/molecules/AlertContext';
import { SEAT_ERROR_MESSAGES, SEAT_FORM_FIELDS } from '@/constants/admin/seat';
import { REGION_OPTIONS } from '@/constants/common';
import { ADMIN_ROUTES } from '@/constants/route/adminRoutes';
import { flattenSeatGroupsToSeats } from '@/lib/admin/seat-calculator';
import { checkDuplicateSeat, createSeat } from '@/lib/api/admin/seat';
import { zodResolver } from '@hookform/resolvers/zod';
import { useMutation } from '@tanstack/react-query';
import { useRouter } from 'next/navigation';
import { useCallback, useState } from 'react';
import { useForm } from 'react-hook-form';
import { z } from 'zod';
import SeatLayoutEditor from './SeatLayoutEditor';

const requiredStringSchema = (field: string) =>
  z.string().min(1, `${field}을(를) 입력하세요.`);

const seatFormSchema = z.object({
  [SEAT_FORM_FIELDS.REGION]: requiredStringSchema('지역'),
  [SEAT_FORM_FIELDS.VENUE_NAME]: requiredStringSchema('장소'),
  [SEAT_FORM_FIELDS.HALL_NAME]: requiredStringSchema('홀'),
});

export type SeatFormData = z.infer<typeof seatFormSchema>;

export default function SeatUpsertForm() {
  const router = useRouter();
  const { confirm, alert } = useAlert();
  const [isValidated, setIsValidated] = useState(false);
  const [editorData, setEditorData] = useState<Seat.EditorLayoutData | null>(null);

  // ✅ 안정적인 콜백 생성
  const handleLayoutChange = useCallback((data: Seat.EditorLayoutData) => {
    setEditorData(data);
  }, []);

  const {
    register,
    handleSubmit,
    control,
    getValues,
    formState: { errors },
  } = useForm<SeatFormData>({
    resolver: zodResolver(seatFormSchema),
    defaultValues: {
      [SEAT_FORM_FIELDS.REGION]: '',
      [SEAT_FORM_FIELDS.VENUE_NAME]: '',
      [SEAT_FORM_FIELDS.HALL_NAME]: '',
    },
  });

  const createSeatMutation = useMutation({
    mutationFn: createSeat,
  });
  const checkDuplicateMutation = useMutation({
    mutationFn: checkDuplicateSeat,
  });

  // 중복 체크
  const handleCheckDuplicate = async () => {
    const values = getValues();

    if (!values.region || !values.venueName || !values.hallName) {
      await alert({
        title: '입력 오류',
        description: '지역, 장소, 홀을 모두 입력해주세요.',
      });
      return;
    }

    try {
      const response = await checkDuplicateMutation.mutateAsync({
        region: values.region,
        venueName: values.venueName,
        hallName: values.hallName,
      });

      if (!response.success || !response.data) {
        await alert({
          title: '중복 확인 실패',
          description: response.message || '중복 확인 중 오류가 발생했습니다.',
        });
        return;
      }

      if (response.data.isDuplicate) {
        await alert({
          title: '중복 확인',
          description: SEAT_ERROR_MESSAGES.DUPLICATE_HALL,
        });
        return;
      }

      setIsValidated(true);
      await alert({
        title: '확인 완료',
        description: '사용 가능한 홀입니다. 좌석 배치도를 작성해주세요.',
      });
    } catch (error) {
      console.error('중복 체크 에러:', error);
      await alert({
        title: '오류',
        description: '중복 확인 중 오류가 발생했습니다.',
      });
    }
  };

  // 제출
  const onSubmit = async (formData: SeatFormData) => {
    if (!isValidated) {
      await alert({
        title: '검증 필요',
        description: SEAT_ERROR_MESSAGES.NOT_VALIDATED,
      });
      return;
    }

    if (!editorData || editorData.seatGroups.length === 0) {
      await alert({
        title: '좌석 배치 필요',
        description: SEAT_ERROR_MESSAGES.NO_SEATS,
      });
      return;
    }

    if (editorData.sections.length === 0) {
      await alert({
        title: '구역 설정 필요',
        description: '최소 1개 이상의 구역을 생성해주세요.',
      });
      return;
    }

    const hasInvalidSection = editorData.sections.some(
      (section) => !section.id?.trim() || !section.name?.trim() || !section.color?.trim(),
    );
    if (hasInvalidSection) {
      await alert({
        title: '구역 정보 오류',
        description: '구역 ID, 이름, 색상을 모두 입력해주세요.',
      });
      return;
    }

    // EditorLayoutData → SeatRequest 변환
    const flattened = flattenSeatGroupsToSeats(editorData.seatGroups, editorData.canvas.seatRadius);
    const totalSeats = flattened.length;
    const hasUnassignedSeat = flattened.some((seat) => !seat.sectionId?.trim());
    const validSectionIds = new Set(editorData.sections.map((section) => section.id.trim()));
    const hasUnknownSectionSeat = flattened.some(
      (seat) => !seat.sectionId || !validSectionIds.has(seat.sectionId.trim()),
    );

    if (hasUnassignedSeat) {
      await alert({
        title: '구역 지정 필요',
        description: '모든 좌석 그룹에 구역을 지정해주세요.',
      });
      return;
    }
    if (hasUnknownSectionSeat) {
      await alert({
        title: '구역 매핑 오류',
        description: '존재하지 않는 구역이 좌석에 지정되어 있습니다. 구역을 다시 지정해주세요.',
      });
      return;
    }

    const confirmed = await confirm({
      title: '좌석 등록',
      description: `${formData.venueName} - ${formData.hallName}의 좌석을 등록하시겠습니까?\n(총 ${totalSeats}석)`,
      confirmText: '등록',
      cancelText: '취소',
    });

    if (!confirmed) return;

    try {
      const request: Seat.CreateSeatRequest = {
        region: formData.region,
        venueName: formData.venueName,
        hallName: formData.hallName,
        layoutData: {
          canvas: editorData.canvas,
          sections: editorData.sections.map((section) => ({
            sectionId: section.id,
            name: section.name,
            color: section.color,
            price: section.price ?? 0,
          })),
          seats: flattened.map((seat) => ({
            ...seat,
            sectionId: seat.sectionId!,
          })),
          totalSeats,
        },
      };

      const response = await createSeatMutation.mutateAsync(request);

      if (response.success) {
        await alert({
          title: '등록 완료',
          description: '좌석이 성공적으로 등록되었습니다.',
        });
        router.push(ADMIN_ROUTES.SEAT);
      }
    } catch (error) {
      console.error('좌석 등록 에러:', error);
      const timeoutMessage =
        (error as { code?: string; message?: string })?.code === 'ECONNABORTED' ||
        (error as { message?: string })?.message?.includes('timeout')
          ? '요청 시간이 초과되었습니다. 잠시 후 다시 시도해주세요.'
          : null;
      const errorMessage =
        timeoutMessage ||
        (error as { response?: { data?: { message?: string } } })?.response?.data?.message ||
        '좌석 등록 중 오류가 발생했습니다.';
      await alert({
        title: '오류',
        description: errorMessage,
      });
    }
  };

  // 총 좌석 수 계산
  const totalSeats = editorData
    ? flattenSeatGroupsToSeats(editorData.seatGroups, editorData.canvas.seatRadius).length
    : 0;

  return (
    <div className="space-y-8">
      {/* Step 1: 기본 정보 */}
      <div className="rounded-lg border bg-white p-6 shadow-sm">
        <h3 className="mb-4 text-lg font-semibold">1. 기본 정보</h3>
        <div className="space-y-6">
          <FormSelectField
            name={SEAT_FORM_FIELDS.REGION}
            label="지역"
            htmlFor={SEAT_FORM_FIELDS.REGION}
            control={control}
            errors={errors}
            options={REGION_OPTIONS}
            placeholder="지역을 선택하세요."
            required={true}
            disabled={isValidated}
          />

          <FormInputField
            name={SEAT_FORM_FIELDS.VENUE_NAME}
            label="장소"
            htmlFor={SEAT_FORM_FIELDS.VENUE_NAME}
            register={register}
            errors={errors}
            placeholder="예: 예술의전당, 세종문화회관"
            required={true}
            disabled={isValidated}
          />

          <FormInputField
            name={SEAT_FORM_FIELDS.HALL_NAME}
            label="홀"
            htmlFor={SEAT_FORM_FIELDS.HALL_NAME}
            register={register}
            errors={errors}
            placeholder="예: 콘서트홀, 대극장"
            required={true}
            disabled={isValidated}
          />

          <div className="flex justify-end">
            <Button
              type="button"
              onClick={handleCheckDuplicate}
              variant={isValidated ? 'outline' : 'default'}
              disabled={isValidated}
            >
              {isValidated ? '확인 완료' : '중복 확인'}
            </Button>
          </div>
        </div>
      </div>

      {/* Step 2: 좌석 배치도 */}
      {isValidated && (
        <div className="rounded-lg border bg-white p-6 shadow-sm">
          <h3 className="mb-4 text-lg font-semibold">
            2. 좌석 배치도{' '}
            <span className="text-sm font-normal text-slate-500">(총 {totalSeats}석)</span>
          </h3>
          <SeatLayoutEditor onLayoutChange={handleLayoutChange} initialData={editorData} />
        </div>
      )}

      {/* 제출 버튼 */}
      {isValidated && (
        <div className="flex justify-end gap-4">
          <Button type="button" variant="outline" onClick={() => window.history.back()}>
            취소
          </Button>
          <Button type="button" onClick={handleSubmit(onSubmit)}>
            등록
          </Button>
        </div>
      )}
    </div>
  );
}
