import { useAuth } from '../context/AuthContext';
import { Link } from 'react-router-dom';
import { User, Package, Shield, Mail } from 'lucide-react';

export default function ProfilePage() {
  const { user, isAdmin } = useAuth();

  return (
    <div className="max-w-3xl mx-auto px-6 py-16">
      <div className="text-center mb-10">
        <p className="text-[#c9a94e] text-xs tracking-[0.3em] uppercase mb-2">Your Account</p>
        <h1 className="font-[Playfair_Display] text-3xl text-[#1a1a2e]">Profile</h1>
      </div>
      <div className="bg-white border border-gray-200 p-8">
        <div className="flex items-center gap-4 mb-8 pb-8 border-b">
          <div className="w-16 h-16 bg-[#1a1a2e] rounded-full flex items-center justify-center">
            <User className="w-8 h-8 text-[#c9a94e]" />
          </div>
          <div>
            <h2 className="text-xl font-[Playfair_Display] text-[#1a1a2e]">{user?.firstName} {user?.lastName}</h2>
            <p className="text-sm text-gray-500 flex items-center gap-1"><Mail size={14} /> {user?.email}</p>
            {isAdmin && (
              <span className="inline-flex items-center gap-1 text-xs bg-[#c9a94e]/10 text-[#c9a94e] px-2 py-0.5 mt-1 rounded">
                <Shield size={12} /> Administrator
              </span>
            )}
          </div>
        </div>
        <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
          <Link to="/orders" className="flex items-center gap-3 p-4 border border-gray-200 hover:border-[#c9a94e] transition-colors">
            <Package className="text-[#c9a94e]" size={20} />
            <div><p className="text-sm font-medium text-[#1a1a2e]">My Orders</p><p className="text-xs text-gray-500">View your order history</p></div>
          </Link>
          {isAdmin && (
            <Link to="/admin" className="flex items-center gap-3 p-4 border border-gray-200 hover:border-[#c9a94e] transition-colors">
              <Shield className="text-[#c9a94e]" size={20} />
              <div><p className="text-sm font-medium text-[#1a1a2e]">Admin Panel</p><p className="text-xs text-gray-500">Manage products</p></div>
            </Link>
          )}
        </div>
      </div>
    </div>
  );
}
