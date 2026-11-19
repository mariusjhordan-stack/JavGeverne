import { useState, useEffect, useRef } from 'react';
import { Link, NavLink, Outlet, useLocation, useNavigate } from 'react-router-dom';
import { ShoppingBag, User, Menu, X, LogOut, Package, Shield, ChevronDown, Check } from 'lucide-react';
import { useAuth } from '../context/AuthContext';
import { useCart } from '../context/CartContext';
import { useLanguage } from '../context/LanguageContext';

const LINKS = [
  ['/collection', 'Collection'],
  ['/collections', 'Collections'],
  ['/maisons', 'Maisons'],
  ['/heritage', 'Heritage'],
  ['/craftsmanship', 'Craftsmanship'],
];

function Wordmark({ light }) {
  const { t } = useLanguage();
  return (
    <Link to="/" className={`font-serif text-[1.6rem] leading-none tracking-[0.18em] ${light ? 'text-white' : 'text-ink'}`} aria-label={t('Cronos Genève, home')}>
      CRONOS <span className="text-gold font-normal italic tracking-[0.12em]">Genève</span>
    </Link>
  );
}

function Navbar({ overHero }) {
  const { isAuthenticated, isAdmin, user, logout } = useAuth();
  const { totalItems } = useCart();
  const { language, setLanguage, t } = useLanguage();
  const [menuOpen, setMenuOpen] = useState(false);
  const [profileOpen, setProfileOpen] = useState(false);
  const [languageOpen, setLanguageOpen] = useState(false);
  const [scrolled, setScrolled] = useState(false);
  const languageSelectorRef = useRef(null);
  const languageButtonRef = useRef(null);
  const navigate = useNavigate();
  const location = useLocation();

  useEffect(() => {
    const onScroll = () => setScrolled(window.scrollY > 40);
    onScroll();
    window.addEventListener('scroll', onScroll, { passive: true });
    return () => window.removeEventListener('scroll', onScroll);
  }, []);
  useEffect(() => { setMenuOpen(false); setProfileOpen(false); }, [location.pathname]);
  useEffect(() => {
    if (!languageOpen) return undefined;
    const closeOnOutsideClick = (event) => {
      if (!languageSelectorRef.current?.contains(event.target)) setLanguageOpen(false);
    };
    const closeOnEscape = (event) => {
      if (event.key === 'Escape') {
        setLanguageOpen(false);
        languageButtonRef.current?.focus();
      }
    };
    document.addEventListener('pointerdown', closeOnOutsideClick);
    document.addEventListener('keydown', closeOnEscape);
    return () => {
      document.removeEventListener('pointerdown', closeOnOutsideClick);
      document.removeEventListener('keydown', closeOnEscape);
    };
  }, [languageOpen]);

  const transparent = overHero && !scrolled && !menuOpen;
  const tone = transparent ? 'text-white' : 'text-ink';

  return (
    <header className={`fixed top-0 inset-x-0 z-50 transition-all duration-500 ${transparent ? 'bg-transparent' : 'bg-bone/95 backdrop-blur border-b hairline'}`}>
      <div className="max-w-[88rem] mx-auto px-4 sm:px-6 lg:px-12 h-20 flex items-center justify-between">
        <div className="shrink-0"><Wordmark light={transparent} /></div>

        <nav className={`hidden xl:flex items-center gap-5 2xl:gap-8 ${tone}`} aria-label={t('Main')}>
          {LINKS.map(([to, label]) => (
            <NavLink key={to} to={to} end className={({ isActive }) => `whitespace-nowrap text-[0.68rem] 2xl:text-[0.7rem] tracking-[0.16em] 2xl:tracking-[0.24em] uppercase pb-1 border-b transition-colors ${isActive ? 'border-gold' : 'border-transparent hover:border-current'}`}>
              {t(label)}
            </NavLink>
          ))}
        </nav>

        <div className={`flex shrink-0 items-center gap-2 sm:gap-4 xl:gap-5 ${tone}`}>
          <div className="relative" ref={languageSelectorRef}>
            <button
              ref={languageButtonRef}
              type="button"
              aria-label={t('Language')}
              aria-expanded={languageOpen}
              aria-controls="language-options"
              onClick={() => setLanguageOpen(!languageOpen)}
              className="inline-flex h-8 items-center gap-1.5 border-b border-current/25 px-1 text-[0.65rem] font-medium tracking-[0.18em] transition-colors hover:border-gold focus-visible:border-gold focus-visible:outline-none"
            >
              <span>{language.toUpperCase()}</span>
              <ChevronDown className={`w-3 h-3 opacity-70 transition-transform ${languageOpen ? 'rotate-180' : ''}`} aria-hidden="true" />
            </button>
            {languageOpen && (
              <div id="language-options" role="group" aria-label={t('Language')} className="absolute right-0 top-full z-[60] mt-2 w-36 border hairline bg-bone py-1 text-ink shadow-xl">
                {[['en', 'English'], ['es', 'Español']].map(([value, label]) => (
                  <button
                    key={value}
                    type="button"
                    aria-pressed={language === value}
                    onClick={() => { setLanguage(value); setLanguageOpen(false); }}
                    className={`flex w-full items-center justify-between px-4 py-2.5 text-left text-xs tracking-wide transition-colors hover:bg-cream ${language === value ? 'text-gold-dark' : 'text-ink'}`}
                  >
                    {label}
                    {language === value && <Check className="w-3.5 h-3.5" aria-hidden="true" />}
                  </button>
                ))}
              </div>
            )}
          </div>
          <Link to="/cart" aria-label={t('Shopping bag')} className="relative p-1">
            <ShoppingBag className="w-[1.15rem] h-[1.15rem]" strokeWidth={1.4} />
            {totalItems > 0 && (
              <span className="absolute -top-1.5 -right-2 bg-gold text-ink text-[0.6rem] font-semibold rounded-full w-4 h-4 flex items-center justify-center">{totalItems}</span>
            )}
          </Link>

          {isAuthenticated ? (
            <div className="relative">
              <button aria-label={t('Account menu')} aria-expanded={profileOpen} onClick={() => setProfileOpen(!profileOpen)} className="flex items-center gap-2 p-1">
                <User className="w-[1.15rem] h-[1.15rem]" strokeWidth={1.4} />
                <span className="hidden xl:inline text-xs tracking-wider">{user?.firstName}</span>
              </button>
              {profileOpen && (
                <div className="absolute right-0 mt-4 w-52 bg-bone text-ink border hairline shadow-xl py-2 z-50">
                  <Link to="/profile" className="flex items-center gap-3 px-5 py-2.5 text-sm hover:bg-cream"><User className="w-4 h-4" /> {t('Profile')}</Link>
                  <Link to="/orders" className="flex items-center gap-3 px-5 py-2.5 text-sm hover:bg-cream"><Package className="w-4 h-4" /> {t('My orders')}</Link>
                  {isAdmin && <Link to="/admin" className="flex items-center gap-3 px-5 py-2.5 text-sm hover:bg-cream"><Shield className="w-4 h-4" /> {t('Administration')}</Link>}
                  <button onClick={() => { logout(); navigate('/'); }} className="flex items-center gap-3 w-full px-5 py-2.5 text-sm text-red-700 hover:bg-cream border-t hairline mt-1">
                    <LogOut className="w-4 h-4" /> {t('Sign out')}
                  </button>
                </div>
              )}
            </div>
          ) : (
            <Link to="/login" className="hidden xl:inline text-[0.7rem] tracking-[0.24em] uppercase">{t('Sign in')}</Link>
          )}

          <button aria-label={t('Menu')} aria-expanded={menuOpen} className="xl:hidden p-1" onClick={() => setMenuOpen(!menuOpen)}>
            {menuOpen ? <X className="w-6 h-6" strokeWidth={1.4} /> : <Menu className="w-6 h-6" strokeWidth={1.4} />}
          </button>
        </div>
      </div>

      {menuOpen && (
        <nav className="xl:hidden bg-bone text-ink border-t hairline px-6 py-8 space-y-5" aria-label={t('Mobile')}>
          {LINKS.map(([to, label]) => <Link key={to} to={to} className="block font-serif text-2xl">{t(label)}</Link>)}
          <Link to="/compare" className="block font-serif text-2xl">{t('Compare')}</Link>
          {!isAuthenticated && <Link to="/login" className="block text-xs tracking-[0.24em] uppercase pt-4 border-t hairline">{t('Sign in · Register')}</Link>}
        </nav>
      )}
    </header>
  );
}

