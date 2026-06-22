import type { ReactNode } from 'react';
import { Icon } from './Icon';
import { Button } from './Button';

export function EmptyState({
  icon = 'inbox',
  title,
  subtitle,
  action,
}: {
  icon?: string;
  title: string;
  subtitle?: string;
  action?: { label: string; onClick: () => void };
}) {
  return (
    <div className="flex flex-col items-center justify-center rounded-lg border border-dashed border-border bg-white px-6 py-16 text-center">
      <span className="mb-4 flex h-16 w-16 items-center justify-center rounded-full bg-page text-ink-muted">
        <Icon name={icon} className="text-[32px]" />
      </span>
      <h3 className="text-base font-semibold text-ink">{title}</h3>
      {subtitle && <p className="mt-1 max-w-sm text-sm text-ink-secondary">{subtitle}</p>}
      {action && (
        <Button className="mt-5" onClick={action.onClick}>
          {action.label}
        </Button>
      )}
    </div>
  );
}

export function ErrorState({
  message = 'Đã có lỗi xảy ra.',
  onRetry,
}: {
  message?: string;
  onRetry?: () => void;
}) {
  return (
    <div className="flex flex-col items-center justify-center rounded-lg border border-danger/30 bg-danger-tint/40 px-6 py-12 text-center">
      <Icon name="error" className="mb-2 text-[32px] text-danger" />
      <p className="text-sm text-ink">{message}</p>
      {onRetry && (
        <Button variant="secondary" className="mt-4" onClick={onRetry}>
          Thử lại
        </Button>
      )}
    </div>
  );
}

export function InlineMessage({
  tone = 'danger',
  children,
}: {
  tone?: 'danger' | 'warning' | 'success' | 'info';
  children: ReactNode;
}) {
  const tones = {
    danger: 'bg-danger-tint text-danger',
    warning: 'bg-warning-tint text-warning',
    success: 'bg-success-tint text-success',
    info: 'bg-info-tint text-info',
  } as const;
  return <div className={`rounded px-3 py-2.5 text-sm ${tones[tone]}`}>{children}</div>;
}
