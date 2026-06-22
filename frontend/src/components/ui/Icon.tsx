import { cn } from '@/lib/cn';

/** Material Symbols Outlined icon (loaded via index.html font link). */
export function Icon({
  name,
  className,
  filled,
}: {
  name: string;
  className?: string;
  filled?: boolean;
}) {
  return (
    <span
      className={cn('material-symbols-outlined select-none leading-none', className)}
      style={filled ? { fontVariationSettings: "'FILL' 1" } : undefined}
      aria-hidden="true"
    >
      {name}
    </span>
  );
}
