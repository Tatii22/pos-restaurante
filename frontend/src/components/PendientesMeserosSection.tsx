import { useMemo, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { BsCheckCircle } from "react-icons/bs";
import { posApi } from "../shared/api/posApi";
import { getErrorMessages, money } from "../shared/utils";

export function PendientesMeserosSection() {
  const qc = useQueryClient();
  const pendientesQ = useQuery({
    queryKey: ["pendientes-meseros"],
    queryFn: () => posApi.getPendientesMeseros()
  });

  const confirmarM = useMutation({
    mutationFn: (ids: number[]) => posApi.confirmarEntregaCaja(ids),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["pendientes-meseros"] });
      qc.invalidateQueries({ queryKey: ["turno-activo-layout"] });
      qc.invalidateQueries({ queryKey: ["reporte-turno-activo"] });
    }
  });

  const [selectedIds, setSelectedIds] = useState<Set<number>>(new Set());

  function toggle(id: number) {
    setSelectedIds((prev) => {
      const next = new Set(prev);
      if (next.has(id)) next.delete(id);
      else next.add(id);
      return next;
    });
  }

  function selectAll() {
    if (!pendientesQ.data) return;
    if (selectedIds.size === pendientesQ.data.length) {
      setSelectedIds(new Set());
    } else {
      setSelectedIds(new Set(pendientesQ.data.map((v) => v.id)));
    }
  }

  const pendientes = pendientesQ.data ?? [];
  const anySelected = selectedIds.size > 0;

  const resumenPorMesero = useMemo(() => {
    const map = new Map<string, { count: number; total: number }>();
    for (const v of pendientes) {
      const user = v.usuario ?? "Desconocido";
      const entry = map.get(user) ?? { count: 0, total: 0 };
      entry.count++;
      entry.total += v.total;
      map.set(user, entry);
    }
    return Array.from(map.entries()).sort((a, b) => b[1].total - a[1].total);
  }, [pendientes]);

  const totalGeneralPendiente = pendientes.reduce((sum, v) => sum + v.total, 0);

  return (
    <div className="card rounded-2xl p-5 shadow-sm border border-pos-border/60">
      <div className="flex items-center justify-between mb-3">
        <div>
          <h3 className="text-xl font-semibold">Pendientes Meseros</h3>
          <p className="text-xs text-pos-muted mt-0.5">
            Efectivo recibido por meseros que aun no entregan a caja
          </p>
        </div>
        <div className="flex items-center gap-2">
          {pendientes.length > 0 && (
            <button className="btn-ghost text-xs" onClick={selectAll}>
              {selectedIds.size === pendientes.length ? "Deseleccionar todo" : "Seleccionar todo"}
            </button>
          )}
          <button
            className="btn-primary text-sm"
            disabled={!anySelected || confirmarM.isPending}
            onClick={() => confirmarM.mutate(Array.from(selectedIds))}
          >
            <BsCheckCircle size={14} className="mr-1 inline" />
            {confirmarM.isPending ? "Confirmando..." : `Confirmar entrega (${selectedIds.size})`}
          </button>
        </div>
      </div>

      {pendientesQ.isLoading && (
        <p className="text-sm text-pos-muted">Cargando...</p>
      )}

      {pendientesQ.isSuccess && pendientes.length === 0 && (
        <p className="text-sm text-pos-muted">No hay ventas de meseros pendientes de entrega.</p>
      )}

      {pendientes.length > 0 && (
        <>
          <div className="mb-4 grid gap-2 sm:grid-cols-2 lg:grid-cols-3">
            {resumenPorMesero.map(([usuario, info]) => (
              <div key={usuario} className="rounded-xl border border-pos-border bg-gray-50 p-3">
                <p className="font-semibold">{usuario}</p>
                <p className="text-xs text-pos-muted">{info.count} venta(s)</p>
                <p className="text-lg font-bold text-orange-700">{money.format(info.total)}</p>
              </div>
            ))}
            <div className="rounded-xl border border-orange-300 bg-orange-50 p-3">
              <p className="font-semibold">Total General</p>
              <p className="text-xs text-pos-muted">{pendientes.length} venta(s)</p>
              <p className="text-lg font-bold text-orange-700">{money.format(totalGeneralPendiente)}</p>
            </div>
          </div>

          <div className="overflow-auto">
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b border-pos-border text-left text-xs uppercase tracking-wide text-pos-muted">
                  <th className="pb-2 pr-2 w-8"></th>
                  <th className="pb-2 pr-2">#</th>
                  <th className="pb-2 pr-2">Mesero</th>
                  <th className="pb-2 pr-2">Total</th>
                  <th className="pb-2 pr-2">Metodo</th>
                  <th className="pb-2 pr-2">Hora</th>
                </tr>
              </thead>
              <tbody>
                {pendientes.map((v) => (
                  <tr key={v.id} className="border-b border-pos-border/50 hover:bg-gray-50">
                    <td className="py-2 pr-2">
                      <input
                        type="checkbox"
                        className="h-4 w-4"
                        checked={selectedIds.has(v.id)}
                        onChange={() => toggle(v.id)}
                      />
                    </td>
                    <td className="py-2 pr-2 font-medium">{v.id}</td>
                    <td className="py-2 pr-2">{v.usuario ?? "-"}</td>
                    <td className="py-2 pr-2 font-semibold">{money.format(v.total)}</td>
                    <td className="py-2 pr-2">{v.formaPago}</td>
                    <td className="py-2 pr-2 text-pos-muted">
                      {v.fecha ? new Date(v.fecha).toLocaleTimeString("es-CO", { hour: "2-digit", minute: "2-digit" }) : "-"}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </>
      )}

      {confirmarM.isError && (
        <p className="mt-2 text-sm text-red-600">{getErrorMessages(confirmarM.error).join(", ")}</p>
      )}
    </div>
  );
}
