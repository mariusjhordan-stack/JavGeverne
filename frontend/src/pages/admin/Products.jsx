import { useEffect, useState, useCallback } from 'react';
import { toast } from 'sonner';
import { listProducts, createProduct, updateProduct, adjustStock } from '../../api/admin';
import { getBrands, getCategories } from '../../api/catalog';
import { errorMessage } from '../../api/client';
import { money } from '../../lib/format';
import { Heading, Table, Pager, Field, inputClass } from './ui';
import { useLanguage } from '../../context/LanguageContext';

const EMPTY = { name: '', referenceNumber: '', brandId: '', categoryId: '', price: '', stock: '0', movementType: 'AUTOMATIC', caseMaterial: '', caseDiameter: '', waterResistance: '', dialColor: '', description: '', imageUrl: '', gallery: '', featured: false };

export default function Products() {
  const { t } = useLanguage();
  const [page, setPage] = useState(0);
  const [q, setQ] = useState('');
  const [data, setData] = useState({ content: [], totalPages: 0 });
  const [brands, setBrands] = useState([]);
  const [categories, setCategories] = useState([]);
  const [editing, setEditing] = useState(null);
  const [form, setForm] = useState(EMPTY);
  const [saving, setSaving] = useState(false);

  const load = useCallback(() => listProducts({ page, q: q || undefined, size: 12, sort: 'name' }).then(setData).catch(() => toast.error(t('Products could not be loaded'))), [page, q, t]);
  useEffect(() => { load(); }, [load]);
  useEffect(() => { getBrands().then(setBrands); getCategories().then(setCategories); }, []);

  const open = (p) => {
    setEditing(p || 'new');
    setForm(p ? { ...EMPTY, ...p, brandId: String(p.brandId), categoryId: String(p.categoryId), price: String(p.price), gallery: (p.gallery || []).join('\n'), imageUrl: p.imageUrl || '' } : EMPTY);
  };
  const set = (k) => (e) => setForm({ ...form, [k]: e.target.type === 'checkbox' ? e.target.checked : e.target.value });

  const save = async (e) => {
    e.preventDefault();
    setSaving(true);
    const payload = {
      name: form.name, referenceNumber: form.referenceNumber, brandId: Number(form.brandId), categoryId: Number(form.categoryId),
      price: Number(form.price), movementType: form.movementType, caseMaterial: form.caseMaterial, caseDiameter: form.caseDiameter,
      waterResistance: form.waterResistance, dialColor: form.dialColor, description: form.description, imageUrl: form.imageUrl,
      gallery: form.gallery.split('\n').map((s) => s.trim()).filter(Boolean), featured: form.featured,
    };
    try {
      if (editing === 'new') await createProduct({ ...payload, stock: Number(form.stock) });
      else await updateProduct(editing.id, payload);
      toast.success(t('Saved'));
      setEditing(null);
      load();
    } catch (err) { toast.error(errorMessage(err, t('Could not save the product'))); } finally { setSaving(false); }
  };

  const toggle = async (p) => { try { await updateProduct(p.id, { active: !p.active }); load(); } catch (err) { toast.error(errorMessage(err)); } };
  const restock = async (p) => {
    const raw = window.prompt(`${t('Stock adjustment for')} ${p.name} (${t('current')} ${p.stock}). ${t('Use a negative number to remove units.')}`, '1');
    const delta = Number(raw);
    if (!raw || !Number.isInteger(delta) || delta === 0) return;
    try { await adjustStock(p.id, delta, 'Manual adjustment'); load(); } catch (err) { toast.error(errorMessage(err)); }
  };

  return (
    <div>
      <Heading title={t('Products')}>
        <div className="flex gap-4">
          <input aria-label={t('Search products')} placeholder={t('Search')} value={q} onChange={(e) => { setPage(0); setQ(e.target.value); }} className={`${inputClass} w-48`} />
          <button onClick={() => open(null)} className="btn-solid !py-2.5">{t('New product')}</button>
        </div>
      </Heading>

      {editing && (
        <form onSubmit={save} className="border hairline bg-white p-6 mb-10 grid md:grid-cols-3 gap-5" aria-label={t('Product form')}>
          <Field label={t('Name')} id="p-name"><input id="p-name" required value={form.name} onChange={set('name')} className={inputClass} /></Field>
          <Field label={t('Reference')} id="p-ref"><input id="p-ref" value={form.referenceNumber || ''} onChange={set('referenceNumber')} className={inputClass} /></Field>
          <Field label={t('Price (USD)')} id="p-price"><input id="p-price" required type="number" min="0.01" step="0.01" value={form.price} onChange={set('price')} className={inputClass} /></Field>
          <Field label={t('Maison')} id="p-brand"><select id="p-brand" required value={form.brandId} onChange={set('brandId')} className={inputClass}><option value="">{t('Select')}</option>{brands.map((b) => <option key={b.id} value={b.id}>{b.name}</option>)}</select></Field>
          <Field label={t('Family')} id="p-cat"><select id="p-cat" required value={form.categoryId} onChange={set('categoryId')} className={inputClass}><option value="">{t('Select')}</option>{categories.map((c) => <option key={c.id} value={c.id}>{t(c.name)}</option>)}</select></Field>
          {editing === 'new'
            ? <Field label={t('Initial stock')} id="p-stock"><input id="p-stock" type="number" min="0" value={form.stock} onChange={set('stock')} className={inputClass} /></Field>
            : <Field label={t('Stock')} id="p-stock"><p className="py-2.5 text-sm">{editing.stock} <span className="text-charcoal/60">({t('use Adjust stock')})</span></p></Field>}
          <Field label={t('Movement')} id="p-mov"><select id="p-mov" value={form.movementType} onChange={set('movementType')} className={inputClass}><option value="AUTOMATIC">{t('Automatic')}</option><option value="MANUAL">{t('Manual')}</option><option value="QUARTZ">{t('Quartz')}</option></select></Field>
          <Field label={t('Case material')} id="p-case"><input id="p-case" value={form.caseMaterial || ''} onChange={set('caseMaterial')} className={inputClass} /></Field>
          <Field label={t('Diameter')} id="p-dia"><input id="p-dia" value={form.caseDiameter || ''} onChange={set('caseDiameter')} className={inputClass} /></Field>
          <Field label={t('Water resistance')} id="p-wr"><input id="p-wr" value={form.waterResistance || ''} onChange={set('waterResistance')} className={inputClass} /></Field>
          <Field label={t('Dial')} id="p-dial"><input id="p-dial" value={form.dialColor || ''} onChange={set('dialColor')} className={inputClass} /></Field>
          <Field label={t('Main image path')} id="p-img"><input id="p-img" placeholder="/images/products/…" value={form.imageUrl} onChange={set('imageUrl')} className={inputClass} /></Field>
          <div className="md:col-span-3"><Field label={t('Gallery paths (one per line)')} id="p-gal"><textarea id="p-gal" rows={3} value={form.gallery} onChange={set('gallery')} className={inputClass} /></Field></div>
          <div className="md:col-span-3"><Field label={t('Description')} id="p-desc"><textarea id="p-desc" rows={3} value={form.description || ''} onChange={set('description')} className={inputClass} /></Field></div>
          <label className="flex items-center gap-2 text-sm"><input type="checkbox" checked={form.featured} onChange={set('featured')} className="accent-gold-dark" /> {t('Featured on the home page')}</label>
          <div className="md:col-span-3 flex gap-4"><button disabled={saving} className="btn-solid">{saving ? t('Saving…') : t('Save product')}</button><button type="button" onClick={() => setEditing(null)} className="text-sm underline">{t('Cancel')}</button></div>
        </form>
      )}

      <Table head={['Product', 'Price', 'Stock', 'Status', '']} empty={data.content.length === 0 ? 'No products found.' : null}>
        {data.content.map((p) => (
          <tr key={p.id} data-testid="admin-product-row">
            <td className="py-3 pr-6"><p className="text-ink">{p.name}</p><p className="text-xs text-charcoal/60">{p.brandName} · {p.referenceNumber}</p></td>
            <td className="py-3 pr-6">{money(p.price, p.currency)}</td>
            <td className={`py-3 pr-6 ${p.stock <= 3 ? 'text-red-700' : ''}`}>{p.stock}</td>
            <td className="py-3 pr-6">{t(p.active ? 'Active' : 'Hidden')}</td>
            <td className="py-3 text-right whitespace-nowrap space-x-4">
              <button onClick={() => open(p)} className="underline hover:text-gold-dark">{t('Edit')}</button>
              <button onClick={() => restock(p)} className="underline hover:text-gold-dark">{t('Adjust stock')}</button>
              <button onClick={() => toggle(p)} className="underline hover:text-gold-dark">{t(p.active ? 'Hide' : 'Show')}</button>
            </td>
          </tr>
        ))}
      </Table>
      <Pager page={page} totalPages={data.totalPages} onPage={setPage} />
    </div>
  );
}
