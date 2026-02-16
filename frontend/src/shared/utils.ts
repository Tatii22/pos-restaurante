import type { ApiError, Role } from "./types";

export const TOKEN_KEY = "pos_token";
export const money = new Intl.NumberFormat("es-CO");

export function getErrorMessage(error: unknown): string {
  const api = error as ApiError;
  if (api?.fieldErrors) {
    const first = Object.values(api.fieldErrors)[0];
    if (first) return first;
  }
  return api?.message || api?.mensaje || "Error inesperado";
}

export function normalizeRole(roles: string[]): Role {
  const normalized = roles.map((r) => r.replace("ROLE_", ""));
  if (normalized.includes("ADMIN")) return "ADMIN";
  if (normalized.includes("CAJA")) return "CAJA";
  return "DOMI";
}
