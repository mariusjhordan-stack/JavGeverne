import { NavLink, Outlet } from 'react-router-dom';
import { LayoutDashboard, Watch, Boxes, ClipboardList, Users, CreditCard } from 'lucide-react';

const ITEMS = [
  ['/admin', 'Overview', LayoutDashboard, true],
  ['/admin/products', 'Products', Watch],
  ['/admin/inventory', 'Inventory', Boxes],
  ['/admin/orders', 'Orders', ClipboardList],
  ['/admin/payments', 'Payments', CreditCard],
  ['/admin/customers', 'Customers', Users],
];

export default function AdminLayout() {
  return (
    <div className="max-w-[88rem] mx-auto px-6 lg:px-12 py-10 grid lg:grid-cols-[13rem_1fr] gap-12">
      <nav aria-label="Administration" className="lg:sticky lg:top-28 self-start flex lg:flex-col gap-1 overflow-x-auto">
        <p className="eyebrow mb-4 hidden lg:block">Administration</p>
        {ITEMS.map(([to, label, Icon, end]) => (
          <NavLink key={to} to={to} end={end}
            className={({ isActive }) => `flex items-center gap-3 px-3 py-2.5 text-sm whitespace-nowrap border-l-2 ${isActive ? 'border-gold-dark text-ink bg-cream' : 'border-transparent text-charcoal/70 hover:text-ink'}`}>
            <Icon size={16} strokeWidth={1.5} /> {label}
          </NavLink>
        ))}
      </nav>
      <div className="min-w-0"><Outlet /></div>
    </div>
  );
}
