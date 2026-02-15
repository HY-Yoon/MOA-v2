import { redirect } from 'next/navigation';
import { MY_PAGE_ROUTES } from '@/constants/route/userRoutes';

export default function MyPage() {
  redirect(MY_PAGE_ROUTES.RESERVATIONS);
}
