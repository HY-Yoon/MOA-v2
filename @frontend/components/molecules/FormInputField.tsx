'use client';

import { UseFormRegister, FieldErrors, FieldValues, Path } from 'react-hook-form';
import { Input } from '@/components/atoms/input';
import { FormField } from './FormField';

interface InputFieldProps<T extends FieldValues> {
  name: string;
  label: string;
  htmlFor: string;
  register: UseFormRegister<T>;
  errors?: FieldErrors<T>;
  placeholder?: string;
  required?: boolean;
  type?: string;
  className?: string;
  min?: string;
}

export function FormInputField<T extends FieldValues>({
  name,
  label,
  htmlFor,
  register,
  errors,
  placeholder,
  required = false,
  type = 'text',
  className,
  min,
}: InputFieldProps<T>) {
  const error = errors?.[name as keyof typeof errors]?.message as string | undefined;
  const hasError = !!error;

  return (
    <FormField
      label={label}
      htmlFor={htmlFor}
      error={error}
      required={required}
      className={className}
    >
      <Input
        id={htmlFor}
        type={type}
        {...register(name as Path<T>)}
        className={hasError ? 'border-red-500' : ''}
        placeholder={placeholder}
        min={min}
      />
    </FormField>
  );
}
