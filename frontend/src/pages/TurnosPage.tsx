import { useState } from "react";
import { useMutation } from "@tanstack/react-query";
import { useNavigate } from "react-router-dom";
import { X } from "lucide-react";
import { posApi } from "../shared/api/posApi";
import { getErrorMessages, money } from "../shared/utils";
import { useTurnoStore } from "../shared/store/turnoStore";
import { useAuthStore } from "../shared/store/authStore";
import type { Turno } from "../shared/types";

export function TurnosPage() {
  const navigate = useNavigate();
  const { clearAuth } = useAuthStore();
  const { turno, setTurno, clearTurno } = useTurnoStore();
  const [montoInicial, setMontoInicial] = useState("");
  const [efectivoContado, setEfectivoContado] = useState("0");
  const [montoFinal, setMontoFinal] = useState("0");
  const [showSimModal, setShowSimModal] = useState(false);
  const [simResult, setSimResult] = useState<Turno | null>(null);
  const [showCloseModal, setShowCloseModal] = useState(false);
  const [closeResult, setCloseResult] = useState<Turno | null>(null);

  const openM = useMutation({
    mutationFn: () => posApi.abrirTurno(Number(montoInicial)),
    onSuccess: setTurno
  });
  const simM = useMutation({
    mutationFn: () => posApi.simularCierre(Number(efectivoContado)),
    onSuccess: (data) => {
      setTurno(data);
      setSimResult(data);
      setShowSimModal(true);
    }
  });
  const closeM = useMutation({
    mutationFn: () => posApi.cerrarTurno(Number(montoFinal)),
    onSuccess: (data) => {
      setTurno(data);
      if (data.estado === "CERRADO") {
        setCloseResult(data);
        setShowCloseModal(true);
      }
    }
  });

  const montoInicialValido = Number(montoInicial) > 0;
  const openErrors = openM.isError ? getErrorMessages(openM.error) : [];
  const closeErrors = (simM.isError || closeM.isError) ? getErrorMessages(simM.error || closeM.error) : [];

  if (!turno) {
    return (
      <div className="grid min-h-[70vh] place-items-center">
        <div className="card w-full max-w-xl p-6 text-center">
          <p className="text-xs uppercase tracking-wide text-pos-muted">Turno de caja</p>
          <h2 className="mt-2 text-3xl font-bold">Estado actual: CERRADO</h2>
          <p className="mt-3 text-sm text-pos-muted">Debes abrir turno para habilitar ventas y gastos.</p>
          <div className="mt-5 grid gap-2">
            <input
              className="input"
              inputMode="decimal"
              value={montoInicial}
              onChange={(e) => setMontoInicial(e.target.value)}
              placeholder="Monto inicial"
            />
            {!montoInicialValido && (
              <p className="text-xs text-orange-700">El monto inicial debe ser mayor a 0.</p>
            )}
            <button
              className="btn-primary py-3 text-base"
              onClick={() => openM.mutate()}
              disabled={openM.isPending || !montoInicialValido}
            >
              {openM.isPending ? "Abriendo..." : "Abrir Turno"}
            </button>
            {openErrors.length > 0 && (
              <ul className="text-left text-sm text-red-600">
                {openErrors.map((msg) => (
                  <li key={msg}>- {msg}</li>
                ))}
              </ul>
            )}
          </div>
        </div>
      </div>
    );
  }

  const esperado = (turno.montoInicial || 0) + (turno.totalVentas || 0) - (turno.totalGastos || 0);

  return (
    <div className="grid gap-4">
      <h2 className="text-2xl font-semibold">Turno de Caja</h2>
      <div className="card grid gap-3 p-4 md:grid-cols-2 xl:grid-cols-4">
        <div>
          <p className="text-sm text-pos-muted">Turno</p>
          <p className="text-xl font-semibold">#{turno.id}</p>
        </div>
        <div>
          <p className="text-sm text-pos-muted">Estado</p>
          <p className="text-xl font-semibold">{turno.estado}</p>
        </div>
        <div>
          <p className="text-sm text-pos-muted">Hora apertura</p>
          <p className="text-xl font-semibold">{new Date(turno.fechaApertura).toLocaleTimeString()}</p>
        </div>
        <div>
          <p className="text-sm text-pos-muted">Usuario</p>
          <p className="text-xl font-semibold">{turno.usuario}</p>
        </div>
      </div>

      <div className="card grid gap-3 p-4 md:grid-cols-2 xl:grid-cols-4">
        <div>
          <p className="text-sm text-pos-muted">Total ventas</p>
          <p className="text-2xl font-bold text-green-700">{money.format(turno.totalVentas || 0)}</p>
        </div>
        <div>
          <p className="text-sm text-pos-muted">Total gastos</p>
          <p className="text-2xl font-bold text-red-700">{money.format(turno.totalGastos || 0)}</p>
        </div>
        <div>
          <p className="text-sm text-pos-muted">Esperado en caja</p>
          <p className="text-2xl font-bold">{money.format(turno.esperado ?? esperado)}</p>
        </div>
        <div>
          <p className="text-sm text-pos-muted">Diferencia</p>
          <p className="text-2xl font-bold">{money.format(Math.abs(turno.faltante || 0))}</p>
        </div>
      </div>

      <div className="grid gap-3 md:grid-cols-2">
        <div className="card p-4">
          <h3 className="mb-2 font-semibold">Simular Cierre</h3>
          <input
            className="input mb-2"
            inputMode="decimal"
            value={efectivoContado}
            onChange={(e) => setEfectivoContado(e.target.value)}
            placeholder="Dinero contado"
          />
          <button className="btn-soft w-full" onClick={() => simM.mutate()} disabled={simM.isPending}>
            {simM.isPending ? "Simulando..." : "Simular Cierre"}
          </button>
        </div>
        <div className="card p-4">
          <h3 className="mb-2 font-semibold">Confirmar Cierre</h3>
          <input
            className="input mb-2"
            inputMode="decimal"
            value={montoFinal}
            onChange={(e) => setMontoFinal(e.target.value)}
            placeholder="Monto final contado"
          />
          <button className="btn-primary w-full" onClick={() => closeM.mutate()} disabled={closeM.isPending}>
            {closeM.isPending ? "Cerrando..." : "Cerrar Turno"}
          </button>
        </div>
      </div>

      {closeErrors.length > 0 && (
        <ul className="text-sm text-red-600">
          {closeErrors.map((msg) => (
            <li key={msg}>- {msg}</li>
          ))}
        </ul>
      )}

      {showSimModal && simResult && (
        <div className="fixed inset-0 z-40 flex items-center justify-center bg-black/40 p-4">
          <div className="card w-full max-w-lg p-5">
            <div className="mb-3 flex items-center justify-between">
              <h3 className="text-lg font-semibold">Simulacion de Cierre</h3>
              <button className="btn-ghost p-1" onClick={() => setShowSimModal(false)}>
                <X size={14} />
              </button>
            </div>
            <div className="grid gap-2 rounded-xl border border-pos-border bg-gray-50 p-3 text-sm">
              <p>Turno: <span className="font-semibold">#{simResult.id}</span></p>
              <p>Estado: <span className="font-semibold">{simResult.estado}</span></p>
              <p>Total ventas: <span className="font-semibold text-green-700">{money.format(simResult.totalVentas || 0)}</span></p>
              <p>Total gastos: <span className="font-semibold text-red-700">{money.format(simResult.totalGastos || 0)}</span></p>
              <p>Monto inicial: <span className="font-semibold">{money.format(simResult.montoInicial || 0)}</span></p>
              <p>Esperado en caja: <span className="font-semibold">{money.format(simResult.esperado || 0)}</span></p>
              <p>Diferencia: <span className="font-semibold">{money.format(Math.abs(simResult.faltante || 0))}</span></p>
              <p>Dinero contado: <span className="font-semibold">{money.format(Number(efectivoContado || 0))}</span></p>
            </div>
            <div className="mt-3 flex justify-end">
              <button className="btn-primary" onClick={() => setShowSimModal(false)}>
                Entendido
              </button>
            </div>
          </div>
        </div>
      )}

      {showCloseModal && closeResult && (
        <div className="fixed inset-0 z-40 flex items-center justify-center bg-black/40 p-4">
          <div className="card w-full max-w-lg p-5">
            <div className="mb-3 flex items-center justify-between">
              <h3 className="text-lg font-semibold">Cierre de Turno Exitoso</h3>
              <button
                className="btn-ghost p-1"
                onClick={() => {
                  setShowCloseModal(false);
                  clearTurno();
                  clearAuth();
                  navigate("/login", { replace: true });
                }}
              >
                <X size={14} />
              </button>
            </div>
            <div className="grid gap-2 rounded-xl border border-pos-border bg-gray-50 p-3 text-sm">
              <p>Turno: <span className="font-semibold">#{closeResult.id}</span></p>
              <p>Estado final: <span className="font-semibold">{closeResult.estado}</span></p>
              <p>Fecha cierre: <span className="font-semibold">{closeResult.fechaCierre ? new Date(closeResult.fechaCierre).toLocaleString() : "-"}</span></p>
              <p>Monto inicial: <span className="font-semibold">{money.format(closeResult.montoInicial || 0)}</span></p>
              <p>Total ventas: <span className="font-semibold text-green-700">{money.format(closeResult.totalVentas || 0)}</span></p>
              <p>Total gastos: <span className="font-semibold text-red-700">{money.format(closeResult.totalGastos || 0)}</span></p>
              <p>Esperado en caja: <span className="font-semibold">{money.format(closeResult.esperado || 0)}</span></p>
              <p>Monto final contado: <span className="font-semibold">{money.format(Number(montoFinal || 0))}</span></p>
              <p>Diferencia final: <span className="font-semibold">{money.format(Math.abs(closeResult.faltante || 0))}</span></p>
            </div>
            <div className="mt-3 flex justify-end">
              <button
                className="btn-primary"
                onClick={() => {
                  setShowCloseModal(false);
                  clearTurno();
                  clearAuth();
                  navigate("/login", { replace: true });
                }}
              >
                Cerrar y salir
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
