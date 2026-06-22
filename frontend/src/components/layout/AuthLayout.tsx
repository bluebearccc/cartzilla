import type { ReactNode } from 'react';
import { Link } from 'react-router-dom';
import { Icon } from '@/components/ui/Icon';

/** Split layout: left brand panel (45%) + right form area (55%). For login/register. */
export function AuthLayout({ children }: { children: ReactNode }) {
  return (
    <div className="flex min-h-screen">
      <div className="relative hidden w-[45%] flex-col justify-between overflow-hidden bg-brand p-12 text-white lg:flex">
        <img
          src="https://images.unsplash.com/photo-1441986300917-64674bd600d8?w=1200&q=80"
          alt=""
          className="absolute inset-0 h-full w-full object-cover opacity-30"
        />
        <Link to="/" className="relative flex items-center gap-2">
          <span className="flex h-10 w-10 items-center justify-center rounded-lg bg-white/20 backdrop-blur">
            <Icon name="shopping_bag" filled className="text-[22px]" />
          </span>
          <span className="font-headline text-2xl font-extrabold">Cartzilla</span>
        </Link>
        <div className="relative">
          <h2 className="font-headline text-4xl font-extrabold leading-tight">
            Thời trang cho mọi phong cách
          </h2>
          <p className="mt-4 max-w-sm text-white/80">
            Hàng ngàn sản phẩm chính hãng, giao hàng nhanh, thanh toán an toàn với VNPay & COD.
          </p>
        </div>
        <p className="relative text-sm text-white/60">© 2026 Cartzilla</p>
      </div>
      <div className="flex flex-1 items-center justify-center bg-page px-4 py-10">
        <div className="w-full max-w-md">{children}</div>
      </div>
    </div>
  );
}

/** Centered minimal card (no brand panel) for verify/forgot/reset/oauth screens. */
export function CenteredAuthLayout({ children }: { children: ReactNode }) {
  return (
    <div className="flex min-h-screen flex-col items-center justify-center bg-page px-4 py-10">
      <Link to="/" className="mb-6 flex items-center gap-2">
        <span className="flex h-9 w-9 items-center justify-center rounded-lg bg-brand text-white">
          <Icon name="shopping_bag" filled className="text-[20px]" />
        </span>
        <span className="font-headline text-xl font-extrabold text-ink">Cartzilla</span>
      </Link>
      <div className="w-full max-w-md rounded-lg border border-border bg-white p-8 shadow-card">
        {children}
      </div>
    </div>
  );
}
