// Vietnamese formatting helpers shared across the app (per Prompt 0).

const vnNumber = new Intl.NumberFormat('vi-VN');

/** Format a VND amount: 1250000 -> "1.250.000 ₫". */
export function formatVnd(value: number | string | null | undefined): string {
  const n = typeof value === 'string' ? Number(value) : value;
  if (n == null || Number.isNaN(n)) return '0 ₫';
  return `${vnNumber.format(Math.round(n))} ₫`;
}

/** Format a date-time: "dd/MM/yyyy HH:mm". Accepts ISO string / Date / epoch. */
export function formatDateTime(input: string | number | Date | null | undefined): string {
  if (!input) return '—';
  const d = new Date(input);
  if (Number.isNaN(d.getTime())) return '—';
  const pad = (x: number) => String(x).padStart(2, '0');
  return `${pad(d.getDate())}/${pad(d.getMonth() + 1)}/${d.getFullYear()} ${pad(d.getHours())}:${pad(d.getMinutes())}`;
}

/** Format a date only: "dd/MM/yyyy". */
export function formatDate(input: string | number | Date | null | undefined): string {
  if (!input) return '—';
  const d = new Date(input);
  if (Number.isNaN(d.getTime())) return '—';
  const pad = (x: number) => String(x).padStart(2, '0');
  return `${pad(d.getDate())}/${pad(d.getMonth() + 1)}/${d.getFullYear()}`;
}

/** Relative time in Vietnamese: "2 giờ trước", "Hôm qua", "vừa xong". */
export function relativeTime(input: string | number | Date | null | undefined): string {
  if (!input) return '';
  const d = new Date(input);
  if (Number.isNaN(d.getTime())) return '';
  const diffMs = Date.now() - d.getTime();
  const sec = Math.floor(diffMs / 1000);
  const min = Math.floor(sec / 60);
  const hour = Math.floor(min / 60);
  const day = Math.floor(hour / 24);
  if (sec < 60) return 'vừa xong';
  if (min < 60) return `${min} phút trước`;
  if (hour < 24) return `${hour} giờ trước`;
  if (day === 1) return 'Hôm qua';
  if (day < 30) return `${day} ngày trước`;
  return formatDate(d);
}

/** Short order code from a UUID: "#A1B2C3" (last 6 hex, uppercased). */
export function shortCode(id: string | null | undefined): string {
  if (!id) return '#—';
  const hex = id.replace(/-/g, '');
  return `#${hex.slice(-6).toUpperCase()}`;
}
