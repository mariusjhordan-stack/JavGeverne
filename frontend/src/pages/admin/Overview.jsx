import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { orderStats, paymentStats, inventorySummary, listOrders } from '../../api/admin';
import { money, shortId, statusLabel, dateTime } from '../../lib/format';
import { Heading, Stat, Table } from './ui';
import { useLanguage } from '../../context/LanguageContext';

export default function Overview() {
  const { t } = useLanguage();
  const [data, setData] = useState({});
  const [recent, setRecent] = useState([]);
  useEffect(() => {
    Promise.all([orderStats(), paymentStats(), inventorySummary(), listOrders({ size: 6 })])
      .then(([orders, payments, inventory, list]) => { setData({ orders, payments, inventory }); setRecent(list.content); })
      .catch(() => setData({ failed: true }));
  }, []);
  if (data.failed) return <p role="alert" className="text-red-700">{t('The dashboard could not be loaded.')}</p>;
  const { orders, payments, inventory } = data;
  if (!orders) return <p role="status" className="text-charcoal/60">{t('Loading…')}</p>;
  return (
    <div>
      <Heading title={t('Overview')} />
      <div className="grid grid-cols-2 lg:grid-cols-4 gap-8 mb-14">
        <Stat label={t('Revenue')} value={money(orders.revenue)} hint={t('Paid orders')} testId="stat-revenue" />
        <Stat label={t('Orders')} value={orders.totalOrders} hint={`${orders.byStatus.PAID + orders.byStatus.PROCESSING} ${t('to fulfil')}`} />
        <Stat label={t('Awaiting payment')} value={orders.byStatus.PENDING + orders.byStatus.PAYMENT_PENDING} hint={`${orders.byStatus.PAYMENT_FAILED} ${t('failed')}`} />
        <Stat label={t('Units in stock')} value={inventory.unitsInStock} hint={`${inventory.lowStock.length} ${t('low')}`} />
      </div>
      <div className="grid lg:grid-cols-2 gap-12">
        <section>
          <h2 className="font-serif text-2xl mb-4">{t('Latest orders')}</h2>
          <Table head={['Order', 'Status', 'Total']}>
            {recent.map((o) => (
              <tr key={o.id}>
                <td className="py-3 pr-6"><Link className="hover:text-gold-dark" to={`/admin/orders/${o.id}`}>{shortId(o.id)}</Link><p className="text-xs text-charcoal/60">{dateTime(o.createdAt)}</p></td>
                <td className="py-3 pr-6">{t(statusLabel(o.status))}</td>
                <td className="py-3">{money(o.total, o.currency)}</td>
              </tr>
            ))}
          </Table>
        </section>
        <section>
          <h2 className="font-serif text-2xl mb-4">{t('Payments')}</h2>
          <dl className="space-y-2 text-sm mb-6">
            {Object.entries(payments.byStatus).map(([k, v]) => <div key={k} className="flex justify-between border-b hairline py-2"><dt>{t(k.replace('_', ' ').toLowerCase())}</dt><dd>{v}</dd></div>)}
          </dl>
          <p className="text-xs text-charcoal/70">{payments.providerConfigured ? t('Stripe credentials are configured.') : t('Stripe is not configured: checkout is unavailable.')} {payments.webhookSecretConfigured ? '' : t('Webhook secret missing: notifications are refused.')}</p>
          {inventory.lowStock.length > 0 && (
            <div className="mt-8"><h2 className="font-serif text-2xl mb-3">{t('Low stock')}</h2>
              <ul className="text-sm space-y-1">{inventory.lowStock.map((p) => <li key={p.id} className="flex justify-between"><span>{p.name}</span><span className="text-red-700">{p.stock}</span></li>)}</ul></div>
          )}
        </section>
      </div>
    </div>
  );
}
