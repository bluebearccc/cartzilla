import { Link } from 'react-router-dom';
import { Icon } from '@/components/ui/Icon';

/** Temporary placeholder for screens not yet ported. */
export function UnderConstruction({ title }: { title: string }) {
  return (
    <div className="flex min-h-[60vh] flex-col items-center justify-center px-6 text-center">
      <span className="mb-4 flex h-16 w-16 items-center justify-center rounded-full bg-brand-tint text-brand">
        <Icon name="construction" className="text-[32px]" />
      </span>
      <h1 className="font-headline text-2xl font-bold text-ink">{title}</h1>
      <p className="mt-2 text-ink-secondary">Màn hình này đang được hoàn thiện.</p>
      <Link to="/" className="mt-4 rounded bg-brand px-4 py-2 text-sm font-medium text-white">
        Về trang chủ
      </Link>
    </div>
  );
}

export function NotFound() {
  return (
    <div className="flex min-h-[70vh] flex-col items-center justify-center px-6 text-center">
      <p className="font-headline text-6xl font-extrabold text-brand">404</p>
      <h1 className="mt-2 font-headline text-2xl font-bold text-ink">Không tìm thấy trang</h1>
      <p className="mt-2 text-ink-secondary">Trang bạn tìm không tồn tại hoặc đã bị di chuyển.</p>
      <Link to="/" className="mt-4 rounded bg-brand px-4 py-2 text-sm font-medium text-white">
        Về trang chủ
      </Link>
    </div>
  );
}
