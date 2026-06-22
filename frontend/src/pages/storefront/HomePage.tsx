import { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { catalogApi } from '@/services/catalog';
import { ProductCard } from '@/components/product/ProductCard';
import { ProductCardSkeleton } from '@/components/ui/Skeleton';
import { Icon } from '@/components/ui/Icon';

const slides = [
  {
    label: 'Bộ sưu tập mới',
    title: 'Phong cách hiện đại 2026',
    subtitle: 'Khám phá xu hướng thời trang mới nhất, định hình cá tính của bạn.',
    image: 'https://images.unsplash.com/photo-1490481651871-ab68de25d43d?w=1600&q=80',
  },
  {
    label: 'Ưu đãi mùa hè',
    title: 'Giảm đến 30% toàn bộ',
    subtitle: 'Hàng ngàn sản phẩm giảm giá sốc, số lượng có hạn.',
    image: 'https://images.unsplash.com/photo-1483985988355-763728e1935b?w=1600&q=80',
  },
  {
    label: 'Tối giản tinh tế',
    title: 'Minimalist Essentials',
    subtitle: 'Những món đồ cơ bản không thể thiếu trong tủ đồ của bạn.',
    image: 'https://images.unsplash.com/photo-1445205170230-053b83016050?w=1600&q=80',
  },
];

const categoryShortcuts = [
  { name: 'Áo', icon: 'checkroom' },
  { name: 'Quần', icon: 'apparel' },
  { name: 'Đầm', icon: 'woman' },
  { name: 'Giày', icon: 'footprint' },
  { name: 'Túi', icon: 'shopping_bag' },
  { name: 'Phụ kiện', icon: 'watch' },
];

const trustItems = [
  { icon: 'local_shipping', title: 'Miễn phí vận chuyển', sub: 'Cho đơn từ 500.000 ₫' },
  { icon: 'autorenew', title: 'Đổi trả 7 ngày', sub: 'Miễn phí đổi trả' },
  { icon: 'verified_user', title: 'Thanh toán VNPay', sub: 'An toàn & bảo mật' },
  { icon: 'support_agent', title: 'Hỗ trợ 24/7', sub: 'Luôn sẵn sàng hỗ trợ' },
];

function Hero() {
  const [active, setActive] = useState(0);
  useEffect(() => {
    const t = setInterval(() => setActive((a) => (a + 1) % slides.length), 5000);
    return () => clearInterval(t);
  }, []);
  const slide = slides[active];
  return (
    <div className="relative aspect-[16/7] w-full overflow-hidden rounded-lg bg-ink md:aspect-[16/6]">
      {slides.map((s, i) => (
        <img
          key={i}
          src={s.image}
          alt=""
          className={`absolute inset-0 h-full w-full object-cover transition-opacity duration-700 ${i === active ? 'opacity-100' : 'opacity-0'}`}
        />
      ))}
      <div className="absolute inset-0 bg-gradient-to-r from-ink/70 to-transparent" />
      <div className="relative flex h-full max-w-2xl flex-col justify-center px-6 sm:px-12">
        <span className="mb-3 w-fit rounded-full bg-white/20 px-3 py-1 text-xs font-semibold text-white backdrop-blur">
          {slide.label}
        </span>
        <h1 className="font-headline text-3xl font-extrabold text-white sm:text-5xl">{slide.title}</h1>
        <p className="mt-3 max-w-md text-sm text-white/90 sm:text-base">{slide.subtitle}</p>
        <Link
          to="/products"
          className="mt-6 inline-flex w-fit items-center gap-2 rounded bg-white px-6 py-3 text-sm font-semibold text-ink hover:bg-white/90"
        >
          Mua ngay <Icon name="arrow_forward" className="text-[18px]" />
        </Link>
      </div>
      <div className="absolute bottom-4 left-1/2 flex -translate-x-1/2 gap-2">
        {slides.map((_, i) => (
          <button
            key={i}
            onClick={() => setActive(i)}
            className={`h-2 rounded-full transition-all ${i === active ? 'w-6 bg-white' : 'w-2 bg-white/50'}`}
            aria-label={`Slide ${i + 1}`}
          />
        ))}
      </div>
    </div>
  );
}

export function HomePage() {
  const { data, isLoading } = useQuery({
    queryKey: ['products', 'featured'],
    queryFn: () => catalogApi.listProducts({ featured: true, limit: 8 }),
  });
  const products = data?.items ?? [];

  return (
    <div className="mx-auto max-w-7xl px-4 py-6 sm:px-6">
      <Hero />

      {/* Category shortcuts */}
      <div className="mt-10 grid grid-cols-3 gap-4 sm:grid-cols-6">
        {categoryShortcuts.map((c) => (
          <Link key={c.name} to={`/products?q=${encodeURIComponent(c.name)}`} className="group flex flex-col items-center gap-2">
            <span className="flex h-16 w-16 items-center justify-center rounded-full bg-brand-tint text-brand transition group-hover:bg-brand group-hover:text-white">
              <Icon name={c.icon} className="text-[28px]" />
            </span>
            <span className="text-sm font-medium text-ink">{c.name}</span>
          </Link>
        ))}
      </div>

      {/* Featured products */}
      <section className="mt-12">
        <div className="mb-5 flex items-end justify-between">
          <h2 className="font-headline text-2xl font-bold text-ink">Sản phẩm nổi bật</h2>
          <Link to="/products" className="flex items-center gap-1 text-sm font-medium text-brand hover:underline">
            Xem tất cả <Icon name="arrow_forward" className="text-[16px]" />
          </Link>
        </div>
        <div className="grid grid-cols-2 gap-4 sm:grid-cols-3 lg:grid-cols-4">
          {isLoading
            ? Array.from({ length: 8 }).map((_, i) => <ProductCardSkeleton key={i} />)
            : products.map((p) => <ProductCard key={p.id} product={p} />)}
          {!isLoading && products.length === 0 && (
            <p className="col-span-full py-12 text-center text-ink-muted">Chưa có sản phẩm nổi bật.</p>
          )}
        </div>
      </section>

      {/* Promo band */}
      <section className="mt-12 grid gap-4 md:grid-cols-2">
        {[
          { title: 'Thời trang nam', sub: 'Lịch lãm & năng động', color: 'from-indigo-600 to-indigo-400' },
          { title: 'Thời trang nữ', sub: 'Thanh lịch & quyến rũ', color: 'from-pink-500 to-rose-400' },
        ].map((b) => (
          <Link
            key={b.title}
            to="/products"
            className={`relative flex h-40 flex-col justify-center overflow-hidden rounded-lg bg-gradient-to-r ${b.color} px-8 text-white`}
          >
            <h3 className="font-headline text-2xl font-bold">{b.title}</h3>
            <p className="mt-1 text-white/90">{b.sub}</p>
            <span className="mt-3 w-fit rounded bg-white/20 px-4 py-1.5 text-sm font-medium backdrop-blur">
              Khám phá ngay
            </span>
          </Link>
        ))}
      </section>

      {/* Trust band */}
      <section className="mt-12 grid grid-cols-2 gap-4 rounded-lg border border-border bg-white p-6 md:grid-cols-4">
        {trustItems.map((t) => (
          <div key={t.title} className="flex items-center gap-3">
            <span className="flex h-11 w-11 items-center justify-center rounded-full bg-brand-tint text-brand">
              <Icon name={t.icon} className="text-[22px]" />
            </span>
            <div>
              <p className="text-sm font-semibold text-ink">{t.title}</p>
              <p className="text-xs text-ink-muted">{t.sub}</p>
            </div>
          </div>
        ))}
      </section>
    </div>
  );
}
