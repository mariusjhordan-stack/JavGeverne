import { useEffect, useState } from 'react';
import { inventorySummary, inventoryMovements } from '../../api/admin';
import { dateTime } from '../../lib/format';
import { Heading, Stat, Table, Pager } from './ui';
import { useLanguage } from '../../context/LanguageContext';

export default function Inventory() {
  const { t } = useLanguage();
  const [summary, setSummary] = useState(null);
  const [page, setPage] = useState(0);
  const [movements, setMovements] = useState({ content: [], totalPages: 0 });
  useEffect(() => { inventorySummary().then(setSummary).catch(() => {}); }, []);
  useEffect(() => { inventoryMovements({ page, size: 15 }).then(setMovements).catch(() => {}); }, [page]);
  if (!summary) return <p role="status" className="text-charcoal/60">{t('Loading…')}</p>;
  return (
    <div>
      <Heading title={t('Inventory')} />
      <div className="grid grid-cols-3 gap-8 mb-12">
        <Stat label={t('Units in stock')} value={summary.unitsInStock} />
        <Stat label={t('Active products')} value={summary.activeProducts} hint={`${t('of')} ${summary.totalProducts}`} />
        <Stat label={t('Low stock')} value={summary.lowStock.length} hint={`${summary.lowStockThreshold} ${t('units or fewer')}`} />
      </div>
      {summary.lowStock.length > 0 && (
        <div className="border-l-2 border-red-700 bg-red-50 px-5 py-4 mb-12 text-sm" role="status">
          <p className="font-medium mb-2">{t('Restock soon')}</p>
          <ul>{summary.lowStock.map((p) => <li key={p.id}>{p.name} — {p.stock} {t('left')}</li>)}</ul>
        </div>
      )}
      <h2 className="font-serif text-2xl mb-4">{t('Movements')}</h2>
      <Table head={['When', 'Change', 'Stock after', 'Reason', 'Reference']} empty={movements.content.length === 0 ? 'No movements yet.' : null}>
        {movements.content.map((m) => (
          <tr key={m.id}>
            <td className="py-3 pr-6">{dateTime(m.createdAt)}</td>
            <td className={`py-3 pr-6 ${m.delta < 0 ? 'text-red-700' : 'text-green-800'}`}>{m.delta > 0 ? `+${m.delta}` : m.delta}</td>
            <td className="py-3 pr-6">{m.stockAfter}</td>
            <td className="py-3 pr-6">{t(m.reason.toLowerCase())}</td>
            <td className="py-3 text-xs text-charcoal/70">{m.reference}{m.actor ? ` · ${m.actor}` : ''}</td>
          </tr>
        ))}
      </Table>
      <Pager page={page} totalPages={movements.totalPages} onPage={setPage} />
    </div>
  );
}
