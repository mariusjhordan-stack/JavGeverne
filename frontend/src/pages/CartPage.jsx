import { Link } from 'react-router-dom';
import { Trash2, Plus, Minus, ShoppingBag, ArrowLeft } from 'lucide-react';
import { useCart } from '../context/CartContext';

export default function CartPage() {
  const { items, updateQuantity, removeItem, totalPrice } = useCart();

  if (items.length === 0)
    return (
      <div className="min-h-[60vh] flex flex-col items-center justify-center px-6">
        <ShoppingBag size={64} className="text-gray-200 mb-6" strokeWidth={1} />
        <h2 className="font-serif text-3xl text-navy mb-3">Your Cart is Empty</h2>
        <p className="text-gray-500 mb-8">Discover our collection of exceptional timepieces.</p>
        <Link
          to="/catalog"
          className="bg-navy text-white px-8 py-3 text-sm uppercase tracking-[0.15em] hover:bg-navy-light transition-colors"
        >
          Explore Collection
        </Link>
      </div>
    );

  return (
    <div className="max-w-5xl mx-auto px-6 py-12">
      <Link to="/catalog" className="inline-flex items-center gap-2 text-sm text-gray-500 hover:text-gold mb-8">
        <ArrowLeft size={16} /> Continue Shopping
      </Link>
      <h1 className="font-serif text-3xl text-navy mb-10">Shopping Cart</h1>

      <div className="grid lg:grid-cols-3 gap-12">
        <div className="lg:col-span-2 space-y-6">
          {items.map((item) => (
            <div key={item.productId} className="flex gap-6 border-b border-gray-100 pb-6">
              <div className="w-28 h-28 bg-cream shrink-0">
                <img
                  src={item.image}
                  alt={item.name}
                  className="w-full h-full object-cover"
                />
              </div>
              <div className="flex-1 min-w-0">
                {item.brandName && (
                  <p className="text-xs text-gold tracking-[0.15em] uppercase">{item.brandName}</p>
                )}
                <h3 className="font-serif text-lg text-navy">{item.name}</h3>
                <p className="text-sm text-gray-600 mt-1">${Number(item.price).toLocaleString()}</p>
                <div className="flex items-center gap-4 mt-3">
                  <div className="flex items-center border border-gray-200">
                    <button onClick={() => updateQuantity(item.productId, item.quantity - 1)} className="px-2 py-1 hover:bg-gray-50">
                      <Minus size={14} />
                    </button>
                    <span className="px-3 text-sm">{item.quantity}</span>
                    <button onClick={() => updateQuantity(item.productId, item.quantity + 1)} className="px-2 py-1 hover:bg-gray-50">
                      <Plus size={14} />
                    </button>
                  </div>
                  <button onClick={() => removeItem(item.productId)} className="text-gray-400 hover:text-red-500">
                    <Trash2 size={16} />
                  </button>
                </div>
              </div>
              <p className="text-navy font-medium whitespace-nowrap">
                ${(item.price * item.quantity).toLocaleString()}
              </p>
            </div>
          ))}
        </div>

        {/* Summary */}
        <div className="bg-cream p-8">
          <h3 className="font-serif text-xl text-navy mb-6">Order Summary</h3>
          <div className="space-y-3 text-sm mb-6">
            <div className="flex justify-between">
              <span className="text-gray-600">Subtotal</span>
              <span>${totalPrice.toLocaleString()}</span>
            </div>
            <div className="flex justify-between">
              <span className="text-gray-600">Shipping</span>
              <span className="text-gold">Complimentary</span>
            </div>
          </div>
          <div className="border-t border-gray-300 pt-4 flex justify-between font-medium text-navy text-lg mb-8">
            <span>Total</span>
            <span>${totalPrice.toLocaleString()}</span>
          </div>
          <Link
            to="/checkout"
            className="block w-full text-center bg-navy text-white py-3 text-sm uppercase tracking-[0.15em] hover:bg-navy-light transition-colors"
          >
            Proceed to Checkout
          </Link>
        </div>
      </div>
    </div>
  );
}
