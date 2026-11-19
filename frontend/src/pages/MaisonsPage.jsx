import { useState, useEffect } from 'react';
import { Link, useParams } from 'react-router-dom';
import { ArrowRight } from 'lucide-react';
import { getBrands, getBrand, getProducts } from '../api/catalog';
import PageHero from '../components/PageHero';
import Reveal from '../components/Reveal';
import Photo from '../components/Photo';
import { money } from '../lib/format';
import { EDITORIAL } from '../lib/images';
import { useLanguage } from '../context/LanguageContext';

const NOTICE = 'Independent academic demonstration. Cronos Genève is not affiliated with, authorised by or sponsored by this maison, and sells no real products.';

export function MaisonsIndex() {
  const { t } = useLanguage();
  const [brands, setBrands] = useState([]);
  useEffect(() => { getBrands().then((b) => setBrands(Array.isArray(b) ? b : [])).catch(() => {}); }, []);
  return (
    <div>
      <PageHero eyebrow="Maisons" title={<>{t('The houses behind the')} <em className="text-gold">{t('hands')}</em></>} intro={t(NOTICE)} image={EDITORIAL.hero} />
      <section className="max-w-[88rem] mx-auto px-6 lg:px-12 py-24 lg:py-36">
        <ul className="divide-y hairline border-y hairline">
          {brands.map((b) => (
            <Reveal as="li" key={b.id}>
              <Link to={`/maisons/${b.slug}`} className="group grid lg:grid-cols-12 gap-6 items-baseline py-12">
                <span className="lg:col-span-5 font-serif text-5xl lg:text-7xl text-ink group-hover:text-gold-dark group-hover:translate-x-3 transition-all duration-500">{b.name}</span>
                <span className="lg:col-span-5 text-charcoal/75 leading-relaxed">{t(b.description)}</span>
                <span className="lg:col-span-2 text-xs tracking-[0.2em] uppercase text-charcoal/60 lg:text-right">{b.country}</span>
              </Link>
            </Reveal>
          ))}
        </ul>
      </section>
    </div>
  );
}

export function MaisonDetail() {
  const { t } = useLanguage();
  const { slug } = useParams();
  const [brand, setBrand] = useState(null);
  const [products, setProducts] = useState([]);
  const [missing, setMissing] = useState(false);

  useEffect(() => {
    getBrand(slug).then((b) => {
      setBrand(b);
      return getProducts({ brandId: b.id, size: 24 });
    }).then((d) => setProducts(d.content ?? [])).catch(() => setMissing(true));
  }, [slug]);

  if (missing) return <div className="pt-40 pb-32 text-center"><p className="font-serif text-3xl mb-6">{t('Maison not found')}</p><Link to="/maisons" className="btn-line">{t('All maisons')}</Link></div>;
  if (!brand) return <div className="pt-40 pb-32 text-center" role="status">{t('Loading…')}</div>;

  return (
    <div>
      <PageHero eyebrow={brand.country} title={brand.name} intro={t(brand.description)} image={brand.imageUrl || EDITORIAL.hero} />
      <section className="max-w-[88rem] mx-auto px-6 lg:px-12 py-24 lg:py-32">
        <Reveal className="max-w-2xl mb-20">
          {brand.story && <p className="font-serif text-2xl lg:text-3xl text-ink leading-snug mb-6">{t(brand.story)}</p>}
          <p className="text-xs text-charcoal/60">{t(NOTICE)}</p>
        </Reveal>
        <div className="grid grid-cols-2 lg:grid-cols-4 gap-x-8 gap-y-16">
          {products.map((p, i) => (
            <Reveal key={p.id} delay={i % 3}>
              <Link to={`/product/${p.id}`} className="group block">
                <Photo src={p.imageUrl} alt={p.name} ratio="3 / 4" />
                <h3 className="font-serif text-xl text-ink mt-4 group-hover:text-gold-dark transition-colors">{p.name}</h3>
                <p className="text-sm text-charcoal/75">{money(p.price, p.currency)}</p>
              </Link>
            </Reveal>
          ))}
        </div>
        <Reveal className="mt-20"><Link to="/maisons" className="btn-line text-ink">{t('All maisons')} <ArrowRight size={14} /></Link></Reveal>
      </section>
    </div>
  );
}
