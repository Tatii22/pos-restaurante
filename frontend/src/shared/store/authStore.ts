import { create } from "zustand";
import { TOKEN_KEY } from "../utils";
import type { Role } from "../types";

const TURN_KEY = "pos_turno_actual";

type AuthState = {
  token: string | null;
  username: string | null;
  role: Role | null;
  setAuth: (payload: { token: string; username: string; role: Role }) => void;
  clearAuth: () => void;
};

export const useAuthStore = create<AuthState>((set) => ({
  token: (() => {
    localStorage.removeItem(TOKEN_KEY);
    return null;
  })(),
  username: null,
  role: null,
  setAuth: ({ token, username, role }) => {
    localStorage.setItem(TOKEN_KEY, token);
    set({ token, username, role });
  },
  clearAuth: () => {
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(TURN_KEY);
    set({ token: null, username: null, role: null });
  }
}));
