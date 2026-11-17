import { useEffect, useState } from 'react';
import { Link, useParams, useSearchParams } from 'react-router-dom';
import { CheckCircle2, Clock, XCircle, Loader2 } from 'lucide-react';
import { getOrderById } from '../api/orders';
import { syncPayment } from '../api/payments';
import { money, shortId } from '../lib/format';

const SETTLED = ['PAID', 'PROCESSING', 'SHIPPED', 'DELIVERED', 'PAYMENT_FAILED', 'CANCELLED', 'REFUNDED'];

/**
 * Where Mercado Pago sends the customer back. Nothing in the URL is believed: the page asks the
 * server to verify the payment with the provider and shows the order's real status.
 */
export default function CheckoutResultPage() {
  const { outcome } = useParams();
  const [params] = useSearchParams();
  const orderId = params.get('order') || params.get('external_reference');
  const [order, setOrder] = useState(null);
  const [state, setState] = useState(orderId ? 'checking' : 'missing');

  useEffect(() => {
    if (!orderId) return undefined;
    let cancelled = false;
    let attempts = 0;
    const check = async () => {
      attempts += 1;
      try {
        await syncPayment(orderId).catch(() => {});
        const current = await getOrderById(orderId);
        if (cancelled) return;
        setOrder(current);
        if (SETTLED.includes(current.status) || attempts >= 8) setState('done');
        else setTimeout(check, 2500);
      } catch {
        if (!cancelled) setState('error');
      }
    };
    check();
    return () => { cancelled = true; };
  }, [orderId]);

  if (state === 'missing' || state === 'error') {
    return (
      <div className="max-w-xl mx-auto px-6 py-24 text-center">
        <h1 className="font-serif text-3xl text-navy mb-4">We could not find that order</h1>
        <p className="text-gray-600 mb-8">Check the status of your purchases in your order history.</p>
        <Link to="/orders" className="bg-navy text-white px-8 py-3 text-sm uppercase tracking-[0.15em]">My orders</Link>
      </div>
    );
  }

  if (state === 'checking' && !order) {
    return (
      <div className="max-w-xl mx-auto px-6 py-24 text-center" role="status">
        <Loader2 className="w-10 h-10 text-gold mx-auto mb-6 animate-spin" />
        <h1 className="font-serif text-2xl text-navy">Confirming your payment…</h1>
      </div>
    );
  }

  const paid = ['PAID', 'PROCESSING', 'SHIPPED', 'DELIVERED'].includes(order.status);
  const failed = ['PAYMENT_FAILED', 'CANCELLED', 'REFUNDED'].includes(order.status);
  const view = paid
    ? { Icon: CheckCircle2, tone: 'text-green-700', title: 'Payment confirmed', text: 'Thank you. We are preparing your order.' }
    : failed
      ? { Icon: XCircle, tone: 'text-red-700', title: order.status === 'PAYMENT_FAILED' ? 'Payment was not completed' : 'This order is no longer active',
          text: order.status === 'PAYMENT_FAILED' ? 'No charge was made. Your pieces remain reserved for a limited time and you can try again.' : 'See the order page for details.' }
      : { Icon: Clock, tone: 'text-amber-700', title: state === 'checking' ? 'Confirming your payment…' : 'Payment is being processed',
          text: 'We have not received confirmation yet. This page does not mean the order is paid; we will update it as soon as the payment provider confirms.' };

  return (
    <div className="max-w-xl mx-auto px-6 py-24 text-center" data-testid="checkout-result" data-order-status={order.status} data-return={outcome}>
      <view.Icon className={`w-14 h-14 mx-auto mb-6 ${view.tone}`} strokeWidth={1.25} />
      <h1 className="font-serif text-3xl text-navy mb-3">{view.title}</h1>
      <p className="text-gray-600 mb-2">{view.text}</p>
      <p className="text-sm text-gray-600 mb-10">Order {shortId(order.id)} · {money(order.total, order.currency)}</p>
      <div className="flex justify-center gap-4">
        <Link to={`/orders/${order.id}`} className="bg-navy text-white px-6 py-3 text-sm uppercase tracking-[0.15em] hover:bg-navy-light transition-colors">
          View order
        </Link>
        <Link to="/collection" className="border border-navy text-navy px-6 py-3 text-sm uppercase tracking-[0.15em] hover:bg-navy hover:text-white transition-colors">
          Continue shopping
        </Link>
      </div>
    </div>
  );
}
