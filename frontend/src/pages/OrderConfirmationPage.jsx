import { useState, useEffect } from 'react';
import { useParams, Link } from 'react-router-dom';
import { CheckCircle, Package } from 'lucide-react';
import { getOrderById } from '../api/orders';

export default function OrderConfirmationPage() {
  const { id } = useParams();
  const [order, setOrder] = useState(null);

  useEffect(() => { getOrderById(id).then(setOrder).catch(() => {}); }, [id]);

  return (
    <div className="max-w-2xl mx-auto px-6 py-20 text-center">
      <CheckCircle className="w-16 h-16 text-green-500 mx-auto mb-6" />
      <h1 className="font-[Playfair_Display] text-3xl text-[#1a1a2e] mb-3">Order Confirmed</h1>
      <p className="text-gray-500 mb-2">Thank you for your purchase.</p>
      {order && <p className="text-sm text-gray-400 mb-8">Order #{order.id?.slice(0, 8)}</p>}
      {order && (
        <div className="border border-gray-200 p-6 text-left mb-8">
          <h3 className="text-sm uppercase tracking-wider text-gray-500 mb-4">Order Summary</h3>
          {order.items?.map((item) => (
            <div key={item.id} className="flex justify-between text-sm mb-2"><span>{item.productName} x {item.quantity}</span><span>${Number(item.totalPrice).toLocaleString()}</span></div>
          ))}
          <div className="border-t mt-4 pt-3 flex justify-between font-semibold"><span>Total</span><span>${Number(order.total).toLocaleString('en-US', { minimumFractionDigits: 2 })}</span></div>
        </div>
      )}
      <div className="flex justify-center gap-4">
        <Link to="/orders" className="inline-flex items-center gap-2 border border-[#1a1a2e] text-[#1a1a2e] px-6 py-2 text-sm uppercase tracking-wider hover:bg-[#1a1a2e] hover:text-white transition-colors"><Package size={16} /> View Orders</Link>
        <Link to="/collection" className="inline-flex items-center gap-2 bg-[#1a1a2e] text-white px-6 py-2 text-sm uppercase tracking-wider hover:bg-[#2d2d44] transition-colors">Continue Shopping</Link>
      </div>
    </div>
  );
}
