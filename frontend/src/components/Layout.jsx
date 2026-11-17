import { useState, useEffect } from 'react';
import { Link, Outlet, useNavigate } from 'react-router-dom';
import { ShoppingBag, User, Menu, X, Watch, LogOut, Package, Shield } from 'lucide-react';
import { useAuth } from '../context/AuthContext';
import { useCart } from '../context/CartContext';

function Navbar() {
  const { isAuthenticated, isAdmin, user, logout, sessionExpired } = useAuth();
  const { totalItems } = useCart();
  const [menuOpen, setMenuOpen] = useState(false);
  const [profileOpen, setProfileOpen] = useState(false);
  const navigate = useNavigate();

  useEffect(() => {
    if (sessionExpired) navigate('/login');
  }, [sessionExpired, navigate]);

  const handleLogout = () => {
    logout();
    setProfileOpen(false);
    navigate('/');
  };

  return (
    <nav className="sticky top-0 z-50 bg-[#1a1a2e] text-white shadow-lg">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="flex items-center justify-between h-16 lg:h-20">
          {/* Logo */}
          <Link to="/" className="flex items-center gap-2 group">
            <Watch className="w-6 h-6 text-[#c9a94e] group-hover:rotate-12 transition-transform" />
            <div>
              <span className="font-[Playfair_Display] text-lg lg:text-xl font-semibold tracking-wider text-white">
                CRONOS
              </span>
              <span className="font-[Playfair_Display] text-lg lg:text-xl font-light tracking-widest text-[#c9a94e] ml-1">
                GENEVE
              </span>
            </div>
          </Link>

          {/* Desktop Nav */}
          <div className="hidden md:flex items-center gap-8">
            <Link to="/" className="text-sm tracking-wider uppercase text-gray-300 hover:text-[#c9a94e] transition-colors">
              Home
            </Link>
            <Link to="/collection" className="text-sm tracking-wider uppercase text-gray-300 hover:text-[#c9a94e] transition-colors">
              Collection
            </Link>
            <Link to="/compare" className="text-sm tracking-wider uppercase text-gray-300 hover:text-[#c9a94e] transition-colors">
              Compare
            </Link>
          </div>

          {/* Actions */}
          <div className="flex items-center gap-4">
            <Link to="/cart" aria-label="Shopping bag" className="relative text-gray-300 hover:text-[#c9a94e] transition-colors">
              <ShoppingBag className="w-5 h-5" />
              {totalItems > 0 && (
                <span className="absolute -top-2 -right-2 bg-[#c9a94e] text-[#1a1a2e] text-xs font-bold rounded-full w-5 h-5 flex items-center justify-center">
                  {totalItems}
                </span>
              )}
            </Link>

            {isAuthenticated ? (
              <div className="relative">
                <button
                  aria-label="Account menu"
                  onClick={() => setProfileOpen(!profileOpen)}
                  className="flex items-center gap-2 text-gray-300 hover:text-[#c9a94e] transition-colors"
                >
                  <User className="w-5 h-5" />
                  <span className="hidden lg:inline text-sm">{user?.firstName}</span>
                </button>
                {profileOpen && (
                  <div className="absolute right-0 mt-2 w-48 bg-white rounded-md shadow-xl py-1 z-50">
                    <Link
                      to="/profile"
                      onClick={() => setProfileOpen(false)}
                      className="flex items-center gap-2 px-4 py-2 text-sm text-gray-700 hover:bg-gray-100"
                    >
                      <User className="w-4 h-4" /> Profile
                    </Link>
                    <Link
                      to="/orders"
                      onClick={() => setProfileOpen(false)}
                      className="flex items-center gap-2 px-4 py-2 text-sm text-gray-700 hover:bg-gray-100"
                    >
                      <Package className="w-4 h-4" /> My Orders
                    </Link>
                    {isAdmin && (
                      <Link
                        to="/admin"
                        onClick={() => setProfileOpen(false)}
                        className="flex items-center gap-2 px-4 py-2 text-sm text-gray-700 hover:bg-gray-100"
                      >
                        <Shield className="w-4 h-4" /> Admin Panel
                      </Link>
                    )}
                    <hr className="my-1" />
                    <button
                      onClick={handleLogout}
                      className="flex items-center gap-2 w-full px-4 py-2 text-sm text-red-600 hover:bg-gray-100"
                    >
                      <LogOut className="w-4 h-4" /> Logout
                    </button>
                  </div>
                )}
              </div>
            ) : (
              <div className="hidden md:flex items-center gap-3">
                <Link
                  to="/login"
                  className="text-sm tracking-wider text-gray-300 hover:text-[#c9a94e] transition-colors"
                >
                  Login
                </Link>
                <Link
                  to="/register"
                  className="text-sm tracking-wider bg-[#c9a94e] text-[#1a1a2e] px-4 py-1.5 rounded hover:bg-[#d4b86a] transition-colors font-medium"
                >
                  Register
                </Link>
              </div>
            )}

            {/* Mobile menu toggle */}
            <button
              aria-label="Menu"
              className="md:hidden text-gray-300"
              onClick={() => setMenuOpen(!menuOpen)}
            >
              {menuOpen ? <X className="w-6 h-6" /> : <Menu className="w-6 h-6" />}
            </button>
          </div>
        </div>
      </div>

      {/* Mobile Nav */}
      {menuOpen && (
        <div className="md:hidden bg-[#2d2d44] border-t border-gray-700 px-4 py-4 space-y-3">
          <Link to="/" onClick={() => setMenuOpen(false)} className="block text-sm tracking-wider uppercase text-gray-300 hover:text-[#c9a94e]">Home</Link>
          <Link to="/collection" onClick={() => setMenuOpen(false)} className="block text-sm tracking-wider uppercase text-gray-300 hover:text-[#c9a94e]">Collection</Link>
          <Link to="/compare" onClick={() => setMenuOpen(false)} className="block text-sm tracking-wider uppercase text-gray-300 hover:text-[#c9a94e]">Compare</Link>
          {!isAuthenticated && (
            <>
              <hr className="border-gray-600" />
              <Link to="/login" onClick={() => setMenuOpen(false)} className="block text-sm text-gray-300 hover:text-[#c9a94e]">Login</Link>
              <Link to="/register" onClick={() => setMenuOpen(false)} className="block text-sm text-[#c9a94e]">Register</Link>
            </>
          )}
        </div>
      )}
    </nav>
  );
}

