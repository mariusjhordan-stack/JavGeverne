import { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { ArrowRight } from 'lucide-react';
import { getFeaturedProducts, getBrands } from '../api/catalog';
import { money } from '../lib/format';
import { EDITORIAL } from '../lib/images';
import Reveal from '../components/Reveal';
import Photo from '../components/Photo';
import { useLanguage } from '../context/LanguageContext';

export default function HomePage() {
  const { t } = useLanguage();
  const [featured, setFeatured] = useState([]);
  const [brands, setBrands] = useState([]);

  useEffect(() => {
    getFeaturedProducts().then((f) => setFeatured(Array.isArray(f) ? f : [])).catch(() => {});
    getBrands().then((b) => setBrands(Array.isArray(b) ? b : [])).catch(() => {});
  }, []);

  const lead = featured[0];
  const rest = featured.slice(1, 5);

  return (
    <div>
      {/* Hero */}
      <section className="relative min-h-[100svh] bg-ink text-white overflow-hidden flex items-end">
        <Photo src={EDITORIAL.hero} alt="" eager className="absolute inset-0 !bg-ink" imgClassName="hero-zoom opacity-70" position="60% 40%" />
        <div className="absolute inset-0 bg-gradient-to-t from-ink via-ink/40 to-ink/30" aria-hidden="true" />
        <div className="relative z-10 max-w-[88rem] w-full mx-auto px-6 lg:px-12 pb-20 lg:pb-28 grid lg:grid-cols-12 gap-10 items-end">
          <div className="lg:col-span-8">
            <p className="eyebrow eyebrow-light mb-8">{t('Genève · Since 1987')}</p>
            <h1 className="display text-[3.4rem] sm:text-7xl lg:text-[8.5rem]">
              {t('The patience')}<br /><em className="text-gold">{t('of precision')}</em>
            </h1>
          </div>
          <div className="lg:col-span-4 lg:pb-3">
            <p className="text-white/75 leading-relaxed mb-10 max-w-sm">
              {t('A considered selection of Swiss timepieces, chosen for how they are made and how long they will be loved.')}
            </p>
            <Link to="/collection" className="btn-line text-white">{t('Enter the collection')} <ArrowRight size={14} /></Link>
          </div>
        </div>
      </section>

      {/* Featured: asymmetric composition */}
      <section className="max-w-[88rem] mx-auto px-6 lg:px-12 py-28 lg:py-40">
        <Reveal className="max-w-xl mb-20">
          <p className="eyebrow mb-5">{t('This season')}</p>
          <h2 className="display text-5xl lg:text-7xl text-ink">{t('Selected pieces')}</h2>
        </Reveal>
        {lead && (
          <div className="grid lg:grid-cols-12 gap-x-12 gap-y-16 items-end">
            <Reveal className="lg:col-span-7">
              <Link to={`/product/${lead.id}`} className="group block">
                <Photo src={lead.imageUrl} alt={lead.name} ratio="4 / 5" className="w-full" position="center 35%" />
                <div className="flex items-baseline justify-between mt-6">
                  <div>
                    <p className="eyebrow">{lead.brandName}</p>
                    <h3 className="font-serif text-3xl text-ink mt-1 group-hover:text-gold-dark transition-colors">{lead.name}</h3>
                  </div>
                  <p className="text-ink">{money(lead.price, lead.currency)}</p>
                </div>
              </Link>
            </Reveal>
            <div className="lg:col-span-5 grid grid-cols-2 gap-x-8 gap-y-14 lg:pb-24">
              {rest.map((p, i) => (
                <Reveal key={p.id} delay={(i % 2) + 1} className={i % 2 ? 'mt-16' : ''}>
                  <Link to={`/product/${p.id}`} className="group block">
                    <Photo src={p.imageUrl} alt={p.name} ratio="3 / 4" />
                    <p className="eyebrow mt-4">{p.brandName}</p>
                    <h3 className="font-serif text-xl text-ink group-hover:text-gold-dark transition-colors">{p.name}</h3>
                    <p className="text-sm text-charcoal/80 mt-1">{money(p.price, p.currency)}</p>
                  </Link>
                </Reveal>
              ))}
            </div>
          </div>
        )}
        <Reveal className="mt-20">
          <Link to="/collection" className="btn-line text-ink">{t('View every timepiece')} <ArrowRight size={14} /></Link>
        </Reveal>
      </section>

      {/* Heritage */}
      <section className="bg-cream">
        <div className="max-w-[88rem] mx-auto grid lg:grid-cols-12 items-center">
          <Reveal variant="fade" className="lg:col-span-6 order-2 lg:order-1">
            <Photo src={EDITORIAL.heritage} alt="" ratio="1 / 1" className="w-full" />
          </Reveal>
          <Reveal className="lg:col-span-5 lg:col-start-8 order-1 lg:order-2 px-6 lg:px-0 py-24 lg:py-32">
            <p className="eyebrow mb-6">{t('Heritage')}</p>
            <h2 className="display text-5xl lg:text-6xl text-ink mb-8">{t('Measured in generations')}</h2>
            <p className="text-charcoal/80 leading-relaxed mb-5">
              {t('Geneva has refined the wristwatch for centuries. We look for pieces that carry that discipline: movements finished by hand, cases that wear in rather than wear out.')}
            </p>
            <Link to="/heritage" className="btn-line text-ink mt-6">{t('Read the story')} <ArrowRight size={14} /></Link>
          </Reveal>
        </div>
      </section>

      {/* Maisons: a typographic list, not a grid of cards */}
      <section className="max-w-[88rem] mx-auto px-6 lg:px-12 py-28 lg:py-40">
        <div className="grid lg:grid-cols-12 gap-16">
          <Reveal className="lg:col-span-4">
            <p className="eyebrow mb-5">{t('The maisons')}</p>
            <h2 className="display text-5xl text-ink mb-6">{t('Houses we admire')}</h2>
            <p className="text-sm text-charcoal/75 leading-relaxed max-w-xs">
              {t('Independent selection for a university demonstration. We are not an authorised dealer of any maison.')}
            </p>
          </Reveal>
          <ul className="lg:col-span-7 lg:col-start-6 divide-y hairline border-y hairline">
            {brands.map((b, i) => (
              <Reveal as="li" key={b.id} delay={i % 3}>
                <Link to={`/maisons/${b.slug}`} className="group flex items-baseline justify-between py-8">
                  <span className="font-serif text-4xl lg:text-5xl text-ink group-hover:text-gold-dark group-hover:translate-x-3 transition-all duration-500">{b.name}</span>
                  <span className="text-xs tracking-[0.2em] uppercase text-charcoal/60">{b.productCount} {t('pieces')}</span>
                </Link>
              </Reveal>
            ))}
          </ul>
        </div>
      </section>

      {/* Craftsmanship */}
      <section className="bg-ink text-white">
        <div className="max-w-[88rem] mx-auto px-6 lg:px-12 py-28 lg:py-40 grid lg:grid-cols-12 gap-16 items-center">
          <Reveal className="lg:col-span-5">
            <p className="eyebrow eyebrow-light mb-6">{t('Craftsmanship')}</p>
            <h2 className="display text-5xl lg:text-6xl mb-8">{t('Where a tolerance is a hair’s width')}</h2>
            <p className="text-white/70 leading-relaxed mb-10 max-w-md">
              {t('Hundreds of components, finished, assembled and regulated by people. We explain what to look for, so a purchase is understood as well as admired.')}
            </p>
            <Link to="/craftsmanship" className="btn-line text-white">{t('Inside the movement')} <ArrowRight size={14} /></Link>
          </Reveal>
          <Reveal variant="fade" className="lg:col-span-6 lg:col-start-7 grid grid-cols-5 gap-6 items-end">
            <Photo src={EDITORIAL.craft} alt="" ratio="3 / 4" className="col-span-3 !bg-navy-light" />
            <Photo src={EDITORIAL.craftDetail} alt="" ratio="1 / 1" className="col-span-2 !bg-navy-light mb-16" />
          </Reveal>
        </div>
      </section>

      {/* Closing call */}
      <section className="relative overflow-hidden">
        <Photo src={EDITORIAL.lifestyle} alt="" className="absolute inset-0" />
        <div className="absolute inset-0 bg-bone/80" aria-hidden="true" />
        <Reveal className="relative max-w-3xl mx-auto px-6 py-32 lg:py-44 text-center">
          <h2 className="display text-5xl lg:text-7xl text-ink mb-10">{t('Find the one that stays')}</h2>
          <Link to="/collection" className="btn-solid">{t('Explore the collection')}</Link>
        </Reveal>
      </section>
    </div>
  );
}
