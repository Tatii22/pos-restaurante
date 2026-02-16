import { create } from "zustand";
import type { Turno } from "../types";

const TURN_KEY = "pos_turno_actual";

function loadTurno(): Turno | null {
  const raw = localStorage.getItem(TURN_KEY);
  if (!raw) return null;
  try {
    return JSON.parse(raw) as Turno;
  } catch {
    return null;
  }
}

type TurnoState = {
  turno: Turno | null;
  setTurno: (turno: Turno | null) => void;
  clearTurno: () => void;
  isAbierto: () => boolean;
  isActivo: () => boolean;
};

export const useTurnoStore = create<TurnoState>((set, get) => ({
  turno: loadTurno(),
  setTurno: (turno) => {
    if (turno) {
      localStorage.setItem(TURN_KEY, JSON.stringify(turno));
    } else {
      localStorage.removeItem(TURN_KEY);
    }
    set({ turno });
  },
  clearTurno: () => {
    localStorage.removeItem(TURN_KEY);
    set({ turno: null });
  },
  isAbierto: () => get().turno?.estado === "ABIERTO",
  isActivo: () => {
    const estado = get().turno?.estado;
    return estado === "ABIERTO" || estado === "SIMULADO";
  }
}));

