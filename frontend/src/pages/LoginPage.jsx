import { useState, useEffect } from 'react';
import { Link, useNavigate, useLocation } from 'react-router-dom';
import { Loader2, Clock } from 'lucide-react';
import { toast } from 'sonner';
import { useAuth } from '../context/AuthContext';
import { errorMessage } from '../api/client';
import { useLanguage } from '../context/LanguageContext';

export default function LoginPage() {
  const { t } = useLanguage();
  const { login, isAuthenticated, sessionExpired, clearSessionExpired } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  const from = location.state?.from?.pathname || '/';

  useEffect(() => {
    if (isAuthenticated) navigate(from, { replace: true });
  }, [isAuthenticated, from, navigate]);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setLoading(true);
    try {
      await login(email, password);
      clearSessionExpired();
      toast.success(t('Welcome back'));
    } catch (err) {
      setError(errorMessage(err, t('Invalid email or password')));
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen bg-cream flex items-center justify-center px-6">
      <div className="w-full max-w-md bg-white p-10 shadow-sm">
        <div className="text-center mb-8">
          <Clock size={32} className="text-gold mx-auto mb-3" strokeWidth={1} />
          <h1 className="font-serif text-3xl text-navy">{t('Welcome Back')}</h1>
          <p className="text-sm text-gray-500 mt-1">{t('Sign in to your Cronos Geneve account')}</p>
        </div>

        {sessionExpired && !error && (
          <div className="bg-amber-50 border border-amber-200 text-amber-800 text-sm px-4 py-3 mb-6" role="status">
            {t('Your session expired. Please sign in again.')}
          </div>
        )}
        {error && (
          <div className="bg-red-50 border border-red-200 text-red-700 text-sm px-4 py-3 mb-6" role="alert">{error}</div>
        )}

        <form onSubmit={handleSubmit} className="space-y-5">
          <div>
            <label htmlFor="login-email" className="block text-sm text-gray-600 mb-1">{t('Email')}</label>
            <input
              id="login-email"
              autoComplete="email"
              type="email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              required
              className="w-full px-4 py-3 border border-gray-200 focus:border-gold focus:outline-none text-sm"
              placeholder="your@email.com"
            />
          </div>
          <div>
            <label htmlFor="login-password" className="block text-sm text-gray-600 mb-1">{t('Password')}</label>
            <input
              id="login-password"
              autoComplete="current-password"
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              required
              className="w-full px-4 py-3 border border-gray-200 focus:border-gold focus:outline-none text-sm"
              placeholder={t('Enter your password')}
            />
          </div>
          <button
            type="submit"
            disabled={loading}
            className="w-full flex items-center justify-center gap-2 bg-navy text-white py-3 text-sm uppercase tracking-[0.15em] hover:bg-navy-light transition-colors disabled:opacity-50"
          >
            {loading ? <><Loader2 size={16} className="animate-spin" /> {t('Signing in...')}</> : t('Sign In')}
          </button>
        </form>

        <p className="text-center text-sm text-gray-500 mt-8">
          {t("Don't have an account?")}{' '}
          <Link to="/register" className="text-gold hover:underline">{t('Create one')}</Link>
        </p>

        {import.meta.env.DEV && (
        <div className="border-t border-gray-100 mt-8 pt-6">
          <p className="text-xs text-gray-400 text-center mb-2">{t('Demo accounts')}</p>
          <div className="grid grid-cols-2 gap-3 text-xs">
            <button
              type="button"
              onClick={() => { setEmail('admin@cronos.com'); setPassword('Admin123!'); }}
              className="border border-gray-200 py-2 rounded hover:bg-gray-50 text-gray-600"
            >
              {t('Admin')}
            </button>
            <button
              type="button"
              onClick={() => { setEmail('user@cronos.com'); setPassword('User123!'); }}
              className="border border-gray-200 py-2 rounded hover:bg-gray-50 text-gray-600"
            >
              {t('User')}
            </button>
          </div>
        </div>
        )}
      </div>
    </div>
  );
}
