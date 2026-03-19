'use client';

import classNames from 'classnames';

interface Props {
  label: string;
  content?: string | string[];
  children?: React.ReactNode;
  className?: string;
  contentBold?: boolean;
  contentColor?: string;
}

/** 예매 상세 등 정보 표시 전용 필드. 라벨 좌측 여백 포함 */
export default function InfoField({
  label,
  content,
  children,
  className,
  contentBold,
  contentColor,
}: Props) {
  function getContentBody() {
    if (content && Array.isArray(content)) {
      return content.map((line, i) => <p key={i}>{line}</p>);
    }

    return content ? content : children ? children : null;
  }

  return (
    <div className={classNames('grid grid-cols-[200px_1fr] items-stretch gap-4', className)}>
      <div className="flex items-center bg-slate-100 py-4 pl-4 text-sm font-medium text-slate-700">
        {label}
      </div>
      <div
        className={classNames(
          'py-4 text-sm',
          contentColor ?? 'text-slate-800',
          contentBold && 'font-bold',
        )}
      >
        {getContentBody()}
      </div>
    </div>
  );
}
