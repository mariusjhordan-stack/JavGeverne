import { useState, useEffect, useCallback } from 'react';
import { useParams, Link } from 'react-router-dom';
import { ArrowLeft, Loader2 } from 'lucide-react';
import { toast } from 'sonner';
import { getOrderById, cancelOrder } from '../api/orders';
import { getPaymentForOrder, startCheckout } from '../api/payments';
import { errorMessage, errorCode } from '../api/client';
import { money, shortId, dateTime, statusLabel } from '../lib/format';
import OrderTimeline from '../components/OrderTimeline';

export default function OrderDetailPage() {
  const { id } = useParams();
  const [order, setOrder] = useState(null);
  const [payment, setPayment] = useState(null);
  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState(false);

  const load = useCallback(() => Promise.all([getOrderById(id).catch(() => null), getPaymentForOrder(id).catch(() => null)])
    .then(([o, p]) => { setOrder(o); setPayment(p); setLoading(false); }), [id]);

  useEffect(() => { load(); }, [load]);

  const handleCancel = async () => {
    setBusy(true);
    try {
      setOrder(await cancelOrder(id));
      toast.success('Order cancelled');
    } catch (err) {
      toast.error(errorMessage(err, 'This order could not be cancelled'));
    } finally {
      setBusy(false);
    }
  };

  const handlePay = async () => {
    setBusy(true);
    try {
      const checkout = await startCheckout(id);
      window.location.assign(checkout.checkoutUrl);
    } catch (err) {
      toast.error(errorCode(err) === 'PAYMENT_PROVIDER_NOT_CONFIGURED'
        ? 'Online payment is not available right now.' : errorMessage(err, 'Payment could not be started'));
      setBusy(false);
      load();
    }
  };

  if (loading) return <div className="flex justify-center py-32" role="status"><Loader2 className="w-10 h-10 text-gold animate-spin" /></div>;
  if (!order) {
    return (
      <div className="max-w-4xl mx-auto px-6 py-20 text-center">
        <h1 className="font-serif text-2xl mb-4">Order not found</h1>
        <Link to="/orders" className="text-gold-dark underline">Back to orders</Link>
      </div>
    );
  }

  const payable = order.status === 'PENDING' || order.status === 'PAYMENT_FAILED';
  const stopped = order.status === 'CANCELLED' || order.status === 'REFUNDED';

  return (
    <div className="max-w-4xl mx-auto px-6 py-12" data-testid="order-detail" data-order-status={order.status}>
      <Link to="/orders" className="inline-flex items-center gap-2 text-sm text-gray-600 hover:text-gold-dark mb-8"><ArrowLeft size={16} /> Back to orders</Link>
      <div className="flex flex-wrap items-end justify-between gap-4 mb-10">
        <div>
          <h1 className="font-serif text-3xl text-navy">Order {shortId(order.id)}</h1>
          <p className="text-sm text-gray-600 mt-1">Placed {dateTime(order.createdAt)}</p>
        </div>
        <span className="text-sm font-medium px-3 py-1 bg-cream text-navy" data-testid="order-status">{statusLabel(order.status)}</span>
      </div>

      {stopped ? (
        <p className="border border-gray-200 p-5 mb-8 text-sm text-gray-700">
          {order.status === 'REFUNDED' ? 'This order was refunded.'
            : order.cancelReason === 'EXPIRED' ? 'This order was cancelled because payment was not completed in time.' : 'This order was cancelled.'}
        </p>
      ) : (
        <div className="mb-10"><OrderTimeline order={order} /></div>
      )}

      {payable && (
        <div className="border border-amber-200 bg-amber-50 p-5 mb-8 flex flex-wrap items-center justify-between gap-4">
          <p className="text-sm text-amber-900">
            {order.status === 'PAYMENT_FAILED' ? 'Your payment was not completed.' : 'This order is waiting for payment.'}
            {order.expiresAt && <> Your pieces are reserved until {dateTime(order.expiresAt)}.</>}
          </p>
          <div className="flex gap-3">
            <button onClick={handlePay} disabled={busy} className="bg-navy text-white px-5 py-2 text-sm uppercase tracking-wider disabled:opacity-50">
              {order.status === 'PAYMENT_FAILED' ? 'Try payment again' : 'Pay now'}
            </button>
            <button onClick={handleCancel} disabled={busy} className="border border-red-300 text-red-700 px-5 py-2 text-sm uppercase tracking-wider disabled:opacity-50">
              Cancel order
            </button>
          </div>
        </div>
      )}

      <div className="border border-gray-200 mb-6">
        {order.items?.map((item) => (
          <div key={item.id} className="flex items-center gap-4 p-4 border-b last:border-0 border-gray-200">
            {item.productImage && <img src={item.productImage} alt="" className="w-16 h-16 object-cover bg-cream" />}
            <div className="flex-1">
              <Link to={`/product/${item.productId}`} className="font-medium text-navy hover:underline">{item.productName}</Link>
              <p className="text-sm text-gray-600">{item.quantity} × {money(item.unitPrice, order.currency)}</p>
            </div>
            <p className="font-medium">{money(item.totalPrice, order.currency)}</p>
          </div>
        ))}
      </div>

      <div className="grid sm:grid-cols-2 gap-6">
        <div className="border border-gray-200 p-6">
          <h2 className="text-xs uppercase tracking-wider text-gray-600 mb-3">Shipping to</h2>
          <p className="text-sm text-gray-800">{order.shippingAddress}</p>
        </div>
        <div className="border border-gray-200 p-6">
          <h2 className="text-xs uppercase tracking-wider text-gray-600 mb-3">Payment</h2>
          <div className="flex justify-between font-medium text-lg text-navy"><span>Total</span><span data-testid="order-total">{money(order.total, order.currency)}</span></div>
          <p className="text-sm text-gray-700 mt-3" data-testid="payment-status">
            {payment ? <>Payment status: <span className="font-medium">{payment.status.replace(/_/g, ' ').toLowerCase()}</span>{payment.paymentMethod ? ` · ${payment.paymentMethod}` : ''}</> : 'No payment has been started for this order.'}
          </p>
        </div>
      </div>
    </div>
  );
}
