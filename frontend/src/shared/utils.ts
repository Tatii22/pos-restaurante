import type { ApiError, Role } from "./types";

export const TOKEN_KEY = "pos_token";
export const money = new Intl.NumberFormat("es-CO");

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
