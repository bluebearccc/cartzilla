import { NavLink, Outlet, useNavigate } from 'react-router-dom';
import { StorefrontHeader } from './StorefrontHeader';
import { StorefrontFooter } from './StorefrontFooter';
import { Icon } from '@/components/ui/Icon';
import { useAuth } from '@/store/auth';
import { cn } from '@/lib/cn';

const links = [
  { to: '/profile', label: 'Hồ sơ', icon: 'person' },
  { to: '/addresses', label: 'Địa chỉ', icon: 'location_on' },
  { to: '/orders', label: 'Đơn hàng', icon: 'receipt_long' },
  { to: '/notifications', label: 'Thông báo', icon: 'notifications' },
];

export function AccountLayout() {
  const { logout } = useAuth();
  const navigate = useNavigate();
  return (
    <div className="flex min-h-screen flex-col">
      <StorefrontHeader />
      <main className="mx-auto w-full max-w-7xl flex-1 px-4 py-8 sm:px-6">
        <div className="grid gap-8 md:grid-cols-[240px_1fr]">
          <aside className="h-fit rounded-lg border border-border bg-white p-2">
            <nav className="flex gap-1 overflow-x-auto md:flex-col">
              {links.map((l) => (
                <NavLink
                  key={l.to}
                  to={l.to}
                  className={({ isActive }) =>
                    cn(
                      'flex shrink-0 items-center gap-3 rounded px-3 py-2.5 text-sm font-medium',
                      isActive
                        ? 'bg-brand-tint text-brand'
                        : 'text-ink-secondary hover:bg-page hover:text-ink',
                    )
                  }
                >
                  <Icon name={l.icon} className="text-[20px]" />
                  {l.label}
                </NavLink>
              ))}
              <button
                onClick={() => logout().then(() => navigate('/'))}
                className="flex shrink-0 items-center gap-3 rounded px-3 py-2.5 text-sm font-medium text-danger hover:bg-page"
              >
                <Icon name="logout" className="text-[20px]" />
                Đăng xuất
              </button>
            </nav>
          </aside>
          <div className="min-w-0">
            <Outlet />
          </div>
        </div>
      </main>
      <StorefrontFooter />
    </div>
  );
}
