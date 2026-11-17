import { useEffect, useState, useCallback } from 'react';
import { Link, useParams } from 'react-router-dom';
import { toast } from 'sonner';
import { listOrders, getOrder, setOrderStatus } from '../../api/admin';
import { errorMessage } from '../../api/client';
import { money, shortId, statusLabel, dateTime } from '../../lib/format';
import { Heading, Table, Pager } from './ui';

const STATUSES = ['', 'PENDING', 'PAYMENT_PENDING', 'PAYMENT_FAILED', 'PAID', 'PROCESSING', 'SHIPPED', 'DELIVERED', 'CANCELLED', 'REFUNDED'];

export function OrdersList() {
  const [page, setPage] = useState(0);
  const [status, setStatus] = useState('');
  const [data, setData] = useState({ content: [], totalPages: 0 });
  useEffect(() => { listOrders({ page, size: 15, status: status || undefined }).then(setData).catch(() => toast.error('Orders could not be loaded')); }, [page, status]);
  return (
    <div>
      <Heading title="Orders">
        <select aria-label="Filter by status" value={status} onChange={(e) => { setPage(0); setStatus(e.target.value); }} className="border hairline bg-white px-3 py-2 text-sm">
          {STATUSES.map((s) => <option key={s} value={s}>{s ? statusLabel(s) : 'All statuses'}</option>)}
        </select>
      </Heading>
      <Table head={['Order', 'Customer', 'Status', 'Total']} empty={data.content.length === 0 ? 'No orders.' : null}>
        {data.content.map((o) => (
          <tr key={o.id} data-testid="admin-order-row">
            <td className="py-3 pr-6"><Link to={`/admin/orders/${o.id}`} className="hover:text-gold-dark">{shortId(o.id)}</Link><p className="text-xs text-charcoal/60">{dateTime(o.createdAt)}</p></td>
            <td className="py-3 pr-6">{o.userEmail}</td>
            <td className="py-3 pr-6">{statusLabel(o.status)}</td>
            <td className="py-3">{money(o.total, o.currency)}</td>
          </tr>
        ))}
      </Table>
      <Pager page={page} totalPages={data.totalPages} onPage={setPage} />
    </div>
  );
}

export function OrderDetail() {
  const { id } = useParams();
  const [order, setOrder] = useState(null);
  const load = useCallback(() => getOrder(id).then(setOrder).catch(() => setOrder(false)), [id]);
  useEffect(() => { load(); }, [load]);
  const move = async (status) => {
    try { setOrder(await setOrderStatus(id, status)); toast.success(`Order is now ${statusLabel(status).toLowerCase()}`); } catch (err) { toast.error(errorMessage(err)); }
  };
  if (order === false) return <p>Order not found. <Link to="/admin/orders" className="underline">Back</Link></p>;
  if (!order) return <p role="status" className="text-charcoal/60">Loading…</p>;
  return (
    <div data-testid="admin-order">
      <Link to="/admin/orders" className="text-xs tracking-[0.18em] uppercase text-charcoal/60 hover:text-gold-dark">← Orders</Link>
      <Heading title={`Order ${shortId(order.id)}`}><span className="text-sm bg-cream px-3 py-1" data-testid="admin-order-status">{statusLabel(order.status)}</span></Heading>
      <div className="grid lg:grid-cols-2 gap-12">
        <section>
          <h2 className="font-serif text-2xl mb-3">Customer</h2>
          <p className="text-sm">{order.userEmail}</p>
          <p className="text-sm text-charcoal/70 mt-2">{order.shippingAddress}</p>
          <h2 className="font-serif text-2xl mt-8 mb-3">Items</h2>
          <ul className="divide-y hairline border-y hairline text-sm">
            {order.items.map((i) => <li key={i.id} className="flex justify-between py-3"><span>{i.productName} × {i.quantity}</span><span>{money(i.totalPrice, order.currency)}</span></li>)}
            <li className="flex justify-between py-3 font-medium"><span>Total</span><span>{money(order.total, order.currency)}</span></li>
          </ul>
          <p className="text-sm mt-6">Payment: {order.paymentStatus ? order.paymentStatus.toLowerCase() : 'none yet'}{order.paymentId ? ` · ${order.paymentId}` : ''}</p>
        </section>
        <section>
          <h2 className="font-serif text-2xl mb-3">Fulfilment</h2>
          {order.allowedNextStatuses.length > 0 ? (
            <div className="flex flex-wrap gap-3 mb-8">
              {order.allowedNextStatuses.map((s) => <button key={s} onClick={() => move(s)} className={s === 'CANCELLED' ? 'border border-red-300 text-red-700 px-4 py-2 text-xs tracking-[0.15em] uppercase' : 'btn-solid !py-2.5'}>Mark {statusLabel(s).toLowerCase()}</button>)}
            </div>
          ) : <p className="text-sm text-charcoal/70 mb-8">No manual steps are available. Payment states are set automatically by the payment process.</p>}
          <h2 className="font-serif text-2xl mb-3">History</h2>
          <ol className="text-sm space-y-2">{order.history.map((h, i) => <li key={i} className="flex justify-between gap-4 border-b hairline pb-2"><span>{statusLabel(h.to)} <span className="text-charcoal/60">· {h.actor}</span></span><span className="text-charcoal/60">{dateTime(h.at)}</span></li>)}</ol>
        </section>
      </div>
    </div>
  );
}
