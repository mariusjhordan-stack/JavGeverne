import { useState, useEffect } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { Loader2, Clock } from 'lucide-react';
import { toast } from 'sonner';
import { useAuth } from '../context/AuthContext';
import { errorMessage } from '../api/client';

export default function RegisterPage() {
  const { register, isAuthenticated } = useAuth();
  const navigate = useNavigate();
  const [form, setForm] = useState({ firstName: '', lastName: '', email: '', password: '', confirmPassword: '' });
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  useEffect(() => {
    if (isAuthenticated) navigate('/', { replace: true });
  }, [isAuthenticated, navigate]);

  const set = (key) => (e) => setForm({ ...form, [key]: e.target.value });

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    if (form.password !== form.confirmPassword) {
      setError('Passwords do not match');
      return;
    }
    if (form.password.length < 8) {
      setError('Password must be at least 8 characters');
      return;
    }
    setLoading(true);
    try {
      await register({ firstName: form.firstName, lastName: form.lastName, email: form.email, password: form.password });
      toast.success('Account created successfully');
    } catch (err) {
      setError(errorMessage(err, 'Registration failed'));
    } finally {
      setLoading(false);
    }
  };

  const field = (label, key, type = 'text', placeholder = '') => (
    <div>
      <label htmlFor={`register-${key}`} className="block text-sm text-gray-600 mb-1">{label}</label>
      <input
        id={`register-${key}`}
        type={type}
        value={form[key]}
        onChange={set(key)}
        required
        className="w-full px-4 py-3 border border-gray-200 focus:border-gold focus:outline-none text-sm"
        placeholder={placeholder}
      />
    </div>
  );

  return (
    <div className="min-h-screen bg-cream flex items-center justify-center px-6 py-12">
      <div className="w-full max-w-md bg-white p-10 shadow-sm">
        <div className="text-center mb-8">
          <Clock size={32} className="text-gold mx-auto mb-3" strokeWidth={1} />
          <h1 className="font-serif text-3xl text-navy">Create Account</h1>
          <p className="text-sm text-gray-500 mt-1">Join the Cronos Geneve community</p>
        </div>

        {error && (
          <div className="bg-red-50 border border-red-200 text-red-700 text-sm px-4 py-3 mb-6" role="alert">{error}</div>
        )}

        <form onSubmit={handleSubmit} className="space-y-5">
          <div className="grid grid-cols-2 gap-4">
            {field('First Name', 'firstName', 'text', 'John')}
            {field('Last Name', 'lastName', 'text', 'Doe')}
          </div>
          {field('Email', 'email', 'email', 'your@email.com')}
          {field('Password', 'password', 'password', 'At least 8 characters')}
          {field('Confirm Password', 'confirmPassword', 'password', 'Repeat password')}
          <button
            type="submit"
            disabled={loading}
            className="w-full flex items-center justify-center gap-2 bg-navy text-white py-3 text-sm uppercase tracking-[0.15em] hover:bg-navy-light transition-colors disabled:opacity-50"
          >
            {loading ? <><Loader2 size={16} className="animate-spin" /> Creating...</> : 'Create Account'}
          </button>
        </form>

        <p className="text-center text-sm text-gray-500 mt-8">
          Already have an account?{' '}
          <Link to="/login" className="text-gold hover:underline">Sign in</Link>
        </p>
      </div>
    </div>
  );
}
