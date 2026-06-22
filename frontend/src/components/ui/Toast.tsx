import { createContext, useCallback, useContext, useState, type ReactNode } from 'react';
import { Icon } from './Icon';
import { cn } from '@/lib/cn';

type ToastTone = 'success' | 'danger' | 'info' | 'warning';
interface Toast {
  id: number;
  tone: ToastTone;
  message: string;
}

interface ToastContextValue {
  notify: (message: string, tone?: ToastTone) => void;
  success: (message: string) => void;
  error: (message: string) => void;
}

const ToastContext = createContext<ToastContextValue | null>(null);

const toneStyle: Record<ToastTone, { bar: string; icon: string }> = {
  success: { bar: 'border-l-success', icon: 'check_circle' },
  danger: { bar: 'border-l-danger', icon: 'error' },
  info: { bar: 'border-l-info', icon: 'info' },
  warning: { bar: 'border-l-warning', icon: 'warning' },
};

export function ToastProvider({ children }: { children: ReactNode }) {
  const [toasts, setToasts] = useState<Toast[]>([]);

  const remove = useCallback((id: number) => {
    setToasts((t) => t.filter((x) => x.id !== id));
  }, []);

  const notify = useCallback(
    (message: string, tone: ToastTone = 'info') => {
      const id = Date.now() + Math.random();
      setToasts((t) => [...t, { id, tone, message }]);
      setTimeout(() => remove(id), 4000);
    },
    [remove],
  );

  const success = useCallback((m: string) => notify(m, 'success'), [notify]);
  const error = useCallback((m: string) => notify(m, 'danger'), [notify]);

  return (
    <ToastContext.Provider value={{ notify, success, error }}>
      {children}
      <div className="pointer-events-none fixed right-4 top-4 z-[100] flex w-80 flex-col gap-2">
        {toasts.map((t) => (
          <div
            key={t.id}
            className={cn(
              'pointer-events-auto flex items-start gap-2 rounded border border-l-4 bg-white px-3 py-2.5 shadow-card',
              toneStyle[t.tone].bar,
            )}
          >
            <Icon
              name={toneStyle[t.tone].icon}
              className={cn(
                'text-[20px]',
                t.tone === 'success' && 'text-success',
                t.tone === 'danger' && 'text-danger',
                t.tone === 'info' && 'text-info',
                t.tone === 'warning' && 'text-warning',
              )}
            />
            <p className="flex-1 text-sm text-ink">{t.message}</p>
            <button onClick={() => remove(t.id)} className="text-ink-muted hover:text-ink">
              <Icon name="close" className="text-[16px]" />
            </button>
          </div>
        ))}
      </div>
    </ToastContext.Provider>
  );
}

export function useToast(): ToastContextValue {
  const ctx = useContext(ToastContext);
  if (!ctx) throw new Error('useToast must be used within <ToastProvider>');
  return ctx;
}
