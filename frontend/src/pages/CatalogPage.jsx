import { useState, useEffect, useCallback } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import { Search, SlidersHorizontal, ChevronLeft, ChevronRight, X } from 'lucide-react';
import { getProducts, getBrands, getCategories } from '../api/catalog';
import { money } from '../lib/format';
import Reveal from '../components/Reveal';
import Photo from '../components/Photo';

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

  const filtersActive = brandId || categoryId || search;
  return (
    <div>
      <header className="max-w-[88rem] mx-auto px-6 lg:px-12 pt-8 pb-14">
        <p className="eyebrow mb-4">The collection</p>
        <h1 className="display text-6xl lg:text-8xl text-ink">Every <em>timepiece</em></h1>
      </header>

      <div className="max-w-[88rem] mx-auto px-6 lg:px-12 pb-28">
        <div className="flex flex-col md:flex-row gap-4 md:items-center border-y hairline py-4 mb-12">
          <div className="relative flex-1">
            <Search size={16} className="absolute left-0 top-1/2 -translate-y-1/2 text-charcoal/50" aria-hidden="true" />
            <input type="text" placeholder="Search by name, maison or reference" defaultValue={search} key={search} aria-label="Search watches"
              onKeyDown={(e) => e.key === 'Enter' && updateParam('q', e.target.value.trim())}
              className="w-full pl-7 py-2 bg-transparent text-sm focus:outline-none placeholder:text-charcoal/50" />
          </div>
          <select value={sort} onChange={(e) => updateParam('sort', e.target.value)} aria-label="Sort products"
            className="bg-transparent text-xs tracking-[0.18em] uppercase py-2 focus:outline-none cursor-pointer">
            <option value="">Sort by</option>
            <option value="newest">Newest first</option>
            <option value="price_asc">Price: low to high</option>
            <option value="price_desc">Price: high to low</option>
            <option value="name">Name</option>
          </select>
          <button onClick={() => setShowFilters(!showFilters)} className="md:hidden flex items-center gap-2 text-xs tracking-[0.18em] uppercase py-2">
            <SlidersHorizontal size={15} /> Filters
          </button>
        </div>

        <div className="grid lg:grid-cols-[13rem_1fr] gap-14">
          <aside className={`${showFilters ? 'block' : 'hidden'} lg:block`} aria-label="Filters">
            {filtersActive && (
              <button onClick={() => setSearchParams({})} className="flex items-center gap-1 text-xs tracking-[0.18em] uppercase text-gold-dark mb-8"><X size={13} /> Clear</button>
            )}
            {[['Maison', brands, brandId, 'brandId'], ['Family', categories, categoryId, 'categoryId']].map(([title, list, current, key]) => (
              <fieldset key={title} className="mb-10">
                <legend className="eyebrow mb-4">{title}</legend>
                <ul className="space-y-2.5">
                  {list.map((o) => (
                    <li key={o.id}>
                      <label className="flex items-center gap-3 cursor-pointer text-sm hover:text-gold-dark">
                        <input type="radio" name={key} checked={current === String(o.id)} onChange={() => updateParam(key, current === String(o.id) ? '' : String(o.id))} className="accent-gold-dark" />
                        {o.name}
                      </label>
                    </li>
                  ))}
                </ul>
              </fieldset>
            ))}
          </aside>

          <div>
            {loading ? (
              <p className="py-24 text-center text-sm text-charcoal/60" role="status">Loading…</p>
            ) : products.length === 0 ? (
              <div className="py-24 text-center">
                <p className="font-serif text-3xl text-ink mb-2">No watch matches</p>
                <p className="text-sm text-charcoal/70">Try another search or clear the filters.</p>
              </div>
            ) : (
              <>
                <div className="grid grid-cols-2 xl:grid-cols-3 gap-x-8 gap-y-16">
                  {products.map((p, i) => (
                    <Reveal key={p.id} delay={i % 3} className={i % 3 === 1 ? 'xl:mt-12' : ''}>
                      <Link to={`/product/${p.id}`} className="group block" data-testid="product-card">
                        <Photo src={p.imageUrl} alt={p.name} ratio="3 / 4" />
                        <div className="flex justify-between gap-4 mt-4">
                          <div>
                            <p className="eyebrow">{p.brandName}</p>
                            <h2 className="font-serif text-xl text-ink group-hover:text-gold-dark transition-colors">{p.name}</h2>
                          </div>
                          <p className="text-sm text-charcoal/80 pt-4 whitespace-nowrap">{money(p.price, p.currency)}</p>
                        </div>
                        {p.stock <= 0 && <p className="text-xs text-red-700 mt-1">Currently unavailable</p>}
                      </Link>
                    </Reveal>
                  ))}
                </div>

                {totalPages > 1 && (
                  <nav className="flex justify-center items-center gap-6 mt-20" aria-label="Pagination">
                    <button disabled={page <= 0} onClick={() => updateParam('page', String(page - 1))} aria-label="Previous page" className="p-2 disabled:opacity-30 hover:text-gold-dark"><ChevronLeft size={20} /></button>
                    <span className="text-xs tracking-[0.2em] uppercase">Page {page + 1} of {totalPages}</span>
                    <button disabled={page >= totalPages - 1} onClick={() => updateParam('page', String(page + 1))} aria-label="Next page" className="p-2 disabled:opacity-30 hover:text-gold-dark"><ChevronRight size={20} /></button>
                  </nav>
                )}
              </>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}
