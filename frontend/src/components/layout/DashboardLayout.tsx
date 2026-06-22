import { useState } from 'react';
import { Link, NavLink, Outlet, useNavigate } from 'react-router-dom';
import { Icon } from '@/components/ui/Icon';
import { useAuth } from '@/store/auth';
import { cn } from '@/lib/cn';

interface NavItem {
  to: string;
  label: string;
  icon: string;
  adminOnly?: boolean;
}

const navItems: NavItem[] = [
  { to: '/staff/orders', label: 'Đơn hàng', icon: 'receipt_long' },
  { to: '/admin/products', label: 'Sản phẩm', icon: 'inventory_2', adminOnly: true },
  { to: '/admin/categories', label: 'Danh mục', icon: 'category', adminOnly: true },
  { to: '/admin/vendors', label: 'Nhà cung cấp', icon: 'store', adminOnly: true },
  { to: '/admin/vouchers', label: 'Mã giảm giá', icon: 'sell', adminOnly: true },
  { to: '/admin/users', label: 'Người dùng', icon: 'group', adminOnly: true },
  { to: '/admin/reports', label: 'Báo cáo', icon: 'monitoring', adminOnly: true },
];

export function DashboardLayout() {
  const { session, hasRole, logout } = useAuth();
  const navigate = useNavigate();
  const [open, setOpen] = useState(false);
  const isAdmin = hasRole('ADMIN');
  const items = navItems.filter((i) => !i.adminOnly || isAdmin);

  return (
    <div className="min-h-screen bg-page">
      {/* Sidebar */}
      <aside
        className={cn(
          'fixed inset-y-0 left-0 z-40 w-64 transform border-r border-border bg-white transition-transform md:translate-x-0',
          open ? 'translate-x-0' : '-translate-x-full',
        )}
      >
        <div className="flex h-16 items-center gap-2 border-b border-divider px-5">
          <span className="flex h-8 w-8 items-center justify-center rounded-lg bg-brand text-white">
            <Icon name="shopping_bag" filled className="text-[18px]" />
          </span>
          <span className="font-headline text-lg font-extrabold text-ink">Cartzilla</span>
        </div>
        <nav className="flex flex-col gap-1 p-3">
          {items.map((i) => (
            <NavLink
              key={i.to}
              to={i.to}
              onClick={() => setOpen(false)}
              className={({ isActive }) =>
                cn(
                  'flex items-center gap-3 rounded px-3 py-2.5 text-sm font-medium',
                  isActive
                    ? 'border-l-[3px] border-brand bg-brand-tint pl-2.5 text-brand'
                    : 'text-ink-secondary hover:bg-page hover:text-ink',
                )
              }
            >
              <Icon name={i.icon} className="text-[20px]" />
              {i.label}
            </NavLink>
          ))}
        </nav>
        <div className="absolute inset-x-0 bottom-0 border-t border-divider p-3">
          <Link to="/" className="flex items-center gap-2 rounded px-3 py-2 text-sm text-ink-secondary hover:bg-page">
            <Icon name="storefront" className="text-[20px]" />
            Về cửa hàng
          </Link>
        </div>
      </aside>

      {open && <div className="fixed inset-0 z-30 bg-ink/30 md:hidden" onClick={() => setOpen(false)} />}

      {/* Content */}
      <div className="md:pl-64">
        <header className="sticky top-0 z-20 flex h-16 items-center justify-between border-b border-border bg-white px-4 sm:px-6">
          <button className="md:hidden" onClick={() => setOpen(true)} aria-label="Mở menu">
            <Icon name="menu" />
          </button>
          <div className="flex flex-1 items-center justify-end gap-3">
            <span className="text-sm text-ink-secondary">{session?.email}</span>
            <span className="flex h-9 w-9 items-center justify-center rounded-full bg-brand-tint text-sm font-semibold text-brand">
              {session?.email?.[0]?.toUpperCase() ?? 'U'}
            </span>
            <button
              onClick={() => logout().then(() => navigate('/login'))}
              className="text-ink-muted hover:text-danger"
              aria-label="Đăng xuất"
            >
              <Icon name="logout" />
            </button>
          </div>
        </header>
        <main className="p-4 sm:p-6">
          <Outlet />
        </main>
      </div>
    </div>
  );
}
