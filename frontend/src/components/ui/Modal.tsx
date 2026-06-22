import { useEffect, type ReactNode } from 'react';
import { cn } from '@/lib/cn';
import { Icon } from './Icon';

export function Modal({
  open,
  onClose,
  title,
  children,
  footer,
  size = 'md',
}: {
  open: boolean;
  onClose: () => void;
  title?: ReactNode;
  children: ReactNode;
  footer?: ReactNode;
  size?: 'sm' | 'md' | 'lg';
}) {
  useEffect(() => {
    if (!open) return;
    const onKey = (e: KeyboardEvent) => e.key === 'Escape' && onClose();
    document.addEventListener('keydown', onKey);
    document.body.style.overflow = 'hidden';
    return () => {
      document.removeEventListener('keydown', onKey);
      document.body.style.overflow = '';
    };
  }, [open, onClose]);

  if (!open) return null;

  const sizes = { sm: 'max-w-sm', md: 'max-w-lg', lg: 'max-w-2xl' };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
      <div className="absolute inset-0 bg-ink/40 backdrop-blur-sm" onClick={onClose} />
      <div
        role="dialog"
        aria-modal="true"
        className={cn(
          'relative z-10 w-full rounded-lg bg-white shadow-card',
          sizes[size],
        )}
      >
        {title && (
          <div className="flex items-center justify-between border-b border-divider px-5 py-4">
            <h3 className="text-base font-semibold text-ink">{title}</h3>
            <button onClick={onClose} className="text-ink-muted hover:text-ink" aria-label="Đóng">
              <Icon name="close" />
            </button>
          </div>
        )}
        <div className="max-h-[70vh] overflow-y-auto px-5 py-4">{children}</div>
        {footer && (
          <div className="flex justify-end gap-2 border-t border-divider px-5 py-3">{footer}</div>
        )}
      </div>
    </div>
  );
}

/** Confirmation dialog convenience wrapper. */
export function ConfirmDialog({
  open,
  onClose,
  onConfirm,
  title,
  message,
  confirmLabel = 'Xác nhận',
  danger,
  loading,
}: {
  open: boolean;
  onClose: () => void;
  onConfirm: () => void;
  title: string;
  message: ReactNode;
  confirmLabel?: string;
  danger?: boolean;
  loading?: boolean;
}) {
  return (
    <Modal open={open} onClose={onClose} title={title} size="sm">
      <p className="text-sm text-ink-secondary">{message}</p>
      <div className="mt-5 flex justify-end gap-2">
        <button
          onClick={onClose}
          className="h-10 rounded border border-border px-4 text-sm hover:bg-page"
        >
          Hủy
        </button>
        <button
          onClick={onConfirm}
          disabled={loading}
          className={cn(
            'h-10 rounded px-4 text-sm font-medium text-white disabled:opacity-60',
            danger ? 'bg-danger hover:bg-danger/90' : 'bg-brand hover:bg-brand-hover',
          )}
        >
          {confirmLabel}
        </button>
      </div>
    </Modal>
  );
}
