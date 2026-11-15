import { useState, useEffect } from 'react';
import { useParams, Link } from 'react-router-dom';
import { getOrderById } from '../api/orders';
import { CheckCircle, Package, ArrowRight } from 'lucide-react';

export default function OrderConfirmationPage() {
  const { id } = useParams();
  const [order, setOrder] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    getOrderById(id).then(setOrder).catch(() => {}).finally(() => setLoading(false));
  }, [id]);

  if (loading) return <div className="flex justify-center py-20"><div className="animate-spin h-8 w-8 border-2 border-gold border-t-transparent rounded-full" /></div>;

  return (
    <div className="max-w-2xl mx-auto px-4 py-16 text-center">
      <CheckCircle className="w-20 h-20 text-green-500 mx-auto mb-6" />
      <h1 className="text-3xl text-navy mb-4">Thank You for Your Order</h1>
      <p className="text-gray-500 mb-2">Your order has been placed successfully.</p>
      {order && <p className="text-sm text-gray-400 mb-8">Order #{order.id?.slice(0, 8)} - Status: {order.status}</p>}
      <div className="flex justify-center gap-4">
        <Link to={`/orders/${id}`} className="inline-flex items-center gap-2 px-6 py-3 bg-navy text-white rounded-lg hover:bg-navy-light transition-colors">
          <Package className="w-4 h-4" /> View Order
        </Link>
        <Link to="/catalog" className="inline-flex items-center gap-2 px-6 py-3 border border-gray-200 text-gray-700 rounded-lg hover:border-gold transition-colors">
          Continue Shopping <ArrowRight className="w-4 h-4" />
        </Link>
      </div>
    </div>
  );
}
