import type { ApiError, Role } from "./types";

export const TOKEN_KEY = "pos_token";
export const money = new Intl.NumberFormat("es-CO");

export function parseCurrencyInput(value: string): number {
  const normalized = value.replace(/[^\d]/g, "");
  return Number(normalized || "0");
}

export function formatCurrencyInput(value: string): string {
  const parsed = parseCurrencyInput(value);
  return parsed > 0 ? money.format(parsed) : "";
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
