import { useEffect, useRef, useState } from "react";
import { DayPicker, type DateRange } from "react-day-picker";
import { format } from "date-fns";
import { es } from "date-fns/locale";
import { parseLocalDate, formatLocalDate } from "./dateUtils";

interface DateRangePickerProps {
  fi: string;
  ff: string;
  onRangeChange: (fi: string, ff: string) => void;
}

const ChevronIcon = () => (
  <svg className="h-3.5 w-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2.5}>
    <path strokeLinecap="round" strokeLinejoin="round" d="M15 19l-7-7 7-7" />
  </svg>
);

const ChevronRightIcon = () => (
  <svg className="h-3.5 w-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2.5}>
    <path strokeLinecap="round" strokeLinejoin="round" d="M9 5l7 7-7 7" />
  </svg>
);

export function DateRangePicker({ fi, ff, onRangeChange }: DateRangePickerProps) {
  const [isOpen, setIsOpen] = useState(false);
  const ref = useRef<HTMLDivElement>(null);

  const from = fi ? parseLocalDate(fi) : undefined;
  const to = ff ? parseLocalDate(ff) : undefined;
  const range: DateRange | undefined = from ? { from, to } : undefined;

  useEffect(() => {
    const handler = (e: MouseEvent) => {
      if (ref.current && !ref.current.contains(e.target as Node)) {
        setIsOpen(false);
      }
    };
    document.addEventListener("mousedown", handler);
    return () => document.removeEventListener("mousedown", handler);
  }, []);

  const handleSelect = (newRange: DateRange | undefined) => {
    if (!newRange?.from) return;
    if (newRange.from && newRange.to) {
      onRangeChange(formatLocalDate(newRange.from), formatLocalDate(newRange.to));
      setIsOpen(false);
    }
  };

  const displayText = from && to
    ? `${format(from, "d MMM yyyy", { locale: es })} → ${format(to, "d MMM yyyy", { locale: es })}`
    : "Seleccionar rango de fechas";

  return (
    <div className="relative" ref={ref}>
      <button
        type="button"
        onClick={() => setIsOpen(!isOpen)}
        className="flex w-full items-center gap-2.5 rounded-xl border border-slate-200 bg-white px-3.5 py-2.5 text-sm shadow-sm transition-all hover:border-emerald-300 hover:shadow-md"
      >
        <svg className="h-4 w-4 shrink-0 text-slate-400" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={1.5}>
          <path strokeLinecap="round" strokeLinejoin="round" d="M6.75 3v2.25M17.25 3v2.25M3 18.75V7.5a2.25 2.25 0 012.25-2.25h13.5A2.25 2.25 0 0121 7.5v11.25m-18 0A2.25 2.25 0 005.25 21h13.5A2.25 2.25 0 0021 18.75m-18 0v-7.5A2.25 2.25 0 015.25 9h13.5A2.25 2.25 0 0121 11.25v7.5" />
        </svg>
        <span className="min-w-0 flex-1 truncate tabular-nums text-slate-700">{displayText}</span>
        <svg
          className={`h-3.5 w-3.5 shrink-0 text-slate-400 transition-transform duration-200 ${isOpen ? "rotate-180" : ""}`}
          fill="none"
          viewBox="0 0 24 24"
          stroke="currentColor"
          strokeWidth={2}
        >
          <path strokeLinecap="round" strokeLinejoin="round" d="M19 9l-7 7-7-7" />
        </svg>
      </button>

      {isOpen && (
        <div className="absolute left-0 top-full z-[100] mt-2 w-[17.5rem] overflow-hidden rounded-2xl border border-slate-200 bg-white p-3 shadow-xl shadow-slate-900/10">
          <DayPicker
            mode="range"
            selected={range}
            onSelect={handleSelect}
            locale={es}
            defaultMonth={from}
            classNames={{
              root: "m-0 p-0",
              months: "flex flex-col",
              month: "m-0",
              month_caption: "flex items-center justify-center py-1",
              caption_label: "text-sm font-semibold text-slate-800",
              nav: "flex items-center gap-1",
              button_previous: "flex h-7 w-7 items-center justify-center rounded-lg text-slate-400 transition-colors hover:bg-slate-100 hover:text-slate-600",
              button_next: "flex h-7 w-7 items-center justify-center rounded-lg text-slate-400 transition-colors hover:bg-slate-100 hover:text-slate-600",
              month_grid: "w-full border-collapse",
              weekdays: "flex",
              weekday: "w-9 text-center text-[11px] font-medium text-slate-400 py-1",
              week: "flex",
              day: "h-8 w-9 p-0 text-sm",
              day_button: "flex h-full w-full items-center justify-center rounded-lg text-sm font-normal text-slate-600 transition-colors hover:bg-emerald-50 hover:text-emerald-700",
              selected: "!bg-emerald-500 !text-white",
              range_start: "rounded-l-full",
              range_end: "rounded-r-full",
              range_middle: "!bg-emerald-50 !text-emerald-700",
              today: "font-semibold text-emerald-600",
              outside: "text-slate-300",
              disabled: "text-slate-200",
              hidden: "invisible",
            }}
            components={{
              Chevron: ({ orientation }) =>
                orientation === "left" ? <ChevronIcon /> : <ChevronRightIcon />,
            }}
          />
        </div>
      )}
    </div>
  );
}
