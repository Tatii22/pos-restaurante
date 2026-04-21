import { useEffect, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useNavigate } from "react-router-dom";
import { X } from "lucide-react";
import { posApi } from "../shared/api/posApi";
import {
  formatCurrencyInput,
  getErrorMessages,
  money,
  normalizeCurrencyInput,
  parseCurrencyInput
} from "../shared/utils";
import { useTurnoStore } from "../shared/store/turnoStore";
import { useAuthStore } from "../shared/store/authStore";
import type { ReporteCierreTurno, Turno } from "../shared/types";

function summaryNumber(value: number | null | undefined) {
  return money.format(value || 0);
}

export function TurnosPage() {
  const qc = useQueryClient();
  const navigate = useNavigate();
  const { clearAuth } = useAuthStore();
  const { turno, setTurno, clearTurno } = useTurnoStore();
  const [montoInicial, setMontoInicial] = useState("");
  const [montoInicialError, setMontoInicialError] = useState("");
  const [efectivoContado, setEfectivoContado] = useState("");
  const [efectivoContadoError, setEfectivoContadoError] = useState("");
  const [montoFinal, setMontoFinal] = useState("");
  const [montoFinalError, setMontoFinalError] = useState("");
  const [showSimModal, setShowSimModal] = useState(false);
  const [simResult, setSimResult] = useState<Turno | null>(null);
  const [showCloseModal, setShowCloseModal] = useState(false);
  const [closeResult, setCloseResult] = useState<Turno | null>(null);

  const turnoActivoQ = useQuery({
    queryKey: ["turno-activo-layout"],
    queryFn: () => posApi.getTurnoActivo(),
    refetchOnMount: true,
    refetchOnWindowFocus: true
  });

  const reporteQ = useQuery({
    queryKey: ["reporte-turno-activo", turnoActivoQ.data?.id ?? turno?.id],
    queryFn: () => posApi.getReporteTurno((turnoActivoQ.data?.id ?? turno?.id)!),
    enabled: Boolean(turnoActivoQ.data?.id ?? turno?.id)
  });

  useEffect(() => {
    if (turnoActivoQ.data !== undefined) {
      setTurno(turnoActivoQ.data);
    }
  }, [turnoActivoQ.data, setTurno]);

  function handleMoneyChange(
    value: string,
    setValue: (next: string) => void,
    setError: (next: string) => void
  ) {
    const result = normalizeCurrencyInput(value, { maxDigits: 9, allowZero: false });
    setError(result.error);
    if (result.value !== null) {
      setValue(result.value);
    }
  }

  const montoInicialValue = parseCurrencyInput(montoInicial);
  const efectivoContadoValue = parseCurrencyInput(efectivoContado);
  const montoFinalValue = parseCurrencyInput(montoFinal);

  const openM = useMutation({
    mutationFn: () => posApi.abrirTurno(montoInicialValue),
    onSuccess: (data) => {
      setTurno(data);
      qc.invalidateQueries({ queryKey: ["turno-activo-layout"] });
      qc.invalidateQueries({ queryKey: ["reporte-turno-activo"] });
    }
  });
  const simM = useMutation({
    mutationFn: () => posApi.simularCierre(efectivoContadoValue),
    onSuccess: (data) => {
      setTurno(data);
      setSimResult(data);
      setShowSimModal(true);
      qc.invalidateQueries({ queryKey: ["turno-activo-layout"] });
      qc.invalidateQueries({ queryKey: ["reporte-turno-activo", data.id] });
    }
  });
  const closeM = useMutation({
    mutationFn: () => posApi.cerrarTurno(montoFinalValue),
    onSuccess: (data) => {
      setTurno(data);
      qc.invalidateQueries({ queryKey: ["turno-activo-layout"] });
      qc.invalidateQueries({ queryKey: ["reporte-turno-activo", data.id] });
      if (data.estado === "CERRADO") {
        setCloseResult(data);
        setShowCloseModal(true);
      }
    }
  });

  const montoInicialValido = montoInicialValue > 0 && !montoInicialError;
  const efectivoContadoValido = efectivoContadoValue > 0 && !efectivoContadoError;
  const montoFinalValido = montoFinalValue > 0 && !montoFinalError;
  const openErrors = openM.isError ? getErrorMessages(openM.error) : [];
  const closeErrors = simM.isError || closeM.isError ? getErrorMessages(simM.error || closeM.error) : [];
  const turnoBase = turnoActivoQ.data ?? turno;
  const reporte = reporteQ.data;
  const turnoResumen =
    closeResult?.id === turnoBase?.id ? closeResult : simResult?.id === turnoBase?.id ? simResult : turnoBase;
  const esperado = (turnoResumen?.montoInicial || 0) + (turnoResumen?.totalVentas || 0) - (turnoResumen?.totalGastos || 0);

  function renderResumenFinanciero(data: ReporteCierreTurno | undefined) {
    return (
      <div className="grid gap-2 rounded-xl border border-pos-border bg-gray-50 p-3 text-sm">
        <p>Ventas efectivo: <span className="font-semibold text-green-700">{summaryNumber(data?.totalEfectivo)}</span></p>
        <p>Ventas transferencia: <span className="font-semibold text-green-700">{summaryNumber(data?.totalTransferencia)}</span></p>
        <p>Gastos efectivo: <span className="font-semibold text-red-700">{summaryNumber(data?.totalGastosEfectivo)}</span></p>
        <p>Gastos transferencia: <span className="font-semibold text-red-700">{summaryNumber(data?.totalGastosTransferencia)}</span></p>
        <p>Ganancia efectivo: <span className="font-semibold">{summaryNumber(data?.gananciaEfectivo)}</span></p>
        <p>Ganancia transferencia: <span className="font-semibold">{summaryNumber(data?.gananciaTransferencia)}</span></p>
      </div>
    );
  }

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
              inputMode="numeric"
              value={formatCurrencyInput(montoInicial)}
              onChange={(e) => handleMoneyChange(e.target.value, setMontoInicial, setMontoInicialError)}
              placeholder="Monto inicial"
            />
            {montoInicialError && <p className="text-xs text-orange-700">{montoInicialError}</p>}
            {!montoInicialError && !montoInicialValido && <p className="text-xs text-orange-700">El monto inicial debe ser mayor a 0.</p>}
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

  const turnoActual = turnoResumen ?? turno;

  return (
    <div className="grid gap-4">
      <h2 className="text-2xl font-semibold">Turno de Caja</h2>
      <div className="card grid gap-3 p-4 md:grid-cols-2 xl:grid-cols-4">
        <div>
          <p className="text-sm text-pos-muted">Turno</p>
          <p className="text-xl font-semibold">#{turnoActual.id}</p>
          <p className="mt-1 text-xs text-pos-muted">Apertura: {new Date(turnoActual.fechaApertura).toLocaleDateString()}</p>
        </div>
        <div>
          <p className="text-sm text-pos-muted">Estado</p>
          <p className="text-xl font-semibold">{turnoActual.estado}</p>
          <p className="mt-1 text-xs text-pos-muted">Reporte: {reporteQ.isFetching ? "actualizando..." : "al dia"}</p>
        </div>
        <div>
          <p className="text-sm text-pos-muted">Hora apertura</p>
          <p className="text-xl font-semibold">{new Date(turnoActual.fechaApertura).toLocaleTimeString()}</p>
          <p className="mt-1 text-xs text-pos-muted">Cierre: {turnoActual.fechaCierre ? new Date(turnoActual.fechaCierre).toLocaleTimeString() : "-"}</p>
        </div>
        <div>
          <p className="text-sm text-pos-muted">Usuario</p>
          <p className="text-xl font-semibold">{turnoActual.usuario}</p>
          <p className="mt-1 text-xs text-pos-muted">Base: {summaryNumber(turnoActual.montoInicial)}</p>
        </div>
      </div>

      <div className="card grid gap-3 p-4 md:grid-cols-2 xl:grid-cols-4">
        <div>
          <p className="text-sm text-pos-muted">Total ventas</p>
          <p className="text-2xl font-bold text-green-700">{summaryNumber(turnoActual.totalVentas)}</p>
          <p className="mt-1 text-xs text-pos-muted">Efec. {summaryNumber(reporte?.totalEfectivo)} / Transf. {summaryNumber(reporte?.totalTransferencia)}</p>
          <p className="mt-1 text-xs text-pos-muted">Neto en caja: {summaryNumber(reporte?.netoEnCaja)}</p>
        </div>
        <div>
          <p className="text-sm text-pos-muted">Total gastos</p>
          <p className="text-2xl font-bold text-red-700">{summaryNumber(turnoActual.totalGastos)}</p>
          <p className="mt-1 text-xs text-pos-muted">Efec. {summaryNumber(reporte?.totalGastosEfectivo)} / Transf. {summaryNumber(reporte?.totalGastosTransferencia)}</p>
          <p className="mt-1 text-xs text-pos-muted">Movimientos: {reporte?.gastos?.length || 0}</p>
        </div>
        <div>
          <p className="text-sm text-pos-muted">Esperado en caja</p>
          <p className="text-2xl font-bold">{summaryNumber(turnoActual.esperado ?? esperado)}</p>
          <p className="mt-1 text-xs text-pos-muted">Gan. efec. {summaryNumber(reporte?.gananciaEfectivo)}</p>
          <p className="mt-1 text-xs text-pos-muted">Base + ventas - gastos</p>
        </div>
        <div>
          <p className="text-sm text-pos-muted">Diferencia</p>
          <p className="text-2xl font-bold">{summaryNumber(Math.abs(turnoActual.faltante || 0))}</p>
          <p className="mt-1 text-xs text-pos-muted">Gan. transf. {summaryNumber(reporte?.gananciaTransferencia)}</p>
          <p className="mt-1 text-xs text-pos-muted">Contado vs esperado</p>
        </div>
      </div>

      <div className="grid gap-3 md:grid-cols-2">
        <div className="card p-4">
          <h3 className="mb-2 font-semibold">Simular Cierre</h3>
          <input
            className="input mb-2"
            inputMode="numeric"
            value={formatCurrencyInput(efectivoContado)}
            onChange={(e) => handleMoneyChange(e.target.value, setEfectivoContado, setEfectivoContadoError)}
            placeholder="Dinero contado"
          />
          {efectivoContadoError && <p className="mb-2 text-xs text-orange-700">{efectivoContadoError}</p>}
          <button className="btn-soft w-full" onClick={() => simM.mutate()} disabled={simM.isPending || !efectivoContadoValido}>
            {simM.isPending ? "Simulando..." : "Simular Cierre"}
          </button>
        </div>
        <div className="card p-4">
          <h3 className="mb-2 font-semibold">Confirmar Cierre</h3>
          <input
            className="input mb-2"
            inputMode="numeric"
            value={formatCurrencyInput(montoFinal)}
            onChange={(e) => handleMoneyChange(e.target.value, setMontoFinal, setMontoFinalError)}
            placeholder="Monto final contado"
          />
          {montoFinalError && <p className="mb-2 text-xs text-orange-700">{montoFinalError}</p>}
          <button className="btn-primary w-full" onClick={() => closeM.mutate()} disabled={closeM.isPending || !montoFinalValido}>
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
              <p>Total ventas: <span className="font-semibold text-green-700">{summaryNumber(simResult.totalVentas)}</span></p>
              <p>Total gastos: <span className="font-semibold text-red-700">{summaryNumber(simResult.totalGastos)}</span></p>
              <p>Monto inicial: <span className="font-semibold">{summaryNumber(simResult.montoInicial)}</span></p>
              <p>Esperado en caja: <span className="font-semibold">{summaryNumber(simResult.esperado)}</span></p>
              <p>Diferencia: <span className="font-semibold">{summaryNumber(Math.abs(simResult.faltante || 0))}</span></p>
              <p>Dinero contado: <span className="font-semibold">{summaryNumber(efectivoContadoValue)}</span></p>
            </div>
            <div className="mt-3">{renderResumenFinanciero(reporte)}</div>
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
              <p>Monto inicial: <span className="font-semibold">{summaryNumber(closeResult.montoInicial)}</span></p>
              <p>Total ventas: <span className="font-semibold text-green-700">{summaryNumber(closeResult.totalVentas)}</span></p>
              <p>Total gastos: <span className="font-semibold text-red-700">{summaryNumber(closeResult.totalGastos)}</span></p>
              <p>Esperado en caja: <span className="font-semibold">{summaryNumber(closeResult.esperado)}</span></p>
              <p>Monto final contado: <span className="font-semibold">{summaryNumber(montoFinalValue)}</span></p>
              <p>Diferencia final: <span className="font-semibold">{summaryNumber(Math.abs(closeResult.faltante || 0))}</span></p>
            </div>
            <div className="mt-3">{renderResumenFinanciero(reporte)}</div>
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
