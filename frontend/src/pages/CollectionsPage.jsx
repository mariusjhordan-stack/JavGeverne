import { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { ArrowRight } from 'lucide-react';
import { getCategories } from '../api/catalog';
import PageHero from '../components/PageHero';
import Reveal from '../components/Reveal';
import Photo from '../components/Photo';
import { EDITORIAL } from '../lib/images';
import { useLanguage } from '../context/LanguageContext';

const IMAGES = { sport: EDITORIAL.collectionSport, dress: EDITORIAL.collectionDress, dive: EDITORIAL.collectionDive, chronograph: EDITORIAL.collectionChronograph };

export default function CollectionsPage() {
  const { t } = useLanguage();
  const [categories, setCategories] = useState([]);
  useEffect(() => { getCategories().then((c) => setCategories(Array.isArray(c) ? c : [])).catch(() => {}); }, []);

  return (
    <div>
      <PageHero eyebrow="Collections" title={<>{t('Four ways to')} <em className="text-gold">{t('wear time')}</em></>}
        intro="Sport, dress, dive and chronograph: the families that define modern watchmaking." image={EDITORIAL.collectionChronograph} />
      <section className="max-w-[88rem] mx-auto px-6 lg:px-12 py-24 lg:py-36 space-y-24 lg:space-y-40">
        {categories.map((c, i) => (
          <Reveal key={c.id} className={`grid lg:grid-cols-12 gap-10 items-center`}>
            <Link to={`/collection?categoryId=${c.id}`} className={`group block lg:col-span-7 ${i % 2 ? 'lg:order-2 lg:col-start-6' : ''}`}>
              <Photo src={IMAGES[c.slug]} alt="" ratio="16 / 10" />
            </Link>
            <div className={`lg:col-span-4 ${i % 2 ? 'lg:order-1' : 'lg:col-start-9'}`}>
              <p className="eyebrow mb-4">0{i + 1}</p>
              <h2 className="display text-5xl lg:text-6xl text-ink mb-5">{t(c.name)}</h2>
              <p className="text-charcoal/80 leading-relaxed mb-8">{t(c.description)}</p>
              <Link to={`/collection?categoryId=${c.id}`} className="btn-line text-ink">{t('View watches')} {t(c.name.toLowerCase())} <ArrowRight size={14} /></Link>
            </div>
          </Reveal>
        ))}
      </section>
    </div>
  );
}
