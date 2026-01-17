'use client';

import { UseFormRegister, FieldErrors, FieldValues, Path } from 'react-hook-form';
import { Input } from '@/components/atoms/input';
import { FormField } from './FormField';
import classNames from 'classnames';

interface InputFieldProps<T extends FieldValues> {
  isLoading?: boolean;
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
  description?: string;
}

export function FormInputField<T extends FieldValues>({
  isLoading,
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
  description,
}: InputFieldProps<T>) {
  const error = errors?.[name as keyof typeof errors]?.message as string | undefined;
  const hasError = !!error;

  return (
    <FormField
      isLoading={isLoading}
      label={label}
      htmlFor={htmlFor}
      error={error}
      required={required}
      className={className}
      description={description}
    >
      <Input
        id={htmlFor}
        type={type}
        {...register(name as Path<T>)}
        className={classNames({ 'border-red-500': hasError, 'w-64': type === 'date' })}
        placeholder={placeholder}
        min={min}
      />
    </FormField>
  );
}
