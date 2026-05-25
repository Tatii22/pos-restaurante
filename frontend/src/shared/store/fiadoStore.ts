import { create } from "zustand";
import type { Cliente } from "../types";

type FiadoState = {
  modoFiado: boolean;
  selectedCliente: Cliente | null;
  activarModoFiado: () => void;
  salirModoFiado: () => void;
  setSelectedCliente: (cliente: Cliente | null) => void;
};

export const useFiadoStore = create<FiadoState>((set) => ({
  modoFiado: false,
  selectedCliente: null,
  activarModoFiado: () => set({ modoFiado: true }),
  salirModoFiado: () => set({ modoFiado: false, selectedCliente: null }),
  setSelectedCliente: (cliente) => set({ selectedCliente: cliente })
}));
