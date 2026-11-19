import { Check } from 'lucide-react';
import { dateTime } from '../lib/format';
import { useLanguage } from '../context/LanguageContext';

// Mirrors the backend OrderStatus values; nothing here exists only in the interface.
const STEPS = [
  { key: 'PENDING', label: 'Order placed' },
  { key: 'PAYMENT_PENDING', label: 'Payment pending' },
  { key: 'PAID', label: 'Payment approved' },
  { key: 'PROCESSING', label: 'Preparing' },
  { key: 'SHIPPED', label: 'Shipped' },
  { key: 'DELIVERED', label: 'Delivered' },
];
const RANK = { PENDING: 0, PAYMENT_FAILED: 1, PAYMENT_PENDING: 1, PAID: 2, PROCESSING: 3, SHIPPED: 4, DELIVERED: 5 };

export default function OrderTimeline({ order }) {
  const { t } = useLanguage();
  const reachedAt = {};
  (order.history || []).forEach((h) => { reachedAt[h.to] = h.at; });
  const stopped = ['CANCELLED', 'REFUNDED'].includes(order.status);
  const rank = stopped
    ? Math.max(0, ...Object.keys(reachedAt).filter((s) => s in RANK).map((s) => RANK[s]))
    : RANK[order.status] ?? 0;

  return (
    <ol className="grid grid-cols-2 sm:grid-cols-6 gap-y-6" data-testid="order-timeline" aria-label={t('Order progress')}>
      {STEPS.map((step, index) => {
        const done = index < rank || (index === rank && (index === 0 || order.status === step.key || rank > 1));
        const current = !stopped && index === rank;
        const at = reachedAt[step.key] || (index === 0 ? order.createdAt : null);
        return (
          <li key={step.key} className="relative pr-4" aria-current={current ? 'step' : undefined}>
            <div className="flex items-center mb-3">
              <span className={`w-7 h-7 rounded-full flex items-center justify-center text-xs border ${done ? 'bg-navy border-navy text-white' : current ? 'border-gold-dark text-gold-dark' : 'border-gray-300 text-gray-500'}`}>
                {done ? <Check size={14} /> : index + 1}
              </span>
              {index < STEPS.length - 1 && <span className={`hidden sm:block flex-1 h-px ml-2 ${index < rank ? 'bg-navy' : 'bg-gray-300'}`} />}
            </div>
            <p className={`text-sm ${done || current ? 'text-navy font-medium' : 'text-gray-500'}`}>{t(step.label)}</p>
            {at && (done || current) && <p className="text-xs text-gray-500 mt-0.5">{dateTime(at)}</p>}
          </li>
        );
      })}
    </ol>
  );
}
