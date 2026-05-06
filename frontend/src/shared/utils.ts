import type { ApiError, Role } from "./types";

export const TOKEN_KEY = "pos_token";

// Helper para formatear números con punto como separador de millar (formato colombiano)
function formatNumberWithDots(num: number): string {
  return num.toString().replace(/\B(?=(\d{3})+(?!\d))/g, ".");
}

// money.format() compat - mantiene el nombre por compatibilidad
export const money = {
  format: (num: number) => formatNumberWithDots(num)
};

export function parseCurrencyInput(value: string): number {
  const normalized = value.replace(/[^\d]/g, "");
  return Number(normalized || "0");
}

export function formatCurrencyInput(value: string | number): string {
  let parsed: number;
  if (typeof value === "number") {
    parsed = value;
  } else {
    parsed = parseCurrencyInput(value);
  }
  if (parsed === 0) return "";

  return formatNumberWithDots(parsed);
}

export function normalizeCurrencyInput(
  value: string,
  options?: { maxDigits?: number; allowZero?: boolean }
): { value: string | null; error: string } {
  const maxDigits = options?.maxDigits ?? 9;
  const allowZero = options?.allowZero ?? true;
  const compact = value.replace(/\./g, "").replace(/\s/g, "");

  if (!compact) {
    return { value: "", error: "" };
  }
  if (!/^\d+$/.test(compact)) {
    return { value: null, error: "Solo se permiten numeros enteros" };
  }

  const next = compact.slice(0, maxDigits);
  if (!allowZero && Number(next || "0") === 0) {
    return { value: next, error: "El valor debe ser mayor a 0" };
  }

  return { value: next, error: "" };
}

export function getErrorMessages(error: unknown): string[] {
  const api = error as ApiError;
  if (api?.fieldErrors) {
    const all = Object.values(api.fieldErrors)
      .map((m) => (m || "").trim())
      .filter(Boolean);
    if (all.length) return Array.from(new Set(all));
  }
  const fallback = (api?.message || api?.mensaje || "Error inesperado").trim();
  return [fallback];
}

export function getErrorMessage(error: unknown): string {
  return getErrorMessages(error).join(" | ");
}

export function normalizeRole(roles: string[]): Role {
  const normalized = roles.map((r) => r.replace("ROLE_", ""));
  if (normalized.includes("ADMIN")) return "ADMIN";
  if (normalized.includes("CAJA")) return "CAJA";
  return "DOMI";
}

export function calculateCurrencyInputCursorPosition(
  oldCleanValue: string,
  newCleanValue: string,
  oldCursorPos: number,
  oldDisplayValue: string
): number {
  if (!oldDisplayValue || oldCursorPos === 0) return 0;

  const digitsBeforeCursor = oldDisplayValue
    .substring(0, oldCursorPos)
    .replace(/\D/g, "").length;

  const newDisplayValue = formatCurrencyInput(newCleanValue);
  let newCursorPos = 0;
  let digitCount = 0;

  for (let i = 0; i < newDisplayValue.length; i++) {
    if (/\d/.test(newDisplayValue[i])) {
      digitCount++;
      if (digitCount === digitsBeforeCursor + 1) {
        return i + 1;
      }
    }
  }

  return newDisplayValue.length;
}

export function handleCurrencyInput(
  value: string,
  options?: { maxDigits?: number; allowZero?: boolean }
): { cleanValue: string; error: string } {
  const maxDigits = options?.maxDigits ?? 9;
  const allowZero = options?.allowZero ?? true;

  const compact = value.replace(/\./g, "").replace(/\s/g, "");

  if (!compact) {
    return { cleanValue: "", error: "" };
  }

  if (!/^\d+$/.test(compact)) {
    return { cleanValue: compact.replace(/\D/g, ""), error: "Solo se permiten números" };
  }

  const next = compact.slice(0, maxDigits);
  if (!allowZero && Number(next || "0") === 0) {
    return { cleanValue: next, error: "El valor debe ser mayor a 0" };
  }

  return { cleanValue: next, error: "" };
}
