'use client';

import classNames from 'classnames';

interface Props {
  title: React.ReactNode;
  children: React.ReactNode;
  /** 그리드 열 수. 1이면 grid gap-6, 2면 grid-cols-2 gap-6. 없으면 래퍼 없이 children만 렌더 */
  cols?: number;
  /** 2열일 때 그리드 열 비율 (예: 'grid-cols-[minmax(200px,280px)_1fr]'). 없으면 grid-cols-2 */
  gridColsClassName?: string;
  /** 섹션 하단에 구분선 표시 여부 */
  showDivider?: boolean;
  /** section에 적용할 커스텀 클래스 (예: pb-12) */
  customClass?: string;
}

export default function SectionLayout({
  title,
  children,
  cols = 1,
  gridColsClassName,
  showDivider,
  customClass,
}: Props) {
  const gridCols = cols === 2 ? (gridColsClassName ?? 'grid-cols-2') : undefined;

  return (
    <>
      <section className={customClass}>
        <h2 className="mb-6 font-bold">{title}</h2>
        <div className={classNames('grid', 'gap-6', gridCols)}>{children}</div>
      </section>
      {showDivider && <hr className="my-8 border-0 border-t border-slate-200" />}
    </>
  );
}
