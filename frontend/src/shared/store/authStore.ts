import { create } from "zustand";
import { TOKEN_KEY } from "../utils";
import type { Role } from "../types";

const TURN_KEY = "pos_turno_actual";
const USERNAME_KEY = "pos_username";
const ROLE_KEY = "pos_role";

type AuthState = {
  token: string | null;
  username: string | null;
  role: Role | null;
  setAuth: (payload: { token: string; username: string; role: Role }) => void;
  clearAuth: () => void;
};

export const useAuthStore = create<AuthState>((set) => ({
  token: localStorage.getItem(TOKEN_KEY),
  username: localStorage.getItem(USERNAME_KEY),
  role: (localStorage.getItem(ROLE_KEY) as Role | null) ?? null,
  setAuth: ({ token, username, role }) => {
    localStorage.setItem(TOKEN_KEY, token);
    localStorage.setItem(USERNAME_KEY, username);
    localStorage.setItem(ROLE_KEY, role);
    set({ token, username, role });
  },
  clearAuth: () => {
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(USERNAME_KEY);
    localStorage.removeItem(ROLE_KEY);
    localStorage.removeItem(TURN_KEY);
    set({ token: null, username: null, role: null });
  }
}));
