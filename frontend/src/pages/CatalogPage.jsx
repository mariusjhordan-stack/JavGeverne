import { useState, useEffect, useCallback } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import { Search, SlidersHorizontal, ChevronLeft, ChevronRight, X } from 'lucide-react';
import { getProducts, getBrands, getCategories } from '../api/catalog';

export default function CatalogPage() {
  const [searchParams, setSearchParams] = useSearchParams();
  const [products, setProducts] = useState([]);
  const [brands, setBrands] = useState([]);
  const [categories, setCategories] = useState([]);
  const [loading, setLoading] = useState(true);
  const [totalPages, setTotalPages] = useState(0);
  const [showFilters, setShowFilters] = useState(false);

  const page = parseInt(searchParams.get('page') || '0');
  const search = searchParams.get('q') || '';
  const brandId = searchParams.get('brandId') || '';
  const categoryId = searchParams.get('categoryId') || '';
  const sort = searchParams.get('sort') || '';

  const updateParam = useCallback(
    (key, value) => {
      const next = new URLSearchParams(searchParams);
      if (value) next.set(key, value);
      else next.delete(key);
      if (key !== 'page') next.delete('page');
      setSearchParams(next);
    },
    [searchParams, setSearchParams]
  );

  useEffect(() => {
    Promise.all([getBrands().catch(() => []), getCategories().catch(() => [])]).then(([b, c]) => {
      setBrands(Array.isArray(b) ? b : []);
      setCategories(Array.isArray(c) ? c : []);
    });
  }, []);

  useEffect(() => {
    setLoading(true);
    const params = { page, size: 12 };
    if (search) params.q = search;
    if (brandId) params.brandId = brandId;
    if (categoryId) params.categoryId = categoryId;
    if (sort) params.sort = sort;
    getProducts(params)
      .then((data) => {
        setProducts(data.content ?? (Array.isArray(data) ? data : []));
        setTotalPages(data.totalPages ?? 1);
      })
      .catch(() => setProducts([]))
      .finally(() => setLoading(false));
  }, [page, search, brandId, categoryId, sort]);

  return (
    <div className="min-h-screen bg-white">
      {/* Header */}
      <div className="bg-navy py-16 text-center">
        <p className="text-gold tracking-[0.3em] uppercase text-xs mb-3">Discover</p>
        <h1 className="font-serif text-4xl md:text-5xl text-white">Our Collection</h1>
      </div>

      <div className="max-w-7xl mx-auto px-6 py-10">
        {/* Search & Controls */}
        <div className="flex flex-col md:flex-row gap-4 items-center mb-8">
          <div className="relative flex-1 w-full">
            <Search size={18} className="absolute left-4 top-1/2 -translate-y-1/2 text-gray-400" />
            <input
              type="text"
              placeholder="Search watches..."
              defaultValue={search}
              key={search}
              aria-label="Search watches"
              onKeyDown={(e) => e.key === 'Enter' && updateParam('q', e.target.value.trim())}
              className="w-full pl-12 pr-4 py-3 border border-gray-200 focus:border-gold focus:outline-none text-sm"
            />
          </div>
          <select
            value={sort}
            aria-label="Sort products"
            onChange={(e) => updateParam('sort', e.target.value)}
            className="px-4 py-3 border border-gray-200 text-sm focus:outline-none focus:border-gold bg-white"
          >
            <option value="">Sort by</option>
            <option value="price_asc">Price: Low to High</option>
            <option value="price_desc">Price: High to Low</option>
            <option value="newest">Newest First</option>
            <option value="name">Name</option>
          </select>
          <button
            onClick={() => setShowFilters(!showFilters)}
            className="md:hidden flex items-center gap-2 px-4 py-3 border border-gray-200 text-sm"
          >
            <SlidersHorizontal size={16} /> Filters
          </button>
        </div>

        <div className="flex gap-10">
          {/* Sidebar Filters */}
          <aside className={`${showFilters ? 'block' : 'hidden'} md:block w-64 shrink-0`}>
            {(brandId || categoryId || search) && (
              <button
                onClick={() => setSearchParams({})}
                className="flex items-center gap-1 text-sm text-gold mb-4 hover:underline"
              >
                <X size={14} /> Clear filters
              </button>
            )}
            <div className="mb-8">
              <h3 className="font-serif text-lg text-navy mb-4">Brand</h3>
              {brands.map((b) => (
                <label key={b.id} className="flex items-center gap-2 mb-2 cursor-pointer text-sm">
                  <input
                    type="radio"
                    name="brand"
                    checked={brandId === String(b.id)}
                    onChange={() => updateParam('brandId', brandId === String(b.id) ? '' : String(b.id))}
                    className="accent-gold"
                  />
                  {b.name}
                </label>
              ))}
            </div>
            <div className="mb-8">
              <h3 className="font-serif text-lg text-navy mb-4">Category</h3>
              {categories.map((c) => (
                <label key={c.id} className="flex items-center gap-2 mb-2 cursor-pointer text-sm">
                  <input
                    type="radio"
                    name="category"
                    checked={categoryId === String(c.id)}
                    onChange={() => updateParam('categoryId', categoryId === String(c.id) ? '' : String(c.id))}
                    className="accent-gold"
                  />
                  {c.name}
                </label>
              ))}
            </div>
          </aside>

          {/* Product Grid */}
          <div className="flex-1">
            {loading ? (
              <div className="flex justify-center py-20">
                <div className="w-10 h-10 border-2 border-gold border-t-transparent rounded-full animate-spin" />
              </div>
            ) : products.length === 0 ? (
              <div className="text-center py-20 text-gray-500">
                <p className="text-lg mb-2">No watches found</p>
                <p className="text-sm">Try adjusting your filters or search terms.</p>
              </div>
            ) : (
              <>
                <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-8">
                  {products.map((p) => (
                    <Link key={p.id} to={`/product/${p.id}`} className="group block" data-testid="product-card">
                      <div className="aspect-square bg-cream overflow-hidden mb-3">
                        <img
                          src={p.imageUrl || 'https://placehold.co/600x600/1a1a2e/c9a94e?text=Watch'}
                          alt={p.name}
                          className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-500"
                        />
                      </div>
                      <p className="text-xs text-gold tracking-[0.15em] uppercase">{p.brandName}</p>
                      <h3 className="font-serif text-base text-navy group-hover:text-gold transition-colors mt-1">{p.name}</h3>
                      <p className="text-sm text-charcoal mt-1">${Number(p.price).toLocaleString()}</p>
                      {p.stock !== undefined && p.stock <= 0 && (
                        <p className="text-xs text-red-500 mt-1">Out of stock</p>
                      )}
                    </Link>
                  ))}
                </div>

                {/* Pagination */}
                {totalPages > 1 && (
                  <div className="flex justify-center items-center gap-4 mt-14">
                    <button
                      disabled={page <= 0}
                      onClick={() => updateParam('page', String(page - 1))}
                      className="p-2 border border-gray-200 disabled:opacity-30 hover:border-gold transition-colors"
                    >
                      <ChevronLeft size={18} />
                    </button>
                    <span className="text-sm text-gray-600">
                      Page {page + 1} of {totalPages}
                    </span>
                    <button
                      disabled={page >= totalPages - 1}
                      onClick={() => updateParam('page', String(page + 1))}
                      className="p-2 border border-gray-200 disabled:opacity-30 hover:border-gold transition-colors"
                    >
                      <ChevronRight size={18} />
                    </button>
                  </div>
                )}
              </>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}
