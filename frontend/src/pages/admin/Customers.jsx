import { useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { listUsers, getUser, listOrders } from '../../api/admin';
import { money, shortId, statusLabel, dateTime } from '../../lib/format';
import { Heading, Table, Pager, inputClass } from './ui';
import { useLanguage } from '../../context/LanguageContext';

export function CustomersList() {
  const { t } = useLanguage();
  const [page, setPage] = useState(0);
  const [q, setQ] = useState('');
  const [data, setData] = useState({ content: [], totalPages: 0 });
  useEffect(() => { listUsers({ page, size: 15, q: q || undefined }).then(setData).catch(() => {}); }, [page, q]);
  return (
    <div>
      <Heading title={t('Customers')}><input aria-label={t('Search customers')} placeholder={t('Search')} value={q} onChange={(e) => { setPage(0); setQ(e.target.value); }} className={`${inputClass} w-56`} /></Heading>
      <Table head={['Customer', 'Role', 'Joined']} empty={data.content.length === 0 ? 'No customers.' : null}>
        {data.content.map((u) => (
          <tr key={u.id}>
            <td className="py-3 pr-6"><Link to={`/admin/customers/${u.id}`} className="hover:text-gold-dark">{u.firstName} {u.lastName}</Link><p className="text-xs text-charcoal/60">{u.email}</p></td>
            <td className="py-3 pr-6">{t(u.role.toLowerCase())}</td>
            <td className="py-3">{dateTime(u.createdAt)}</td>
          </tr>
        ))}
      </Table>
      <Pager page={page} totalPages={data.totalPages} onPage={setPage} />
    </div>
  );
}

export function CustomerDetail() {
  const { id } = useParams();
  const { t } = useLanguage();
  const [user, setUser] = useState(null);
  const [orders, setOrders] = useState([]);
  useEffect(() => {
    getUser(id).then(setUser).catch(() => setUser(false));
    listOrders({ userId: id, size: 20 }).then((d) => setOrders(d.content)).catch(() => {});
  }, [id]);
  if (user === false) return <p>{t('Customer not found.')}</p>;
  if (!user) return <p role="status" className="text-charcoal/60">{t('Loading…')}</p>;
  return (
    <div>
      <Link to="/admin/customers" className="text-xs tracking-[0.18em] uppercase text-charcoal/60 hover:text-gold-dark">← {t('Customers')}</Link>
      <Heading title={`${user.firstName} ${user.lastName}`} />
      <p className="text-sm mb-10">{user.email} · {t(user.role.toLowerCase())} · {t('joined')} {dateTime(user.createdAt)}</p>
      <h2 className="font-serif text-2xl mb-3">{t('Orders')}</h2>
      <Table head={['Order', 'Status', 'Total']} empty={orders.length === 0 ? 'No orders yet.' : null}>
        {orders.map((o) => <tr key={o.id}><td className="py-3 pr-6"><Link to={`/admin/orders/${o.id}`} className="hover:text-gold-dark">{shortId(o.id)}</Link></td><td className="py-3 pr-6">{t(statusLabel(o.status))}</td><td className="py-3">{money(o.total, o.currency)}</td></tr>)}
      </Table>
    </div>
  );
}
