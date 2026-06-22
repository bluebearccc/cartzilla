import { forwardRef, useId, useState, type InputHTMLAttributes, type TextareaHTMLAttributes, type SelectHTMLAttributes, type ReactNode } from 'react';
import { cn } from '@/lib/cn';
import { Icon } from './Icon';

interface FieldShellProps {
  label?: ReactNode;
  error?: string;
  helper?: string;
  required?: boolean;
  id: string;
  children: ReactNode;
}

function FieldShell({ label, error, helper, required, id, children }: FieldShellProps) {
  return (
    <div className="w-full">
      {label && (
        <label htmlFor={id} className="mb-1.5 block text-sm font-medium text-ink">
          {label}
          {required && <span className="ml-0.5 text-danger">*</span>}
        </label>
      )}
      {children}
      {error ? (
        <p className="mt-1 text-xs text-danger">{error}</p>
      ) : helper ? (
        <p className="mt-1 text-xs text-ink-muted">{helper}</p>
      ) : null}
    </div>
  );
}

const baseField =
  'w-full rounded border bg-white px-3 text-sm text-ink placeholder:text-ink-muted ' +
  'focus:outline-none focus:ring-2 focus:ring-brand/30 focus:border-brand transition disabled:bg-page disabled:text-ink-muted';

interface InputProps extends InputHTMLAttributes<HTMLInputElement> {
  label?: ReactNode;
  error?: string;
  helper?: string;
}

export const Input = forwardRef<HTMLInputElement, InputProps>(function Input(
  { label, error, helper, required, className, id, type = 'text', ...rest },
  ref,
) {
  const autoId = useId();
  const fieldId = id ?? autoId;
  const [show, setShow] = useState(false);
  const isPassword = type === 'password';
  const effectiveType = isPassword ? (show ? 'text' : 'password') : type;

  return (
    <FieldShell label={label} error={error} helper={helper} required={required} id={fieldId}>
      <div className="relative">
        <input
          ref={ref}
          id={fieldId}
          type={effectiveType}
          className={cn(
            baseField,
            'h-11',
            isPassword && 'pr-10',
            error && 'border-danger focus:border-danger focus:ring-danger/30',
            !error && 'border-border',
            className,
          )}
          {...rest}
        />
        {isPassword && (
          <button
            type="button"
            tabIndex={-1}
            onClick={() => setShow((s) => !s)}
            className="absolute right-2 top-1/2 -translate-y-1/2 text-ink-muted hover:text-ink"
            aria-label={show ? 'Ẩn mật khẩu' : 'Hiện mật khẩu'}
          >
            <Icon name={show ? 'visibility_off' : 'visibility'} className="text-[20px]" />
          </button>
        )}
      </div>
    </FieldShell>
  );
});

interface TextareaProps extends TextareaHTMLAttributes<HTMLTextAreaElement> {
  label?: ReactNode;
  error?: string;
  helper?: string;
}

export const Textarea = forwardRef<HTMLTextAreaElement, TextareaProps>(function Textarea(
  { label, error, helper, required, className, id, rows = 3, ...rest },
  ref,
) {
  const autoId = useId();
  const fieldId = id ?? autoId;
  return (
    <FieldShell label={label} error={error} helper={helper} required={required} id={fieldId}>
      <textarea
        ref={ref}
        id={fieldId}
        rows={rows}
        className={cn(baseField, 'py-2.5', error ? 'border-danger' : 'border-border', className)}
        {...rest}
      />
    </FieldShell>
  );
});

interface SelectProps extends SelectHTMLAttributes<HTMLSelectElement> {
  label?: ReactNode;
  error?: string;
  helper?: string;
}

export const Select = forwardRef<HTMLSelectElement, SelectProps>(function Select(
  { label, error, helper, required, className, id, children, ...rest },
  ref,
) {
  const autoId = useId();
  const fieldId = id ?? autoId;
  return (
    <FieldShell label={label} error={error} helper={helper} required={required} id={fieldId}>
      <select
        ref={ref}
        id={fieldId}
        className={cn(baseField, 'h-11', error ? 'border-danger' : 'border-border', className)}
        {...rest}
      >
        {children}
      </select>
    </FieldShell>
  );
});
