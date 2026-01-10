# Page-specific Components

이 폴더에는 **admin/show/upsert 페이지에서만 사용되는 로컬 컴포넌트들**이 포함됩니다.

> **📝 참고**: 폴더 이름이 `_components`인 이유는 Next.js에서 `_`로 시작하는 폴더는 라우팅에서 제외되기 때문입니다.

Atomic Design에서는:
- **organisms**: 여러 곳에서 재사용되는 복잡 컴포넌트들
- **page components**: 특정 페이지에서만 사용되는 컴포넌트들

이 폴더의 컴포넌트들은 다른 페이지에서 재사용되지 않으므로 페이지 근처에 위치시켰습니다.

## Components
- `ShowUpsertForm.tsx`: 공연 등록/수정 폼 컴포넌트