import CheckoutPage from '../_components/checkout';

type CheckoutRouteProps = {
  searchParams: Promise<{
    orderId?: string | string[];
    orderName?: string | string[];
    amount?: string | string[];
    customerName?: string | string[];
    customerEmail?: string | string[];
    customerMobilePhone?: string | string[];
  }>;
};

const toSingleValue = (value?: string | string[]) => (Array.isArray(value) ? value[0] : value);

export default async function ReservationPaymentCheckoutPage({ searchParams }: CheckoutRouteProps) {
  const params = await searchParams;
  const orderId = toSingleValue(params?.orderId) ?? '';
  const orderName = toSingleValue(params?.orderName) ?? '결제';
  const amount = Number(toSingleValue(params?.amount) ?? 0);
  const customerName = toSingleValue(params?.customerName) ?? '';
  const customerEmail = toSingleValue(params?.customerEmail) ?? '';
  const customerMobilePhone = toSingleValue(params?.customerMobilePhone) ?? '';

  return (
    <section className="min-h-screen bg-slate-50 px-5 py-8">
      <CheckoutPage
        orderId={orderId}
        orderName={orderName}
        amountValue={Math.max(0, amount)}
        customerName={customerName}
        customerEmail={customerEmail}
        customerMobilePhone={customerMobilePhone}
      />
    </section>
  );
}
