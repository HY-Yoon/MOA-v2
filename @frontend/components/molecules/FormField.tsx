'use client';

import * as React from 'react';
import { Label } from '@/components/atoms/label';
import classNames from 'classnames';

interface FormFieldProps {
  label: string;
  htmlFor: string;
  required?: boolean;
  error?: string;
  children: React.ReactNode;
  className?: string;
}

export function FormField({
  label,
  htmlFor,
  required = false,
  error,
  children,
  className,
}: FormFieldProps) {
  return (
    <div className={classNames('grid grid-cols-[200px_1fr] items-start gap-4', className)}>
      <Label htmlFor={htmlFor} className="pt-2 text-sm font-medium">
        {label} {required && <span className="text-red-500">*</span>}
      </Label>
      <div>
        {children}
        <div className="pl-2">{error && <p className="mt-1 text-sm text-red-500">{error}</p>}</div>
      </div>
    </div>
  );
}
