import { lazy, Suspense } from 'react';
import { Routes, Route, Navigate, useParams } from 'react-router-dom';
import Layout from './components/Layout';
import ProtectedRoute from './components/ProtectedRoute';

const HomePage = lazy(() => import('./pages/HomePage'));
const CatalogPage = lazy(() => import('./pages/CatalogPage'));
const ProductDetailPage = lazy(() => import('./pages/ProductDetailPage'));
const ComparePage = lazy(() => import('./pages/ComparePage'));
const CartPage = lazy(() => import('./pages/CartPage'));
const CheckoutPage = lazy(() => import('./pages/CheckoutPage'));
const LoginPage = lazy(() => import('./pages/LoginPage'));
const RegisterPage = lazy(() => import('./pages/RegisterPage'));
const ProfilePage = lazy(() => import('./pages/ProfilePage'));
const OrdersPage = lazy(() => import('./pages/OrdersPage'));
const OrderDetailPage = lazy(() => import('./pages/OrderDetailPage'));
const HeritagePage = lazy(() => import('./pages/HeritagePage'));
const CraftsmanshipPage = lazy(() => import('./pages/CraftsmanshipPage'));
const CollectionsPage = lazy(() => import('./pages/CollectionsPage'));
const MaisonsIndex = lazy(() => import('./pages/MaisonsPage').then((m) => ({ default: m.MaisonsIndex })));
const MaisonDetail = lazy(() => import('./pages/MaisonsPage').then((m) => ({ default: m.MaisonDetail })));
const CheckoutResultPage = lazy(() => import('./pages/CheckoutResultPage'));
const NotFoundPage = lazy(() => import('./pages/NotFoundPage'));
const AdminLayout = lazy(() => import('./pages/admin/AdminLayout'));
const AdminOverview = lazy(() => import('./pages/admin/Overview'));
const AdminProducts = lazy(() => import('./pages/admin/Products'));
const AdminInventory = lazy(() => import('./pages/admin/Inventory'));
const AdminPayments = lazy(() => import('./pages/admin/Payments'));
const AdminOrdersList = lazy(() => import('./pages/admin/Orders').then((m) => ({ default: m.OrdersList })));
const AdminOrderDetail = lazy(() => import('./pages/admin/Orders').then((m) => ({ default: m.OrderDetail })));
const AdminCustomersList = lazy(() => import('./pages/admin/Customers').then((m) => ({ default: m.CustomersList })));
const AdminCustomerDetail = lazy(() => import('./pages/admin/Customers').then((m) => ({ default: m.CustomerDetail })));

function Loading() {
  return (
    <div className="flex items-center justify-center min-h-[60vh]">
      <div className="text-center">
        <div className="w-8 h-8 border-2 border-[#c9a94e] border-t-transparent rounded-full animate-spin mx-auto mb-4" />
        <p className="text-sm text-gray-500 tracking-wider uppercase">Loading</p>
      </div>
    </div>
  );
}

function LegacyConfirmation() {
  const { id } = useParams();
  return <Navigate to={`/orders/${id}`} replace />;
}

export default function App() {
  return (
    <Suspense fallback={<Loading />}>
      <Routes>
        <Route element={<Layout />}>
          <Route path="/" element={<HomePage />} />
          <Route path="/catalog" element={<CatalogPage />} />
          <Route path="/collection" element={<CatalogPage />} />
          <Route path="/collections" element={<CollectionsPage />} />
          <Route path="/maisons" element={<MaisonsIndex />} />
          <Route path="/maisons/:slug" element={<MaisonDetail />} />
          <Route path="/heritage" element={<HeritagePage />} />
          <Route path="/craftsmanship" element={<CraftsmanshipPage />} />
          <Route path="/product/:id" element={<ProductDetailPage />} />
          <Route path="/compare" element={<ComparePage />} />
          <Route path="/cart" element={<CartPage />} />
          <Route path="/login" element={<LoginPage />} />
          <Route path="/register" element={<RegisterPage />} />
          <Route
            path="/checkout"
            element={<ProtectedRoute><CheckoutPage /></ProtectedRoute>}
          />
          <Route
            path="/profile"
            element={<ProtectedRoute><ProfilePage /></ProtectedRoute>}
          />
          <Route
            path="/orders"
            element={<ProtectedRoute><OrdersPage /></ProtectedRoute>}
          />
          <Route
            path="/orders/:id"
            element={<ProtectedRoute><OrderDetailPage /></ProtectedRoute>}
          />
          <Route
            path="/checkout/:outcome"
            element={<ProtectedRoute><CheckoutResultPage /></ProtectedRoute>}
          />
          <Route path="/order-confirmation/:id" element={<LegacyConfirmation />} />
          <Route path="/admin" element={<ProtectedRoute adminOnly><AdminLayout /></ProtectedRoute>}>
            <Route index element={<AdminOverview />} />
            <Route path="products" element={<AdminProducts />} />
            <Route path="inventory" element={<AdminInventory />} />
            <Route path="orders" element={<AdminOrdersList />} />
            <Route path="orders/:id" element={<AdminOrderDetail />} />
            <Route path="payments" element={<AdminPayments />} />
            <Route path="customers" element={<AdminCustomersList />} />
            <Route path="customers/:id" element={<AdminCustomerDetail />} />
          </Route>
          <Route path="*" element={<NotFoundPage />} />
        </Route>
      </Routes>
    </Suspense>
  );
}
