'use client';

import { Button } from '@/components/atoms';
import { HEADER_ROUTES, MY_PAGE_ROUTES, USER_ROUTES } from '@/constants/route/userRoutes';
import { useAuth } from '@/lib/auth/AuthContext';
import { ChevronDown, ChevronUp } from 'lucide-react';
import Link from 'next/link';
import { useEffect, useMemo, useState } from 'react';
import PaymentCountdown from './PaymentCountdown';

interface ReservationPaymentScreenProps {
  showTitle: string;
  scheduleText: string;
  scheduleSeatIds: string[];
  selectedSeats: Array<{
    seatId?: string;
    sectionName?: string;
    row?: string;
    number?: number;
    price?: number;
  }>;
  seatCount: number;
  totalAmount: number;
  remainingSeconds: number;
  expiresAt: number;
}

type SectionKey = 'ticket' | 'user' | 'delivery' | 'payment' | 'terms';

function AccordionSection({
  title,
  open,
  onToggle,
  children,
}: {
  title: string;
  open: boolean;
  onToggle: () => void;
  children: React.ReactNode;
}) {
  return (
    <section className="border-b border-slate-200 py-6">
      <button
        type="button"
        onClick={onToggle}
        className="flex w-full items-center justify-between text-left"
      >
        <h2 className="text-lg font-semibold text-slate-900">{title}</h2>
        {open ? <ChevronUp className="h-4 w-4 text-slate-400" /> : <ChevronDown className="h-4 w-4 text-slate-400" />}
      </button>
      {open && <div className="pt-4">{children}</div>}
    </section>
  );
}

