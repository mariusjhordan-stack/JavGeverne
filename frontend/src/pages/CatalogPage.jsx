import { useState, useEffect, useCallback, useRef } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import { Search, SlidersHorizontal, ChevronLeft, ChevronRight, ChevronDown, Check, X } from 'lucide-react';
import { getProducts, getBrands, getCategories } from '../api/catalog';
import { money } from '../lib/format';
import { useLanguage } from '../context/LanguageContext';
import Reveal from '../components/Reveal';
import Photo from '../components/Photo';

const SORT_OPTIONS = [
  ['', 'Sort by'],
  ['newest', 'Newest first'],
  ['price_asc', 'Price: low to high'],
  ['price_desc', 'Price: high to low'],
  ['name', 'Name'],
];

export default function CatalogPage() {
  const { t } = useLanguage();
  const [searchParams, setSearchParams] = useSearchParams();
  const [products, setProducts] = useState([]);
  const [brands, setBrands] = useState([]);
  const [categories, setCategories] = useState([]);
  const [loading, setLoading] = useState(true);
  const [totalPages, setTotalPages] = useState(0);
  const [showFilters, setShowFilters] = useState(false);
  const [sortOpen, setSortOpen] = useState(false);
  const sortMenuRef = useRef(null);
  const sortButtonRef = useRef(null);

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

  useEffect(() => {
    if (!sortOpen) return undefined;
    const closeOnOutsideClick = (event) => {
      if (!sortMenuRef.current?.contains(event.target)) setSortOpen(false);
    };
    const closeOnEscape = (event) => {
      if (event.key === 'Escape') {
        setSortOpen(false);
        sortButtonRef.current?.focus();
      }
    };
    document.addEventListener('pointerdown', closeOnOutsideClick);
    document.addEventListener('keydown', closeOnEscape);
    return () => {
      document.removeEventListener('pointerdown', closeOnOutsideClick);
      document.removeEventListener('keydown', closeOnEscape);
    };
  }, [sortOpen]);

  const filtersActive = brandId || categoryId || search;
  const selectedSortLabel = SORT_OPTIONS.find(([value]) => value === sort)?.[1] || 'Sort by';
  return (
    <div>
      <header className="max-w-[88rem] mx-auto px-6 lg:px-12 pt-8 pb-14">
        <p className="eyebrow mb-4">{t('The collection')}</p>
        <h1 className="display text-6xl lg:text-8xl text-ink">{t('Every timepiece')}</h1>
      </header>

      <div className="max-w-[88rem] mx-auto px-6 lg:px-12 pb-28">
        <div className="flex flex-col md:flex-row gap-4 md:items-center border-y hairline py-4 mb-12">
          <div className="relative flex-1">
            <Search size={16} className="absolute left-0 top-1/2 -translate-y-1/2 text-charcoal/50" aria-hidden="true" />
            <input type="text" placeholder={t('Search by name, maison or reference')} defaultValue={search} key={search} aria-label={t('Search watches')}
              onKeyDown={(e) => e.key === 'Enter' && updateParam('q', e.target.value.trim())}
              className="w-full pl-7 py-2 bg-transparent text-sm focus:outline-none placeholder:text-charcoal/50" />
          </div>
          <div className="relative w-full shrink-0 md:w-56" ref={sortMenuRef}>
            <button
              ref={sortButtonRef}
              type="button"
              aria-label={t('Sort products')}
              aria-expanded={sortOpen}
              aria-controls="catalog-sort-options"
              onClick={() => setSortOpen(!sortOpen)}
              className="flex w-full items-center justify-between border-b hairline py-3 text-left text-xs tracking-[0.18em] uppercase transition-colors hover:border-gold focus-visible:border-gold focus-visible:outline-none"
            >
              <span>{t(selectedSortLabel)}</span>
              <ChevronDown className={`ml-3 h-3.5 w-3.5 shrink-0 transition-transform ${sortOpen ? 'rotate-180' : ''}`} aria-hidden="true" />
            </button>
            {sortOpen && (
              <div id="catalog-sort-options" role="group" aria-label={t('Sort products')} className="absolute right-0 top-full z-[60] mt-2 w-full border hairline bg-bone py-1 text-ink shadow-xl md:w-64">
                {SORT_OPTIONS.map(([value, label]) => (
                  <button
                    key={value || 'default'}
                    type="button"
                    aria-pressed={sort === value}
                    onClick={() => { updateParam('sort', value); setSortOpen(false); }}
                    className={`flex w-full items-center justify-between px-4 py-3 text-left text-xs tracking-wide transition-colors hover:bg-cream ${sort === value ? 'text-gold-dark' : 'text-ink'}`}
                  >
                    {t(label)}
                    {sort === value && <Check className="ml-3 h-3.5 w-3.5 shrink-0" aria-hidden="true" />}
                  </button>
                ))}
              </div>
            )}
          </div>
          <button onClick={() => setShowFilters(!showFilters)} className="md:hidden flex items-center gap-2 text-xs tracking-[0.18em] uppercase py-2">
            <SlidersHorizontal size={15} /> {t('Filters')}
          </button>
        </div>

        <div className="grid lg:grid-cols-[13rem_1fr] gap-14">
          <aside className={`${showFilters ? 'block' : 'hidden'} lg:block`} aria-label={t('Filters')}>
            {filtersActive && (
              <button onClick={() => setSearchParams({})} className="flex items-center gap-1 text-xs tracking-[0.18em] uppercase text-gold-dark mb-8"><X size={13} /> {t('Clear')}</button>
            )}
            {[['Maison', brands, brandId, 'brandId'], ['Family', categories, categoryId, 'categoryId']].map(([title, list, current, key]) => (
              <fieldset key={title} className="mb-10">
                <legend className="eyebrow mb-4">{t(title)}</legend>
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
              <p className="py-24 text-center text-sm text-charcoal/60" role="status">{t('Loading…')}</p>
            ) : products.length === 0 ? (
              <div className="py-24 text-center">
                <p className="font-serif text-3xl text-ink mb-2">{t('No watch matches')}</p>
                <p className="text-sm text-charcoal/70">{t('Try another search or clear the filters.')}</p>
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
                        {p.stock <= 0 && <p className="text-xs text-red-700 mt-1">{t('Currently unavailable')}</p>}
                      </Link>
                    </Reveal>
                  ))}
                </div>

                {totalPages > 1 && (
                  <nav className="flex justify-center items-center gap-6 mt-20" aria-label={t('Pagination')}>
                    <button disabled={page <= 0} onClick={() => updateParam('page', String(page - 1))} aria-label={t('Previous page')} className="p-2 disabled:opacity-30 hover:text-gold-dark"><ChevronLeft size={20} /></button>
                    <span className="text-xs tracking-[0.2em] uppercase">{t('Page')} {page + 1} {t('of')} {totalPages}</span>
                    <button disabled={page >= totalPages - 1} onClick={() => updateParam('page', String(page + 1))} aria-label={t('Next page')} className="p-2 disabled:opacity-30 hover:text-gold-dark"><ChevronRight size={20} /></button>
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
