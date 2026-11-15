import { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { ArrowLeft, Loader2 } from 'lucide-react';
import { toast } from 'sonner';
import { useCart } from '../context/CartContext';
import { useAuth } from '../context/AuthContext';
import { createOrder } from '../api/orders';
import { createPreference } from '../api/payments';

export default function CheckoutPage() {
  const { items, totalPrice, clearCart } = useCart();
  const { user, isAuthenticated } = useAuth();
  const navigate = useNavigate();
  const [processing, setProcessing] = useState(false);
  const [address, setAddress] = useState({ name: '', street: '', city: '', state: '', zip: '', country: '' });

  if (!isAuthenticated) {
    return (
      <div className="min-h-[60vh] flex flex-col items-center justify-center px-6">
        <h2 className="font-serif text-3xl text-navy mb-4">Sign In Required</h2>
        <p className="text-gray-500 mb-6">Please sign in to complete your purchase.</p>
        <Link to="/login" className="bg-navy text-white px-8 py-3 text-sm uppercase tracking-[0.15em]">
          Sign In
        </Link>
      </div>
    );
  }

  if (items.length === 0) {
    return (
      <div className="min-h-[60vh] flex flex-col items-center justify-center px-6">
        <h2 className="font-serif text-3xl text-navy mb-4">Cart is Empty</h2>
        <Link to="/catalog" className="text-gold hover:underline">
          Browse Collection
        </Link>
      </div>
    );
  }

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!address.name || !address.street || !address.city || !address.country) {
      toast.error('Please fill in all required address fields');
      return;
    }
    setProcessing(true);
    try {
      const shippingAddress = `${address.name}, ${address.street}, ${address.city}, ${address.state} ${address.zip}, ${address.country}`;
      const orderData = {
        items: items.map((i) => ({
          productId: i.productId,
          productName: i.name,
          productImage: i.image,
          quantity: i.quantity,
          unitPrice: i.price,
        })),
        shippingAddress,
      };
      const order = await createOrder(orderData);
      toast.success('Order created');

      try {
        const prefData = {
          orderId: order.id,
          items: items.map((i) => ({ title: i.name, quantity: i.quantity, unitPrice: i.price })),
          payerEmail: user.email,
          totalAmount: totalPrice,
          currency: 'USD',
        };
        const pref = await createPreference(prefData);
        clearCart();
        if (pref.sandboxInitPoint) {
          window.location.href = pref.sandboxInitPoint;
        } else if (pref.initPoint) {
          window.location.href = pref.initPoint;
        } else {
          navigate(`/order-confirmation/${order.id}`);
        }
      } catch {
        clearCart();
        navigate(`/order-confirmation/${order.id}`);
      }
    } catch (err) {
      toast.error(err.response?.data?.message || 'Failed to create order');
    } finally {
      setProcessing(false);
    }
  };

  const field = (label, key, required = true) => (
    <div>
      <label className="block text-sm text-gray-600 mb-1">
        {label} {required && <span className="text-red-400">*</span>}
      </label>
      <input
        type="text"
        value={address[key]}
        onChange={(e) => setAddress({ ...address, [key]: e.target.value })}
        className="w-full px-4 py-2.5 border border-gray-200 focus:border-gold focus:outline-none text-sm"
        required={required}
      />
    </div>
  );

  return (
    <div className="max-w-5xl mx-auto px-6 py-12">
      <Link to="/cart" className="inline-flex items-center gap-2 text-sm text-gray-500 hover:text-gold mb-8">
        <ArrowLeft size={16} /> Back to Cart
      </Link>
      <h1 className="font-serif text-3xl text-navy mb-10">Checkout</h1>

      <form onSubmit={handleSubmit} className="grid lg:grid-cols-3 gap-12">
        {/* Shipping */}
        <div className="lg:col-span-2 space-y-5">
          <h2 className="font-serif text-xl text-navy mb-4">Shipping Address</h2>
          {field('Full Name', 'name')}
          {field('Street Address', 'street')}
          <div className="grid grid-cols-2 gap-4">
            {field('City', 'city')}
            {field('State / Province', 'state', false)}
          </div>
          <div className="grid grid-cols-2 gap-4">
            {field('ZIP / Postal Code', 'zip', false)}
            {field('Country', 'country')}
          </div>
        </div>

        {/* Summary */}
        <div className="bg-cream p-8 self-start">
          <h2 className="font-serif text-xl text-navy mb-6">Order Summary</h2>
          <div className="space-y-4 mb-6">
            {items.map((i) => (
              <div key={i.productId} className="flex justify-between text-sm">
                <span className="text-gray-600">
                  {i.name} <span className="text-gray-400">x{i.quantity}</span>
                </span>
                <span>${(i.price * i.quantity).toLocaleString()}</span>
              </div>
            ))}
          </div>
          <div className="border-t border-gray-300 pt-4 flex justify-between text-lg font-medium text-navy mb-8">
            <span>Total</span>
            <span>${totalPrice.toLocaleString()}</span>
          </div>
          <button
            type="submit"
            disabled={processing}
            className="w-full flex items-center justify-center gap-2 bg-gold hover:bg-gold-light text-navy py-3 text-sm uppercase tracking-[0.15em] transition-colors disabled:opacity-50"
          >
            {processing ? <><Loader2 size={16} className="animate-spin" /> Processing...</> : 'Place Order'}
          </button>
          <p className="text-xs text-gray-500 mt-4 text-center">Secure payment via Mercado Pago</p>
        </div>
      </form>
    </div>
  );
}
