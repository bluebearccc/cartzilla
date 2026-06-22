import { cn } from '@/lib/cn';
import { Icon } from './Icon';

/** Page index is 0-based (matches Spring Page). */
export function Pagination({
  page,
  totalPages,
  onChange,
}: {
  page: number;
  totalPages: number;
  onChange: (page: number) => void;
}) {
  if (totalPages <= 1) return null;

  const pages: number[] = [];
  const start = Math.max(0, Math.min(page - 2, totalPages - 5));
  const end = Math.min(totalPages, start + 5);
  for (let i = start; i < end; i++) pages.push(i);

  const btn = 'inline-flex h-9 min-w-9 items-center justify-center rounded border px-2 text-sm';

  return (
    <nav className="flex items-center justify-center gap-1.5" aria-label="Phân trang">
      <button
        className={cn(btn, 'border-border disabled:opacity-40')}
        onClick={() => onChange(page - 1)}
        disabled={page <= 0}
        aria-label="Trang trước"
      >
        <Icon name="chevron_left" className="text-[18px]" />
      </button>
      {start > 0 && <span className="px-1 text-ink-muted">…</span>}
      {pages.map((p) => (
        <button
          key={p}
          className={cn(
            btn,
            p === page ? 'border-brand bg-brand text-white' : 'border-border hover:bg-page',
          )}
          onClick={() => onChange(p)}
        >
          {p + 1}
        </button>
      ))}
      {end < totalPages && <span className="px-1 text-ink-muted">…</span>}
      <button
        className={cn(btn, 'border-border disabled:opacity-40')}
        onClick={() => onChange(page + 1)}
        disabled={page >= totalPages - 1}
        aria-label="Trang sau"
      >
        <Icon name="chevron_right" className="text-[18px]" />
      </button>
    </nav>
  );
}
