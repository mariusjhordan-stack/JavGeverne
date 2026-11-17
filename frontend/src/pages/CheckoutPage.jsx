import { useEffect, useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { ArrowLeft, Loader2 } from 'lucide-react';
import { toast } from 'sonner';
import { useCart } from '../context/CartContext';
import { createOrder } from '../api/orders';
import { startCheckout } from '../api/payments';
import { listAddresses } from '../api/account';
import { errorMessage, errorCode } from '../api/client';
import { money } from '../lib/format';

const EMPTY = { recipient: '', line1: '', city: '', region: '', postalCode: '', country: '' };

const formatAddress = (a) =>
  [a.recipient, a.line1, a.line2, [a.city, a.region, a.postalCode].filter(Boolean).join(' '), a.country, a.phone]
    .filter(Boolean).join(', ');

export default function CheckoutPage() {
  const { items, totalPrice, clearCart } = useCart();
  const navigate = useNavigate();
  const [processing, setProcessing] = useState(false);
  const [error, setError] = useState('');
  const [address, setAddress] = useState(EMPTY);
  const [saved, setSaved] = useState([]);
  const [selected, setSelected] = useState('new');

  useEffect(() => {
    listAddresses().then((list) => {
      setSaved(list);
      const preferred = list.find((a) => a.defaultAddress) || list[0];
      if (preferred) setSelected(preferred.id);
    }).catch(() => {});
  }, []);

  if (items.length === 0 && !processing) {
    return (
      <div className="min-h-[60vh] flex flex-col items-center justify-center px-6">
        <h2 className="font-serif text-3xl text-navy mb-4">Your bag is empty</h2>
        <Link to="/collection" className="text-gold-dark underline">Browse the collection</Link>
      </div>
    );
  }

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    const chosen = selected === 'new' ? address : saved.find((a) => a.id === selected);
    if (!chosen?.recipient || !chosen.line1 || !chosen.city || !chosen.country) {
      setError('Please complete the shipping address.');
      return;
    }
    setProcessing(true);
    let order;
    try {
      // Only ids and quantities are sent: the server prices the order from the catalogue.
      order = await createOrder(items.map((i) => ({ productId: i.productId, quantity: i.quantity })), formatAddress(chosen));
    } catch (err) {
      setError(errorMessage(err, 'We could not place your order.'));
      setProcessing(false);
      return;
    }
    clearCart();
    try {
      const checkout = await startCheckout(order.id);
      window.location.assign(checkout.checkoutUrl);
    } catch (err) {
      toast.info(errorCode(err) === 'PAYMENT_PROVIDER_NOT_CONFIGURED'
        ? 'Your order is reserved. Online payment is not available right now.'
        : 'Your order is reserved, but payment could not be started. You can pay from the order page.');
      navigate(`/orders/${order.id}`);
    }
  };

  const field = (label, key, required = true) => (
    <div>
      <label htmlFor={`ship-${key}`} className="block text-sm text-gray-600 mb-1">
        {label} {required && <span className="text-red-600" aria-hidden="true">*</span>}
      </label>
      <input
        id={`ship-${key}`}
        type="text"
        value={address[key]}
        onChange={(e) => setAddress({ ...address, [key]: e.target.value })}
        className="w-full px-4 py-2.5 border border-gray-300 focus:border-gold focus:outline-none text-sm"
        required={required}
        maxLength={150}
      />
    </div>
  );

  return (
    <div className="max-w-5xl mx-auto px-6 py-12">
      <Link to="/cart" className="inline-flex items-center gap-2 text-sm text-gray-600 hover:text-gold-dark mb-8">
        <ArrowLeft size={16} /> Back to bag
      </Link>
      <h1 className="font-serif text-3xl text-navy mb-10">Checkout</h1>

      <form onSubmit={handleSubmit} className="grid lg:grid-cols-3 gap-12">
        <div className="lg:col-span-2 space-y-5">
          <h2 className="font-serif text-xl text-navy mb-4">Shipping address</h2>
          {saved.length > 0 && (
            <div className="space-y-2">
              {saved.map((a) => (
                <label key={a.id} className="flex items-start gap-3 border border-gray-200 p-4 cursor-pointer text-sm">
                  <input type="radio" name="address" checked={selected === a.id} onChange={() => setSelected(a.id)} className="mt-1 accent-gold" />
                  <span>{formatAddress(a)}</span>
                </label>
              ))}
              <label className="flex items-center gap-3 border border-gray-200 p-4 cursor-pointer text-sm">
                <input type="radio" name="address" checked={selected === 'new'} onChange={() => setSelected('new')} className="accent-gold" />
                Use a different address
              </label>
            </div>
          )}
          {selected === 'new' && (
            <>
              {field('Full name', 'recipient')}
              {field('Street address', 'line1')}
              <div className="grid grid-cols-2 gap-4">
                {field('City', 'city')}
                {field('State / Province', 'region', false)}
              </div>
              <div className="grid grid-cols-2 gap-4">
                {field('Postal code', 'postalCode', false)}
                {field('Country', 'country')}
              </div>
            </>
          )}
          {error && <div className="bg-red-50 border border-red-200 text-red-700 text-sm px-4 py-3" role="alert">{error}</div>}
        </div>

        <div className="bg-cream p-8 self-start">
          <h2 className="font-serif text-xl text-navy mb-6">Order summary</h2>
          <div className="space-y-4 mb-6">
            {items.map((i) => (
              <div key={i.productId} className="flex justify-between text-sm gap-4">
                <span className="text-gray-700">{i.name} <span className="text-gray-500">× {i.quantity}</span></span>
                <span>{money(i.price * i.quantity)}</span>
              </div>
            ))}
          </div>
          <div className="border-t border-gray-300 pt-4 flex justify-between text-lg font-medium text-navy mb-2">
            <span>Estimated total</span>
            <span>{money(totalPrice)}</span>
          </div>
          <p className="text-xs text-gray-600 mb-6">The final amount is confirmed by the store when the order is placed.</p>
          <button
            type="submit"
            disabled={processing}
            className="w-full flex items-center justify-center gap-2 bg-gold hover:bg-gold-light text-navy py-3 text-sm uppercase tracking-[0.15em] transition-colors disabled:opacity-50"
          >
            {processing ? <><Loader2 size={16} className="animate-spin" /> Processing…</> : 'Place order and pay'}
          </button>
          <p className="text-xs text-gray-600 mt-4 text-center">You will be redirected to Mercado Pago to pay securely.</p>
        </div>
      </form>
    </div>
  );
}
