import { redirect } from 'next/navigation';
import { ADMIN_ROUTES } from '@/constants/route/adminRoutes';

export default function AdminPage() {
  // return (
  //   <>
  //     <div>
  //       관리자 메인 대시보드입니다
  //     </div>
  //     <div>추후 통계 같은게 들어갈 예정</div>
  //   </>
  // )
  // TODO: 관리자 메인 대시보드 추가 전까지 공연 목록으로 리다이렉트
  redirect(ADMIN_ROUTES.SHOW);
}
