import { useMemo, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { posApi } from "../shared/api/posApi";
import { useTurnoStore } from "../shared/store/turnoStore";
import { getErrorMessage, money } from "../shared/utils";

function estadoClass(estado: string): string {
  if (estado === "EN_PROCESO") return "bg-yellow-100 text-yellow-800";
  if (estado === "DESPACHADA") return "bg-green-100 text-green-800";
  if (estado === "ANULADA") return "bg-red-100 text-red-800";
  return "bg-gray-100 text-gray-700";
}

export function HistorialVentasPage() {
  const qc = useQueryClient();
  const { turno } = useTurnoStore();
  const [confirmAnularId, setConfirmAnularId] = useState<number | null>(null);
  const [search, setSearch] = useState("");

  const listQ = useQuery({
    queryKey: ["historial-turno-ventas", turno?.id],
    enabled: !!turno?.id,
    queryFn: () =>
      posApi.getVentas({
        turnoId: turno?.id,
        page: 0,
        size: 200
      })
  });

  const anularM = useMutation({
    mutationFn: (id: number) => posApi.anular(id),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["historial-turno-ventas"] });
      qc.invalidateQueries({ queryKey: ["turno-activo-layout"] });
      qc.invalidateQueries({ queryKey: ["inventario-ventas"] });
      qc.invalidateQueries({ queryKey: ["catalogo-hoy"] });
    }
  });

  const pedidoNumeroByVentaId = useMemo(() => {
    const all = [...(listQ.data?.content || [])].sort((a, b) => {
      const fa = new Date(a.fecha).getTime();
      const fb = new Date(b.fecha).getTime();
      if (fa !== fb) return fa - fb;
      return a.id - b.id;
    });
    return new Map(all.map((v, index) => [v.id, index + 1]));
  }, [listQ.data]);

  const ventas = useMemo(() => {
    const term = search.trim().toLowerCase();
    const all = (listQ.data?.content || []).filter((v) => v.estado !== "EN_PROCESO");
    if (!term) return all;
    return all.filter((v) => {
      const cliente = (v.clienteNombre || "").toLowerCase();
      const totalNumber = Number(v.total || 0);
      const totalRaw = String(totalNumber);
      const totalNoDecimals = String(Math.trunc(totalNumber));
      const totalPretty = totalNumber.toLocaleString("es-CO");
      return (
        cliente.includes(term) ||
        totalRaw.includes(term) ||
        totalNoDecimals.includes(term) ||
        totalPretty.toLowerCase().includes(term)
      );
    });
  }, [listQ.data, search]);

  if (!turno) {
    return (
      <div className="card p-4">
        <p className="text-sm text-pos-muted">Debes abrir turno para ver el historial.</p>
      </div>
    );
  }

  if (listQ.isError) {
    return <p className="text-sm text-red-600">{getErrorMessage(listQ.error)}</p>;
  }

  return (
    <div className="grid gap-4">
      <section className="card p-4">
        <h2 className="text-xl font-semibold">Historial de Ventas del Turno #{turno.id}</h2>
        <p className="mt-1 text-sm text-pos-muted">
          Aqui puedes revisar todas las ventas del turno actual y anular las despachadas.
        </p>
        <div className="mt-3">
          <input
            className="input max-w-md"
            placeholder="Buscar por cliente o monto..."
            value={search}
            onChange={(e) => setSearch(e.target.value)}
          />
        </div>
      </section>

      <section className="card md:hidden p-3">
        <div className="grid gap-2">
          {ventas.map((v) => (
            <div key={v.id} className="rounded-xl border border-pos-border p-3">
              <div className="flex items-start justify-between gap-2">
                <p className="font-semibold">Pedido #{pedidoNumeroByVentaId.get(v.id) ?? "-"}</p>
                <span className={`rounded-full px-2 py-1 text-xs font-semibold ${estadoClass(v.estado)}`}>{v.estado}</span>
              </div>
              <p className="text-xs text-pos-muted">{new Date(v.fecha).toLocaleString()}</p>
              <p className="text-sm">Tipo: {v.tipoVenta}</p>
              <p className="text-sm">Cliente: {v.clienteNombre || "-"}</p>
              <p className="text-sm">Pago: {v.formaPago}</p>
              <p className="text-sm font-semibold">{money.format(v.total)}</p>
              <div className="mt-2">
                {v.estado === "DESPACHADA" ? (
                  <button className="btn-ghost bg-red-100 text-red-700 hover:bg-red-200" onClick={() => setConfirmAnularId(v.id)}>
                    Anular
                  </button>
                ) : (
                  <span className="text-xs text-pos-muted">No disponible</span>
                )}
              </div>
            </div>
          ))}
          {!ventas.length && <p className="text-sm text-pos-muted">No hay ventas registradas en este turno.</p>}
        </div>
      </section>

      <section className="card hidden overflow-x-auto md:block">
        <table className="w-full min-w-[820px] text-sm">
          <thead>
            <tr className="border-b border-pos-border">
              <th className="p-3 text-left">Pedido</th>
              <th className="p-3 text-left">Fecha</th>
              <th className="p-3 text-left">Tipo</th>
              <th className="p-3 text-left">Cliente</th>
              <th className="p-3 text-left">Estado</th>
              <th className="p-3 text-left">Pago</th>
              <th className="p-3 text-left">Total</th>
              <th className="p-3 text-left">Accion</th>
            </tr>
          </thead>
          <tbody>
            {ventas.map((v) => (
              <tr key={v.id} className="border-b border-pos-border/70">
                <td className="p-3">#{pedidoNumeroByVentaId.get(v.id) ?? "-"}</td>
                <td className="p-3">{new Date(v.fecha).toLocaleString()}</td>
                <td className="p-3">{v.tipoVenta}</td>
                <td className="p-3">{v.clienteNombre || "-"}</td>
                <td className="p-3 whitespace-nowrap">
                  <span className={`rounded-full px-2 py-1 text-xs font-semibold ${estadoClass(v.estado)}`}>{v.estado}</span>
                </td>
                <td className="p-3">{v.formaPago}</td>
                <td className="p-3">{money.format(v.total)}</td>
                <td className="p-3">
                  {v.estado === "DESPACHADA" ? (
                    <button className="btn-ghost bg-red-100 text-red-700 hover:bg-red-200" onClick={() => setConfirmAnularId(v.id)}>
                      Anular
                    </button>
                  ) : (
                    <span className="text-xs text-pos-muted">No disponible</span>
                  )}
                </td>
              </tr>
            ))}
            {!ventas.length && (
              <tr>
                <td className="p-4 text-center text-pos-muted" colSpan={8}>
                  No hay ventas registradas en este turno.
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </section>

      {confirmAnularId && (
        <div className="fixed inset-0 z-40 flex items-center justify-center bg-black/40 p-4">
          <div className="card w-full max-w-md p-5">
            <h3 className="text-lg font-semibold">Confirmar anulacion</h3>
            <p className="mt-2 text-sm text-pos-muted">
              Esta accion devolvera inventario y dinero del turno. Deseas continuar?
            </p>
            <div className="mt-4 flex gap-2">
              <button className="btn-ghost flex-1" onClick={() => setConfirmAnularId(null)}>
                No
              </button>
              <button
                className="btn-primary flex-1"
                onClick={() => {
                  anularM.mutate(confirmAnularId);
                  setConfirmAnularId(null);
                }}
                disabled={anularM.isPending}
              >
                Si, anular
              </button>
            </div>
          </div>
        </div>
      )}

      {anularM.isError && <p className="text-sm text-red-600">{getErrorMessage(anularM.error)}</p>}
    </div>
  );
}
