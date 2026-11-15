import { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { Plus, X, ShoppingBag } from 'lucide-react';
import { toast } from 'sonner';
import { getProducts, getProductById } from '../api/catalog';
import { useCart } from '../context/CartContext';

export default function ComparePage() {
  const { addItem } = useCart();
  const [slots, setSlots] = useState([null, null, null]);
  const [catalog, setCatalog] = useState([]);
  const [picking, setPicking] = useState(null);
  const [search, setSearch] = useState('');

  useEffect(() => {
    getProducts({ size: 50 })
      .then((data) => setCatalog(data.content ?? (Array.isArray(data) ? data : [])))
      .catch(() => {});
  }, []);

  const pickProduct = async (product) => {
    if (picking === null) return;
    try {
      const full = await getProductById(product.id);
      const next = [...slots];
      next[picking] = full;
      setSlots(next);
    } catch {
      toast.error('Failed to load product details');
    }
    setPicking(null);
    setSearch('');
  };

  const clearSlot = (i) => {
    const next = [...slots];
    next[i] = null;
    setSlots(next);
  };

  const active = slots.filter(Boolean);
  const filtered = catalog.filter(
    (p) => p.name.toLowerCase().includes(search.toLowerCase()) && !slots.find((s) => s?.id === p.id)
  );

  const specRows = [
    ['Brand', (p) => p.brandName],
    ['Reference', (p) => p.referenceNumber],
    ['Price', (p) => `$${Number(p.price).toLocaleString()}`],
    ['Movement', (p) => p.movementType],
    ['Case Material', (p) => p.caseMaterial],
    ['Case Diameter', (p) => p.caseDiameter],
    ['Water Resistance', (p) => p.waterResistance],
    ['Crystal', (p) => p.crystalType],
    ['Dial Color', (p) => p.dialColor],
    ['Strap', (p) => p.strapMaterial],
    ['Power Reserve', (p) => p.powerReserve],
  ];

  return (
    <div className="min-h-screen bg-white">
      <div className="bg-[#1a1a2e] py-16 text-center">
        <p className="text-[#c9a94e] tracking-[0.3em] uppercase text-xs mb-3">Side by Side</p>
        <h1 className="font-[Playfair_Display] text-4xl text-white">Compare Timepieces</h1>
      </div>

      <div className="max-w-7xl mx-auto px-6 py-10">
        {/* Slots */}
        <div className="grid grid-cols-1 md:grid-cols-3 gap-6 mb-10">
          {slots.map((product, i) => (
            <div key={i} className="border border-gray-200 p-4 text-center relative min-h-[280px] flex flex-col items-center justify-center">
              {product ? (
                <>
                  <button onClick={() => clearSlot(i)} className="absolute top-2 right-2 text-gray-400 hover:text-red-500">
                    <X size={18} />
                  </button>
                  <img
                    src={product.imageUrl || 'https://placehold.co/300x300/1a1a2e/c9a94e?text=Watch'}
                    alt={product.name}
                    className="w-32 h-32 object-cover mb-3"
                  />
                  <p className="text-xs text-[#c9a94e] uppercase tracking-wider">{product.brandName}</p>
                  <h3 className="font-[Playfair_Display] text-base text-[#1a1a2e]">{product.name}</h3>
                  <p className="text-sm text-gray-600 mt-1">${Number(product.price).toLocaleString()}</p>
                  <button
                    onClick={() => { addItem(product, 1); toast.success('Added to bag'); }}
                    className="mt-3 flex items-center gap-1 text-xs text-[#1a1a2e] border border-gray-300 px-3 py-1.5 hover:border-[#c9a94e] transition-colors"
                  >
                    <ShoppingBag size={12} /> Add to Bag
                  </button>
                </>
              ) : (
                <button
                  onClick={() => setPicking(i)}
                  className="flex flex-col items-center gap-2 text-gray-400 hover:text-[#c9a94e] transition-colors"
                >
                  <Plus size={32} />
                  <span className="text-sm">Select Watch</span>
                </button>
              )}
            </div>
          ))}
        </div>

        {/* Picker modal */}
        {picking !== null && (
          <div className="fixed inset-0 bg-black/50 z-50 flex items-center justify-center p-4" onClick={() => setPicking(null)}>
            <div className="bg-white rounded-lg max-w-lg w-full max-h-[70vh] overflow-hidden" onClick={(e) => e.stopPropagation()}>
              <div className="p-4 border-b">
                <input
                  type="text"
                  placeholder="Search watches..."
                  value={search}
                  onChange={(e) => setSearch(e.target.value)}
                  autoFocus
                  className="w-full px-3 py-2 border border-gray-200 focus:border-[#c9a94e] focus:outline-none text-sm"
                />
              </div>
              <div className="overflow-y-auto max-h-[55vh] p-2">
                {filtered.length === 0 ? (
                  <p className="p-4 text-sm text-gray-500 text-center">No watches found</p>
                ) : (
                  filtered.map((p) => (
                    <button
                      key={p.id}
                      onClick={() => pickProduct(p)}
                      className="w-full flex items-center gap-3 p-3 hover:bg-gray-50 text-left rounded"
                    >
                      <img
                        src={p.imageUrl || 'https://placehold.co/80x80/1a1a2e/c9a94e?text=W'}
                        alt={p.name}
                        className="w-12 h-12 object-cover bg-[#f5f0e8]"
                      />
                      <div>
                        <p className="text-xs text-[#c9a94e]">{p.brandName}</p>
                        <p className="text-sm text-[#1a1a2e] font-medium">{p.name}</p>
                        <p className="text-xs text-gray-500">${Number(p.price).toLocaleString()}</p>
                      </div>
                    </button>
                  ))
                )}
              </div>
            </div>
          </div>
        )}

        {/* Comparison Table */}
        {active.length >= 2 && (
          <div className="border border-gray-200 overflow-x-auto">
            <table className="w-full text-sm">
              <tbody>
                {specRows.map(([label, getter]) => (
                  <tr key={label} className="border-b border-gray-100">
                    <td className="px-4 py-3 text-gray-400 text-xs uppercase tracking-wider w-40 bg-gray-50">{label}</td>
                    {slots.map((p, i) => (
                      <td key={i} className="px-4 py-3 text-[#1a1a2e]">
                        {p ? (getter(p) || '—') : ''}
                      </td>
                    ))}
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}

        {active.length < 2 && (
          <p className="text-center text-gray-500 text-sm mt-8">
            Select at least two watches to compare their specifications.
          </p>
        )}
      </div>
    </div>
  );
}