export default function ReservationPaymentScreen({
  showTitle,
  scheduleText,
  scheduleSeatIds,
  selectedSeats,
  seatCount,
  totalAmount,
  remainingSeconds,
  expiresAt,
}: ReservationPaymentScreenProps) {
  const { user } = useAuth();
  const [bookerName, setBookerName] = useState('');
  const [bookerEmail, setBookerEmail] = useState('');
  const [bookerPhone, setBookerPhone] = useState('');
  const [deliveryMethod, setDeliveryMethod] = useState('onsite');
  const [paymentMethod, setPaymentMethod] = useState('card');
  const [cardCompany, setCardCompany] = useState('');
  const [installment, setInstallment] = useState('');
  const [agreedPersonalInfo, setAgreedPersonalInfo] = useState(false);
  const [agreedRefundPolicy, setAgreedRefundPolicy] = useState(false);
  const [openSections, setOpenSections] = useState<Record<SectionKey, boolean>>({
    ticket: true,
    user: true,
    delivery: true,
    payment: true,
    terms: true,
  });

  const toggleSection = (key: SectionKey) => {
    setOpenSections((prev) => ({ ...prev, [key]: !prev[key] }));
  };

  const parsedSeatCount = seatCount || selectedSeats.length || scheduleSeatIds.length;
  const selectedSeatTotalAmount = selectedSeats.reduce((sum, seat) => sum + (seat.price ?? 0), 0);
  const ticketAmount = totalAmount || selectedSeatTotalAmount || parsedSeatCount * 66000;
  const bookingFee = parsedSeatCount * 2000;
  const finalAmount = ticketAmount + bookingFee;
  const currency = useMemo(() => new Intl.NumberFormat('ko-KR'), []);
  const isValidName = bookerName.trim().length > 0;
  const isValidEmail = /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(bookerEmail.trim());
  const isValidPhone = /^[0-9-+\s]{8,}$/.test(bookerPhone.trim());
  const isDeliverySelected = deliveryMethod.length > 0;
  const isPaymentSelected =
    paymentMethod.length > 0 && cardCompany.length > 0 && installment.length > 0;
  const isTermsAgreed = agreedPersonalInfo && agreedRefundPolicy;
  const isPaymentEnabled =
    isDeliverySelected &&
    isPaymentSelected &&
    isValidName &&
    isValidEmail &&
    isValidPhone &&
    isTermsAgreed;

  useEffect(() => {
    if (!user) return;
    setBookerName((prev) => prev || user.name || '');
    setBookerEmail((prev) => prev || user.email || '');
    setBookerPhone((prev) => prev || user.phone || '');
  }, [user]);

  return (
    <section className="min-h-screen bg-white">
      <header className="border-b border-slate-200 px-5 py-3">
        <div className="mx-auto flex max-w-[1400px] items-center justify-between">
          <Link href={USER_ROUTES.HOME} className="font-logo text-lg font-bold tracking-tight text-slate-900">
            MOA
          </Link>
          <div className="flex items-center gap-3 text-xs text-slate-500">
            <Link href={HEADER_ROUTES.MY_PAGE} className="hover:text-slate-800">
              마이페이지
            </Link>
            <span>|</span>
            <Link href={MY_PAGE_ROUTES.RESERVATIONS} className="hover:text-slate-800">
              예매확인/취소
            </Link>
          </div>
        </div>
      </header>

      <div className="border-b border-slate-200 px-5 py-3">
        <div className="mx-auto flex max-w-[1400px] items-center justify-between gap-4">
          <p className="truncate text-sm font-semibold text-slate-800">
            {showTitle} · {scheduleText}
          </p>
          <PaymentCountdown expiresAt={expiresAt} fallbackRemainingSeconds={remainingSeconds} />
        </div>
      </div>

      <div className="mx-auto grid max-w-[1400px] grid-cols-1 gap-8 px-5 py-6 lg:grid-cols-[minmax(0,1fr)_360px]">
        <div className="rounded-xl border border-slate-200 bg-white px-5">
          <AccordionSection
            title="티켓 주문상세"
            open={openSections.ticket}
            onToggle={() => toggleSection('ticket')}
          >
            <div className="space-y-3 text-sm text-slate-700">
              {selectedSeats.length > 0 ? (
                <ul className="space-y-1">
                  {selectedSeats.map((seat, index) => (
                    <li key={`${seat.seatId ?? 'seat'}-${index}`}>
                      {(seat.sectionName ?? '-')}구역 / {seat.row ?? '-'}열 {seat.number ?? '-'}번
                    </li>
                  ))}
                </ul>
              ) : (
                <p>선택 좌석: {scheduleSeatIds.length ? scheduleSeatIds.join(', ') : '-'}</p>
              )}
            </div>
          </AccordionSection>

          <AccordionSection
            title="예약자 정보"
            open={openSections.user}
            onToggle={() => toggleSection('user')}
          >
            <div className="space-y-3">
              <input
                className="w-full rounded-md border border-slate-300 px-3 py-2 text-sm text-slate-700"
                value={bookerName}
                onChange={(event) => setBookerName(event.target.value)}
                placeholder="이름"
              />
              <input
                className="w-full rounded-md border border-slate-300 px-3 py-2 text-sm text-slate-700"
                value={bookerEmail}
                onChange={(event) => setBookerEmail(event.target.value)}
                placeholder="이메일"
              />
              <input
                className="w-full rounded-md border border-slate-300 px-3 py-2 text-sm text-slate-700"
                value={bookerPhone}
                onChange={(event) => setBookerPhone(event.target.value)}
                placeholder="전화번호"
              />
            </div>
          </AccordionSection>

          <AccordionSection
            title="티켓 수령 방법"
            open={openSections.delivery}
            onToggle={() => toggleSection('delivery')}
          >
            <label className="inline-flex items-center gap-2 text-sm text-slate-700">
              <input
                type="radio"
                name="delivery-method"
                value="onsite"
                checked={deliveryMethod === 'onsite'}
                onChange={(event) => setDeliveryMethod(event.target.value)}
              />
              현장수령
            </label>
          </AccordionSection>

          <AccordionSection
            title="결제수단"
            open={openSections.payment}
            onToggle={() => toggleSection('payment')}
          >
            <div className="space-y-3 text-sm text-slate-700">
              <label className="flex items-center gap-2">
                <input
                  type="radio"
                  name="payment-method"
                  value="card"
                  checked={paymentMethod === 'card'}
                  onChange={(event) => setPaymentMethod(event.target.value)}
                />
                카드 및 기타 결제수단
              </label>
              <select
                className="w-full rounded-md border border-slate-300 px-3 py-2"
                value={cardCompany}
                onChange={(event) => setCardCompany(event.target.value)}
              >
                <option value="">카드사 선택</option>
                <option value="kb">KB국민카드</option>
                <option value="shinhan">신한카드</option>
                <option value="hyundai">현대카드</option>
                <option value="samsung">삼성카드</option>
              </select>
              <select
                className="w-full rounded-md border border-slate-300 px-3 py-2"
                value={installment}
                onChange={(event) => setInstallment(event.target.value)}
              >
                <option value="">할부 선택</option>
                <option value="lump-sum">일시불</option>
                <option value="2">2개월</option>
                <option value="3">3개월</option>
              </select>
            </div>
          </AccordionSection>

          <AccordionSection
            title="약관 동의"
            open={openSections.terms}
            onToggle={() => toggleSection('terms')}
          >
            <div className="space-y-2 text-sm text-slate-700">
              <label className="flex items-center gap-2">
                <input
                  type="checkbox"
                  checked={agreedPersonalInfo}
                  onChange={(event) => setAgreedPersonalInfo(event.target.checked)}
                />
                개인정보 제3자 제공 동의
              </label>
              <label className="flex items-center gap-2">
                <input
                  type="checkbox"
                  checked={agreedRefundPolicy}
                  onChange={(event) => setAgreedRefundPolicy(event.target.checked)}
                />
                취소수수료/환불 규정 동의
              </label>
            </div>
          </AccordionSection>
        </div>

        <aside className="h-fit rounded-xl border border-slate-200 bg-white p-4 lg:sticky lg:top-6">
          <h2 className="text-base font-semibold text-slate-900">결제 정보</h2>
          <dl className="mt-4 space-y-2 text-sm">
            <div className="flex justify-between text-slate-600">
              <dt>티켓금액</dt>
              <dd>{currency.format(ticketAmount)}원</dd>
            </div>
            <div className="flex justify-between text-slate-600">
              <dt>예매수수료</dt>
              <dd>+{currency.format(bookingFee)}원</dd>
            </div>
            <div className="mt-4 flex justify-between border-t border-slate-200 pt-3 text-base font-bold text-primary">
              <dt>최종 결제금액</dt>
              <dd>{currency.format(finalAmount)}원</dd>
            </div>
          </dl>
          <Button className="mt-5 w-full" disabled={!isPaymentEnabled}>
            총 {currency.format(finalAmount)}원 결제하기
          </Button>
        </aside>
      </div>
    </section>
  );
}
