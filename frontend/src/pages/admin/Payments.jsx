import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { listPayments } from '../../api/admin';
import { money, shortId, dateTime } from '../../lib/format';
import { Heading, Table, Pager } from './ui';

export default function Payments() {
  const [page, setPage] = useState(0);
  const [data, setData] = useState({ content: [], totalPages: 0 });
  useEffect(() => { listPayments({ page, size: 15 }).then(setData).catch(() => {}); }, [page]);
  return (
    <div>
      <Heading title="Payments" />
      <Table head={['Order', 'Status', 'Amount', 'Provider reference', 'Method']} empty={data.content.length === 0 ? 'No payments yet.' : null}>
        {data.content.map((p) => (
          <tr key={p.id}>
            <td className="py-3 pr-6"><Link to={`/admin/orders/${p.orderId}`} className="hover:text-gold-dark">{shortId(p.orderId)}</Link><p className="text-xs text-charcoal/60">{dateTime(p.createdAt)}</p></td>
            <td className="py-3 pr-6">{p.status.replace('_', ' ').toLowerCase()}</td>
            <td className="py-3 pr-6">{money(p.amount, p.currency)}</td>
            <td className="py-3 pr-6 text-xs">{p.externalPaymentId || '—'}</td>
            <td className="py-3">{p.paymentMethod || '—'}</td>
          </tr>
        ))}
      </Table>
      <Pager page={page} totalPages={data.totalPages} onPage={setPage} />
    </div>
  );
}