function Footer() {
  const { t } = useLanguage();
  return (
    <footer className="bg-ink text-white/70 mt-auto">
      <div className="max-w-[88rem] mx-auto px-6 lg:px-12 pt-24 pb-10">
        <div className="grid lg:grid-cols-12 gap-16">
          <div className="lg:col-span-5">
            <p className="font-serif text-4xl text-white leading-tight max-w-md">{t('Time, kept with conviction.')}</p>
            <p className="text-sm leading-relaxed max-w-sm mt-6">{t('A curated house of Swiss timepieces, presented with the patience the craft deserves.')}</p>
          </div>
          <nav className="lg:col-span-3 lg:col-start-7" aria-label={t('Explore')}>
            <p className="eyebrow eyebrow-light mb-6">{t('Explore')}</p>
            <ul className="space-y-3 text-sm">
              <li><Link className="hover:text-gold transition-colors" to="/collection">{t('The collection')}</Link></li>
              <li><Link className="hover:text-gold transition-colors" to="/collections">{t('Collections')}</Link></li>
              <li><Link className="hover:text-gold transition-colors" to="/maisons">{t('Maisons')}</Link></li>
              <li><Link className="hover:text-gold transition-colors" to="/compare">{t('Compare')}</Link></li>
            </ul>
          </nav>
          <nav className="lg:col-span-3" aria-label={t('The house')}>
            <p className="eyebrow eyebrow-light mb-6">{t('The house')}</p>
            <ul className="space-y-3 text-sm">
              <li><Link className="hover:text-gold transition-colors" to="/heritage">{t('Heritage')}</Link></li>
              <li><Link className="hover:text-gold transition-colors" to="/craftsmanship">{t('Craftsmanship')}</Link></li>
              <li><Link className="hover:text-gold transition-colors" to="/orders">{t('Order history')}</Link></li>
              <li><Link className="hover:text-gold transition-colors" to="/profile">{t('Account')}</Link></li>
            </ul>
          </nav>
        </div>
        <div className="border-t border-white/10 mt-20 pt-8 text-xs leading-relaxed text-white/55 space-y-2">
          <p>&copy; {new Date().getFullYear()} Cronos Genève. {t('Academic demonstration project.')}</p>
          <p>{t('Rolex, Omega and TAG Heuer are trademarks of their respective owners. This site is not affiliated with, endorsed or sponsored by any of them, and no real purchase is possible.')}</p>
        </div>
      </div>
    </footer>
  );
}

export default function Layout() {
  const { t } = useLanguage();
  const { pathname } = useLocation();
  const overHero = pathname === '/' || pathname === '/heritage' || pathname === '/craftsmanship' || pathname === '/collections' || pathname === '/maisons';
  useEffect(() => { window.scrollTo(0, 0); }, [pathname]);
  return (
    <div className="min-h-screen flex flex-col">
      <a href="#main" className="sr-only focus:not-sr-only focus:fixed focus:top-3 focus:left-3 focus:z-[60] focus:bg-white focus:px-4 focus:py-2">{t('Skip to content')}</a>
      <Navbar overHero={overHero} />
      <main id="main" key={pathname} className={`flex-1 page-enter ${overHero ? '' : 'pt-20'}`}>
        <Outlet />
      </main>
      <Footer />
    </div>
  );
}
