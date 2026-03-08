'use client';

import * as React from 'react';
import { DayPicker, getDefaultClassNames } from 'react-day-picker';
import { ChevronLeft, ChevronRight } from 'lucide-react';
import { buttonVariants } from '@/components/atoms/button';
import { cn } from '@/lib/utils';

export type CalendarProps = React.ComponentProps<typeof DayPicker>;

function formatCaption(date: Date): string {
  const y = date.getFullYear();
  const m = String(date.getMonth() + 1).padStart(2, '0');
  return `${y}. ${m}`;
}

function Calendar({
  className,
  classNames,
  showOutsideDays = true,
  components,
  formatters,
  ...props
}: CalendarProps) {
  const defaultClassNames = getDefaultClassNames();

  const monthClassNames = cn('relative w-full min-w-0 flex flex-col gap-4');

  return (
    <DayPicker
      showOutsideDays={showOutsideDays}
      className={cn(
        'group/calendar bg-background box-border w-full py-3 [--cell-size:2rem]',
        className,
      )}
      classNames={{
        ...defaultClassNames,
        root: cn('box-border w-full min-w-0 overflow-hidden p-0', defaultClassNames?.root),
        months: 'box-border w-full max-w-full flex flex-col gap-4 p-0',
        month: cn(monthClassNames, 'box-border p-0'),
        month_caption: 'flex-1 flex justify-center items-center min-w-0',
        nav: 'shrink-0',
        button_previous: cn(
          buttonVariants({ variant: 'ghost' }),
          'size-[var(--cell-size)] shrink-0 p-0',
        ),
        button_next: cn(
          buttonVariants({ variant: 'ghost' }),
          'size-[var(--cell-size)] shrink-0 p-0',
        ),
        caption_label: 'text-base font-lg',
        table: 'w-full border-collapse table-fixed',
        month_grid: 'w-full border-collapse table-fixed',
        weekdays: 'flex w-full',
        weekday: 'flex-1 min-w-0 w-0 text-[0.8rem] font-normal text-muted-foreground text-center',
        weeks: 'flex flex-col w-full',
        week: 'flex w-full mt-1 first:mt-2',
        day: 'flex-1 min-w-0 w-0 aspect-square p-0 text-center text-sm align-top',
        day_button: cn(
          'size-full p-0 font-normal rounded-full',
          'hover:bg-accent hover:text-accent-foreground',
          'focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2',
          '[.rdp-selected_&]:bg-primary [.rdp-selected_&]:text-primary-foreground [.rdp-selected_&]:hover:bg-primary/90',
          'disabled:opacity-50 disabled:pointer-events-none',
        ),
        outside:
          'text-muted-foreground opacity-50 aria-selected:bg-accent/50 aria-selected:text-muted-foreground',
        disabled: 'text-muted-foreground opacity-50',
        ...classNames,
      }}
      components={{
        Month: ({ children, className, displayIndex, calendarMonth, ...rest }) => {
          const childArray = React.Children.toArray(children);
          if (childArray.length < 4) {
            return (
              <div className={cn(monthClassNames, className)} {...rest}>
                {children}
              </div>
            );
          }
          const prevButton = childArray[0];
          const caption = childArray[1];
          const nextButton = childArray[2];
          const table = childArray[3];
          return (
            <div className={cn(monthClassNames, className)} {...rest}>
              <div className="flex w-full shrink-0 flex-nowrap items-center justify-between gap-2">
                {prevButton}
                {caption}
                {nextButton}
              </div>
              {table}
            </div>
          );
        },
        Chevron: ({ orientation, ...rest }) =>
          orientation === 'left' ? (
            <ChevronLeft className="size-4" {...rest} />
          ) : (
            <ChevronRight className="size-4" {...rest} />
          ),
        ...components,
      }}
      formatters={{
        formatCaption,
        ...formatters,
      }}
      {...props}
    />
  );
}
Calendar.displayName = 'Calendar';

export { Calendar };
