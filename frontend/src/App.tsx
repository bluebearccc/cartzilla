import { Routes, Route } from 'react-router-dom';
import { StorefrontLayout } from '@/components/layout/StorefrontLayout';
import { AccountLayout } from '@/components/layout/AccountLayout';
import { DashboardLayout } from '@/components/layout/DashboardLayout';
import { ProtectedRoute, RoleRoute } from '@/routes/guards';
import { UnderConstruction, NotFound } from '@/pages/misc/UnderConstruction';

// Storefront
import { HomePage } from '@/pages/storefront/HomePage';
import { ProductListPage } from '@/pages/storefront/ProductListPage';
import { ProductDetailPage } from '@/pages/storefront/ProductDetailPage';
// Auth
import { LoginPage } from '@/pages/auth/LoginPage';
import { RegisterPage } from '@/pages/auth/RegisterPage';
import { VerifyEmailPage } from '@/pages/auth/VerifyEmailPage';
import { OAuthCallbackPage } from '@/pages/auth/OAuthCallbackPage';
import { ForgotPasswordPage } from '@/pages/auth/ForgotPasswordPage';
import { ResetPasswordPage } from '@/pages/auth/ResetPasswordPage';
// Account
import { ProfilePage } from '@/pages/account/ProfilePage';
import { AddressPage } from '@/pages/account/AddressPage';
import { NotificationPage } from '@/pages/account/NotificationPage';
// Purchase
import { CartPage } from '@/pages/purchase/CartPage';
import { CheckoutPage } from '@/pages/purchase/CheckoutPage';
import { PaymentPage } from '@/pages/purchase/PaymentPage';
import { PaymentResultPage } from '@/pages/purchase/PaymentResultPage';
// Orders
import { OrderListPage } from '@/pages/orders/OrderListPage';
import { OrderDetailPage } from '@/pages/orders/OrderDetailPage';
// Staff / Admin
import { StaffOrderListPage } from '@/pages/staff/StaffOrderListPage';
import { StaffOrderDetailPage } from '@/pages/staff/StaffOrderDetailPage';
import { AdminProductPage } from '@/pages/admin/AdminProductPage';
import { AdminProductForm } from '@/pages/admin/AdminProductForm';
import { AdminCategoryPage } from '@/pages/admin/AdminCategoryPage';
import { AdminVendorPage } from '@/pages/admin/AdminVendorPage';
import { AdminVoucherPage } from '@/pages/admin/AdminVoucherPage';
import { AdminUserPage } from '@/pages/admin/AdminUserPage';
import { AdminReportsPage } from '@/pages/admin/AdminReportsPage';

export function App() {
  return (
    <Routes>
      {/* Auth (standalone, no storefront chrome) */}
      <Route path="/login" element={<LoginPage />} />
      <Route path="/register" element={<RegisterPage />} />
      <Route path="/verify-email" element={<VerifyEmailPage />} />
      <Route path="/oauth/callback" element={<OAuthCallbackPage />} />
      <Route path="/forgot-password" element={<ForgotPasswordPage />} />
      <Route path="/reset-password" element={<ResetPasswordPage />} />

      {/* Storefront + purchase (storefront chrome) */}
      <Route element={<StorefrontLayout />}>
        <Route path="/" element={<HomePage />} />
        <Route path="/products" element={<ProductListPage />} />
        <Route path="/products/:id" element={<ProductDetailPage />} />
        <Route element={<ProtectedRoute />}>
          <Route path="/cart" element={<CartPage />} />
          <Route path="/checkout" element={<CheckoutPage />} />
          <Route path="/checkout/payment" element={<PaymentPage />} />
          <Route path="/payment/result" element={<PaymentResultPage />} />
        </Route>
      </Route>

      {/* Customer account (storefront header + account menu) */}
      <Route element={<ProtectedRoute />}>
        <Route element={<AccountLayout />}>
          <Route path="/profile" element={<ProfilePage />} />
          <Route path="/addresses" element={<AddressPage />} />
          <Route path="/notifications" element={<NotificationPage />} />
          <Route path="/orders" element={<OrderListPage />} />
          <Route path="/orders/:id" element={<OrderDetailPage />} />
        </Route>
      </Route>

      {/* Staff / Admin dashboard */}
      <Route element={<RoleRoute roles={['STAFF', 'ADMIN']} />}>
        <Route element={<DashboardLayout />}>
          <Route path="/staff/orders" element={<StaffOrderListPage />} />
          <Route path="/staff/orders/:id" element={<StaffOrderDetailPage />} />
        </Route>
      </Route>
      <Route element={<RoleRoute roles={['ADMIN']} />}>
        <Route element={<DashboardLayout />}>
          <Route path="/admin/products" element={<AdminProductPage />} />
          <Route path="/admin/products/new" element={<AdminProductForm />} />
          <Route path="/admin/products/:id/edit" element={<AdminProductForm />} />
          <Route path="/admin/categories" element={<AdminCategoryPage />} />
          <Route path="/admin/vendors" element={<AdminVendorPage />} />
          <Route path="/admin/vouchers" element={<AdminVoucherPage />} />
          <Route path="/admin/users" element={<AdminUserPage />} />
          <Route path="/admin/reports" element={<AdminReportsPage />} />
        </Route>
      </Route>

      <Route path="/under-construction" element={<UnderConstruction title="Đang xây dựng" />} />
      <Route path="*" element={<NotFound />} />
    </Routes>
  );
}
