export interface BaseTableFilterOption {
  value: string;
  label: string;
}

export interface DeriveFilterOptionsConfig<T> {
  key: string;
  getValue: (item: T) => string | undefined;
  getLabel?: (value: string) => string;
}

/**
 * data에서 중복 제외 후 컬럼별 필터 옵션 목록 추출
 */
export function deriveFilterOptions<T>(
  data: T[],
  config: DeriveFilterOptionsConfig<T>[],
): Record<string, BaseTableFilterOption[]> {
  const options: Record<string, BaseTableFilterOption[]> = {};

  for (const { key, getValue, getLabel } of config) {
    const valueToItem = new Map<string, T>();

    // 1. 중복 제거한 옵션 목록 추출
    for (const item of data) {
      const val = getValue(item);
      if (val != null && !valueToItem.has(val)) valueToItem.set(val, item);
    }

    // 2. 라벨이 있는 경우 적용, 없으면 그대로 표출
    const values = [...valueToItem.keys()].sort();
    options[key] = values
      .map((value) => ({ value, label: getLabel?.(value) || value }))
      .sort((a, b) => a.label.localeCompare(b.label));
  }

  return options;
}
