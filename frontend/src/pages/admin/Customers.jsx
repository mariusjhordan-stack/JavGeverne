import { useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { listUsers, getUser, listOrders } from '../../api/admin';
import { money, shortId, statusLabel, dateTime } from '../../lib/format';
import { Heading, Table, Pager, inputClass } from './ui';

export function CustomersList() {
  const [page, setPage] = useState(0);
  const [q, setQ] = useState('');
  const [data, setData] = useState({ content: [], totalPages: 0 });
  useEffect(() => { listUsers({ page, size: 15, q: q || undefined }).then(setData).catch(() => {}); }, [page, q]);
  return (
    <div>
      <Heading title="Customers"><input aria-label="Search customers" placeholder="Search" value={q} onChange={(e) => { setPage(0); setQ(e.target.value); }} className={`${inputClass} w-56`} /></Heading>
      <Table head={['Customer', 'Role', 'Joined']} empty={data.content.length === 0 ? 'No customers.' : null}>
        {data.content.map((u) => (
          <tr key={u.id}>
            <td className="py-3 pr-6"><Link to={`/admin/customers/${u.id}`} className="hover:text-gold-dark">{u.firstName} {u.lastName}</Link><p className="text-xs text-charcoal/60">{u.email}</p></td>
            <td className="py-3 pr-6">{u.role.toLowerCase()}</td>
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
  const [user, setUser] = useState(null);
  const [orders, setOrders] = useState([]);
  useEffect(() => {
    getUser(id).then(setUser).catch(() => setUser(false));
    listOrders({ userId: id, size: 20 }).then((d) => setOrders(d.content)).catch(() => {});
  }, [id]);
  if (user === false) return <p>Customer not found.</p>;
  if (!user) return <p role="status" className="text-charcoal/60">Loading…</p>;
  return (
    <div>
      <Link to="/admin/customers" className="text-xs tracking-[0.18em] uppercase text-charcoal/60 hover:text-gold-dark">← Customers</Link>
      <Heading title={`${user.firstName} ${user.lastName}`} />
      <p className="text-sm mb-10">{user.email} · {user.role.toLowerCase()} · joined {dateTime(user.createdAt)}</p>
      <h2 className="font-serif text-2xl mb-3">Orders</h2>
      <Table head={['Order', 'Status', 'Total']} empty={orders.length === 0 ? 'No orders yet.' : null}>
        {orders.map((o) => <tr key={o.id}><td className="py-3 pr-6"><Link to={`/admin/orders/${o.id}`} className="hover:text-gold-dark">{shortId(o.id)}</Link></td><td className="py-3 pr-6">{statusLabel(o.status)}</td><td className="py-3">{money(o.total, o.currency)}</td></tr>)}
      </Table>
    </div>
  );
}
