import type { ReactNode } from 'react';
import { cn } from '@/lib/cn';

export function Card({
  children,
  className,
  title,
  action,
}: {
  children: ReactNode;
  className?: string;
  title?: ReactNode;
  action?: ReactNode;
}) {
  return (
    <section className={cn('rounded-lg border border-border bg-white shadow-card', className)}>
      {(title || action) && (
        <header className="flex items-center justify-between border-b border-divider px-5 py-4">
          {title && <h3 className="text-base font-semibold text-ink">{title}</h3>}
          {action}
        </header>
      )}
      <div className={cn(title || action ? 'p-5' : 'p-5')}>{children}</div>
    </section>
  );
}
