import { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { Plus, X, ShoppingBag } from 'lucide-react';
import { toast } from 'sonner';
import { getProducts, getProductById } from '../api/catalog';
import { useCart } from '../context/CartContext';
import { useCompare, MAX_COMPARE } from '../context/CompareContext';
import { money, withUnit } from '../lib/format';

const ROWS = [
  ['Brand', (p) => p.brandName],
  ['Reference', (p) => p.referenceNumber],
  ['Price', (p) => money(p.price, p.currency)],
  ['Movement', (p) => p.movementType?.toLowerCase()],
  ['Case material', (p) => p.caseMaterial],
  ['Case diameter', (p) => withUnit(p.caseDiameter, 'mm')],
  ['Water resistance', (p) => p.waterResistance],
  ['Crystal', (p) => p.crystalType],
  ['Dial', (p) => p.dialColor],
  ['Strap', (p) => p.strapMaterial],
  ['Power reserve', (p) => p.powerReserve],
  ['Availability', (p) => (p.stock > 0 ? 'In stock' : 'Out of stock')],
];

export default function ComparePage() {
  const { addItem } = useCart();
  const { ids, add, remove } = useCompare();
  const [products, setProducts] = useState({});
  const [catalog, setCatalog] = useState([]);
  const [picking, setPicking] = useState(false);
  const [search, setSearch] = useState('');

  useEffect(() => {
    ids.filter((id) => !products[id]).forEach((id) => {
      getProductById(id)
        .then((p) => setProducts((prev) => ({ ...prev, [id]: p })))
        .catch(() => remove(id));
    });
  }, [ids, products, remove]);

  useEffect(() => {
    if (!picking) return;
    getProducts({ size: 48, q: search || undefined, sort: 'name' })
      .then((data) => setCatalog(data.content ?? []))
      .catch(() => setCatalog([]));
  }, [picking, search]);

  const selected = ids.map((id) => products[id]).filter(Boolean);
  const slots = [...ids, ...Array(MAX_COMPARE - ids.length).fill(null)];

  return (
    <div className="min-h-screen bg-white">
      <div className="bg-navy py-16 text-center">
        <p className="text-gold tracking-[0.3em] uppercase text-xs mb-3">Side by side</p>
        <h1 className="font-serif text-4xl text-white">Compare timepieces</h1>
      </div>

      <div className="max-w-7xl mx-auto px-6 py-10">
        <div className="grid grid-cols-1 md:grid-cols-3 gap-6 mb-10">
          {slots.map((id, i) => {
            const product = id ? products[id] : null;
            return (
              <div key={id || `empty-${i}`} className="border border-gray-200 p-4 text-center relative min-h-[280px] flex flex-col items-center justify-center" data-testid="compare-slot">
                {product ? (
                  <>
                    <button onClick={() => remove(id)} aria-label={`Remove ${product.name}`} className="absolute top-2 right-2 text-gray-500 hover:text-red-700">
                      <X size={18} />
                    </button>
                    {product.imageUrl && <img src={product.imageUrl} alt={product.name} className="w-32 h-32 object-cover mb-3" />}
                    <p className="text-xs text-gold-dark uppercase tracking-wider">{product.brandName}</p>
                    <Link to={`/product/${product.id}`} className="font-serif text-base text-navy hover:underline">{product.name}</Link>
                    <p className="text-sm text-gray-700 mt-1">{money(product.price, product.currency)}</p>
                    {product.stock > 0 && (
                      <button
                        onClick={() => { addItem(product, 1); toast.success('Added to bag'); }}
                        className="mt-3 flex items-center gap-1 text-xs text-navy border border-gray-300 px-3 py-1.5 hover:border-gold-dark transition-colors"
                      >
                        <ShoppingBag size={12} /> Add to bag
                      </button>
                    )}
                  </>
                ) : id ? (
                  <span className="text-sm text-gray-500" role="status">Loading…</span>
                ) : (
                  <button onClick={() => setPicking(true)} className="flex flex-col items-center gap-2 text-gray-600 hover:text-gold-dark transition-colors">
                    <Plus size={32} />
                    <span className="text-sm">Select watch</span>
                  </button>
                )}
              </div>
            );
          })}
        </div>

        {picking && (
          <div className="fixed inset-0 bg-black/50 z-50 flex items-center justify-center p-4" onClick={() => setPicking(false)}>
            <div role="dialog" aria-label="Select a watch" className="bg-white max-w-lg w-full max-h-[70vh] overflow-hidden" onClick={(e) => e.stopPropagation()}>
              <div className="p-4 border-b border-gray-200">
                <input
                  type="text"
                  aria-label="Search watches"
                  placeholder="Search watches..."
                  value={search}
                  onChange={(e) => setSearch(e.target.value)}
                  autoFocus
                  className="w-full px-3 py-2 border border-gray-300 focus:border-gold focus:outline-none text-sm"
                />
              </div>
              <div className="overflow-y-auto max-h-[55vh] p-2">
                {catalog.filter((p) => !ids.includes(p.id)).map((p) => (
                  <button
                    key={p.id}
                    onClick={() => { add(p.id); setPicking(false); setSearch(''); }}
                    className="w-full flex items-center gap-3 p-3 hover:bg-gray-50 text-left"
                  >
                    {p.imageUrl && <img src={p.imageUrl} alt="" className="w-12 h-12 object-cover bg-cream" />}
                    <div>
                      <p className="text-xs text-gold-dark">{p.brandName}</p>
                      <p className="text-sm text-navy font-medium">{p.name}</p>
                      <p className="text-xs text-gray-600">{money(p.price, p.currency)}</p>
                    </div>
                  </button>
                ))}
              </div>
            </div>
          </div>
        )}

        {selected.length >= 2 ? (
          <div className="border border-gray-200 overflow-x-auto">
            <table className="w-full text-sm" data-testid="compare-table">
              <tbody>
                {ROWS.map(([label, getter]) => (
                  <tr key={label} className="border-b border-gray-100 last:border-0">
                    <th scope="row" className="px-4 py-3 text-left font-normal text-gray-600 text-xs uppercase tracking-wider w-40 bg-gray-50">{label}</th>
                    {selected.map((p) => <td key={p.id} className="px-4 py-3 text-navy first-letter:uppercase">{getter(p) || '—'}</td>)}
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        ) : (
          <p className="text-center text-gray-600 text-sm mt-8">Select at least two watches to compare their specifications.</p>
        )}
      </div>
    </div>
  );
}
