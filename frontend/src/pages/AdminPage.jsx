import { useState, useEffect } from 'react';
import { Plus, Package, Layers } from 'lucide-react';
import { toast } from 'sonner';
import { getProducts, createProduct, getBrands, getCategories } from '../api/catalog';

export default function AdminPage() {
  const [products, setProducts] = useState([]);
  const [brands, setBrands] = useState([]);
  const [categories, setCategories] = useState([]);
  const [loading, setLoading] = useState(true);
  const [showForm, setShowForm] = useState(false);
  const [form, setForm] = useState({ name: '', description: '', referenceNumber: '', brandId: '', categoryId: '', price: '', stock: '10', caseMaterial: '', caseDiameter: '', movementType: 'AUTOMATIC', waterResistance: '', crystalType: '', dialColor: '', strapMaterial: '', powerReserve: '', imageUrl: '', featured: false, active: true });

  useEffect(() => {
    Promise.all([getProducts({ size: 100 }).catch(() => ({ content: [] })), getBrands().catch(() => []), getCategories().catch(() => [])])
      .then(([p, b, c]) => { setProducts(p.content ?? (Array.isArray(p) ? p : [])); setBrands(Array.isArray(b) ? b : []); setCategories(Array.isArray(c) ? c : []); setLoading(false); });
  }, []);

  const handleChange = (e) => { const { name, value, type, checked } = e.target; setForm((prev) => ({ ...prev, [name]: type === 'checkbox' ? checked : value })); };

  const handleSubmit = async (e) => {
    e.preventDefault();
    try {
      const payload = { ...form, brandId: Number(form.brandId), categoryId: Number(form.categoryId), price: parseFloat(form.price), stock: parseInt(form.stock, 10) };
      const created = await createProduct(payload);
      setProducts((prev) => [created, ...prev]);
      setShowForm(false);
      toast.success('Product created');
    } catch (err) { toast.error(err.response?.data?.message || 'Failed to create product'); }
  };

  return (
    <div className="max-w-7xl mx-auto px-6 py-12">
      <div className="flex items-center justify-between mb-8">
        <div><p className="text-[#c9a94e] text-xs tracking-[0.3em] uppercase mb-1">Management</p><h1 className="font-[Playfair_Display] text-3xl text-[#1a1a2e]">Admin Panel</h1></div>
        <button onClick={() => setShowForm(!showForm)} className="flex items-center gap-2 bg-[#1a1a2e] text-white px-5 py-2 text-sm uppercase tracking-wider hover:bg-[#2d2d44] transition-colors"><Plus size={16} /> Add Product</button>
      </div>
      <div className="grid grid-cols-1 sm:grid-cols-3 gap-4 mb-8">
        <div className="border border-gray-200 p-5"><Package className="text-[#c9a94e] mb-2" size={20} /><p className="text-2xl font-light text-[#1a1a2e]">{products.length}</p><p className="text-xs text-gray-500 uppercase tracking-wider">Products</p></div>
        <div className="border border-gray-200 p-5"><Layers className="text-[#c9a94e] mb-2" size={20} /><p className="text-2xl font-light text-[#1a1a2e]">{brands.length}</p><p className="text-xs text-gray-500 uppercase tracking-wider">Brands</p></div>
        <div className="border border-gray-200 p-5"><Layers className="text-[#c9a94e] mb-2" size={20} /><p className="text-2xl font-light text-[#1a1a2e]">{categories.length}</p><p className="text-xs text-gray-500 uppercase tracking-wider">Categories</p></div>
      </div>
      {showForm && (
        <form onSubmit={handleSubmit} className="border border-gray-200 p-6 mb-8">
          <h3 className="font-[Playfair_Display] text-xl text-[#1a1a2e] mb-4">New Product</h3>
          <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
            <input name="name" value={form.name} onChange={handleChange} placeholder="Name" required className="px-3 py-2 border border-gray-200 text-sm focus:border-[#c9a94e] focus:outline-none" />
            <input name="referenceNumber" value={form.referenceNumber} onChange={handleChange} placeholder="Ref. Number" className="px-3 py-2 border border-gray-200 text-sm focus:border-[#c9a94e] focus:outline-none" />
            <input name="price" value={form.price} onChange={handleChange} placeholder="Price" type="number" step="0.01" required className="px-3 py-2 border border-gray-200 text-sm focus:border-[#c9a94e] focus:outline-none" />
            <select name="brandId" value={form.brandId} onChange={handleChange} required className="px-3 py-2 border border-gray-200 text-sm bg-white"><option value="">Brand</option>{brands.map((b) => <option key={b.id} value={b.id}>{b.name}</option>)}</select>
            <select name="categoryId" value={form.categoryId} onChange={handleChange} required className="px-3 py-2 border border-gray-200 text-sm bg-white"><option value="">Category</option>{categories.map((c) => <option key={c.id} value={c.id}>{c.name}</option>)}</select>
            <input name="stock" value={form.stock} onChange={handleChange} placeholder="Stock" type="number" className="px-3 py-2 border border-gray-200 text-sm" />
            <select name="movementType" value={form.movementType} onChange={handleChange} className="px-3 py-2 border border-gray-200 text-sm bg-white"><option value="AUTOMATIC">Automatic</option><option value="MANUAL">Manual</option><option value="QUARTZ">Quartz</option></select>
            <input name="caseMaterial" value={form.caseMaterial} onChange={handleChange} placeholder="Case Material" className="px-3 py-2 border border-gray-200 text-sm" />
            <input name="imageUrl" value={form.imageUrl} onChange={handleChange} placeholder="Image URL" className="px-3 py-2 border border-gray-200 text-sm" />
          </div>
          <textarea name="description" value={form.description} onChange={handleChange} placeholder="Description" rows={3} className="w-full mt-4 px-3 py-2 border border-gray-200 text-sm" />
          <div className="flex items-center gap-6 mt-4">
            <label className="flex items-center gap-2 text-sm"><input type="checkbox" name="featured" checked={form.featured} onChange={handleChange} className="accent-[#c9a94e]" /> Featured</label>
            <label className="flex items-center gap-2 text-sm"><input type="checkbox" name="active" checked={form.active} onChange={handleChange} className="accent-[#c9a94e]" /> Active</label>
          </div>
          <div className="flex gap-3 mt-6">
            <button type="submit" className="bg-[#c9a94e] text-[#1a1a2e] px-6 py-2 text-sm uppercase tracking-wider font-medium hover:bg-[#d4b86a] transition-colors">Create</button>
            <button type="button" onClick={() => setShowForm(false)} className="text-sm text-gray-500">Cancel</button>
          </div>
        </form>
      )}
      {loading ? (
        <div className="flex justify-center py-20"><div className="w-10 h-10 border-2 border-[#c9a94e] border-t-transparent rounded-full animate-spin" /></div>
      ) : (
        <div className="border border-gray-200 overflow-x-auto">
          <table className="w-full text-sm">
            <thead><tr className="bg-gray-50 text-left text-xs uppercase tracking-wider text-gray-500"><th className="px-4 py-3">Product</th><th className="px-4 py-3">Brand</th><th className="px-4 py-3">Price</th><th className="px-4 py-3">Stock</th><th className="px-4 py-3">Status</th></tr></thead>
            <tbody>
              {products.map((p) => (
                <tr key={p.id} className="border-t border-gray-100 hover:bg-gray-50">
                  <td className="px-4 py-3 flex items-center gap-3">
                    <img src={p.imageUrl || 'https://placehold.co/40x40/1a1a2e/c9a94e?text=W'} alt={p.name} className="w-10 h-10 object-cover bg-[#f5f0e8]" />
                    <div><p className="font-medium text-[#1a1a2e]">{p.name}</p><p className="text-xs text-gray-400">{p.referenceNumber}</p></div>
                  </td>
                  <td className="px-4 py-3 text-gray-600">{p.brandName}</td>
                  <td className="px-4 py-3">${Number(p.price).toLocaleString()}</td>
                  <td className="px-4 py-3"><span className={p.stock <= 3 ? 'text-red-500 font-medium' : ''}>{p.stock}</span></td>
                  <td className="px-4 py-3"><span className={`text-xs px-2 py-0.5 rounded ${p.active ? 'bg-green-100 text-green-700' : 'bg-gray-100 text-gray-500'}`}>{p.active ? 'Active' : 'Inactive'}</span></td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}
