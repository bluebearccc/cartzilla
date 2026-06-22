import { useState, useRef, useEffect } from 'react';
import { Link, NavLink, useNavigate } from 'react-router-dom';
import { useAuth } from '@/store/auth';
import { useCart } from '@/store/cart';
import { Icon } from '@/components/ui/Icon';
import { cn } from '@/lib/cn';

export function StorefrontHeader() {
  const { session, isAuthenticated, logout } = useAuth();
  const { count } = useCart();
  const navigate = useNavigate();
  const [query, setQuery] = useState('');
  const [menuOpen, setMenuOpen] = useState(false);
  const menuRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    const onClick = (e: MouseEvent) => {
      if (menuRef.current && !menuRef.current.contains(e.target as Node)) setMenuOpen(false);
    };
    document.addEventListener('mousedown', onClick);
    return () => document.removeEventListener('mousedown', onClick);
  }, []);

  const onSearch = (e: React.FormEvent) => {
    e.preventDefault();
    navigate(`/products?q=${encodeURIComponent(query.trim())}`);
  };

  return (
    <header className="sticky top-0 z-40 border-b border-border bg-white/95 backdrop-blur">
      <div className="mx-auto flex h-16 max-w-7xl items-center gap-4 px-4 sm:px-6">
        <Link to="/" className="flex items-center gap-2 shrink-0">
          <span className="flex h-9 w-9 items-center justify-center rounded-lg bg-brand text-white">
            <Icon name="shopping_bag" filled className="text-[20px]" />
          </span>
          <span className="font-headline text-xl font-extrabold tracking-tight text-ink">
            Cartzilla
          </span>
        </Link>

        <form onSubmit={onSearch} className="relative hidden flex-1 md:block">
          <Icon
            name="search"
            className="absolute left-3 top-1/2 -translate-y-1/2 text-[20px] text-ink-muted"
          />
          <input
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            placeholder="Tìm sản phẩm..."
            className="h-10 w-full rounded-full border border-border bg-page pl-10 pr-4 text-sm focus:border-brand focus:outline-none focus:ring-2 focus:ring-brand/20"
          />
        </form>

        <nav className="hidden items-center gap-1 lg:flex">
          {[
            { to: '/', label: 'Trang chủ' },
            { to: '/products', label: 'Sản phẩm' },
          ].map((l) => (
            <NavLink
              key={l.to}
              to={l.to}
              end={l.to === '/'}
              className={({ isActive }) =>
                cn(
                  'rounded px-3 py-2 text-sm font-medium',
                  isActive ? 'text-brand' : 'text-ink-secondary hover:text-ink',
                )
              }
            >
              {l.label}
            </NavLink>
          ))}
        </nav>

        <div className="flex items-center gap-1">
          {isAuthenticated && (
            <Link
              to="/notifications"
              className="relative flex h-10 w-10 items-center justify-center rounded-full text-ink-secondary hover:bg-page"
              aria-label="Thông báo"
            >
              <Icon name="notifications" />
            </Link>
          )}
          <Link
            to="/cart"
            className="relative flex h-10 w-10 items-center justify-center rounded-full text-ink-secondary hover:bg-page"
            aria-label="Giỏ hàng"
          >
            <Icon name="shopping_cart" />
            {count > 0 && (
              <span className="absolute right-1 top-1 flex h-4 min-w-4 items-center justify-center rounded-full bg-danger px-1 text-[10px] font-semibold text-white">
                {count}
              </span>
            )}
          </Link>

          {isAuthenticated ? (
            <div className="relative" ref={menuRef}>
              <button
                onClick={() => setMenuOpen((o) => !o)}
                className="flex h-10 items-center gap-2 rounded-full pl-1 pr-2 hover:bg-page"
              >
                <span className="flex h-8 w-8 items-center justify-center rounded-full bg-brand-tint text-sm font-semibold text-brand">
                  {session?.email?.[0]?.toUpperCase() ?? 'U'}
                </span>
                <Icon name="expand_more" className="text-[18px] text-ink-muted" />
              </button>
              {menuOpen && (
                <div className="absolute right-0 mt-1 w-56 rounded-lg border border-border bg-white py-1 shadow-card">
                  <div className="border-b border-divider px-4 py-2">
                    <p className="truncate text-sm font-medium text-ink">{session?.email}</p>
                    <p className="text-xs text-ink-muted">{roleLabel(session?.role)}</p>
                  </div>
                  {accountLinks.map((l) => (
                    <Link
                      key={l.to}
                      to={l.to}
                      onClick={() => setMenuOpen(false)}
                      className="flex items-center gap-2 px-4 py-2 text-sm text-ink hover:bg-page"
                    >
                      <Icon name={l.icon} className="text-[18px] text-ink-muted" />
                      {l.label}
                    </Link>
                  ))}
                  {(session?.role === 'STAFF' || session?.role === 'ADMIN') && (
                    <Link
                      to="/staff/orders"
                      onClick={() => setMenuOpen(false)}
                      className="flex items-center gap-2 px-4 py-2 text-sm text-ink hover:bg-page"
                    >
                      <Icon name="dashboard" className="text-[18px] text-ink-muted" />
                      Trang quản trị
                    </Link>
                  )}
                  <button
                    onClick={() => {
                      setMenuOpen(false);
                      logout().then(() => navigate('/'));
                    }}
                    className="flex w-full items-center gap-2 border-t border-divider px-4 py-2 text-sm text-danger hover:bg-page"
                  >
                    <Icon name="logout" className="text-[18px]" />
                    Đăng xuất
                  </button>
                </div>
              )}
            </div>
          ) : (
            <div className="ml-1 hidden items-center gap-2 sm:flex">
              <Link
                to="/login"
                className="rounded px-3 py-2 text-sm font-medium text-ink hover:bg-page"
              >
                Đăng nhập
              </Link>
              <Link
                to="/register"
                className="rounded bg-brand px-4 py-2 text-sm font-medium text-white hover:bg-brand-hover"
              >
                Đăng ký
              </Link>
            </div>
          )}
        </div>
      </div>
    </header>
  );
}

const accountLinks = [
  { to: '/profile', label: 'Hồ sơ', icon: 'person' },
  { to: '/orders', label: 'Đơn hàng', icon: 'receipt_long' },
  { to: '/addresses', label: 'Địa chỉ', icon: 'location_on' },
  { to: '/notifications', label: 'Thông báo', icon: 'notifications' },
];

function roleLabel(role?: string) {
  if (role === 'ADMIN') return 'Quản trị viên';
  if (role === 'STAFF') return 'Nhân viên';
  return 'Khách hàng';
}
