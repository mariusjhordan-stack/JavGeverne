import { useState, useEffect } from 'react';
import { Link, NavLink, Outlet, useLocation, useNavigate } from 'react-router-dom';
import { ShoppingBag, User, Menu, X, LogOut, Package, Shield } from 'lucide-react';
import { useAuth } from '../context/AuthContext';
import { useCart } from '../context/CartContext';

const LINKS = [
  ['/collection', 'Collection'],
  ['/collections', 'Collections'],
  ['/maisons', 'Maisons'],
  ['/heritage', 'Heritage'],
  ['/craftsmanship', 'Craftsmanship'],
];

function Wordmark({ light }) {
  return (
    <Link to="/" className={`font-serif text-[1.6rem] leading-none tracking-[0.18em] ${light ? 'text-white' : 'text-ink'}`} aria-label="Cronos Genève, home">
      CRONOS <span className="text-gold font-normal italic tracking-[0.12em]">Genève</span>
    </Link>
  );
}

function Navbar({ overHero }) {
  const { isAuthenticated, isAdmin, user, logout } = useAuth();
  const { totalItems } = useCart();
  const [menuOpen, setMenuOpen] = useState(false);
  const [profileOpen, setProfileOpen] = useState(false);
  const [scrolled, setScrolled] = useState(false);
  const navigate = useNavigate();
  const location = useLocation();

  useEffect(() => {
    const onScroll = () => setScrolled(window.scrollY > 40);
    onScroll();
    window.addEventListener('scroll', onScroll, { passive: true });
    return () => window.removeEventListener('scroll', onScroll);
  }, []);
  useEffect(() => { setMenuOpen(false); setProfileOpen(false); }, [location.pathname]);

  const transparent = overHero && !scrolled && !menuOpen;
  const tone = transparent ? 'text-white' : 'text-ink';

  return (
    <header className={`fixed top-0 inset-x-0 z-50 transition-all duration-500 ${transparent ? 'bg-transparent' : 'bg-bone/95 backdrop-blur border-b hairline'}`}>
      <div className="max-w-[88rem] mx-auto px-6 lg:px-12 h-20 flex items-center justify-between">
        <Wordmark light={transparent} />

        <nav className={`hidden lg:flex items-center gap-10 ${tone}`} aria-label="Main">
          {LINKS.map(([to, label]) => (
            <NavLink key={to} to={to} end className={({ isActive }) => `text-[0.7rem] tracking-[0.24em] uppercase pb-1 border-b transition-colors ${isActive ? 'border-gold' : 'border-transparent hover:border-current'}`}>
              {label}
            </NavLink>
          ))}
        </nav>

        <div className={`flex items-center gap-5 ${tone}`}>
          <Link to="/cart" aria-label="Shopping bag" className="relative p-1">
            <ShoppingBag className="w-[1.15rem] h-[1.15rem]" strokeWidth={1.4} />
            {totalItems > 0 && (
              <span className="absolute -top-1.5 -right-2 bg-gold text-ink text-[0.6rem] font-semibold rounded-full w-4 h-4 flex items-center justify-center">{totalItems}</span>
            )}
          </Link>

          {isAuthenticated ? (
            <div className="relative">
              <button aria-label="Account menu" aria-expanded={profileOpen} onClick={() => setProfileOpen(!profileOpen)} className="flex items-center gap-2 p-1">
                <User className="w-[1.15rem] h-[1.15rem]" strokeWidth={1.4} />
                <span className="hidden xl:inline text-xs tracking-wider">{user?.firstName}</span>
              </button>
              {profileOpen && (
                <div className="absolute right-0 mt-4 w-52 bg-bone text-ink border hairline shadow-xl py-2 z-50">
                  <Link to="/profile" className="flex items-center gap-3 px-5 py-2.5 text-sm hover:bg-cream"><User className="w-4 h-4" /> Profile</Link>
                  <Link to="/orders" className="flex items-center gap-3 px-5 py-2.5 text-sm hover:bg-cream"><Package className="w-4 h-4" /> My orders</Link>
                  {isAdmin && <Link to="/admin" className="flex items-center gap-3 px-5 py-2.5 text-sm hover:bg-cream"><Shield className="w-4 h-4" /> Administration</Link>}
                  <button onClick={() => { logout(); navigate('/'); }} className="flex items-center gap-3 w-full px-5 py-2.5 text-sm text-red-700 hover:bg-cream border-t hairline mt-1">
                    <LogOut className="w-4 h-4" /> Sign out
                  </button>
                </div>
              )}
            </div>
          ) : (
            <Link to="/login" className="hidden md:inline text-[0.7rem] tracking-[0.24em] uppercase">Sign in</Link>
          )}

          <button aria-label="Menu" aria-expanded={menuOpen} className="lg:hidden p-1" onClick={() => setMenuOpen(!menuOpen)}>
            {menuOpen ? <X className="w-6 h-6" strokeWidth={1.4} /> : <Menu className="w-6 h-6" strokeWidth={1.4} />}
          </button>
        </div>
      </div>

      {menuOpen && (
        <nav className="lg:hidden bg-bone text-ink border-t hairline px-6 py-8 space-y-5" aria-label="Mobile">
          {LINKS.map(([to, label]) => <Link key={to} to={to} className="block font-serif text-2xl">{label}</Link>)}
          <Link to="/compare" className="block font-serif text-2xl">Compare</Link>
          {!isAuthenticated && <Link to="/login" className="block text-xs tracking-[0.24em] uppercase pt-4 border-t hairline">Sign in · Register</Link>}
        </nav>
      )}
    </header>
  );
}

