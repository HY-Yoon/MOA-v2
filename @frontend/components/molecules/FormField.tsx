'use client';

import * as React from 'react';
import { Label } from '@/components/atoms/label';
import classNames from 'classnames';
import { Skeleton } from '@/components/atoms';

interface FormFieldProps {
  isLoading?: boolean;
  label: string;
  htmlFor: string;
  required?: boolean;
  error?: string;
  children: React.ReactNode;
  className?: string;
  description?: string;
}

export function FormField({
  isLoading,
  label,
  htmlFor,
  required = false,
  error,
  children,
  className,
  description,
}: FormFieldProps) {
  return isLoading ? (
    <div className="space-y-2">
      <Skeleton className="h-4 w-16" />
      <Skeleton className="h-10 w-full" />
    </div>
  ) : (
    <div className={classNames('grid grid-cols-[200px_1fr] items-start gap-4', className)}>
      <Label htmlFor={htmlFor} className="pt-2 text-sm font-medium">
        {label} {required && <span className="text-red-500">*</span>}
      </Label>
      <div>
        {children}
        {description && <p className="mt-1.5 text-xs text-slate-500">{description}</p>}
        {error && <p className="mt-1 text-sm text-red-500">{error}</p>}
      </div>
    </div>
  );
}
