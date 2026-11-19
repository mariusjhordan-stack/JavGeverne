import { useLanguage } from '../../context/LanguageContext';

export const Heading = ({ title, children }) => (
  <div className="flex flex-wrap items-end justify-between gap-4 mb-8">
    <h1 className="font-serif text-4xl text-ink">{title}</h1>
    {children}
  </div>
);

export const Stat = ({ label, value, hint, testId }) => (
  <div className="border-t hairline pt-4">
    <p className="eyebrow mb-2">{label}</p>
    <p className="font-serif text-4xl text-ink" data-testid={testId}>{value}</p>
    {hint && <p className="text-xs text-charcoal/60 mt-1">{hint}</p>}
  </div>
);

export const Table = ({ head, children, empty }) => {
  const { t } = useLanguage();
  return (
    <div className="overflow-x-auto border-y hairline">
      <table className="w-full text-sm">
        <thead><tr className="text-left text-[0.68rem] tracking-[0.2em] uppercase text-charcoal/60">{head.map((h) => <th key={h} scope="col" className="py-3 pr-6 font-normal">{t(h)}</th>)}</tr></thead>
        <tbody className="divide-y hairline">{children}</tbody>
      </table>
      {empty && <p className="py-12 text-center text-charcoal/60">{t(empty)}</p>}
    </div>
  );
};

export const Pager = ({ page, totalPages, onPage }) => {
  const { t } = useLanguage();
  return totalPages > 1 && (
    <div className="flex items-center justify-end gap-4 mt-6 text-xs tracking-[0.18em] uppercase">
      <button disabled={page <= 0} onClick={() => onPage(page - 1)} className="disabled:opacity-30 hover:text-gold-dark">{t('Previous')}</button>
      <span>{page + 1} / {totalPages}</span>
      <button disabled={page >= totalPages - 1} onClick={() => onPage(page + 1)} className="disabled:opacity-30 hover:text-gold-dark">{t('Next')}</button>
    </div>
  );
};

export const Field = ({ label, id, children }) => (
  <div>
    <label htmlFor={id} className="block text-xs tracking-wider uppercase text-charcoal/70 mb-1.5">{label}</label>
    {children}
  </div>
);

export const inputClass = 'w-full border hairline bg-white px-3 py-2.5 text-sm focus:border-gold-dark focus:outline-none';
