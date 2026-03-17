'use client';

interface Props {
  /** 테이블 헤더 라벨 */
  columns: string[];
  /** 각 행의 셀 데이터 (열 순서와 columns 길이 일치) */
  rows: React.ReactNode[][];
  /** 헤더 배경/글자 스타일. 기본: bg-slate-100 text-slate-700 */
  headerClassName?: string;
  /** 행 셀 글자 스타일. 기본: text-slate-800 */
  cellClassName?: string;
}

/** 가로형 테이블 — 헤더 1행 + 데이터 행들. 좌석/취소수수료 등 공통 사용 */
export default function HorizontalTable({
  columns,
  rows,
  headerClassName = 'bg-slate-100',
  cellClassName = 'text-slate-800',
}: Props) {
  const colCount = columns.length;

  return (
    <div className="w-full overflow-hidden border border-slate-200">
      <div
        className={`grid divide-x divide-slate-200 border-b border-slate-200 text-center text-sm font-medium text-slate-700 ${headerClassName}`}
        style={{ gridTemplateColumns: `repeat(${colCount}, minmax(0, 1fr))` }}
      >
        {columns.map((col, i) => (
          <span key={i} className="px-3 py-3">
            {col}
          </span>
        ))}
      </div>
      {rows.map((row, i) => (
        <div
          key={i}
          className="grid divide-x divide-slate-200 border-b border-slate-200 text-center last:border-b-0"
          style={{ gridTemplateColumns: `repeat(${colCount}, minmax(0, 1fr))` }}
        >
          {row.map((cell, j) => (
            <div key={j} className={`px-3 py-3 text-sm whitespace-pre-line ${cellClassName}`}>
              {cell}
            </div>
          ))}
        </div>
      ))}
    </div>
  );
}
