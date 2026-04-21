import { useMemo, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { BsEye, BsFilter, BsXLg } from "react-icons/bs";
import { posApi } from "../shared/api/posApi";
import type { Venta, VentaDetalle } from "../shared/types";
import { useTurnoStore } from "../shared/store/turnoStore";
import { getErrorMessage, money } from "../shared/utils";

type FiltroTipo = "ALL" | "LOCAL" | "DOMICILIO" | "PARA_LLEVAR";
type FiltroEstado = "ALL" | "EN_PROCESO" | "DESPACHADA" | "CANCELADA" | "ANULADA";
type FiltroPago = "ALL" | "EFECTIVO" | "TRANSFERENCIA" | "MIXTO";

function tipoVentaLabel(venta: Venta): string {
  if (venta.tipoVenta === "LOCAL" && venta.paraLlevar) return "PARA LLEVAR";
  if (venta.tipoVenta === "DOMICILIO") return "DOMICILIO";
  return "LOCAL";
}

function estadoClass(estado: Venta["estado"]): string {
  if (estado === "DESPACHADA") return "bg-green-100 text-green-800";
  if (estado === "ANULADA") return "bg-red-100 text-red-800";
  if (estado === "CANCELADA") return "bg-gray-200 text-gray-700";
  return "bg-yellow-100 text-yellow-800";
}

function pagoLabel(venta: Pick<Venta, "formaPago" | "pagoEfectivo" | "pagoTransferencia">): string {
  const pagoEfectivo = Number(venta.pagoEfectivo ?? 0);
  const pagoTransferencia = Number(venta.pagoTransferencia ?? 0);

  if (pagoEfectivo > 0 && pagoTransferencia > 0) return "MIXTO";
  if (pagoTransferencia > 0) return "TRANSFERENCIA";
  if (pagoEfectivo > 0) return "EFECTIVO";
  return venta.formaPago === "TRANSFERENCIA" ? "TRANSFERENCIA" : "EFECTIVO";
}

function fechaHora(value?: string | null): string {
  if (!value) return "-";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return value;
  return date.toLocaleString("es-CO", {
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
    hour: "2-digit",
    minute: "2-digit"
  });
}

function hasText(value?: string | null): boolean {
  return !!value && value.trim().length > 0;
}

function hasPositiveNumber(value?: number | null): boolean {
  return value != null && Number(value) > 0;
}

function coincideBusqueda(venta: Venta, term: string): boolean {
  if (!term) return true;
  const text = [
    venta.id,
    venta.clienteNombre || "",
    venta.telefono || "",
    venta.estado,
    pagoLabel(venta),
    tipoVentaLabel(venta)
  ]
    .join(" ")
    .toLowerCase();
  return text.includes(term);
}

export function HistorialVentasPage() {
  const { turno, isActivo, setTurno } = useTurnoStore();
  const qc = useQueryClient();
  const [search, setSearch] = useState("");
  const [filtroTipo, setFiltroTipo] = useState<FiltroTipo>("ALL");
  const [filtroEstado, setFiltroEstado] = useState<FiltroEstado>("ALL");
  const [filtroPago, setFiltroPago] = useState<FiltroPago>("ALL");
  const [selectedVentaId, setSelectedVentaId] = useState<number | null>(null);
  const [confirmAnularId, setConfirmAnularId] = useState<number | null>(null);
  const [motivoAnulacion, setMotivoAnulacion] = useState("");

  const listQ = useQuery({
    queryKey: ["historial-turno-ventas", turno?.id],
    enabled: !!turno?.id && isActivo(),
    queryFn: () =>
      posApi.getVentas({
        turnoId: turno?.id,
        page: 0,
        size: 200
      })
  });

  const detalleQ = useQuery({
    queryKey: ["historial-turno-venta-detalle", selectedVentaId],
    enabled: !!selectedVentaId,
    queryFn: () => posApi.getVentaById(selectedVentaId as number)
  });

  const anularM = useMutation({
    mutationFn: ({ id, motivo }: { id: number; motivo: string }) => posApi.anular(id, { motivo }),
    onSuccess: async (_, variables) => {
      await qc.invalidateQueries({ queryKey: ["historial-turno-ventas"] });
      await qc.invalidateQueries({ queryKey: ["historial-turno-venta-detalle", variables.id] });
      await qc.invalidateQueries({ queryKey: ["turno-activo-layout"] });
      await qc.invalidateQueries({ queryKey: ["inventario-ventas"] });
      await qc.invalidateQueries({ queryKey: ["catalogo-hoy"] });
      posApi.getTurnoActivo().then((t) => setTurno(t)).catch(() => {});
      setConfirmAnularId(null);
      setMotivoAnulacion("");
    }
  });

  const numeracionTurno = useMemo(() => {
    const ventasDelTurno = [...(listQ.data?.content || [])]
      .filter((venta) => venta.estado !== "EN_PROCESO")
      .sort((a, b) => {
        const fechaA = new Date(a.fecha).getTime();
        const fechaB = new Date(b.fecha).getTime();
        if (fechaA !== fechaB) return fechaA - fechaB;
        return a.id - b.id;
      });

    return new Map(ventasDelTurno.map((venta, index) => [venta.id, index + 1]));
  }, [listQ.data?.content]);

  const ventas = useMemo(() => {
    const term = search.trim().toLowerCase();
    return (listQ.data?.content || [])
      .filter((venta) => venta.estado !== "EN_PROCESO")
      .filter((venta) => coincideBusqueda(venta, term))
      .filter((venta) => {
        if (filtroTipo === "ALL") return true;
        if (filtroTipo === "PARA_LLEVAR") return venta.tipoVenta === "LOCAL" && !!venta.paraLlevar;
        return venta.tipoVenta === filtroTipo && !venta.paraLlevar;
      })
      .filter((venta) => (filtroEstado === "ALL" ? true : venta.estado === filtroEstado))
      .filter((venta) => (filtroPago === "ALL" ? true : pagoLabel(venta) === filtroPago));
  }, [filtroEstado, filtroPago, filtroTipo, listQ.data?.content, search]);

  const selectedVenta = useMemo(
    () => ventas.find((venta) => venta.id === selectedVentaId) || (listQ.data?.content || []).find((venta) => venta.id === selectedVentaId) || null,
    [listQ.data?.content, selectedVentaId, ventas]
  );

  if (!turno || !isActivo()) {
    return (
      <div className="card p-4">
        <p className="text-sm text-pos-muted">No hay turno activo. El historial del turno solo aparece cuando caja tiene un turno abierto o simulado.</p>
      </div>
    );
  }

  if (listQ.isError) {
    return <p className="text-sm text-red-600">{getErrorMessage(listQ.error)}</p>;
  }

  return (
    <div className="grid gap-4">
      <section className="card p-4">
        <div className="flex flex-col gap-3 lg:flex-row lg:items-end lg:justify-between">
          <div>
            <h1 className="text-xl font-semibold">Historial de Ventas del Turno</h1>
            <p className="text-sm text-pos-muted">Turno #{turno.id}. Total Ventas: <span className="font-semibold text-pos-text">{ventas.length}</span></p>
          </div>
          <div className="flex items-center gap-2 rounded-xl px-3 py-2 text-sm text-pos-muted">
            <BsFilter size={14} />
            <span>Filtra por tipo, estado y pago, y abre cada venta para ver su detalle.</span>
          </div>
        </div>

        <div className="mt-4 grid gap-3 md:grid-cols-2 xl:grid-cols-4">
          <label className="text-sm">
            Buscar
            <input
              className="input mt-1"
              value={search}
              onChange={(e) => setSearch(e.target.value.slice(0, 80))}
              placeholder="ID, cliente, telefono..."
              maxLength={80}
            />
          </label>

          <label className="text-sm">
            Tipo
            <select className="input mt-1" value={filtroTipo} onChange={(e) => setFiltroTipo(e.target.value as FiltroTipo)}>
              <option value="ALL">Todos</option>
              <option value="LOCAL">Local</option>
              <option value="PARA_LLEVAR">Para llevar</option>
              <option value="DOMICILIO">Domicilio</option>
            </select>
          </label>

          <label className="text-sm">
            Estado
            <select className="input mt-1" value={filtroEstado} onChange={(e) => setFiltroEstado(e.target.value as FiltroEstado)}>
              <option value="ALL">Todos</option>
              <option value="DESPACHADA">Despachada</option>
              <option value="ANULADA">Anulada</option>
              <option value="CANCELADA">Cancelada</option>
            </select>
          </label>

          <label className="text-sm">
            Pago
            <select className="input mt-1" value={filtroPago} onChange={(e) => setFiltroPago(e.target.value as FiltroPago)}>
              <option value="ALL">Todos</option>
              <option value="EFECTIVO">Efectivo</option>
              <option value="TRANSFERENCIA">Transferencia</option>
              <option value="MIXTO">Mixto</option>
            </select>
          </label>
        </div>
      </section>

      <section className="card p-3 md:hidden">
        <div className="grid gap-3">
          {ventas.map((venta) => (
            <div key={venta.id} className="rounded-xl border border-pos-border p-3">
              <div className="flex items-start justify-between gap-2">
                <div>
                  <p className="font-semibold">Venta {numeracionTurno.get(venta.id) || "-"} {venta.clienteNombre || "-"}</p>
                  <p className="text-xs text-pos-muted">{fechaHora(venta.fecha)}</p>
                </div>
                <span className={`rounded-full px-2 py-1 text-xs font-semibold ${estadoClass(venta.estado)}`}>{venta.estado}</span>
              </div>
              <div className="mt-2 space-y-1 text-sm">
                <p><span className="text-pos-muted">Tipo:</span> {tipoVentaLabel(venta)}</p>
                <p><span className="text-pos-muted">Pago:</span> {pagoLabel(venta)}</p>
                <p><span className="text-pos-muted">Total:</span> {money.format(venta.total)}</p>
              </div>
              <div className="mt-3 flex gap-2">
                <button className="rounded-lg border border-pos-border px-2 py-1 text-xs font-medium text-pos-text hover:bg-gray-50" onClick={() => setSelectedVentaId(venta.id)}>
                  Ver detalle
                </button>
                {venta.estado === "DESPACHADA" && (
                  <button
                    className="rounded-lg border border-red-200 bg-red-50 px-2 py-1 text-xs font-medium text-red-700 hover:bg-red-100"
                    onClick={() => {
                      setConfirmAnularId(venta.id);
                      setMotivoAnulacion("");
                    }}
                  >
                    Anular
                  </button>
                )}
              </div>
            </div>
          ))}
          {!ventas.length && <p className="p-3 text-sm text-pos-muted">No hay ventas que coincidan con los filtros actuales.</p>}
        </div>
      </section>

      <section className="card hidden overflow-x-auto md:block">
        <table className="w-full min-w-[860px] table-fixed text-sm">
          <thead>
            <tr className="border-b border-pos-border">
              <th className="w-16 p-3 text-left">N</th>
              <th className="w-36 p-3 text-left">Fecha</th>
              <th className="w-32 p-3 text-left">Cliente</th>
              <th className="w-28 p-3 text-left">Tipo</th>
              <th className="w-28 p-3 text-left">Estado</th>
              <th className="w-24 p-3 text-left">Pago</th>
              <th className="w-24 p-3 text-left">Total</th>
              <th className="w-28 p-3 text-left">Acciones</th>
            </tr>
          </thead>
          <tbody>
            {ventas.map((venta) => (
              <tr key={venta.id} className="border-b border-pos-border/70">
                <td className="p-3 font-semibold">{numeracionTurno.get(venta.id) || "-"}</td>
                <td className="p-3">{fechaHora(venta.fecha)}</td>
                <td className="truncate p-3" title={venta.clienteNombre || "-"}>{venta.clienteNombre || "-"}</td>
                <td className="p-3">{tipoVentaLabel(venta)}</td>
                <td className="p-3">
                  <span className={`rounded-full px-2 py-1 text-xs font-semibold ${estadoClass(venta.estado)}`}>{venta.estado}</span>
                </td>
                <td className="p-3">{pagoLabel(venta)}</td>
                <td className="p-3 font-semibold">{money.format(venta.total)}</td>
                <td className="p-3">
                  <div className="flex flex-wrap gap-1">
                    <button
                      className="inline-flex items-center gap-1 rounded-lg border border-pos-border px-2 py-1 text-xs font-medium text-pos-text hover:bg-gray-50"
                      onClick={() => setSelectedVentaId(venta.id)}
                    >
                      <BsEye size={14} />
                      Detalle
                    </button>
                    {venta.estado === "DESPACHADA" && (
                      <button
                        className="rounded-lg border border-red-200 bg-red-50 px-2 py-1 text-xs font-medium text-red-700 hover:bg-red-100"
                        onClick={() => {
                          setConfirmAnularId(venta.id);
                          setMotivoAnulacion("");
                        }}
                      >
                        Anular
                      </button>
                    )}
                  </div>
                </td>
              </tr>
            ))}
            {!ventas.length && (
              <tr>
                <td className="p-4 text-sm text-pos-muted" colSpan={8}>
                  No hay ventas que coincidan con los filtros actuales.
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </section>

      {selectedVentaId && (
        <div className="fixed inset-0 z-40 flex items-center justify-center bg-black/40 p-4">
          <div className="card max-h-[92vh] w-full max-w-3xl overflow-y-auto p-5">
            <div className="mb-4 flex items-start justify-between gap-3">
              <div>
                <h2 className="text-xl font-semibold">Detalle de la venta {selectedVenta ? numeracionTurno.get(selectedVenta.id) || "-" : "-"}</h2>
                {selectedVenta && <p className="text-sm text-pos-muted">{fechaHora(selectedVenta.fecha)}</p>}
              </div>
              <button className="btn-ghost p-1" onClick={() => setSelectedVentaId(null)}>
                <BsXLg size={14} />
              </button>
            </div>

            {detalleQ.isLoading && <p className="text-sm text-pos-muted">Cargando detalle...</p>}
            {detalleQ.isError && <p className="text-sm text-red-600">{getErrorMessage(detalleQ.error)}</p>}
            {detalleQ.data && <DetalleVentaPanel detalle={detalleQ.data} />}
          </div>
        </div>
      )}

      {confirmAnularId !== null && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40 p-4">
          <div className="card w-full max-w-md p-5">
            <h3 className="text-lg font-semibold">Anular venta {confirmAnularId !== null ? numeracionTurno.get(confirmAnularId) || "-" : "-"}</h3>
            <p className="mt-2 text-sm text-pos-muted">La venta quedará anulada, se ajustará el turno y se devolverá inventario.</p>
            <label className="mt-4 block text-sm">
              Motivo de anulacion
              <textarea
                className="input mt-1 min-h-[110px]"
                value={motivoAnulacion}
                onChange={(e) => setMotivoAnulacion(e.target.value.slice(0, 255))}
                placeholder="Escribe por que se anula la venta"
                maxLength={255}
              />
            </label>
            <div className="mt-4 flex gap-2">
              <button
                className="btn-ghost flex-1"
                onClick={() => {
                  setConfirmAnularId(null);
                  setMotivoAnulacion("");
                }}
              >
                Cancelar
              </button>
              <button
                className="btn-primary flex-1"
                disabled={!motivoAnulacion.trim() || anularM.isPending}
                onClick={() => anularM.mutate({ id: confirmAnularId, motivo: motivoAnulacion.trim() })}
              >
                Confirmar anulacion
              </button>
            </div>
            {anularM.isError && <p className="mt-3 text-sm text-red-600">{getErrorMessage(anularM.error)}</p>}
          </div>
        </div>
      )}
    </div>
  );
}

function DetalleVentaPanel({ detalle }: { detalle: VentaDetalle }) {
  return (
    <div className="grid gap-4">
      <div className="grid gap-3 md:grid-cols-2">
        <div className="rounded-xl border border-pos-border p-4 text-sm">
          <h3 className="mb-3 font-semibold">Resumen</h3>
          <div className="space-y-2">
            <p><span className="text-pos-muted">Tipo:</span> {tipoVentaLabel(detalle)}</p>
            <p><span className="text-pos-muted">Estado:</span> {detalle.estado}</p>
            {hasText(detalle.clienteNombre) && <p><span className="text-pos-muted">Cliente:</span> {detalle.clienteNombre}</p>}
            {hasText(detalle.telefono) && <p><span className="text-pos-muted">Telefono:</span> {detalle.telefono}</p>}
            {hasText(detalle.direccion) && <p><span className="text-pos-muted">Direccion:</span> {detalle.direccion}</p>}
            <p><span className="text-pos-muted">Fecha de venta:</span> {fechaHora(detalle.fecha)}</p>
          </div>
        </div>

        <div className="rounded-xl border border-pos-border p-4 text-sm">
          <h3 className="mb-3 font-semibold">Pago</h3>
          <div className="space-y-2">
            <p><span className="text-pos-muted">Forma de pago:</span> {pagoLabel(detalle)}</p>
            {hasPositiveNumber(detalle.pagoEfectivo) && <p><span className="text-pos-muted">Pago efectivo:</span> {money.format(detalle.pagoEfectivo)}</p>}
            {hasPositiveNumber(detalle.pagoTransferencia) && <p><span className="text-pos-muted">Pago transferencia:</span> {money.format(detalle.pagoTransferencia)}</p>}
            {hasPositiveNumber(detalle.valorDomicilio) && <p><span className="text-pos-muted">Valor domicilio:</span> {money.format(detalle.valorDomicilio ?? 0)}</p>}
            {hasPositiveNumber(detalle.descuentoPorcentaje) && <p><span className="text-pos-muted">Descuento %:</span> {detalle.descuentoPorcentaje}%</p>}
            {hasPositiveNumber(detalle.descuentoValor) && <p><span className="text-pos-muted">Descuento valor:</span> {money.format(detalle.descuentoValor ?? 0)}</p>}
            <p className="font-semibold"><span className="text-pos-muted font-normal">Total:</span> {money.format(detalle.total)}</p>
          </div>
        </div>
      </div>

      <div className="rounded-xl border border-pos-border p-4">
        <h3 className="mb-3 font-semibold">Productos</h3>
        <div className="grid gap-3">
          {detalle.detalles.map((item, index) => (
            <div key={`${item.productoId || index}-${index}`} className="rounded-xl border border-pos-border/70 bg-gray-50 p-3 text-sm">
              <div className="flex flex-col gap-1 md:flex-row md:items-center md:justify-between">
                <p className="font-semibold">{item.productoNombre}</p>
                <p className="font-semibold">{money.format(item.subtotal)}</p>
              </div>
              <p className="text-pos-muted">
                {item.cantidad} x {money.format(item.precioUnitario)}
              </p>
              {hasText(item.observacion) && <p className="mt-1"><span className="text-pos-muted">Observacion:</span> {item.observacion}</p>}
            </div>
          ))}
          {!detalle.detalles.length && <p className="text-sm text-pos-muted">Esta venta no tiene items registrados.</p>}
        </div>
      </div>

      {detalle.estado === "ANULADA" && (
        <div className="rounded-xl border border-red-200 bg-red-50 p-4 text-sm text-red-800">
          <h3 className="mb-3 font-semibold">Datos de anulacion</h3>
          <div className="space-y-2">
            {detalle.fechaAnulacion && <p><span className="text-red-700/80">Hora de anulacion:</span> {fechaHora(detalle.fechaAnulacion)}</p>}
            {hasText(detalle.motivoAnulacion) && <p><span className="text-red-700/80">Motivo:</span> {detalle.motivoAnulacion}</p>}
          </div>
        </div>
      )}
    </div>
  );
}
