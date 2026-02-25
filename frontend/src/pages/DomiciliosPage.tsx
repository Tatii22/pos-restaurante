import { useMemo, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { BsXLg } from "react-icons/bs";
import { posApi } from "../shared/api/posApi";
import { useAuthStore } from "../shared/store/authStore";
import { useTurnoStore } from "../shared/store/turnoStore";
import { getErrorMessage, money } from "../shared/utils";

function estadoClass(estado: string): string {
  if (estado === "EN_PROCESO") return "bg-yellow-100 text-yellow-800";
  if (estado === "DESPACHADA") return "bg-green-100 text-green-800";
  if (estado === "ANULADA") return "bg-red-100 text-red-800";
  return "bg-gray-100 text-gray-700";
}

export function DomiciliosPage() {
  const { role } = useAuthStore();
  const { turno, isAbierto, setTurno } = useTurnoStore();
  const qc = useQueryClient();
  const [selectedId, setSelectedId] = useState<number | null>(null);
  const [nuevoDomicilio, setNuevoDomicilio] = useState<string>("0");
  const [confirm, setConfirm] = useState<{ id: number; action: "cancelar" | "anular" } | null>(null);
  const [showCobro, setShowCobro] = useState(false);
  const [pagoEfectivo, setPagoEfectivo] = useState<string>("");
  const [pagoTransferencia, setPagoTransferencia] = useState<string>("");
  const [activeCalcField, setActiveCalcField] = useState<"TRANSFERENCIA" | "EFECTIVO">("EFECTIVO");

  const turnoActivoDomiQ = useQuery({
    queryKey: ["turno-activo-domi-domicilios", role],
    queryFn: () => posApi.getTurnoActivo(),
    enabled: role === "DOMI",
    retry: false
  });

  const turnoIdFiltro = role === "CAJA" ? turno?.id : turnoActivoDomiQ.data?.id;

  const listQ = useQuery({
    queryKey: ["domicilios-list", role, turnoIdFiltro],
    enabled: !!turnoIdFiltro,
    queryFn: () =>
      posApi.getVentas({
        tipoVenta: "DOMICILIO",
        turnoId: turnoIdFiltro,
        page: 0,
        size: 80
      })
  });

  const selected = useMemo(
    () => (listQ.data?.content || []).find((v) => v.id === selectedId) ?? null,
    [listQ.data, selectedId]
  );
  const montoTotal = selected?.total || 0;
  const montoEfectivo = parseAmount(pagoEfectivo);
  const montoTransferencia = parseAmount(pagoTransferencia);
  const totalPagado = montoEfectivo + montoTransferencia;
  const faltante = Math.max(0, montoTotal - totalPagado);
  const vueltas = Math.max(0, totalPagado - montoTotal);
  const nuevoDomicilioNumero = parseAmount(nuevoDomicilio);
  const valorDomicilioValido = nuevoDomicilio.trim().length > 0 && nuevoDomicilioNumero >= 0;

  function parseAmount(value: string): number {
    const normalized = value.replace(/[^\d]/g, "");
    return Number(normalized || "0");
  }

  function cleanDigits(value: string, maxLen = 10): string {
    return value.replace(/[^\d]/g, "").slice(0, maxLen);
  }

  function pushCalcValue(token: string) {
    const update = (curr: string) => {
      if (token === "C") return "0";
      if (token === "<") return curr.length <= 1 ? "0" : curr.slice(0, -1);
      if (curr === "0") return token;
      return curr + token;
    };
    if (activeCalcField === "TRANSFERENCIA") {
      setPagoTransferencia((prev) => update(prev));
    } else {
      setPagoEfectivo((prev) => update(prev));
    }
  }

  function pagarExacto() {
    if (!selected || dispatchM.isPending) return;
    let efectivoFinal = montoEfectivo;
    let transferenciaFinal = montoTransferencia;
    if (faltante > 0) {
      if (activeCalcField === "TRANSFERENCIA") {
        transferenciaFinal = montoTransferencia + faltante;
        setPagoTransferencia(String(transferenciaFinal));
      } else {
        efectivoFinal = montoEfectivo + faltante;
        setPagoEfectivo(String(efectivoFinal));
      }
    }
    dispatchM.mutate({
      id: selected.id,
      pagoEfectivo: efectivoFinal,
      pagoTransferencia: transferenciaFinal,
      formaPago: resolveFormaPagoFrom(transferenciaFinal, efectivoFinal)
    });
    setShowCobro(false);
  }

  function resolveFormaPagoFrom(transferValue: number, cashValue: number): "EFECTIVO" | "TRANSFERENCIA" {
    if (transferValue > 0 && cashValue === 0) return "TRANSFERENCIA";
    if (cashValue > 0 && transferValue === 0) return "EFECTIVO";
    return transferValue >= cashValue ? "TRANSFERENCIA" : "EFECTIVO";
  }

  const dispatchM = useMutation({
    mutationFn: (payload: { id: number; pagoEfectivo: number; pagoTransferencia: number; formaPago: "EFECTIVO" | "TRANSFERENCIA" }) =>
      posApi.despachar(payload.id, {
        formaPago: payload.formaPago,
        pagoEfectivo: payload.pagoEfectivo,
        pagoTransferencia: payload.pagoTransferencia
      }),
    onSuccess: async () => {
      await qc.invalidateQueries({ queryKey: ["domicilios-list"] });
      await qc.invalidateQueries({ queryKey: ["turno-activo-layout"] });
      await qc.invalidateQueries({ queryKey: ["historial-turno-ventas"] });
      if (role === "CAJA") {
        posApi.getTurnoActivo().then((t) => setTurno(t)).catch(() => {});
      }
    }
  });
  const cancelM = useMutation({
    mutationFn: (id: number) => posApi.cancelar(id),
    onSuccess: async () => {
      await qc.invalidateQueries({ queryKey: ["domicilios-list"] });
      await qc.invalidateQueries({ queryKey: ["inventario-ventas"] });
      await qc.invalidateQueries({ queryKey: ["catalogo-hoy"] });
    }
  });
  const anularM = useMutation({
    mutationFn: (id: number) => posApi.anular(id),
    onSuccess: async () => {
      await qc.invalidateQueries({ queryKey: ["domicilios-list"] });
      await qc.invalidateQueries({ queryKey: ["turno-activo-layout"] });
      await qc.invalidateQueries({ queryKey: ["historial-turno-ventas"] });
      await qc.invalidateQueries({ queryKey: ["inventario-ventas"] });
      await qc.invalidateQueries({ queryKey: ["catalogo-hoy"] });
      if (role === "CAJA") {
        posApi.getTurnoActivo().then((t) => setTurno(t)).catch(() => {});
      }
    }
  });
  const domicilioM = useMutation({
    mutationFn: ({ id, valor }: { id: number; valor: number }) => posApi.actualizarValorDomicilio(id, valor),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["domicilios-list"] })
  });
  const printCocinaM = useMutation({
    mutationFn: (id: number) => posApi.imprimirCocina(id)
  });
  const printFacturaM = useMutation({
    mutationFn: (id: number) => posApi.imprimirFactura(id)
  });

  if (role === "CAJA" && (!turno || !isAbierto())) {
    return (
      <div className="card p-4">
        <p className="text-sm text-pos-muted">No hay turno ABIERTO. No se pueden gestionar domicilios.</p>
      </div>
    );
  }

  if (role === "DOMI" && !turnoActivoDomiQ.data) {
    return (
      <div className="card p-4">
        <p className="text-sm text-pos-muted">No hay turno ABIERTO. No se pueden gestionar domicilios.</p>
      </div>
    );
  }

  if (listQ.isError || turnoActivoDomiQ.isError) {
    return <p className="text-sm text-red-600">{getErrorMessage(listQ.error || turnoActivoDomiQ.error)}</p>;
  }

  return (
    <div className="grid gap-4 xl:grid-cols-[minmax(0,1.4fr)_minmax(0,1fr)]">
      <section className="card p-3 md:hidden">
        <div className="grid gap-2">
          {(listQ.data?.content || []).map((v) => (
            <div key={v.id} className="rounded-xl border border-pos-border p-3">
              <div className="flex items-start justify-between gap-2">
                <p className="font-semibold">#{v.id} {v.clienteNombre || "-"}</p>
                <span className={`rounded-full px-2 py-1 text-xs font-semibold ${estadoClass(v.estado)}`}>{v.estado}</span>
              </div>
              <p className="text-sm break-words">{v.direccion || "-"}</p>
              <p className="text-sm">{v.telefono || "-"}</p>
              <p className="text-sm font-semibold">{money.format(v.total)}</p>
              <div className="mt-2">
                <button
                  className="btn-ghost"
                  onClick={() => {
                    setSelectedId(v.id);
                    setNuevoDomicilio(String(v.valorDomicilio || 0));
                  }}
                >
                  Ver
                </button>
              </div>
            </div>
          ))}
        </div>
      </section>

      <section className="card hidden overflow-x-auto md:block">
        <table className="w-full min-w-[680px] table-fixed text-sm">
          <thead>
            <tr className="border-b border-pos-border">
              <th className="w-16 p-3 text-left">ID</th>
              <th className="w-32 p-3 text-left">Cliente</th>
              <th className="p-3 text-left">Direccion</th>
              <th className="w-28 p-3 text-left">Telefono</th>
              <th className="w-28 p-3 text-left">Estado</th>
              <th className="w-24 p-3 text-left">Total</th>
              <th className="w-24 p-3 text-left">Acciones</th>
            </tr>
          </thead>
          <tbody>
            {(listQ.data?.content || []).map((v) => (
              <tr key={v.id} className="border-b border-pos-border/70">
                <td className="p-3">#{v.id}</td>
                <td className="truncate p-3" title={v.clienteNombre || "-"}>{v.clienteNombre || "-"}</td>
                <td className="truncate p-3" title={v.direccion || "-"}>{v.direccion || "-"}</td>
                <td className="truncate p-3" title={v.telefono || "-"}>{v.telefono || "-"}</td>
                <td className="p-3">
                  <span className={`rounded-full px-2 py-1 text-xs font-semibold ${estadoClass(v.estado)}`}>{v.estado}</span>
                </td>
                <td className="p-3">{money.format(v.total)}</td>
                <td className="p-3 whitespace-nowrap">
                  <button className="btn-ghost" onClick={() => {
                    setSelectedId(v.id);
                    setNuevoDomicilio(String(v.valorDomicilio || 0));
                  }}>
                    Ver
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </section>

      <section className="card p-4">
        <h2 className="mb-2 text-xl font-semibold">Domicilio {selected ? `#${selected.id}` : ""}</h2>
        {!selected && <p className="text-sm text-pos-muted">Selecciona un pedido para gestionar.</p>}
        {selected && (
          <div className="grid gap-3">
            <div className="space-y-1 text-sm break-words">
              <p><span className="text-pos-muted">Estado:</span> {selected.estado}</p>
              <p><span className="text-pos-muted">Cliente:</span> {selected.clienteNombre || "-"}</p>
              <p><span className="text-pos-muted">Direccion:</span> {selected.direccion || "-"}</p>
              <p><span className="text-pos-muted">Telefono:</span> {selected.telefono || "-"}</p>
              <p><span className="text-pos-muted">Total:</span> {money.format(selected.total)}</p>
            </div>

            {selected.estado === "EN_PROCESO" && (role === "CAJA" || role === "DOMI") && (
              <>
                <label className="text-sm">
                  Valor domicilio
                  <div className="mt-1 flex gap-2">
                    <input
                      className="input"
                      value={nuevoDomicilio}
                      onChange={(e) => setNuevoDomicilio(cleanDigits(e.target.value, 9))}
                      inputMode="numeric"
                      maxLength={9}
                    />
                    <button
                      className="btn-soft"
                      disabled={!valorDomicilioValido || domicilioM.isPending}
                      onClick={() => domicilioM.mutate({ id: selected.id, valor: nuevoDomicilioNumero })}
                    >
                      Actualizar
                    </button>
                  </div>
                  {!valorDomicilioValido && <p className="mt-1 text-xs text-orange-700">Ingresa un valor valido para domicilio.</p>}
                </label>

                <div className="grid gap-2">
                  <button className="btn-soft" onClick={() => printCocinaM.mutate(selected.id)}>Imprimir Cocina</button>
                  <button className="btn-soft" onClick={() => printFacturaM.mutate(selected.id)}>Imprimir Factura</button>
                  {role === "CAJA" && (
                    <button
                      className="btn-primary"
                      onClick={() => {
                        setPagoEfectivo("0");
                        setPagoTransferencia("0");
                        setActiveCalcField("EFECTIVO");
                        setShowCobro(true);
                      }}
                    >
                      Despachar
                    </button>
                  )}
                  <button className="btn-ghost bg-yellow-100 text-yellow-800 hover:bg-yellow-200" onClick={() => setConfirm({ id: selected.id, action: "cancelar" })}>
                    Cancelar
                  </button>
                </div>
              </>
            )}

            {selected.estado === "DESPACHADA" && role === "CAJA" && (
              <button className="btn-ghost bg-red-100 text-red-700 hover:bg-red-200" onClick={() => setConfirm({ id: selected.id, action: "anular" })}>
                Anular
              </button>
            )}
          </div>
        )}
      </section>

      {confirm && (
        <div className="fixed inset-0 z-40 flex items-center justify-center bg-black/40 p-4">
          <div className="card w-full max-w-md p-5">
            <h3 className="text-lg font-semibold">Confirmar accion</h3>
            <p className="mt-2 text-sm text-pos-muted">Esta accion devolvera inventario. Deseas continuar?</p>
            <div className="mt-4 flex gap-2">
              <button className="btn-ghost flex-1" onClick={() => setConfirm(null)}>No</button>
              <button
                className="btn-primary flex-1"
                onClick={() => {
                  if (confirm.action === "cancelar") cancelM.mutate(confirm.id);
                  if (confirm.action === "anular") anularM.mutate(confirm.id);
                  setConfirm(null);
                }}
              >
                Si, continuar
              </button>
            </div>
          </div>
        </div>
      )}

      {showCobro && selected && role === "CAJA" && (
        <div className="fixed inset-0 z-40 flex items-center justify-center bg-black/40 p-4">
          <div className="card w-full max-w-lg p-5">
            <div className="mb-3 flex items-center justify-between">
              <h4 className="font-semibold">Cobro de domicilio #{selected.id}</h4>
              <button className="btn-ghost p-1" onClick={() => setShowCobro(false)}>
                <BsXLg size={14} />
              </button>
            </div>

            <div className="grid gap-2 rounded-xl border border-pos-border bg-gray-50 p-3 text-sm">
              <p>Total: <span className="font-semibold">{money.format(montoTotal)}</span></p>
              <p>Pagado: <span className="font-semibold">{money.format(totalPagado)}</span></p>
              <p>Falta: <span className="font-semibold text-orange-700">{money.format(faltante)}</span></p>
              <p>Vueltos: <span className="font-semibold text-green-700">{money.format(vueltas)}</span></p>
            </div>

            <div className="mt-3 grid gap-2 md:grid-cols-2">
              <div>
                <button
                  className={activeCalcField === "TRANSFERENCIA" ? "btn-soft w-full border border-green-300 bg-green-50 text-green-700" : "btn-ghost w-full"}
                  onClick={() => setActiveCalcField("TRANSFERENCIA")}
                >
                  Transferencia
                </button>
                <input
                  className="input mt-1"
                  value={pagoTransferencia}
                  onChange={(e) => setPagoTransferencia(cleanDigits(e.target.value, 9))}
                  inputMode="numeric"
                  maxLength={9}
                />
              </div>
              <div>
                <button
                  className={activeCalcField === "EFECTIVO" ? "btn-soft w-full border border-green-300 bg-green-50 text-green-700" : "btn-ghost w-full"}
                  onClick={() => setActiveCalcField("EFECTIVO")}
                >
                  Efectivo
                </button>
                <input
                  className="input mt-2"
                  value={pagoEfectivo}
                  onChange={(e) => setPagoEfectivo(cleanDigits(e.target.value, 9))}
                  inputMode="numeric"
                  maxLength={9}
                />
              </div>
            </div>

            <div className="mt-3 grid grid-cols-3 gap-2">
              <button className="btn-soft col-span-3" onClick={pagarExacto} disabled={dispatchM.isPending}>
                Pagar exacto ({money.format(faltante)})
              </button>
              {["7", "8", "9", "4", "5", "6", "1", "2", "3", "000", "0", "<"].map((k) => (
                <button key={k} className="btn-ghost" onClick={() => pushCalcValue(k)}>
                  {k}
                </button>
              ))}
              <button className="btn-ghost col-span-3" onClick={() => pushCalcValue("C")}>
                Limpiar
              </button>
            </div>

            <div className="mt-3 flex gap-2">
              <button className="btn-ghost flex-1" onClick={() => setShowCobro(false)}>
                Cancelar
              </button>
              <button
                className="btn-primary flex-1"
                disabled={faltante > 0 || dispatchM.isPending}
                onClick={() => {
                  dispatchM.mutate({
                    id: selected.id,
                    pagoEfectivo: montoEfectivo,
                    pagoTransferencia: montoTransferencia,
                    formaPago: resolveFormaPagoFrom(montoTransferencia, montoEfectivo)
                  });
                  setShowCobro(false);
                }}
              >
                Confirmar despacho
              </button>
            </div>
            {faltante > 0 && (
              <p className="mt-2 text-xs text-orange-700">Aun falta dinero por registrar para completar el pago.</p>
            )}
          </div>
        </div>
      )}

      {(dispatchM.isError || cancelM.isError || anularM.isError || domicilioM.isError || printCocinaM.isError || printFacturaM.isError) && (
        <p className="xl:col-span-2 text-sm text-red-600">
          {getErrorMessage(dispatchM.error || cancelM.error || anularM.error || domicilioM.error || printCocinaM.error || printFacturaM.error)}
        </p>
      )}
    </div>
  );
}
