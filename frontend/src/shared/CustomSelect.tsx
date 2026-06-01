import { useEffect, useRef, useState } from "react";
import { ChevronDown } from "lucide-react";

export type SelectOption = {
  value: string;
  label: string;
};

type CustomSelectProps = {
  value: string;
  onChange: (value: string) => void;
  options: SelectOption[];
  placeholder?: string;
  required?: boolean;
  disabled?: boolean;
  className?: string;
};

export function CustomSelect({ value, onChange, options, placeholder, required, disabled, className = "" }: CustomSelectProps) {
  const [open, setOpen] = useState(false);
  const ref = useRef<HTMLDivElement>(null);
  const selected = options.find((o) => o.value === value);

  useEffect(() => {
    function handleClick(e: MouseEvent) {
      if (ref.current && !ref.current.contains(e.target as Node)) {
        setOpen(false);
      }
    }
    document.addEventListener("mousedown", handleClick);
    return () => document.removeEventListener("mousedown", handleClick);
  }, []);

  return (
    <div ref={ref} className={`relative ${className}`}>
      <button
        type="button"
        disabled={disabled}
        className={`flex h-11 w-full items-center justify-between rounded-xl border px-3 py-2 text-sm outline-none focus:border-pos-mint ${
          disabled
            ? "cursor-not-allowed border-pos-border/50 bg-gray-100 text-pos-muted"
            : "border-pos-border bg-pos-card"
        }`}
        onClick={() => setOpen(!open)}
      >
        <span className={`whitespace-nowrap ${selected ? "text-pos-text" : "text-pos-muted"}`}>
          {selected ? selected.label : placeholder || "Seleccionar"}
        </span>
        <ChevronDown size={16} className={`text-pos-muted transition-transform ${open ? "rotate-180" : ""}`} />
      </button>

      {open && (
        <div className="absolute left-0 right-0 z-50 mt-1 overflow-hidden rounded-xl border border-pos-border bg-white shadow-lg">
          {options.map((opt) => (
            <button
              key={opt.value}
              type="button"
              className={`flex w-full whitespace-nowrap px-3 py-2.5 text-left text-sm transition-colors ${
                opt.value === value
                  ? "bg-emerald-50 font-medium text-pos-forest"
                  : "text-pos-text hover:bg-emerald-50"
              }`}
              onClick={() => {
                onChange(opt.value);
                setOpen(false);
              }}
            >
              {opt.label}
            </button>
          ))}
        </div>
      )}
    </div>
  );
}