function Footer() {
  return (
    <footer className="bg-ink text-white/70 mt-auto">
      <div className="max-w-[88rem] mx-auto px-6 lg:px-12 pt-24 pb-10">
        <div className="grid lg:grid-cols-12 gap-16">
          <div className="lg:col-span-5">
            <p className="font-serif text-4xl text-white leading-tight max-w-md">Time, kept with <em className="text-gold">conviction</em>.</p>
            <p className="text-sm leading-relaxed max-w-sm mt-6">A curated house of Swiss timepieces, presented with the patience the craft deserves.</p>
          </div>
          <nav className="lg:col-span-3 lg:col-start-7" aria-label="Explore">
            <p className="eyebrow eyebrow-light mb-6">Explore</p>
            <ul className="space-y-3 text-sm">
              <li><Link className="hover:text-gold transition-colors" to="/collection">The collection</Link></li>
              <li><Link className="hover:text-gold transition-colors" to="/collections">Collections</Link></li>
              <li><Link className="hover:text-gold transition-colors" to="/maisons">Maisons</Link></li>
              <li><Link className="hover:text-gold transition-colors" to="/compare">Compare</Link></li>
            </ul>
          </nav>
          <nav className="lg:col-span-3" aria-label="The house">
            <p className="eyebrow eyebrow-light mb-6">The house</p>
            <ul className="space-y-3 text-sm">
              <li><Link className="hover:text-gold transition-colors" to="/heritage">Heritage</Link></li>
              <li><Link className="hover:text-gold transition-colors" to="/craftsmanship">Craftsmanship</Link></li>
              <li><Link className="hover:text-gold transition-colors" to="/orders">Order history</Link></li>
              <li><Link className="hover:text-gold transition-colors" to="/profile">Account</Link></li>
            </ul>
          </nav>
        </div>
        <div className="border-t border-white/10 mt-20 pt-8 text-xs leading-relaxed text-white/55 space-y-2">
          <p>&copy; {new Date().getFullYear()} Cronos Genève. Academic demonstration project.</p>
          <p>Rolex, Omega and TAG Heuer are trademarks of their respective owners. This site is not affiliated with, endorsed or sponsored by any of them, and no real purchase is possible.</p>
        </div>
      </div>
    </footer>
  );
}

export default function Layout() {
  const { pathname } = useLocation();
  const overHero = pathname === '/' || pathname === '/heritage' || pathname === '/craftsmanship' || pathname === '/collections' || pathname === '/maisons';
  useEffect(() => { window.scrollTo(0, 0); }, [pathname]);
  return (
    <div className="min-h-screen flex flex-col">
      <a href="#main" className="sr-only focus:not-sr-only focus:fixed focus:top-3 focus:left-3 focus:z-[60] focus:bg-white focus:px-4 focus:py-2">Skip to content</a>
      <Navbar overHero={overHero} />
      <main id="main" key={pathname} className={`flex-1 page-enter ${overHero ? '' : 'pt-20'}`}>
        <Outlet />
      </main>
      <Footer />
    </div>
  );
}
