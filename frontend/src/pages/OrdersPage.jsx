import { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { Package, ChevronRight } from 'lucide-react';
import { getOrders } from '../api/orders';
import { money, statusLabel } from '../lib/format';
import { useLanguage } from '../context/LanguageContext';

const STATUS_COLORS = {
  PENDING: 'bg-yellow-100 text-yellow-800', CONFIRMED: 'bg-blue-100 text-blue-800',
  PAYMENT_PENDING: 'bg-orange-100 text-orange-800', PAYMENT_FAILED: 'bg-red-100 text-red-800', REFUNDED: 'bg-gray-100 text-gray-800', PAID: 'bg-green-100 text-green-800',
  PROCESSING: 'bg-blue-100 text-blue-800', SHIPPED: 'bg-purple-100 text-purple-800',
  DELIVERED: 'bg-green-100 text-green-800', CANCELLED: 'bg-red-100 text-red-800',
};

export default function OrdersPage() {
  const { language, t } = useLanguage();
  const [orders, setOrders] = useState([]);
  const [loading, setLoading] = useState(true);
  const [failed, setFailed] = useState(false);

  useEffect(() => {
    getOrders().then((data) => setOrders(Array.isArray(data) ? data : [])).catch(() => setFailed(true)).finally(() => setLoading(false));
  }, []);

  return (
    <div className="max-w-4xl mx-auto px-6 py-16">
      <div className="text-center mb-10">
        <p className="text-gold-dark text-xs tracking-[0.3em] uppercase mb-2">{t('Your Orders')}</p>
        <h1 className="font-serif text-3xl text-[#1a1a2e]">{t('Order History')}</h1>
      </div>
      {loading ? (
        <div className="flex justify-center py-20"><div className="w-10 h-10 border-2 border-[#c9a94e] border-t-transparent rounded-full animate-spin" /></div>
      ) : failed ? (
        <p className="text-center py-20 text-red-600" role="alert">{t('We could not load your orders. Please try again.')}</p>
      ) : orders.length === 0 ? (
        <div className="text-center py-20">
          <Package className="w-12 h-12 text-gray-300 mx-auto mb-4" />
          <p className="text-gray-500 mb-4">{t('No orders yet.')}</p>
          <Link to="/collection" className="text-gold-dark hover:underline text-sm">{t('Browse Collection')}</Link>
        </div>
      ) : (
        <div className="space-y-4">
          {orders.map((order) => (
            <Link key={order.id} to={`/orders/${order.id}`} className="block border border-gray-200 hover:border-ink transition-colors p-5" data-testid="order-row">
              <div className="flex items-center justify-between">
                <div>
                  <p className="text-sm text-gray-500 mb-1">{t('Order')} #{order.id?.slice(0, 8)} &middot; {new Date(order.createdAt).toLocaleDateString(language === 'es' ? 'es-ES' : 'en-US')}</p>
                  <p className="font-medium text-[#1a1a2e]">{money(order.total, order.currency)}</p>
                  <p className="text-xs text-gray-500 mt-1">{order.items?.length || 0} {t('item(s)')}</p>
                </div>
                <div className="flex items-center gap-3">
                  <span className={`text-xs px-2 py-1 rounded ${STATUS_COLORS[order.status] || 'bg-gray-100 text-gray-800'}`}>{t(statusLabel(order.status))}</span>
                  <ChevronRight size={18} className="text-gray-400" />
                </div>
              </div>
            </Link>
          ))}
        </div>
      )}
    </div>
  );
}
