import { useMemo, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
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
  const { turno, isAbierto } = useTurnoStore();
  const qc = useQueryClient();
  const [selectedId, setSelectedId] = useState<number | null>(null);
  const [nuevoDomicilio, setNuevoDomicilio] = useState<string>("0");
  const [confirm, setConfirm] = useState<{ id: number; action: "cancelar" | "anular" } | null>(null);

  const listQ = useQuery({
    queryKey: ["domicilios-list", role, turno?.id],
    enabled: !!turno?.id,
    queryFn: () => posApi.getVentas({ tipoVenta: "DOMICILIO", turnoId: turno?.id, page: 0, size: 80 })
  });

  const selected = useMemo(
    () => (listQ.data?.content || []).find((v) => v.id === selectedId) ?? null,
    [listQ.data, selectedId]
  );

  const dispatchM = useMutation({
    mutationFn: (id: number) => posApi.despachar(id),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["domicilios-list"] })
  });
  const cancelM = useMutation({
    mutationFn: (id: number) => posApi.cancelar(id),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["domicilios-list"] })
  });
  const anularM = useMutation({
    mutationFn: (id: number) => posApi.anular(id),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["domicilios-list"] })
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

  if (!turno || !isAbierto()) {
    return (
      <div className="card p-4">
        <p className="text-sm text-pos-muted">No hay turno ABIERTO. No se pueden gestionar domicilios.</p>
      </div>
    );
  }

  if (listQ.isError) return <p className="text-sm text-red-600">{getErrorMessage(listQ.error)}</p>;

  return (
    <div className="grid gap-4 xl:grid-cols-[1.4fr_1fr]">
      <section className="card overflow-auto">
        <table className="w-full min-w-[760px] text-sm">
          <thead>
            <tr className="border-b border-pos-border">
              <th className="p-3 text-left">ID</th>
              <th className="p-3 text-left">Cliente</th>
              <th className="p-3 text-left">Direccion</th>
              <th className="p-3 text-left">Telefono</th>
              <th className="p-3 text-left">Estado</th>
              <th className="p-3 text-left">Total</th>
              <th className="p-3 text-left">Acciones</th>
            </tr>
          </thead>
          <tbody>
            {(listQ.data?.content || []).map((v) => (
              <tr key={v.id} className="border-b border-pos-border/70">
                <td className="p-3">#{v.id}</td>
                <td className="p-3">{v.clienteNombre || "-"}</td>
                <td className="p-3">{v.direccion || "-"}</td>
                <td className="p-3">{v.telefono || "-"}</td>
                <td className="p-3">
                  <span className={`rounded-full px-2 py-1 text-xs font-semibold ${estadoClass(v.estado)}`}>{v.estado}</span>
                </td>
                <td className="p-3">{money.format(v.total)}</td>
                <td className="p-3">
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
            <div className="text-sm">
              <p><span className="text-pos-muted">Estado:</span> {selected.estado}</p>
              <p><span className="text-pos-muted">Cliente:</span> {selected.clienteNombre || "-"}</p>
              <p><span className="text-pos-muted">Direccion:</span> {selected.direccion || "-"}</p>
              <p><span className="text-pos-muted">Telefono:</span> {selected.telefono || "-"}</p>
              <p><span className="text-pos-muted">Total:</span> {money.format(selected.total)}</p>
            </div>

            {selected.estado === "EN_PROCESO" && role === "CAJA" && (
              <>
                <label className="text-sm">
                  Valor domicilio
                  <div className="mt-1 flex gap-2">
                    <input
                      className="input"
                      value={nuevoDomicilio}
                      onChange={(e) => setNuevoDomicilio(e.target.value)}
                    />
                    <button
                      className="btn-soft"
                      onClick={() => domicilioM.mutate({ id: selected.id, valor: Number(nuevoDomicilio || 0) })}
                    >
                      Actualizar
                    </button>
                  </div>
                </label>

                <div className="grid gap-2">
                  <button className="btn-soft" onClick={() => printCocinaM.mutate(selected.id)}>Imprimir Cocina</button>
                  <button className="btn-soft" onClick={() => printFacturaM.mutate(selected.id)}>Imprimir Factura</button>
                  <button className="btn-primary" onClick={() => dispatchM.mutate(selected.id)}>Despachar</button>
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

      {(dispatchM.isError || cancelM.isError || anularM.isError || domicilioM.isError || printCocinaM.isError || printFacturaM.isError) && (
        <p className="xl:col-span-2 text-sm text-red-600">
          {getErrorMessage(dispatchM.error || cancelM.error || anularM.error || domicilioM.error || printCocinaM.error || printFacturaM.error)}
        </p>
      )}
    </div>
  );
}
