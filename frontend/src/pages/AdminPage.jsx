import { useState, useEffect } from 'react';
import { getProducts, createProduct, updateProduct } from '../api/catalog';
import { useAuth } from '../context/AuthContext';
import { toast } from 'sonner';
import { Plus, Edit3, Package, X } from 'lucide-react';

const emptyProduct = { name: '', description: '', referenceNumber: '', brandId: '', categoryId: '', price: '', stock: '', caseMaterial: '', caseDiameter: '', movementType: 'AUTOMATIC', waterResistance: '', crystalType: '', dialColor: '', strapMaterial: '', powerReserve: '', imageUrl: '', featured: false, active: true };

export default function AdminPage() {
  const { user } = useAuth();
  const [products, setProducts] = useState([]);
  const [loading, setLoading] = useState(true);
  const [showForm, setShowForm] = useState(false);
  const [editing, setEditing] = useState(null);
  const [form, setForm] = useState(emptyProduct);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);

  const fetchProducts = () => {
    setLoading(true);
    getProducts({ page, size: 10 }).then(res => {
      setProducts(res.data.content || res.data || []);
      setTotalPages(res.data.totalPages || 1);
    }).catch(() => toast.error('Failed to load products')).finally(() => setLoading(false));
  };

  useEffect(fetchProducts, [page]);

  if (user?.role !== 'ADMIN') {
    return <div className="text-center py-20"><p className="text-gray-500">Access denied. Admin role required.</p></div>;
  }

  const handleChange = (e) => {
    const { name, value, type, checked } = e.target;
    setForm({ ...form, [name]: type === 'checkbox' ? checked : value });
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    try {
      const data = { ...form, price: parseFloat(form.price), stock: parseInt(form.stock) || 0, caseDiameter: form.caseDiameter ? parseFloat(form.caseDiameter) : null, brandId: form.brandId ? parseInt(form.brandId) : null, categoryId: form.categoryId ? parseInt(form.categoryId) : null };
      if (editing) {
        await updateProduct(editing, data);
        toast.success('Product updated');
      } else {
        await createProduct(data);
        toast.success('Product created');
      }
      setShowForm(false);
      setEditing(null);
      setForm(emptyProduct);
      fetchProducts();
    } catch (err) {
      toast.error(err.response?.data?.message || 'Operation failed');
    }
  };

  const startEdit = (product) => {
    setForm({ name: product.name || '', description: product.description || '', referenceNumber: product.referenceNumber || '', brandId: product.brandId || '', categoryId: product.categoryId || '', price: product.price || '', stock: product.stock || '', caseMaterial: product.caseMaterial || '', caseDiameter: product.caseDiameter || '', movementType: product.movementType || 'AUTOMATIC', waterResistance: product.waterResistance || '', crystalType: product.crystalType || '', dialColor: product.dialColor || '', strapMaterial: product.strapMaterial || '', powerReserve: product.powerReserve || '', imageUrl: product.imageUrl || '', featured: product.featured || false, active: product.active !== false });
    setEditing(product.id);
    setShowForm(true);
  };

  return (
    <div className="max-w-7xl mx-auto px-4 py-12">
      <div className="flex items-center justify-between mb-8">
        <h1 className="text-3xl text-navy">Admin Panel</h1>
        <button onClick={() => { setShowForm(true); setEditing(null); setForm(emptyProduct); }}
          className="flex items-center gap-2 px-5 py-2.5 bg-gold text-white rounded-lg hover:bg-gold-dark transition-colors">
          <Plus className="w-4 h-4" /> Add Product
        </button>
      </div>

      {showForm && (
        <div className="bg-white p-6 rounded-lg shadow-sm border border-gray-100 mb-8">
          <div className="flex justify-between items-center mb-4">
            <h2 className="text-xl font-sans font-semibold text-navy">{editing ? 'Edit Product' : 'New Product'}</h2>
            <button onClick={() => { setShowForm(false); setEditing(null); }} className="text-gray-400 hover:text-gray-600"><X className="w-5 h-5" /></button>
          </div>
          <form onSubmit={handleSubmit} className="grid md:grid-cols-3 gap-4">
            <input name="name" placeholder="Product Name *" value={form.name} onChange={handleChange} required className="px-3 py-2 border border-gray-200 rounded-lg text-sm focus:outline-none focus:border-gold" />
            <input name="referenceNumber" placeholder="Reference Number" value={form.referenceNumber} onChange={handleChange} className="px-3 py-2 border border-gray-200 rounded-lg text-sm focus:outline-none focus:border-gold" />
            <input name="price" placeholder="Price *" type="number" step="0.01" value={form.price} onChange={handleChange} required className="px-3 py-2 border border-gray-200 rounded-lg text-sm focus:outline-none focus:border-gold" />
            <input name="stock" placeholder="Stock" type="number" value={form.stock} onChange={handleChange} className="px-3 py-2 border border-gray-200 rounded-lg text-sm focus:outline-none focus:border-gold" />
            <input name="brandId" placeholder="Brand ID" type="number" value={form.brandId} onChange={handleChange} className="px-3 py-2 border border-gray-200 rounded-lg text-sm focus:outline-none focus:border-gold" />
            <input name="categoryId" placeholder="Category ID" type="number" value={form.categoryId} onChange={handleChange} className="px-3 py-2 border border-gray-200 rounded-lg text-sm focus:outline-none focus:border-gold" />
            <select name="movementType" value={form.movementType} onChange={handleChange} className="px-3 py-2 border border-gray-200 rounded-lg text-sm focus:outline-none focus:border-gold">
              <option value="AUTOMATIC">Automatic</option>
              <option value="MANUAL">Manual</option>
              <option value="QUARTZ">Quartz</option>
            </select>
            <input name="caseMaterial" placeholder="Case Material" value={form.caseMaterial} onChange={handleChange} className="px-3 py-2 border border-gray-200 rounded-lg text-sm focus:outline-none focus:border-gold" />
            <input name="caseDiameter" placeholder="Case Diameter (mm)" type="number" step="0.1" value={form.caseDiameter} onChange={handleChange} className="px-3 py-2 border border-gray-200 rounded-lg text-sm focus:outline-none focus:border-gold" />
            <input name="waterResistance" placeholder="Water Resistance" value={form.waterResistance} onChange={handleChange} className="px-3 py-2 border border-gray-200 rounded-lg text-sm focus:outline-none focus:border-gold" />
            <input name="dialColor" placeholder="Dial Color" value={form.dialColor} onChange={handleChange} className="px-3 py-2 border border-gray-200 rounded-lg text-sm focus:outline-none focus:border-gold" />
            <input name="strapMaterial" placeholder="Strap Material" value={form.strapMaterial} onChange={handleChange} className="px-3 py-2 border border-gray-200 rounded-lg text-sm focus:outline-none focus:border-gold" />
            <input name="imageUrl" placeholder="Image URL" value={form.imageUrl} onChange={handleChange} className="px-3 py-2 border border-gray-200 rounded-lg text-sm focus:outline-none focus:border-gold md:col-span-2" />
            <div className="flex items-center gap-6">
              <label className="flex items-center gap-2 text-sm"><input type="checkbox" name="featured" checked={form.featured} onChange={handleChange} className="accent-gold" /> Featured</label>
              <label className="flex items-center gap-2 text-sm"><input type="checkbox" name="active" checked={form.active} onChange={handleChange} className="accent-gold" /> Active</label>
            </div>
            <div className="md:col-span-3">
              <textarea name="description" placeholder="Description" value={form.description} onChange={handleChange} rows={3} className="w-full px-3 py-2 border border-gray-200 rounded-lg text-sm focus:outline-none focus:border-gold" />
            </div>
            <div className="md:col-span-3">
              <button type="submit" className="px-6 py-2.5 bg-navy text-white rounded-lg hover:bg-navy-light transition-colors text-sm">
                {editing ? 'Update Product' : 'Create Product'}
              </button>
            </div>
          </form>
        </div>
      )}

      {loading ? (
        <div className="flex justify-center py-12"><div className="animate-spin h-8 w-8 border-2 border-gold border-t-transparent rounded-full" /></div>
      ) : (
        <div className="bg-white rounded-lg shadow-sm border border-gray-100 overflow-hidden">
          <table className="w-full text-sm">
            <thead className="bg-navy text-white">
              <tr>
                <th className="px-4 py-3 text-left font-medium">Product</th>
                <th className="px-4 py-3 text-left font-medium">Price</th>
                <th className="px-4 py-3 text-left font-medium">Stock</th>
                <th className="px-4 py-3 text-left font-medium">Status</th>
                <th className="px-4 py-3 text-left font-medium">Actions</th>
              </tr>
            </thead>
            <tbody>
              {products.map(p => (
                <tr key={p.id} className="border-b border-gray-50 hover:bg-gray-50">
                  <td className="px-4 py-3">
                    <div className="flex items-center gap-3">
                      <div className="w-10 h-10 bg-cream rounded flex items-center justify-center">
                        {p.imageUrl ? <img src={p.imageUrl} alt="" className="w-full h-full object-cover rounded" /> : <Package className="w-5 h-5 text-gray-400" />}
                      </div>
                      <div>
                        <p className="font-medium text-navy">{p.name}</p>
                        <p className="text-xs text-gray-400">{p.referenceNumber}</p>
                      </div>
                    </div>
                  </td>
                  <td className="px-4 py-3 font-medium">${p.price?.toLocaleString()}</td>
                  <td className="px-4 py-3">
                    <span className={p.stock > 0 ? 'text-green-600' : 'text-red-600'}>{p.stock}</span>
                  </td>
                  <td className="px-4 py-3">
                    <span className={`px-2 py-1 rounded text-xs ${p.active ? 'bg-green-100 text-green-700' : 'bg-gray-100 text-gray-600'}`}>
                      {p.active ? 'Active' : 'Inactive'}
                    </span>
                    {p.featured && <span className="ml-1 px-2 py-1 rounded text-xs bg-gold/10 text-gold">Featured</span>}
                  </td>
                  <td className="px-4 py-3">
                    <button onClick={() => startEdit(p)} className="text-gold hover:text-gold-dark"><Edit3 className="w-4 h-4" /></button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
          {totalPages > 1 && (
            <div className="flex justify-center gap-2 p-4">
              {Array.from({ length: totalPages }, (_, i) => (
                <button key={i} onClick={() => setPage(i)}
                  className={`px-3 py-1 rounded text-sm ${page === i ? 'bg-navy text-white' : 'text-gray-500 hover:bg-gray-100'}`}>
                  {i + 1}
                </button>
              ))}
            </div>
          )}
        </div>
      )}
    </div>
  );
}
