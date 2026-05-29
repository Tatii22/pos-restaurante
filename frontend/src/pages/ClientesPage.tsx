import { useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { HandCoins, Plus, Search, X } from "lucide-react";
import { posApi } from "../shared/api/posApi";
import { ClienteSearch } from "../shared/ClienteSearch";
import { formatCurrencyInput, getErrorMessage, money, parseCurrencyInput } from "../shared/utils";
import type { Cliente, ClienteDetalle, AbonoFiado, ClienteSearch as ClienteSearchType } from "../types";

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
  const [soloConDeuda, setSoloConDeuda] = useState(true);

  const [abonoEfectivo, setAbonoEfectivo] = useState("0");
  const [abonoTransferencia, setAbonoTransferencia] = useState("0");
  const [abonoObservacion, setAbonoObservacion] = useState("");

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
      // Mostrar cambio si el servidor lo informa
      if (data.cambioEfectivo && data.cambioEfectivo > 0) {
        window.alert(
          `✅ Abono registrado correctamente.\n💵 Devolver al cliente: ${data.cambioEfectivo.toLocaleString("es-CO")}`
        );
      }
    }
  });

  const filteredClientes = (clientesQ.data || []).filter(
    (d) =>
      d.nombre.toLowerCase().includes(search.toLowerCase()) ||
      d.telefono.includes(search)
  );

  function formatCurrencyChange(setter: (value: string) => void) {
    return (value: string) => {
      const cleaned = value.replace(/[^\d]/g, "");
      setter(cleaned || "0");
    };
  }

  return (
    <div className="grid gap-4">
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
            className="input pl-9"
            placeholder="Buscar por nombre o teléfono..."
            value={search}
            onChange={(e) => setSearch(e.target.value)}
          />
        </div>
        <button
          className={soloConDeuda ? "btn-soft" : "btn-ghost"}
          onClick={() => setSoloConDeuda(!soloConDeuda)}
        >
          Solo con deuda
        </button>
      </div>

      <div className="card overflow-hidden">
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
                  <th className="px-3 py-2 text-left text-xs font-semibold uppercase text-pos-muted">Nombre</th>
                  <th className="px-3 py-2 text-left text-xs font-semibold uppercase text-pos-muted">Teléfono</th>
                  <th className="px-3 py-2 text-right text-xs font-semibold uppercase text-pos-muted">Deuda total</th>
                  <th className="px-3 py-2 text-right text-xs font-semibold uppercase text-pos-muted">Ventas pendientes</th>
                  <th className="px-3 py-2 text-center text-xs font-semibold uppercase text-pos-muted">Acciones</th>
                </tr>
              </thead>
              <tbody>
                {filteredClientes.map((cliente) => (
                  <tr key={cliente.id} className="border-t border-pos-border">
                    <td className="px-3 py-2">{cliente.nombre}</td>
                    <td className="px-3 py-2 font-mono text-sm">{cliente.telefono}</td>
                    <td className={`px-3 py-2 text-right font-semibold ${cliente.deudaTotal > 0 ? "text-red-600" : "text-green-600"}`}>
                      {money.format(cliente.deudaTotal)}
                    </td>
                    <td className="px-3 py-2 text-right">{cliente.ventasPendientes}</td>
                    <td className="px-3 py-2 text-center">
                      <button
                        className="btn-ghost text-xs"
                        onClick={() => {
                          setShowDetalle({ id: cliente.id, nombre: cliente.nombre, telefono: cliente.telefono, deudaTotal: cliente.deudaTotal, ventasPendientes: [], abonos: [] } as ClienteDetalle);
                        }}
                      >
                        Ver detalle
                      </button>
                      {cliente.deudaTotal > 0 && (
                        <button
                          className="btn-ghost text-xs text-green-600"
                          onClick={() => setShowAbono(cliente)}
                        >
                          Registrar abono
                        </button>
                      )}
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
            <ClienteSearch
              onSelect={(cliente: ClienteSearchType) => {
                setShowNuevoCliente(false);
                qc.invalidateQueries({ queryKey: ["clientes-page"] });
              }}
              label="Buscar o crear cliente"
              placeholder="Nombre o teléfono del nuevo cliente..."
              allowCreate={true}
              autoFocus={true}
              showDebt={false}
            />
            <div className="mt-4">
              <button className="btn-ghost w-full" onClick={() => setShowNuevoCliente(false)}>
                Cancelar
              </button>
            </div>
          </div>
        </div>
      )}

      {showDetalle && (
        <div className="fixed inset-0 z-40 flex items-center justify-center bg-black/40 p-4">
          <div className="card w-full max-w-2xl max-h-[90vh] overflow-auto p-5">
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
                              <p className="font-semibold">{money.format(v.total)}</p>
                              <p className="text-xs text-orange-600">Pendiente: {money.format(v.saldoPendiente || 0)}</p>
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
            <div className="mt-4 flex justify-end">
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
                  className="input mt-1"
                  value={formatCurrencyInput(abonoEfectivo)}
                  onChange={(e) => formatCurrencyChange(setAbonoEfectivo)(e.target.value)}
                  inputMode="numeric"
                />
              </label>
              <label className="text-sm">
                Transferencia
                <input
                  className={`input mt-1 ${transferenciaExcede ? "border-red-400" : ""}`}
                  value={formatCurrencyInput(abonoTransferencia)}
                  onChange={(e) => formatCurrencyChange(setAbonoTransferencia)(e.target.value)}
                  inputMode="numeric"
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
                  className="input mt-1"
                  value={abonoObservacion}
                  onChange={(e) => setAbonoObservacion(e.target.value)}
                  placeholder="Nota sobre el abono"
                />
              </label>
              {/* Resumen en tiempo real */}
              <div className="rounded-lg border border-pos-border bg-gray-50 p-3 text-sm space-y-1">
                <div className="flex justify-between">
                  <span className="text-pos-muted">Abono que se aplica</span>
                  <span className="font-semibold">
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
              <button className="btn-ghost flex-1" onClick={() => setShowAbono(null)}>
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