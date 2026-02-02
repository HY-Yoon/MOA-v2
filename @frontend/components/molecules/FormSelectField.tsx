'use client';

import { Controller, Control, FieldErrors, FieldValues, Path } from 'react-hook-form';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/atoms/select';
import { FormField } from './FormField';

interface Option {
  label: string;
  value: string;
}

interface SelectFieldProps<T extends FieldValues> {
  isLoading?: boolean;
  name: string;
  label: string;
  htmlFor: string;
  control: Control<T>;
  errors?: FieldErrors<T>;
  options: Option[];
  placeholder: string;
  required?: boolean;
  className?: string;
  disabled?: boolean;
}

export function FormSelectField<T extends FieldValues>({
  isLoading,
  name,
  label,
  htmlFor,
  control,
  errors,
  options,
  placeholder,
  required = false,
  className,
  disabled = false,
}: SelectFieldProps<T>) {
  const error = errors?.[name as keyof typeof errors]?.message as string | undefined;

  return (
    <FormField
      isLoading={isLoading}
      label={label}
      htmlFor={htmlFor}
      error={error}
      required={required}
      className={className}
    >
      <Controller
        name={name as Path<T>}
        control={control}
        render={({ field }) => (
          <Select value={field.value || ''} onValueChange={field.onChange} disabled={disabled}>
            <SelectTrigger id={htmlFor} className="w-64">
              <SelectValue placeholder={placeholder} />
            </SelectTrigger>
            <SelectContent>
              {options.map((option) => (
                <SelectItem key={option.value} value={option.value}>
                  {option.label}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
        )}
      />
    </FormField>
  );
}
