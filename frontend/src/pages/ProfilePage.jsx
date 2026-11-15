import { useAuth } from '../context/AuthContext';
import { Link } from 'react-router-dom';
import { User, Package, ShieldCheck } from 'lucide-react';

export default function ProfilePage() {
  const { user } = useAuth();

  if (!user) return null;

  return (
    <div className="max-w-2xl mx-auto px-4 py-12">
      <h1 className="text-3xl text-navy mb-8">My Profile</h1>
      <div className="bg-white p-8 rounded-lg shadow-sm border border-gray-100">
        <div className="flex items-center mb-8">
          <div className="w-16 h-16 bg-navy rounded-full flex items-center justify-center">
            <User className="w-8 h-8 text-gold" />
          </div>
          <div className="ml-4">
            <h2 className="text-xl font-sans font-semibold text-navy">{user.firstName} {user.lastName}</h2>
            <p className="text-gray-500">{user.email}</p>
          </div>
        </div>
        <div className="space-y-4">
          <div className="flex items-center justify-between py-3 border-b border-gray-100">
            <span className="text-gray-600">Role</span>
            <span className="flex items-center gap-2">
              {user.role === 'ADMIN' && <ShieldCheck className="w-4 h-4 text-gold" />}
              <span className="font-medium">{user.role}</span>
            </span>
          </div>
          <div className="flex items-center justify-between py-3 border-b border-gray-100">
            <span className="text-gray-600">Email</span>
            <span className="font-medium">{user.email}</span>
          </div>
        </div>
        <div className="mt-8 flex gap-4">
          <Link to="/orders" className="flex items-center gap-2 px-6 py-3 bg-navy text-white rounded-lg hover:bg-navy-light transition-colors">
            <Package className="w-4 h-4" /> My Orders
          </Link>
          {user.role === 'ADMIN' && (
            <Link to="/admin" className="flex items-center gap-2 px-6 py-3 border border-gold text-gold rounded-lg hover:bg-gold hover:text-white transition-colors">
              <ShieldCheck className="w-4 h-4" /> Admin Panel
            </Link>
          )}
        </div>
      </div>
    </div>
  );
}