function Footer() {
  return (
    <footer className="bg-[#1a1a2e] text-gray-400 mt-auto">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-12">
        <div className="grid grid-cols-1 md:grid-cols-4 gap-8">
          <div className="md:col-span-2">
            <div className="flex items-center gap-2 mb-4">
              <Watch className="w-6 h-6 text-[#c9a94e]" />
              <span className="font-[Playfair_Display] text-xl text-white tracking-wider">
                CRONOS <span className="text-[#c9a94e] font-light">GENEVE</span>
              </span>
            </div>
            <p className="text-sm leading-relaxed max-w-md">
              Curating the finest Swiss timepieces since 1987. Each watch in our collection
              represents the pinnacle of horological craftsmanship and timeless design.
            </p>
          </div>
          <div>
            <h4 className="text-white text-sm font-semibold uppercase tracking-wider mb-4">Navigation</h4>
            <ul className="space-y-2 text-sm">
              <li><Link to="/collection" className="hover:text-[#c9a94e] transition-colors">Collection</Link></li>
              <li><Link to="/compare" className="hover:text-[#c9a94e] transition-colors">Compare</Link></li>
              <li><Link to="/cart" className="hover:text-[#c9a94e] transition-colors">Shopping Bag</Link></li>
            </ul>
          </div>
          <div>
            <h4 className="text-white text-sm font-semibold uppercase tracking-wider mb-4">Account</h4>
            <ul className="space-y-2 text-sm">
              <li><Link to="/login" className="hover:text-[#c9a94e] transition-colors">Sign In</Link></li>
              <li><Link to="/register" className="hover:text-[#c9a94e] transition-colors">Create Account</Link></li>
              <li><Link to="/orders" className="hover:text-[#c9a94e] transition-colors">Order History</Link></li>
            </ul>
          </div>
        </div>
        <div className="border-t border-gray-700 mt-8 pt-8 text-center text-xs">
          <p>&copy; {new Date().getFullYear()} Cronos Geneve. All rights reserved.</p>
          <p className="mt-1 text-gray-500">Swiss Luxury Timepieces &mdash; Java Microservices Edition</p>
        </div>
      </div>
    </footer>
  );
}

export default function Layout() {
  return (
    <div className="min-h-screen flex flex-col">
      <Navbar />
      <main className="flex-1">
        <Outlet />
      </main>
      <Footer />
    </div>
  );
}
