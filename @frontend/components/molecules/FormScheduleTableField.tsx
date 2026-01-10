'use client';

import {
  UseFormRegister,
  FieldArrayWithId,
  FieldValues,
  ArrayPath,
  FieldErrors,
  Path,
} from 'react-hook-form';
import { Button } from '@/components/atoms/button';
import { Input } from '@/components/atoms/input';
import { SHOW_FORM_FIELDS } from '@/constants/admin/show';
import { DATE_FORMAT } from '@/constants/common/dateFormat';
import dayjs from '@/plugins/dayjs';
import classNames from 'classnames';
import { Plus } from 'lucide-react';

interface Props<T extends FieldValues> {
  fields: FieldArrayWithId<T, ArrayPath<T>, 'id'>[];
  register: UseFormRegister<T>;
  scheduleErrors?: FieldErrors<ShowUpsertType.ScheduleItem[]>;
  removeSchedule: (index: number) => void;
  addSchedule: () => void;
}

type ScheduleFieldConfig = {
  fieldName: string;
  header: string;
  inputType?: string;
  placeholder?: string;
  min?: string;
};

const SCHEDULE_FIELDS: ScheduleFieldConfig[] = [
  {
    fieldName: SHOW_FORM_FIELDS.SHOW_DATE,
    header: '공연일',
    inputType: 'date',
    min: dayjs().format(DATE_FORMAT.DATE_ONLY),
  },
  {
    fieldName: SHOW_FORM_FIELDS.SHOW_TIME,
    header: '회차',
    placeholder: '회차를 입력하세요. (ex. 1회 17:00)',
  },
  {
    fieldName: SHOW_FORM_FIELDS.TICKET_OPEN_TIME,
    header: '티켓 오픈 시간',
    inputType: 'time',
  },
];

const CELL_CLASSNAME = 'border-r px-4 py-3';

export function FormScheduleTableField<T extends FieldValues>({
  fields,
  register,
  scheduleErrors,
  removeSchedule,
  addSchedule,
}: Props<T>) {
  function renderScheduleCell(fieldConfig: ScheduleFieldConfig, index: number) {
    const fieldPath = `${SHOW_FORM_FIELDS.SCHEDULES}.${index}.${fieldConfig.fieldName}` as Path<T>;
    const error =
      scheduleErrors?.[index]?.[fieldConfig.fieldName as keyof ShowUpsertType.ScheduleItem];

    return (
      <td key={fieldConfig.fieldName} className={CELL_CLASSNAME}>
        <Input
          type={fieldConfig.inputType}
          placeholder={fieldConfig.placeholder}
          min={fieldConfig.min}
          {...register(fieldPath)}
          className={error ? 'border-red-500' : ''}
        />
        {error && <p className="mt-1 text-xs text-red-500">{error.message}</p>}
      </td>
    );
  }

  return (
    <div className="overflow-hidden rounded-lg border border-slate-200">
      <table className="w-full">
        <thead className="bg-slate-50">
          <tr>
            {SCHEDULE_FIELDS.map((field) => (
              <th
                key={field.fieldName}
                className={classNames(
                  CELL_CLASSNAME,
                  'text-center',
                  'text-sm',
                  'font-medium',
                  'text-slate-700',
                )}
              >
                {field.header}
              </th>
            ))}
            {/*삭제 버튼용 빈 헤더*/}
            <th
              className={classNames(
                'w-16',
                CELL_CLASSNAME,
                'text-center',
                'text-sm',
                'font-medium',
                'text-slate-700',
              )}
            />
          </tr>
        </thead>
        <tbody>
          {fields.map((field, index) => (
            <tr key={field.id} className="border-b last:border-b-0">
              {SCHEDULE_FIELDS.map((fieldConfig) => renderScheduleCell(fieldConfig, index))}
              <td className="px-4 py-3 text-center">
                <Button
                  type="button"
                  variant="ghost"
                  size="icon"
                  onClick={() => removeSchedule(index)}
                  disabled={fields.length === 1}
                >
                  X
                </Button>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
      <div className="border-t bg-slate-50 p-4">
        <Button type="button" variant="outline" onClick={addSchedule} className="w-full">
          <Plus className="mr-2 h-4 w-4" />
          추가
        </Button>
      </div>
    </div>
  );
}
