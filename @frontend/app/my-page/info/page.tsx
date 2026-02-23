'use client';

import { useAuth } from '@/lib/auth/AuthContext';
import type { User } from '@/lib/auth/AuthContext';
import { Button } from '@/components/atoms/button';
import { Card, CardContent } from '@/components/atoms/card';
import { UserRole } from '@shared/enums';
import { useMutation } from '@tanstack/react-query';
import { deleteUser } from '@/lib/api/auth/auth';
import { useAlert } from '@/components/molecules/AlertContext';

const ROLE_LABELS: Record<UserRole, string> = {
  USER: '일반',
  ADMIN: '관리자',
} as const;

const PROVIDER_LABELS: Record<string, string> = {
  google: '구글',
  naver: '네이버',
  kakao: '카카오',
} as const;

export default function MyPage() {
  const { user, logout } = useAuth();
  const { confirm } = useAlert();

  const { mutateAsync: onDeleteUser } = useMutation(deleteUser());

  const INFO_FIELDS: { label: string; getValue: (user: User) => string }[] = [
    { label: '등급', getValue: (user) => ROLE_LABELS[user.role] ?? user.role },
    { label: '이름', getValue: (user) => user.name },
    { label: '이메일', getValue: (user) => user.email },
    { label: '휴대폰번호', getValue: (user) => user.phone ?? '-' },
    {
      label: '간편 로그인',
      getValue: (user) => PROVIDER_LABELS[user.provider?.toLowerCase()] ?? user.provider,
    },
  ];

  async function handleDeleteUser() {
    const confirmed = await confirm({
      title: '회원 탈퇴',
      description: '정말로 회원 탈퇴하시겠습니까? 탈퇴 후에는 복구할 수 없습니다.',
      confirmText: '탈퇴',
    });
    if (!confirmed) return;

    await onDeleteUser();
    await logout();
  }

  if (!user) return null;

  return (
    <section>
      <h1 className="mb-6 text-2xl font-bold">내 정보</h1>
      <Card>
        <CardContent className="px-6 py-4">
          {INFO_FIELDS.map(({ label, getValue }) => (
            <div key={label} className="flex py-3">
              <span className="text-muted-foreground w-28 shrink-0 text-sm">{label}</span>
              <span className="text-sm font-medium">{getValue(user)}</span>
            </div>
          ))}
        </CardContent>
      </Card>

      <div className="mt-8 flex justify-end gap-2">
        <Button variant="secondary" onClick={handleDeleteUser}>
          회원 탈퇴
        </Button>
      </div>
    </section>
  );
}
