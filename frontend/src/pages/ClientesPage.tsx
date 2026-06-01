import { useEffect, useRef, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { HandCoins, Plus, Search, X } from "lucide-react";
import { posApi } from "../shared/api/posApi";
import { formatCurrencyInput, getErrorMessage, money, parseCurrencyInput } from "../shared/utils";
import type { Cliente, ClienteDetalle, AbonoFiado } from "../types";

function formatDate(d: string) {
  const date = new Date(d);
  const day = String(date.getDate()).padStart(2, "0");
  const month = String(date.getMonth() + 1).padStart(2, "0");
  const year = date.getFullYear();
  let hours = date.getHours();
  const minutes = String(date.getMinutes()).padStart(2, "0");
  const ampm = hours >= 12 ? "p.m." : "a.m.";
  hours = hours % 12 || 12;
  return `${day}/${month}/${year} ${hours}:${minutes} ${ampm}`;
}

export function ClientesPage() {
  const qc = useQueryClient();
  const [search, setSearch] = useState("");
  const [showNuevoCliente, setShowNuevoCliente] = useState(false);
  const [showDetalle, setShowDetalle] = useState<ClienteDetalle | null>(null);
  const [showAbono, setShowAbono] = useState<Cliente | null>(null);
  const [soloConDeuda, setSoloConDeuda] = useState(false);

  const [abonoEfectivo, setAbonoEfectivo] = useState("0");
  const [abonoTransferencia, setAbonoTransferencia] = useState("0");
  const [abonoObservacion, setAbonoObservacion] = useState("");
  const [successMsg, setSuccessMsg] = useState<string | null>(null);
  const successTimerRef = useRef<ReturnType<typeof setTimeout> | null>(null);

  useEffect(() => {
    return () => {
      if (successTimerRef.current) clearTimeout(successTimerRef.current);
    };
  }, []);

  const [nuevoNombre, setNuevoNombre] = useState("");
  const [nuevoTelefono, setNuevoTelefono] = useState("");
  const [nuevoError, setNuevoError] = useState<string | null>(null);

  const crearClienteM = useMutation({
    mutationFn: () => posApi.crearCliente({ nombre: nuevoNombre.trim(), telefono: nuevoTelefono.replace(/\D/g, "") }),
    onSuccess: () => {
      setShowNuevoCliente(false);
      setNuevoNombre("");
      setNuevoTelefono("");
      setNuevoError(null);
      qc.invalidateQueries({ queryKey: ["clientes-page"] });
    },
    onError: (err) => {
      const msg = getErrorMessage(err);
      setNuevoError(
        msg.toLowerCase().includes("duplicate") || msg.includes("ya está")
          ? "Este teléfono ya está registrado"
          : msg
      );
    },
  });

  // Cambio estimado en tiempo real (solo orientativo; el servidor valida la cifra exacta)
  const abonoEfectivoNum      = parseCurrencyInput(abonoEfectivo);
  const abonoTransferenciaNum = parseCurrencyInput(abonoTransferencia);
  const deudaActualAbono      = showAbono?.deudaTotal ?? 0;
  const transferenciaExcede   = abonoTransferenciaNum > deudaActualAbono;
  const faltanteTrasTransf    = Math.max(0, deudaActualAbono - abonoTransferenciaNum);
  const efectivoAplicadoEst   = Math.min(abonoEfectivoNum, faltanteTrasTransf);
  const cambioEstimado        = Math.max(0, abonoEfectivoNum - efectivoAplicadoEst);

  const clientesQ = useQuery({
    queryKey: ["clientes-page", soloConDeuda],
    queryFn: () => posApi.getClientes(soloConDeuda)
  });

  const detalleQ = useQuery({
    queryKey: ["cliente-detalle", showDetalle?.id],
    queryFn: () => posApi.getClienteById(showDetalle!.id),
    enabled: !!showDetalle
  });

  const abonoM = useMutation({
    mutationFn: () =>
      posApi.registrarAbonoFiado({
         clienteId: showAbono!.id,
        montoEfectivo: parseCurrencyInput(abonoEfectivo),
        montoTransferencia: parseCurrencyInput(abonoTransferencia),
        observacion: abonoObservacion.trim() || undefined
      }),
    onSuccess: (data: AbonoFiado) => {
      setShowAbono(null);
      setAbonoEfectivo("0");
      setAbonoTransferencia("0");
      setAbonoObservacion("");
      qc.invalidateQueries({ queryKey: ["clientes-page"] });
      qc.invalidateQueries({ queryKey: ["cliente-detalle"] });
      if (data.cambioEfectivo && data.cambioEfectivo > 0) {
        setSuccessMsg(`Abono registrado. Devolver al cliente: ${money.format(data.cambioEfectivo)}`);
        if (successTimerRef.current) clearTimeout(successTimerRef.current);
        successTimerRef.current = setTimeout(() => setSuccessMsg(null), 4000);
      }
    }
  });

  const filteredClientes = (clientesQ.data || [])
    .filter(
      (d) =>
        d.nombre.toLowerCase().includes(search.toLowerCase()) ||
        d.telefono.includes(search)
    )
    .sort((a, b) => {
      if (a.deudaTotal > 0 && b.deudaTotal === 0) return -1;
      if (a.deudaTotal === 0 && b.deudaTotal > 0) return 1;
      return 0;
    });

  useEffect(() => {
    if (showDetalle) {
      document.body.style.overflow = "hidden";
      return () => { document.body.style.overflow = ""; };
    }
  }, [showDetalle]);

  function formatCurrencyChange(setter: (value: string) => void) {
    return (value: string) => {
      const cleaned = value.replace(/[^\d]/g, "");
      setter(cleaned || "0");
    };
  }

  return (
    <div className="grid gap-4">
      {successMsg && (
        <div className="fixed right-4 top-20 z-50 rounded-xl border border-green-300 bg-green-50 px-4 py-3 text-sm font-semibold text-green-700 shadow-pos">
          {successMsg}
        </div>
      )}
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold">Clientes frecuentes</h1>
        <button className="btn-primary" onClick={() => setShowNuevoCliente(true)}>
          <Plus size={16} className="mr-1" />
          Nuevo cliente
        </button>
      </div>

      <div className="flex gap-2">
        <div className="relative flex-1">
          <Search size={16} className="absolute left-3 top-1/2 -translate-y-1/2 text-pos-muted" />
          <input
            className="input pl-9 h-10"
            placeholder="Buscar por nombre o teléfono..."
            value={search}
            onChange={(e) => setSearch(e.target.value)}
          />
        </div>
        <button
          className={`btn-ghost h-10 px-5 gap-2 ${soloConDeuda ? "bg-pos-accentSoft text-pos-forest" : ""}`}
          onClick={() => setSoloConDeuda(!soloConDeuda)}
        >
          {soloConDeuda && <span>✓</span>}
          Solo con deuda
        </button>
      </div>

      <div className="card overflow-hidden mt-6">
        {clientesQ.isLoading && (
          <p className="px-3 py-8 text-center text-pos-muted">Cargando...</p>
        )}
        {clientesQ.isError && (
          <p className="px-3 py-8 text-center text-red-600">{getErrorMessage(clientesQ.error)}</p>
        )}
        {clientesQ.data && filteredClientes.length === 0 && (
          <p className="px-3 py-8 text-center text-pos-muted">No hay clientes registrados</p>
        )}
        {filteredClientes.length > 0 && (
          <>
            <div className="grid gap-2 p-3 md:hidden">
              {filteredClientes.map((cliente) => (
                <div key={cliente.id} className="rounded-xl border border-pos-border p-3">
                  <div className="flex items-start justify-between">
                    <div>
                      <p className="font-semibold">{cliente.nombre}</p>
                      <p className="font-mono text-sm text-pos-muted">{cliente.telefono}</p>
                    </div>
                    <p className={`font-semibold ${cliente.deudaTotal > 0 ? "text-red-600" : "text-green-600"}`}>
                      {money.format(cliente.deudaTotal)}
                    </p>
                  </div>
                  <div className="mt-2 flex items-center justify-between">
                    <p className="text-xs text-pos-muted">{cliente.ventasPendientes} ventas pendientes</p>
                    <div className="flex gap-1">
                      <button
                        className="btn-ghost text-xs"
                        onClick={() => setShowDetalle({ id: cliente.id, nombre: cliente.nombre, telefono: cliente.telefono, deudaTotal: cliente.deudaTotal, ventasPendientes: [], abonos: [] } as ClienteDetalle)}
                      >
                        Ver detalle
                      </button>
                      {cliente.deudaTotal > 0 && (
                        <button
                          className="btn-ghost text-xs text-green-600"
                          onClick={() => setShowAbono(cliente)}
                        >
                          Abono
                        </button>
                      )}
                    </div>
                  </div>
                </div>
              ))}
            </div>
            <table className="hidden w-full md:table">
              <thead className="bg-gray-50">
                <tr>
                  <th className="pl-6 pr-4 py-3 text-left text-xs font-semibold uppercase text-pos-muted">Nombre</th>
                  <th className="px-4 py-3 text-right text-xs font-semibold uppercase text-pos-muted">Teléfono</th>
                  <th className="px-4 py-3 text-right text-xs font-semibold uppercase text-pos-muted">Deuda total</th>
                  <th className="px-4 py-3 text-right text-xs font-semibold uppercase text-pos-muted">Ventas pendientes</th>
                  <th className="pl-4 pr-6 py-3 text-center text-xs font-semibold uppercase text-pos-muted">Acciones</th>
                </tr>
              </thead>
              <tbody>
                {filteredClientes.map((cliente) => (
                  <tr key={cliente.id} className="border-t border-pos-border">
                    <td className="py-4 pl-6 pr-4">{cliente.nombre}</td>
                    <td className="py-4 px-4 font-mono text-sm text-right">{cliente.telefono}</td>
                    <td className={`py-4 px-4 text-right font-semibold ${cliente.deudaTotal > 0 ? "text-red-600" : "text-green-600"}`}>
                      {money.format(cliente.deudaTotal)}
                    </td>
                    <td className="py-4 px-4 text-right">{cliente.ventasPendientes}</td>
                    <td className="py-4 pl-4 pr-6 text-center">
                      <div className="flex items-center justify-center gap-x-3">
                        <button
                          className="btn-ghost min-w-[120px] px-4 py-1.5 text-xs"
                          onClick={() => {
                            setShowDetalle({ id: cliente.id, nombre: cliente.nombre, telefono: cliente.telefono, deudaTotal: cliente.deudaTotal, ventasPendientes: [], abonos: [] } as ClienteDetalle);
                          }}
                        >
                          Ver detalle
                        </button>
                        <button
                          className={`btn-ghost min-w-[120px] px-4 py-1.5 text-xs text-green-600 ${cliente.deudaTotal > 0 ? '' : 'invisible pointer-events-none'}`}
                          onClick={() => setShowAbono(cliente)}
                        >
                          Registrar abono
                        </button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </>
        )}
      </div>

      {showNuevoCliente && (
        <div className="fixed inset-0 z-40 flex items-center justify-center bg-black/40 p-4">
          <div className="card w-full max-w-md p-5">
            <div className="mb-4 flex items-center justify-between">
              <h3 className="text-lg font-semibold">Nuevo cliente</h3>
              <button className="btn-ghost p-1" onClick={() => setShowNuevoCliente(false)}>
                <X size={16} />
              </button>
            </div>
            <form onSubmit={(e) => { e.preventDefault(); if (!crearClienteM.isPending) crearClienteM.mutate(); }} className="grid gap-3">
              <label className="text-sm">
                Nombre *
                <input
                  className="input mt-1"
                  value={nuevoNombre}
                  onChange={(e) => setNuevoNombre(e.target.value)}
                  placeholder="Nombre del cliente"
                  autoFocus
                />
              </label>
              <label className="text-sm">
                Teléfono *
                <input
                  className="input mt-1"
                  value={nuevoTelefono}
                  onChange={(e) => setNuevoTelefono(e.target.value.replace(/\D/g, "").slice(0, 15))}
                  placeholder="Ej: 3123456789"
                  inputMode="numeric"
                />
              </label>
              {nuevoError && <p className="text-sm text-red-600">{nuevoError}</p>}
              <div className="mt-2 flex gap-2">
                <button
                  type="button"
                  className="btn-ghost flex-1"
                  onClick={() => { setShowNuevoCliente(false); setNuevoError(null); setNuevoNombre(""); setNuevoTelefono(""); }}
                >
                  Cancelar
                </button>
                <button
                  type="submit"
                  className="btn-primary flex-1"
                  disabled={!nuevoNombre.trim() || nuevoTelefono.replace(/\D/g, "").length < 7 || crearClienteM.isPending}
                >
                  {crearClienteM.isPending ? "Creando..." : "Crear cliente"}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {showDetalle && (
        <div className="fixed inset-0 z-40 flex items-center justify-center bg-black/40 p-4">
          <div className="card w-full max-w-2xl max-h-[90vh] overflow-y-auto p-5 [&::-webkit-scrollbar]:w-2 [&::-webkit-scrollbar-thumb]:rounded-full [&::-webkit-scrollbar-thumb]:bg-gray-300 [&::-webkit-scrollbar-track]:bg-transparent">
            <div className="mb-4 flex items-center justify-between">
              <h3 className="text-lg font-semibold">Detalle de {showDetalle.nombre}</h3>
              <button className="btn-ghost p-1" onClick={() => setShowDetalle(null)}>
                <X size={16} />
              </button>
            </div>
            {detalleQ.isLoading && <p className="text-center text-pos-muted">Cargando...</p>}
            {detalleQ.data && (
              <div className="grid gap-4">
                <div className="rounded-xl border border-pos-border bg-gray-50 p-3">
                  <p className="text-xs text-pos-muted">Deuda total</p>
                  <p className="text-2xl font-bold text-red-600">{money.format(detalleQ.data.deudaTotal)}</p>
                  <p className="text-sm text-pos-muted">{detalleQ.data.ventasPendientes.length} ventas pendientes</p>
                </div>

                {detalleQ.data.ventasPendientes.length > 0 && (
                  <div>
                    <h4 className="mb-2 font-semibold">Ventas pendientes</h4>
                    <div className="space-y-2">
                      {detalleQ.data.ventasPendientes.map((v) => (
                        <div key={v.id} className="rounded-lg border border-pos-border p-3">
                          <div className="flex items-center justify-between">
                            <div>
                              <p className="font-semibold">Venta #{v.id}</p>
                              <p className="text-xs text-pos-muted">{formatDate(v.fecha)}</p>
                            </div>
                            <div className="text-right">
                              <p className="text-sm text-pos-muted">{money.format(v.total)}</p>
                              <p className="text-xs font-semibold text-orange-600">Pendiente: {money.format(v.saldoPendiente || 0)}</p>
                            </div>
                          </div>
                        </div>
                      ))}
                    </div>
                  </div>
                )}

                {detalleQ.data.abonos.length > 0 && (
                  <div>
                    <h4 className="mb-2 font-semibold">Historial de abonos</h4>
                    <div className="space-y-2">
                      {detalleQ.data.abonos.map((a) => (
                        <div key={a.id} className="rounded-lg border border-green-200 bg-green-50 p-3">
                          <div className="flex items-center justify-between">
                            <div>
                              <p className="font-semibold">{money.format(a.monto)}</p>
                              <p className="text-xs text-pos-muted">{formatDate(a.fecha)}</p>
                              <p className="text-xs text-pos-muted">{a.formaPago}</p>
                            </div>
                            <div className="text-right">
                              <p className="text-xs text-pos-muted">Usuario: {a.usuario}</p>
                              {a.observacion && <p className="text-xs italic text-pos-muted">{a.observacion}</p>}
                            </div>
                          </div>
                        </div>
                      ))}
                    </div>
                  </div>
                )}
              </div>
            )}
            <div className="mt-4 flex justify-end pb-1">
              <button className="btn-ghost" onClick={() => setShowDetalle(null)}>
                Cerrar
              </button>
            </div>
          </div>
        </div>
      )}

      {showAbono && (
        <div className="fixed inset-0 z-40 flex items-center justify-center bg-black/40 p-4">
          <div className="card w-full max-w-md p-5">
            <div className="mb-4 flex items-center justify-between">
              <h3 className="text-lg font-semibold">Registrar abono</h3>
              <button className="btn-ghost p-1" onClick={() => setShowAbono(null)}>
                <X size={16} />
              </button>
            </div>
            <div className="mb-4 rounded-xl border border-green-200 bg-green-50 p-3">
              <p className="text-xs text-pos-muted">Cliente</p>
              <p className="font-semibold">{showAbono.nombre}</p>
              <p className="text-sm text-pos-muted">{showAbono.telefono}</p>
              <p className="mt-2 text-lg font-bold text-red-600">
                Deuda actual: {money.format(showAbono.deudaTotal)}
              </p>
            </div>
            <div className="grid gap-3">
              <label className="text-sm">
                Efectivo
                <input
                  className="input mt-1 border-gray-200"
                  value={formatCurrencyInput(abonoEfectivo)}
                  onChange={(e) => formatCurrencyChange(setAbonoEfectivo)(e.target.value)}
                  inputMode="numeric"
                  placeholder="$ 0"
                />
              </label>
              <label className="text-sm">
                Transferencia
                <input
                  className={`input mt-1 border-gray-200 ${transferenciaExcede ? "border-red-400" : ""}`}
                  value={formatCurrencyInput(abonoTransferencia)}
                  onChange={(e) => formatCurrencyChange(setAbonoTransferencia)(e.target.value)}
                  inputMode="numeric"
                  placeholder="$ 0"
                />
                {transferenciaExcede && (
                  <p className="mt-1 text-xs text-red-600">
                    La transferencia no puede superar la deuda pendiente.
                  </p>
                )}
              </label>
              <label className="text-sm">
                Observación (opcional)
                <input
                  className="input mt-1 border-gray-200"
                  value={abonoObservacion}
                  onChange={(e) => setAbonoObservacion(e.target.value)}
                  placeholder="Nota sobre el abono"
                />
              </label>
              {/* Resumen en tiempo real */}
              <div className="rounded-lg border border-pos-border bg-slate-50 p-3 text-sm space-y-1">
                <div className="flex justify-between">
                  <span className="text-pos-muted">Abono que se aplica</span>
                  <span className="font-bold text-slate-700">
                    {money.format(efectivoAplicadoEst + abonoTransferenciaNum)}
                  </span>
                </div>
                {cambioEstimado > 0 && (
                  <div className="flex justify-between border-t pt-1">
                    <span className="text-pos-muted">💵 Cambio a devolver</span>
                    <span className="font-bold text-green-700">
                      {money.format(cambioEstimado)}
                    </span>
                  </div>
                )}
              </div>
            </div>
              {abonoM.isError && (
                <p className="text-sm text-red-600">{getErrorMessage(abonoM.error)}</p>
              )}
              <div className="mt-4 flex gap-2">
              <button className="btn-ghost flex-1 text-slate-600" onClick={() => setShowAbono(null)}>
                Cancelar
              </button>
              <button
                className="btn-primary flex-1"
                disabled={
                (parseCurrencyInput(abonoEfectivo) <= 0 && parseCurrencyInput(abonoTransferencia) <= 0) ||
                transferenciaExcede ||
                  abonoM.isPending
              }
                onClick={() => abonoM.mutate()}
              >
                Registrar abono
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}