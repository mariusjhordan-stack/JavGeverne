import { useState, useEffect } from 'react';
import { useParams, Link } from 'react-router-dom';
import { getOrderById } from '../api/orders';
import { getPaymentByOrder } from '../api/payments';
import { ArrowLeft, Package } from 'lucide-react';

export default function OrderDetailPage() {
  const { id } = useParams();
  const [order, setOrder] = useState(null);
  const [payment, setPayment] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    Promise.all([
      getOrderById(id).then(r => setOrder(r.data)),
      getPaymentByOrder(id).then(r => setPayment(r.data)).catch(() => {}),
    ]).finally(() => setLoading(false));
  }, [id]);

  if (loading) return <div className="flex justify-center py-20"><div className="animate-spin h-8 w-8 border-2 border-gold border-t-transparent rounded-full" /></div>;
  if (!order) return <div className="text-center py-20"><p>Order not found</p></div>;

  return (
    <div className="max-w-4xl mx-auto px-4 py-12">
      <Link to="/orders" className="inline-flex items-center gap-2 text-gray-500 hover:text-navy mb-6">
        <ArrowLeft className="w-4 h-4" /> Back to orders
      </Link>
      <div className="flex items-center justify-between mb-8">
        <h1 className="text-3xl text-navy">Order #{order.id?.slice(0, 8)}</h1>
        <span className="px-4 py-2 bg-navy text-white rounded-lg text-sm">{order.status}</span>
      </div>
      <div className="grid md:grid-cols-3 gap-8">
        <div className="md:col-span-2">
          <div className="bg-white rounded-lg shadow-sm border border-gray-100 overflow-hidden">
            <div className="p-6 border-b border-gray-100">
              <h2 className="text-lg font-sans font-semibold text-navy">Items</h2>
            </div>
            {order.items?.map(item => (
              <div key={item.id} className="flex items-center gap-4 p-6 border-b border-gray-50">
                <div className="w-16 h-16 bg-cream rounded-lg flex items-center justify-center">
                  {item.productImage ? <img src={item.productImage} alt="" className="w-full h-full object-cover rounded-lg" /> : <Package className="w-8 h-8 text-gray-400" />}
                </div>
                <div className="flex-1">
                  <p className="font-medium text-navy">{item.productName}</p>
                  <p className="text-sm text-gray-500">Qty: {item.quantity} x ${item.unitPrice?.toLocaleString()}</p>
                </div>
                <p className="font-semibold">${item.totalPrice?.toLocaleString()}</p>
              </div>
            ))}
          </div>
        </div>
        <div className="space-y-6">
          <div className="bg-white rounded-lg shadow-sm border border-gray-100 p-6">
            <h3 className="font-sans font-semibold text-navy mb-4">Summary</h3>
            <div className="space-y-3 text-sm">
              <div className="flex justify-between"><span className="text-gray-500">Subtotal</span><span>${order.subtotal?.toLocaleString()}</span></div>
              <div className="flex justify-between"><span className="text-gray-500">Tax</span><span>${order.tax?.toLocaleString()}</span></div>
              <div className="flex justify-between border-t pt-3 font-semibold text-lg"><span>Total</span><span className="text-navy">${order.total?.toLocaleString()}</span></div>
            </div>
          </div>
          {payment && (
            <div className="bg-white rounded-lg shadow-sm border border-gray-100 p-6">
              <h3 className="font-sans font-semibold text-navy mb-4">Payment</h3>
              <div className="space-y-2 text-sm">
                <div className="flex justify-between"><span className="text-gray-500">Status</span><span className="font-medium">{payment.status}</span></div>
                {payment.paymentMethod && <div className="flex justify-between"><span className="text-gray-500">Method</span><span>{payment.paymentMethod}</span></div>}
              </div>
            </div>
          )}
          {order.shippingAddress && (
            <div className="bg-white rounded-lg shadow-sm border border-gray-100 p-6">
              <h3 className="font-sans font-semibold text-navy mb-4">Shipping</h3>
              <p className="text-sm text-gray-600 whitespace-pre-line">{order.shippingAddress}</p>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
