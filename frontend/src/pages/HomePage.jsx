import { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { Clock, Shield, Star, ArrowRight } from 'lucide-react';
import { getFeaturedProducts, getBrands } from '../api/catalog';

const BRAND_TAGLINES = {
  Rolex: 'A Crown for Every Achievement',
  Omega: 'The Sign of Excellence',
  'TAG Heuer': "Don't Crack Under Pressure",
};

export default function HomePage() {
  const [featured, setFeatured] = useState([]);
  const [brands, setBrands] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    Promise.all([getFeaturedProducts().catch(() => []), getBrands().catch(() => [])])
      .then(([f, b]) => {
        setFeatured(Array.isArray(f) ? f : f?.content ?? []);
        setBrands(Array.isArray(b) ? b : []);
      })
      .finally(() => setLoading(false));
  }, []);

  return (
    <div>
      {/* Hero */}
      <section className="relative bg-navy min-h-[85vh] flex items-center justify-center text-center overflow-hidden">
        <div className="absolute inset-0 bg-gradient-to-b from-navy/80 via-navy/60 to-navy" />
        <div className="relative z-10 max-w-4xl mx-auto px-6">
          <p className="text-gold tracking-[0.35em] uppercase text-sm mb-6 font-light">Est. Geneva, Switzerland</p>
          <h1 className="font-serif text-5xl md:text-7xl lg:text-8xl text-white leading-tight mb-6">
            The Art of Swiss Timekeeping
          </h1>
          <p className="text-gray-300 text-lg md:text-xl max-w-2xl mx-auto mb-10 font-light leading-relaxed">
            Discover our curated collection of the world's finest timepieces, each a masterpiece of precision engineering and timeless design.
          </p>
          <Link
            to="/collection"
            className="inline-flex items-center gap-3 bg-gold hover:bg-gold-light text-navy px-10 py-4 text-sm uppercase tracking-[0.2em] font-medium transition-colors"
          >
            Explore Collection <ArrowRight size={16} />
          </Link>
        </div>
      </section>

      {/* Featured */}
      <section className="max-w-7xl mx-auto px-6 py-24">
        <div className="text-center mb-16">
          <p className="text-gold tracking-[0.3em] uppercase text-xs mb-3">Curated Selection</p>
          <h2 className="font-serif text-4xl md:text-5xl text-navy">Featured Timepieces</h2>
        </div>
        {loading ? (
          <div className="flex justify-center py-20">
            <div className="w-10 h-10 border-2 border-gold border-t-transparent rounded-full animate-spin" />
          </div>
        ) : featured.length === 0 ? (
          <p className="text-center text-gray-500">No featured watches at the moment.</p>
        ) : (
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-8">
            {featured.slice(0, 4).map((p) => (
              <Link key={p.id} to={`/product/${p.id}`} className="group block">
                <div className="aspect-square bg-cream overflow-hidden mb-4">
                  <img
                    src={p.imageUrl || 'https://placehold.co/600x600/1a1a2e/c9a94e?text=Watch'}
                    alt={p.name}
                    className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-700"
                  />
                </div>
                <p className="text-xs text-gold tracking-[0.2em] uppercase mb-1">{p.brandName}</p>
                <h3 className="font-serif text-lg text-navy mb-1 group-hover:text-gold transition-colors">{p.name}</h3>
                <p className="text-charcoal font-light">${Number(p.price).toLocaleString()}</p>
              </Link>
            ))}
          </div>
        )}
        <div className="text-center mt-14">
          <Link
            to="/collection"
            className="inline-flex items-center gap-2 border border-navy text-navy hover:bg-navy hover:text-white px-8 py-3 text-sm uppercase tracking-[0.2em] transition-colors"
          >
            View All Timepieces <ArrowRight size={14} />
          </Link>
        </div>
      </section>

      {/* Philosophy */}
      <section className="bg-navy text-white py-24">
        <div className="max-w-6xl mx-auto px-6 grid md:grid-cols-3 gap-16 text-center">
          {[
            { icon: Clock, title: 'Precision', text: 'Every timepiece in our collection undergoes rigorous testing, ensuring accuracy that stands the test of time.' },
            { icon: Shield, title: 'Authenticity', text: 'We guarantee the provenance of every watch, sourced directly from authorized Swiss manufacturers.' },
            { icon: Star, title: 'Excellence', text: 'Curated by experts with decades of horological knowledge, our collection represents the pinnacle of Swiss watchmaking.' },
          ].map(({ icon: Icon, title, text }) => (
            <div key={title}>
              <Icon size={36} className="text-gold mx-auto mb-6" strokeWidth={1} />
              <h3 className="font-serif text-2xl mb-4">{title}</h3>
              <p className="text-gray-400 font-light leading-relaxed">{text}</p>
            </div>
          ))}
        </div>
      </section>

      {/* Brands */}
      <section className="max-w-6xl mx-auto px-6 py-24">
        <div className="text-center mb-16">
          <p className="text-gold tracking-[0.3em] uppercase text-xs mb-3">Our Maisons</p>
          <h2 className="font-serif text-4xl text-navy">Distinguished Houses</h2>
        </div>
        <div className="grid md:grid-cols-3 gap-10">
          {(brands.length ? brands : [{ id: 1, name: 'Rolex' }, { id: 2, name: 'Omega' }, { id: 3, name: 'TAG Heuer' }]).map((b) => (
            <Link
              key={b.id}
              to={`/collection?brandId=${b.id}`}
              className="group border border-gray-200 hover:border-gold p-10 text-center transition-colors"
            >
              <h3 className="font-serif text-2xl text-navy group-hover:text-gold transition-colors mb-2">{b.name}</h3>
              <p className="text-sm text-gray-500 font-light">{BRAND_TAGLINES[b.name] || b.description || 'Swiss Excellence'}</p>
            </Link>
          ))}
        </div>
      </section>

      {/* Newsletter */}
      <section className="bg-cream py-20">
        <div className="max-w-xl mx-auto px-6 text-center">
          <h2 className="font-serif text-3xl text-navy mb-4">Stay Informed</h2>
          <p className="text-gray-600 font-light mb-8">
            Receive exclusive previews of new arrivals and private collection events.
          </p>
          <form className="flex gap-3" onSubmit={(e) => e.preventDefault()}>
            <input
              type="email"
              placeholder="Your email address"
              className="flex-1 px-5 py-3 border border-gray-300 focus:border-gold focus:outline-none text-sm"
            />
            <button className="bg-navy text-white px-8 py-3 text-sm uppercase tracking-[0.15em] hover:bg-navy-light transition-colors">
              Subscribe
            </button>
          </form>
        </div>
      </section>
    </div>
  );
}
