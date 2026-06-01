import { create } from "zustand";
import type { Turno } from "../types";

const TURN_KEY = "pos_turno_actual";

function loadTurno(): Turno | null {
  const raw = localStorage.getItem(TURN_KEY);
  if (!raw) return null;
  try {
    const t = JSON.parse(raw) as Turno;
    if (t.estado === "ABIERTO" || t.estado === "SIMULADO") return t;
    return null;
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
    // Invariante: solo persis turnos ABIERTO o SIMULADO
    // Turnos CERRADO se descartan automáticamente
    if (turno && (turno.estado === "ABIERTO" || turno.estado === "SIMULADO")) {
      localStorage.setItem(TURN_KEY, JSON.stringify(turno));
      set({ turno });
    } else {
      // Si recibe CERRADO o null, limpia el store
      localStorage.removeItem(TURN_KEY);
      set({ turno: null });
    }
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

