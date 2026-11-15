import { useState, useEffect } from 'react';
import { useParams, Link } from 'react-router-dom';
import { ArrowLeft } from 'lucide-react';
import { toast } from 'sonner';
import { getOrderById, cancelOrder } from '../api/orders';
import { getPaymentByOrder } from '../api/payments';

export default function OrderDetailPage() {
  const { id } = useParams();
  const [order, setOrder] = useState(null);
  const [payment, setPayment] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    Promise.all([getOrderById(id).catch(() => null), getPaymentByOrder(id).catch(() => null)])
      .then(([o, p]) => { setOrder(o); setPayment(p); setLoading(false); });
  }, [id]);

  const handleCancel = async () => {
    try { await cancelOrder(id); toast.success('Order cancelled'); setOrder((p) => ({ ...p, status: 'CANCELLED' })); }
    catch (err) { toast.error(err.response?.data?.message || 'Failed to cancel'); }
  };

  if (loading) return <div className="flex justify-center py-32"><div className="w-10 h-10 border-2 border-[#c9a94e] border-t-transparent rounded-full animate-spin" /></div>;
  if (!order) return <div className="max-w-4xl mx-auto px-6 py-20 text-center"><h2 className="font-[Playfair_Display] text-2xl mb-4">Order Not Found</h2><Link to="/orders" className="text-[#c9a94e] hover:underline">Back to Orders</Link></div>;

  return (
    <div className="max-w-4xl mx-auto px-6 py-12">
      <Link to="/orders" className="inline-flex items-center gap-2 text-sm text-gray-500 hover:text-[#c9a94e] mb-8"><ArrowLeft size={16} /> Back to Orders</Link>
      <div className="flex items-center justify-between mb-8">
        <div><h1 className="font-[Playfair_Display] text-2xl text-[#1a1a2e]">Order Details</h1><p className="text-sm text-gray-500 mt-1">#{order.id?.slice(0, 8)} &middot; {new Date(order.createdAt).toLocaleString()}</p></div>
        <span className="text-sm font-medium px-3 py-1 bg-gray-100 rounded">{order.status?.replace(/_/g, ' ')}</span>
      </div>
      <div className="border border-gray-200 mb-6">
        <div className="p-4 bg-gray-50 border-b text-xs uppercase tracking-wider text-gray-500">Items</div>
        {order.items?.map((item) => (
          <div key={item.id} className="flex items-center gap-4 p-4 border-b last:border-0">
            <img src={item.productImage || 'https://placehold.co/80x80/1a1a2e/c9a94e?text=W'} alt={item.productName} className="w-16 h-16 object-cover bg-[#f5f0e8]" />
            <div className="flex-1"><p className="font-medium text-[#1a1a2e]">{item.productName}</p><p className="text-sm text-gray-500">Qty: {item.quantity} x ${Number(item.unitPrice).toLocaleString()}</p></div>
            <p className="font-medium">${Number(item.totalPrice).toLocaleString()}</p>
          </div>
        ))}
      </div>
      <div className="border border-gray-200 p-6 mb-6">
        <div className="flex justify-between text-sm mb-2"><span className="text-gray-500">Subtotal</span><span>${Number(order.subtotal).toLocaleString('en-US', { minimumFractionDigits: 2 })}</span></div>
        <div className="flex justify-between font-semibold text-lg border-t pt-3 mt-3"><span>Total</span><span>${Number(order.total).toLocaleString('en-US', { minimumFractionDigits: 2 })}</span></div>
      </div>
      {payment && <div className="border border-gray-200 p-6 mb-6"><h3 className="text-sm uppercase tracking-wider text-gray-500 mb-3">Payment</h3><p className="text-sm">Status: <span className="font-medium">{payment.status}</span></p></div>}
      {order.status === 'PENDING' && <button onClick={handleCancel} className="text-sm text-red-600 border border-red-200 px-4 py-2 hover:bg-red-50 transition-colors">Cancel Order</button>}
    </div>
  );
}
