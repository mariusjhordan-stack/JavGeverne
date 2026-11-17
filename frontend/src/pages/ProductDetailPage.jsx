import { useState, useEffect } from 'react';
import { useParams, Link } from 'react-router-dom';
import { Plus, Minus, Heart, BarChart3, X } from 'lucide-react';
import { toast } from 'sonner';
import { getProductById, getProducts } from '../api/catalog';
import { addFavorite, removeFavorite, listFavorites } from '../api/favorites';
import { useCart } from '../context/CartContext';
import { useCompare } from '../context/CompareContext';
import { useAuth } from '../context/AuthContext';
import { money, withUnit } from '../lib/format';
import Reveal from '../components/Reveal';
import Photo from '../components/Photo';

export default function ProductDetailPage() {
  const { id } = useParams();
  const { addItem } = useCart();
  const compare = useCompare();
  const { isAuthenticated } = useAuth();
  const [product, setProduct] = useState(null);
  const [related, setRelated] = useState([]);
  const [state, setState] = useState('loading');
  const [quantity, setQuantity] = useState(1);
  const [active, setActive] = useState(0);
  const [zoom, setZoom] = useState(false);
  const [favorite, setFavorite] = useState(false);

  useEffect(() => {
    let cancelled = false;
    getProductById(id)
      .then((p) => {
        if (cancelled) return;
        setProduct(p); setState('ready'); setQuantity(1); setActive(0);
        getProducts({ categoryId: p.categoryId, size: 5 }).then((d) => {
          if (!cancelled) setRelated((d.content ?? []).filter((x) => x.id !== p.id).slice(0, 4));
        }).catch(() => {});
      })
      .catch(() => !cancelled && setState('missing'));
    return () => { cancelled = true; };
  }, [id]);

  useEffect(() => {
    if (!isAuthenticated) { setFavorite(false); return; }
    listFavorites().then((list) => setFavorite(list.some((f) => f.id === id))).catch(() => {});
  }, [id, isAuthenticated]);

  useEffect(() => {
    if (!zoom) return undefined;
    const onKey = (e) => e.key === 'Escape' && setZoom(false);
    window.addEventListener('keydown', onKey);
    return () => window.removeEventListener('keydown', onKey);
  }, [zoom]);

  if (state === 'loading') return <div className="py-40 text-center text-sm text-charcoal/60" role="status">Loading…</div>;
  if (state === 'missing') {
    return (
      <div className="max-w-xl mx-auto px-6 py-32 text-center">
        <h1 className="font-serif text-4xl text-ink mb-6">This watch is no longer available</h1>
        <Link to="/collection" className="btn-line text-ink">Back to the collection</Link>
      </div>
    );
  }

  const images = [product.imageUrl, ...(product.gallery || [])].filter(Boolean);
  const inStock = product.stock > 0;
  const specs = [
    ['Movement', product.movementType?.toLowerCase()],
    ['Case', product.caseMaterial],
    ['Diameter', withUnit(product.caseDiameter, 'mm')],
    ['Water resistance', product.waterResistance],
    ['Crystal', product.crystalType],
    ['Dial', product.dialColor],
    ['Strap', product.strapMaterial],
    ['Power reserve', product.powerReserve],
    ['Reference', product.referenceNumber],
  ].filter(([, v]) => v);

  const toggleFavorite = async () => {
    if (!isAuthenticated) { toast.info('Sign in to keep favourites'); return; }
    try {
      if (favorite) await removeFavorite(product.id); else await addFavorite(product.id);
      setFavorite(!favorite);
    } catch { toast.error('Could not update favourites'); }
  };

  return (
    <article className="max-w-[88rem] mx-auto px-6 lg:px-12 pt-6 pb-28">
      <nav className="text-xs tracking-[0.18em] uppercase text-charcoal/60 mb-10" aria-label="Breadcrumb">
        <Link to="/collection" className="hover:text-gold-dark">Collection</Link> <span aria-hidden="true">/</span>{' '}
        <Link to={`/maisons/${product.brandSlug}`} className="hover:text-gold-dark">{product.brandName}</Link>
      </nav>

      <div className="grid lg:grid-cols-12 gap-12 lg:gap-20">
        {/* Gallery */}
        <div className="lg:col-span-7">
          <div className="grid grid-cols-[4.5rem_1fr] gap-4">
            <div className="flex flex-col gap-3" role="tablist" aria-label="Photographs">
              {images.map((src, i) => (
                <button key={src} role="tab" aria-selected={i === active} aria-label={`Photograph ${i + 1}`} onClick={() => setActive(i)}
                  className={`aspect-[3/4] overflow-hidden border transition-opacity ${i === active ? 'border-ink' : 'border-transparent opacity-60 hover:opacity-100'}`}>
                  <img src={src} alt="" loading="lazy" className="w-full h-full object-cover" />
                </button>
              ))}
            </div>
            <button onClick={() => images.length && setZoom(true)} className="block cursor-zoom-in text-left" aria-label="Enlarge photograph">
              <Photo src={images[active]} alt={product.name} ratio="4 / 5" eager className="w-full" />
            </button>
          </div>
        </div>

        {/* Details */}
        <div className="lg:col-span-5 lg:sticky lg:top-28 self-start">
          <p className="eyebrow mb-4">{product.brandName}</p>
          <h1 className="display text-5xl lg:text-6xl text-ink mb-3">{product.name}</h1>
          {product.referenceNumber && <p className="text-xs tracking-[0.2em] uppercase text-charcoal/60 mb-8">Ref. {product.referenceNumber}</p>}
          <p className="text-3xl text-ink mb-8" data-testid="product-price">{money(product.price, product.currency)}</p>
          {product.description && <p className="text-charcoal/80 leading-relaxed mb-8">{product.description}</p>}

          <p className="flex items-center gap-2 text-sm mb-6" data-testid="availability">
            <span className={`w-2 h-2 rounded-full ${inStock ? 'bg-green-700' : 'bg-red-700'}`} aria-hidden="true" />
            {inStock ? (product.stock <= 3 ? `Only ${product.stock} remaining` : 'Available') : 'Currently unavailable'}
          </p>

          {inStock && (
            <div className="flex items-stretch gap-3 mb-6">
              <div className="flex items-center border hairline">
                <button aria-label="Decrease quantity" onClick={() => setQuantity(Math.max(1, quantity - 1))} className="px-3 h-full hover:bg-cream"><Minus size={14} /></button>
                <span className="w-10 text-center text-sm" aria-live="polite">{quantity}</span>
                <button aria-label="Increase quantity" onClick={() => setQuantity(Math.min(product.stock, quantity + 1))} className="px-3 h-full hover:bg-cream"><Plus size={14} /></button>
              </div>
              <button onClick={() => { addItem(product, quantity); toast.success(`${product.name} added to your bag`); }} className="btn-solid flex-1">
                Add to bag
              </button>
            </div>
          )}

          <div className="flex gap-8 text-xs tracking-[0.18em] uppercase mb-12">
            <button onClick={toggleFavorite} aria-pressed={favorite} className="flex items-center gap-2 hover:text-gold-dark transition-colors">
              <Heart size={15} className={favorite ? 'fill-gold-dark text-gold-dark' : ''} /> {favorite ? 'Saved' : 'Save'}
            </button>
            <button onClick={() => {
              const r = compare.add(product.id);
              if (r === 'exists') toast.info('Already in comparison'); else if (r === 'full') toast.error('You can compare up to 3 watches'); else toast.success('Added to comparison');
            }} className="flex items-center gap-2 hover:text-gold-dark transition-colors"><BarChart3 size={15} /> Compare</button>
          </div>

          <p className="text-xs text-charcoal/55 mb-10">Photographs are representative of the model and may show another version of it.</p>

          {specs.length > 0 && (
            <dl className="border-t hairline">
              {specs.map(([label, value]) => (
                <div key={label} className="flex justify-between gap-6 py-3.5 border-b hairline text-sm">
                  <dt className="text-charcoal/60">{label}</dt>
                  <dd className="text-ink text-right first-letter:uppercase">{value}</dd>
                </div>
              ))}
            </dl>
          )}
        </div>
      </div>

      {related.length > 0 && (
        <section className="mt-32" aria-labelledby="related-title">
          <Reveal><h2 id="related-title" className="display text-4xl lg:text-5xl text-ink mb-12">Also in this <em>family</em></h2></Reveal>
          <div className="grid grid-cols-2 lg:grid-cols-4 gap-x-8 gap-y-14">
            {related.map((p, i) => (
              <Reveal key={p.id} delay={i % 3}>
                <Link to={`/product/${p.id}`} className="group block">
                  <Photo src={p.imageUrl} alt={p.name} ratio="3 / 4" />
                  <p className="eyebrow mt-4">{p.brandName}</p>
                  <h3 className="font-serif text-xl text-ink group-hover:text-gold-dark transition-colors">{p.name}</h3>
                  <p className="text-sm text-charcoal/75">{money(p.price, p.currency)}</p>
                </Link>
              </Reveal>
            ))}
          </div>
        </section>
      )}

      {zoom && (
        <div role="dialog" aria-modal="true" aria-label="Enlarged photograph" className="fixed inset-0 z-[70] bg-ink/95 flex items-center justify-center p-6" onClick={() => setZoom(false)}>
          <button aria-label="Close" className="absolute top-6 right-6 text-white" onClick={() => setZoom(false)}><X size={28} strokeWidth={1.3} /></button>
          <img src={images[active]} alt={product.name} className="max-h-full max-w-full object-contain" onClick={(e) => e.stopPropagation()} />
        </div>
      )}
    </article>
  );
}
