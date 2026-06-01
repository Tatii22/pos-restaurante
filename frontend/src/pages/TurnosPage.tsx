import { useEffect, useRef, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useNavigate } from "react-router-dom";
import { X } from "lucide-react";
import { posApi } from "../shared/api/posApi";
import {
  getErrorMessages,
  money,
} from "../shared/utils";
import { useCurrencyInput } from "../shared/hooks";
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
  const montoInicial = useCurrencyInput("", { maxDigits: 9, allowZero: false });
  const efectivoContado = useCurrencyInput("", { maxDigits: 9, allowZero: false });
  const transferenciasVerificadas = useCurrencyInput("", { maxDigits: 9, allowZero: true });
  const [showSimModal, setShowSimModal] = useState(false);
  const [simResult, setSimResult] = useState<Turno | null>(null);
  const [showCloseModal, setShowCloseModal] = useState(false);
  const [closeResult, setCloseResult] = useState<Turno | null>(null);
  const [valoresArqueo, setValoresArqueo] = useState<{ ef: number; trans: number } | null>(null);
  const [observacionCierre, setObservacionCierre] = useState("");
  const obsInputRef = useRef<HTMLTextAreaElement>(null);

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

  const openM = useMutation({
    mutationFn: () => posApi.abrirTurno(montoInicial.numericValue),
    onSuccess: (data) => {
      setTurno(data);
      qc.invalidateQueries({ queryKey: ["turno-activo-layout"] });
      qc.invalidateQueries({ queryKey: ["reporte-turno-activo"] });
      qc.invalidateQueries({ queryKey: ["inventario-arranque-caja"] });
    }
  });
  const simM = useMutation({
    mutationFn: () => posApi.simularCierre(efectivoContado.numericValue, transferenciasVerificadas.numericValue),
    onSuccess: (data) => {
      setTurno(data);
      setSimResult(data);
      setValoresArqueo({ ef: efectivoContado.numericValue, trans: transferenciasVerificadas.numericValue });
      setObservacionCierre("");
      setShowSimModal(true);
      qc.invalidateQueries({ queryKey: ["turno-activo-layout"] });
      qc.invalidateQueries({ queryKey: ["reporte-turno-activo", data.id] });
    }
  });
  const confirmarCierreM = useMutation({
    mutationFn: () => {
      const v = valoresArqueo!;
      return posApi.cerrarTurno(v.ef, v.trans, observacionCierre || undefined);
    },
    onSuccess: (data) => {
      if (data.estado === "CERRADO") {
        clearTurno();
        clearAuth();
        navigate("/login", { replace: true });
        return;
      }

      setTurno(data);
      setShowSimModal(false);
      setSimResult(null);
      setValoresArqueo(null);
      setObservacionCierre("");
      qc.invalidateQueries({ queryKey: ["turno-activo-layout"] });
      qc.invalidateQueries({ queryKey: ["reporte-turno-activo", data.id] });
    }
  });

  const openErrors = openM.isError ? getErrorMessages(openM.error) : [];
  const closeErrors = simM.isError || confirmarCierreM.isError ? getErrorMessages(simM.error || confirmarCierreM.error) : [];
  const turnoBase = turnoActivoQ.data ?? turno;
  const reporte = reporteQ.data;
  const turnoResumen =
    closeResult?.id === turnoBase?.id ? closeResult : simResult?.id === turnoBase?.id ? simResult : turnoBase;

  const turnoActual = (turnoResumen ?? turno)!;
  const esperado = reporte?.cajaFisicaEsperada ?? turnoActual?.esperado ?? (turnoActual?.montoInicial || 0);

  // Considerar turno cerrado como equivalente a "no existe turno"
  const turnoEstaActivo = turno && turno.estado === "ABIERTO";

  useEffect(() => {
    if (turnoEstaActivo && turno.estado !== "CERRADO") {
      const fisico = String(Math.round(esperado));
      efectivoContado.setValue(fisico);

      const transEsperadas = Number(reporte?.transferenciasNetas ?? turnoActual?.transferenciasNetas ?? 0);
      transferenciasVerificadas.setValue(String(Math.round(transEsperadas)));
    }
  }, [turno?.id, esperado, reporte?.transferenciasNetas, transferenciasVerificadas.setValue, efectivoContado.setValue]);

  function renderResumenFinanciero(data: ReporteCierreTurno | undefined) {
    return (
      <div className="grid gap-2 rounded-xl border border-pos-border bg-gray-50 p-3 text-sm">
        <p>Ventas efectivo: <span className="font-semibold text-green-700">{summaryNumber(data?.totalEfectivo)}</span></p>
        <p>Ventas transferencia: <span className="font-semibold text-green-700">{summaryNumber(data?.totalTransferencia)}</span></p>
        <p>Abonos efectivo: <span className="font-semibold text-green-600">{summaryNumber(data?.totalAbonosEfectivo)}</span></p>
        <p>Abonos transferencia: <span className="font-semibold text-green-600">{summaryNumber(data?.totalAbonosTransferencia)}</span></p>
        <p>Gastos efectivo: <span className="font-semibold text-red-700">{summaryNumber(data?.totalGastosEfectivo)}</span></p>
        <p>Gastos transferencia: <span className="font-semibold text-red-700">{summaryNumber(data?.totalGastosTransferencia)}</span></p>
        <div className="border-t pt-2">
          <p>Ingresos netos (efectivo): <span className="font-semibold text-emerald-700">{summaryNumber(data?.gananciaEfectivo)}</span></p>
          <p>Ingresos netos (transferencia): <span className="font-semibold text-emerald-700">{summaryNumber(data?.gananciaTransferencia)}</span></p>
        </div>
      </div>
    );
  }

  if (!turnoEstaActivo) {
    return (
      <div className="grid min-h-[70vh] place-items-center">
        <div className="card w-full max-w-xl p-6 text-center">
          <p className="text-xs uppercase tracking-wide text-pos-muted">Turno de caja</p>
          <h2 className="mt-2 text-3xl font-bold">Estado actual: CERRADO</h2>
          <p className="mt-3 text-sm text-pos-muted">Debes abrir turno para habilitar ventas y gastos.</p>
          <div className="mt-5 grid gap-2">
            <input
              ref={montoInicial.inputRef}
              className="input"
              inputMode="decimal"
              value={montoInicial.displayValue}
              onChange={montoInicial.handleChange}
              placeholder="Monto inicial"
            />
            {montoInicial.error && <p className="text-xs text-orange-700">{montoInicial.error}</p>}
            {!montoInicial.error && !montoInicial.isValid && <p className="text-xs text-orange-700">El monto inicial debe ser mayor a 0.</p>}
            <button
              className="btn-primary py-3 text-base"
              onClick={() => openM.mutate()}
              disabled={openM.isPending || !montoInicial.isValid}
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

  // Cálculos en vivo para el Resumen Total (se actualizan mientras se escribe)
  const fisicoContado = Number(efectivoContado.numericValue) || 0;
  const transVerificadas = Number(transferenciasVerificadas.numericValue) || 0;
  const totalVerificado = fisicoContado + transVerificadas;
  const totalOperativoEsperado = Number(reporte?.totalOperativoTurno ?? turnoActual.totalOperativoTurno ?? 0);
  const diferenciaTotal = totalVerificado - totalOperativoEsperado;

  return (
    <div className="grid gap-4">
      <h2 className="text-2xl font-semibold tracking-tight">Turno de Caja</h2>

      {/* === CUADRO GENERAL DE LA CAJA - Premium Financial Summary === */}
      <div className="card rounded-2xl p-7 shadow-sm border border-pos-border/60 bg-white dark:bg-neutral-950 transition-all hover:border-pos-border/80">
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-8 lg:gap-14 lg:divide-x divide-pos-border/30">

          {/* COLUMNA 1 — IDENTIDAD DEL TURNO */}
          <div className="space-y-3">
            <div>
              <div className="flex items-baseline gap-2">
                <span className="text-[11px] uppercase tracking-[1.5px] text-pos-muted font-medium">TURNO</span>
                <span className="inline-flex items-center rounded-full px-2.5 py-0.5 text-[10px] font-semibold tracking-wide
                  {turnoActual.estado === 'ABIERTO' ? 'bg-emerald-100 text-emerald-700 dark:bg-emerald-900/30 dark:text-emerald-400' :
                   turnoActual.estado === 'CERRADO' ? 'bg-neutral-200 text-neutral-600 dark:bg-neutral-800 dark:text-neutral-400' :
                   'bg-amber-100 text-amber-700 dark:bg-amber-900/30 dark:text-amber-400'}">
                  {turnoActual.estado}
                </span>
              </div>
               <div className="text-[52px] font-medium tracking-tighter text-neutral-900 dark:text-white mt-1">
                  #{turnoActual.numeroTurno ?? turnoActual.id}
               </div>
            </div>

            <div className="space-y-1.5 text-sm">
              <div>
                <span className="text-[11px] text-pos-muted">Apertura</span>
                <div className="font-medium">{new Date(turnoActual.fechaApertura).toLocaleString('es-CO', { dateStyle: 'medium', timeStyle: 'short' })}</div>
              </div>
              <div>
                <span className="text-[11px] text-pos-muted">Cajero</span>
                <div className="font-medium">{turnoActual.usuario}</div>
              </div>
              <div>
                <span className="text-[11px] text-pos-muted">Base de caja</span>
                <div className="font-semibold text-lg tabular-nums">{summaryNumber(turnoActual.montoInicial)}</div>
              </div>
            </div>
          </div>

          {/* COLUMNA 2 — FLUJO OPERATIVO (Ventas / Gastos) */}
          <div className="space-y-6 lg:pl-8 lg:pr-6">
            {/* VENTAS */}
            <div>
              <div className="flex items-center justify-between mb-1">
                <span className="text-[11px] uppercase tracking-[1.5px] text-emerald-600/90 dark:text-emerald-400 font-semibold">VENTAS</span>
              </div>
                <div className="text-[32px] font-medium text-emerald-600 dark:text-emerald-400 tabular-nums tracking-tight">
                  {summaryNumber(turnoActual.totalVentas)}
                </div>
              <div className="mt-1.5 text-xs text-pos-muted space-x-3">
                <span>Efec: <span className="font-medium text-neutral-700 dark:text-neutral-300">{summaryNumber((reporte?.totalEfectivo ?? 0) + (reporte?.totalAbonosEfectivo ?? 0))}</span></span>
                <span>Transf: <span className="font-medium text-neutral-700 dark:text-neutral-300">{summaryNumber((reporte?.totalTransferencia ?? 0) + (reporte?.totalAbonosTransferencia ?? 0))}</span></span>
              </div>
            </div>

            {/* GASTOS */}
            <div>
              <div className="flex items-center justify-between mb-1">
                <span className="text-[11px] uppercase tracking-[1.5px] text-rose-600/90 dark:text-rose-400 font-semibold">GASTOS</span>
                <span className="text-[10px] text-pos-muted">{reporte?.gastos?.length || 0} mov.</span>
              </div>
               <div className="text-[32px] font-medium text-rose-600 dark:text-rose-400 tabular-nums tracking-tight">
                 {summaryNumber(turnoActual.totalGastos)}
               </div>
              <div className="mt-1.5 text-xs text-pos-muted space-x-3">
                <span>Efec: <span className="font-medium text-neutral-700 dark:text-neutral-300">{summaryNumber(reporte?.totalGastosEfectivo)}</span></span>
                <span>Transf: <span className="font-medium text-neutral-700 dark:text-neutral-300">{summaryNumber(reporte?.totalGastosTransferencia)}</span></span>
              </div>
            </div>
          </div>

           {/* COLUMNA 3 — BALANCE NETO / TOTAL OPERATIVO */}
           <div className="lg:pl-4">
            <div className="mb-2">
              <span className="text-[11px] uppercase tracking-[1.5px] text-neutral-500 dark:text-neutral-400 font-semibold">BALANCE NETO DEL TURNO</span>
            </div>

            <div>
              <div className="text-[11px] text-pos-muted">Total (efectivo + transferencias)</div>
              <div className="text-[32px] font-medium tabular-nums tracking-[-1px] text-neutral-900 dark:text-white">
                {summaryNumber(reporte?.totalOperativoTurno ?? turnoActual.totalOperativoTurno)}
              </div>
              <div className="text-xs text-pos-muted mt-1">Base + Ingresos − Gastos (efectivo + transferencias)</div>
            </div>

            <div className="mt-5 pt-4 border-t border-pos-border/60 text-xs text-pos-muted">
              Resumen general del turno.<br />
              Incluye efectivo y transferencias.
            </div>
          </div>

        </div>
      </div>

      {/* === CONCILIACIÓN DUAL === */}
      <div className="grid gap-3 lg:grid-cols-3">
        {/* CAJA FÍSICA */}
        <div className="card p-4 shadow-sm">
          <div className="mb-3">
            <h3 className="font-semibold text-lg">Caja Física (Efectivo)</h3>
            <p className="text-xs text-pos-muted mt-1">Cuenta únicamente el dinero físico real en la caja.</p>
          </div>

          <div className="space-y-3">
            <div>
              <p className="text-sm text-pos-muted">Efectivo (sistema)</p>
              <p className="text-2xl font-semibold">{summaryNumber(esperado)}</p>
            </div>

            <div>
              <label className="text-sm text-pos-muted block mb-1">Efectivo físico contado</label>
              <input
                ref={efectivoContado.inputRef}
                className="input transition-colors"
                inputMode="decimal"
                value={efectivoContado.displayValue}
                onChange={efectivoContado.handleChange}
                placeholder="Ej: 178000"
              />
              {efectivoContado.error && <p className="mt-1 text-xs text-orange-700">{efectivoContado.error}</p>}
            </div>

            <div className="pt-2 border-t">
              <p className="text-sm text-pos-muted">Diferencia efectivo</p>
              <p className="text-lg font-medium">
                {summaryNumber(efectivoContado.numericValue - esperado)}
              </p>
            </div>
          </div>
        </div>

        {/* CAJA VIRTUAL / TRANSFERENCIAS */}
        <div className="card p-4">
          <div className="mb-2.5">
            <h3 className="font-semibold text-base">Caja Virtual (Transferencias)</h3>
            <p className="text-[11px] text-pos-muted mt-0.5 leading-tight">Cuenta únicamente las transferencias verificadas.</p>
          </div>

          <div className="space-y-2.5">
            <div>
              <p className="text-sm text-pos-muted">Transferencias (sistema)</p>
              <p className="text-2xl font-semibold">{summaryNumber(reporte?.transferenciasNetas ?? turnoActual.transferenciasNetas)}</p>
            </div>

            <div>
              <label className="text-sm text-pos-muted block mb-1">Transferencias verificadas</label>
              <input
                ref={transferenciasVerificadas.inputRef}
                className="input transition-colors"
                inputMode="decimal"
                value={transferenciasVerificadas.displayValue}
                onChange={transferenciasVerificadas.handleChange}
                placeholder="Ej: 60000"
              />
              {transferenciasVerificadas.error && <p className="mt-1 text-xs text-orange-700">{transferenciasVerificadas.error}</p>}
            </div>

            <div className="pt-2 border-t">
              <p className="text-sm text-pos-muted">Diferencia transferencias</p>
              <p className="text-lg font-medium">
                {summaryNumber(transferenciasVerificadas.numericValue - Number(reporte?.transferenciasNetas ?? turnoActual.transferenciasNetas ?? 0))}
              </p>
            </div>
          </div>
        </div>

        {/* RESUMEN TOTAL */}
        <div className="card p-4 border border-pos-border/50 bg-neutral-50/60 dark:bg-neutral-900/40">
          <h3 className="font-semibold text-base mb-2.5 text-neutral-700 dark:text-neutral-300">Resumen Total del Turno</h3>

          <div className="space-y-2 text-sm">
            <div className="flex justify-between">
              <span>Total (sistema)</span>
              <span className="font-semibold">{summaryNumber(reporte?.totalOperativoTurno ?? turnoActual.totalOperativoTurno)}</span>
            </div>
            <div className="flex justify-between">
              <span>Total verificado</span>
              <span className="font-semibold">
                 {summaryNumber(totalVerificado)}
              </span>
            </div>
            <div className="flex justify-between border-t pt-2">
              <span className="font-semibold">Diferencia total</span>
               <span className="font-bold text-lg">
                 {summaryNumber(diferenciaTotal)}
               </span>
            </div>
          </div>

          <div className="mt-4 grid grid-cols-1 gap-2">
            <button
              className="btn-primary"
              onClick={() => simM.mutate()}
              disabled={simM.isPending || !efectivoContado.isValid}
            >
              {simM.isPending ? "Simulando..." : "Simular y Confirmar Cierre"}
            </button>
          </div>
        </div>
      </div>

      {closeErrors.length > 0 && (
        <ul className="text-sm text-red-600">
          {closeErrors.map((msg) => (
            <li key={msg}>- {msg}</li>
          ))}
        </ul>
      )}

      {showSimModal && simResult && (() => {
        const umbral = simResult.umbralDescuadre;
        const difAbs = Math.abs(simResult.diferenciaTotal ?? 0);
        const obsRequerida = umbral != null && difAbs > umbral;
        const obsValida = !obsRequerida || observacionCierre.trim().length > 0;

        return (
        <div className="fixed inset-0 z-40 flex items-center justify-center bg-black/40 p-4">
          <div className="card w-full max-w-lg p-5">
            <div className="mb-3 flex items-center justify-between">
              <h3 className="text-lg font-semibold">Simulacion de Cierre</h3>
              <button className="btn-ghost p-1" onClick={() => { setShowSimModal(false); setSimResult(null); setValoresArqueo(null); }}>
                <X size={14} />
              </button>
            </div>
            <div className="grid gap-2 rounded-xl border border-pos-border bg-gray-50 p-3 text-sm">
               <p>Turno: <span className="font-semibold">#{simResult.numeroTurno ?? simResult.id}</span></p>
              <p>Estado: <span className="font-semibold">{simResult.estado}</span></p>

              <div className="mt-2">
                <p className="font-semibold">Caja Física</p>
                <p>Sistema: {summaryNumber(simResult.esperado)} | Contado: {summaryNumber(simResult.efectivoContado)} | Dif: {summaryNumber(simResult.diferenciaEfectivo)}</p>
              </div>
              <div>
                <p className="font-semibold">Caja Virtual</p>
                <p>Sistema: {summaryNumber(simResult.transferenciasNetas)} | Verificado: {summaryNumber(simResult.transferenciasVerificadas)} | Dif: {summaryNumber(simResult.diferenciaTransferencias)}</p>
              </div>
              <div className="border-t pt-2">
                <p>Total (sistema): <span className="font-semibold">{summaryNumber(simResult.totalOperativoTurno)}</span></p>
                <p>Total verificado: <span className="font-semibold">{summaryNumber(simResult.totalVerificado)}</span></p>
                <p className="font-bold">Diferencia total: {summaryNumber(simResult.diferenciaTotal)}</p>
              </div>
            </div>
            <div className="mt-3">{renderResumenFinanciero(reporte)}</div>

            <div className="mt-4 space-y-2">
              <label className="block text-sm font-medium text-pos-muted">
                Observación del cajero
                {obsRequerida && (
                  <span className="ml-1 text-orange-600 font-semibold">(requerida — descuadre supera el umbral)</span>
                )}
              </label>
              <textarea
                ref={obsInputRef}
                className="input w-full resize-none"
                rows={2}
                maxLength={500}
                placeholder="Ingresa una observación sobre el descuadre..."
                value={observacionCierre}
                onChange={(e) => setObservacionCierre(e.target.value)}
              />
            </div>

            {confirmarCierreM.isError && (
              <ul className="mt-2 text-sm text-red-600">
                {getErrorMessages(confirmarCierreM.error).map((msg) => (
                  <li key={msg}>- {msg}</li>
                ))}
              </ul>
            )}

            <div className="mt-4 grid grid-cols-2 gap-2">
              <button
                className="btn-soft"
                onClick={() => { setShowSimModal(false); setSimResult(null); setValoresArqueo(null); }}
                disabled={confirmarCierreM.isPending}
              >
                Volver
              </button>
              <button
                className="btn-primary"
                onClick={() => confirmarCierreM.mutate()}
                disabled={confirmarCierreM.isPending || !obsValida}
              >
                {confirmarCierreM.isPending ? "Cerrando..." : "Confirmar cierre definitivo"}
              </button>
            </div>
          </div>
        </div>
        );
      })()}

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
               <p>Turno: <span className="font-semibold">#{closeResult.numeroTurno ?? closeResult.id}</span></p>
              <p>Estado final: <span className="font-semibold">{closeResult.estado}</span></p>
              <p>Fecha cierre: <span className="font-semibold">{closeResult.fechaCierre ? new Date(closeResult.fechaCierre).toLocaleString() : "-"}</span></p>

              <div className="mt-2">
                <p className="font-semibold">Caja Física</p>
                <p>Sistema: {summaryNumber(closeResult.esperado)} | Contado: {summaryNumber(closeResult.efectivoContado)} | Dif: {summaryNumber(closeResult.diferenciaEfectivo)}</p>
              </div>
              <div>
                <p className="font-semibold">Caja Virtual</p>
                <p>Sistema: {summaryNumber(closeResult.transferenciasNetas)} | Verificado: {summaryNumber(closeResult.transferenciasVerificadas)} | Dif: {summaryNumber(closeResult.diferenciaTransferencias)}</p>
              </div>
              <div className="border-t pt-2">
                <p>Total (sistema): <span className="font-semibold">{summaryNumber(closeResult.totalOperativoTurno)}</span></p>
                <p>Total verificado: <span className="font-semibold">{summaryNumber(closeResult.totalVerificado)}</span></p>
                <p className="font-bold">Diferencia total del cierre: {summaryNumber(closeResult.diferenciaTotal)}</p>
              </div>
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
