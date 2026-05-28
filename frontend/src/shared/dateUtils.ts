/** Format a Date to YYYY-MM-DD using local timezone (no UTC) */
export function formatLocalDate(date: Date): string {
  const y = date.getFullYear();
  const m = String(date.getMonth() + 1).padStart(2, "0");
  const d = String(date.getDate()).padStart(2, "0");
  return `${y}-${m}-${d}`;
}

/** Parse YYYY-MM-DD string to Date using local timezone (no UTC) */
export function parseLocalDate(dateStr: string): Date {
  const [y, m, d] = dateStr.split("-").map(Number);
  return new Date(y, m - 1, d);
}

export function today(): string {
  return formatLocalDate(new Date());
}

export function startOfMonth(): string {
  const d = new Date();
  d.setDate(1);
  return formatLocalDate(d);
}

export function lastDayOfMonth(year: number, month: number): number {
  return new Date(year, month, 0).getDate();
}
