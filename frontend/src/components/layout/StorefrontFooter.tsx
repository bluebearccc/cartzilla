import { Link } from 'react-router-dom';
import { Icon } from '@/components/ui/Icon';

const columns = [
  {
    title: 'Về Cartzilla',
    links: ['Giới thiệu', 'Tuyển dụng', 'Tin tức', 'Liên hệ'],
  },
  {
    title: 'Hỗ trợ',
    links: ['Trung tâm trợ giúp', 'Hướng dẫn mua hàng', 'Vận chuyển', 'Trả hàng & hoàn tiền'],
  },
  {
    title: 'Chính sách',
    links: ['Điều khoản dịch vụ', 'Chính sách bảo mật', 'Chính sách đổi trả', 'Bảo hành'],
  },
];

export function StorefrontFooter() {
  return (
    <footer className="mt-16 border-t border-border bg-white">
      <div className="mx-auto max-w-7xl px-4 py-12 sm:px-6">
        <div className="grid grid-cols-2 gap-8 md:grid-cols-5">
          <div className="col-span-2">
            <Link to="/" className="flex items-center gap-2">
              <span className="flex h-9 w-9 items-center justify-center rounded-lg bg-brand text-white">
                <Icon name="shopping_bag" filled className="text-[20px]" />
              </span>
              <span className="font-headline text-xl font-extrabold text-ink">Cartzilla</span>
            </Link>
            <p className="mt-3 max-w-xs text-sm text-ink-secondary">
              Thời trang cho mọi phong cách. Mua sắm dễ dàng, giao hàng nhanh chóng, thanh toán an
              toàn.
            </p>
            <form className="mt-4 flex max-w-sm gap-2" onSubmit={(e) => e.preventDefault()}>
              <input
                placeholder="Nhập email nhận ưu đãi"
                className="h-10 flex-1 rounded border border-border px-3 text-sm focus:border-brand focus:outline-none"
              />
              <button className="h-10 rounded bg-brand px-4 text-sm font-medium text-white hover:bg-brand-hover">
                Đăng ký
              </button>
            </form>
          </div>
          {columns.map((c) => (
            <div key={c.title}>
              <h4 className="mb-3 text-sm font-semibold text-ink">{c.title}</h4>
              <ul className="space-y-2">
                {c.links.map((l) => (
                  <li key={l}>
                    <a href="#" className="text-sm text-ink-secondary hover:text-brand">
                      {l}
                    </a>
                  </li>
                ))}
              </ul>
            </div>
          ))}
        </div>
        <div className="mt-10 flex flex-col items-center justify-between gap-4 border-t border-divider pt-6 sm:flex-row">
          <p className="text-sm text-ink-muted">© 2026 Cartzilla. Đã đăng ký bản quyền.</p>
          <div className="flex items-center gap-2 text-ink-muted">
            <span className="text-xs">Thanh toán:</span>
            <span className="rounded border border-border px-2 py-1 text-xs font-semibold">VNPay</span>
            <span className="rounded border border-border px-2 py-1 text-xs font-semibold">COD</span>
          </div>
        </div>
      </div>
    </footer>
  );
}
