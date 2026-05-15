import { create } from "zustand";
import type { Deudor } from "../types";

type FiadoState = {
  modoFiado: boolean;
  selectedDeudor: Deudor | null;
  activarModoFiado: () => void;
  salirModoFiado: () => void;
  setSelectedDeudor: (deudor: Deudor | null) => void;
};

export const useFiadoStore = create<FiadoState>((set) => ({
  modoFiado: false,
  selectedDeudor: null,
  activarModoFiado: () => set({ modoFiado: true }),
  salirModoFiado: () => set({ modoFiado: false, selectedDeudor: null }),
  setSelectedDeudor: (deudor) => set({ selectedDeudor: deudor })
}));
