import { useMemo, useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { posApi } from "../shared/api/posApi";
import { getErrorMessage } from "../shared/utils";

type KitchenState = "pendiente" | "preparando" | "listo";

export function CocinaPage() {
  const [stateMap, setStateMap] = useState<Record<number, KitchenState>>({});
  const ventasQ = useQuery({
    queryKey: ["kitchen-ventas"],
    queryFn: () => posApi.getVentas({ estado: "EN_PROCESO", page: 0, size: 60 })
  });

  const cards = useMemo(() => ventasQ.data?.content ?? [], [ventasQ.data]);

  const badgeStyle = (canal: string | null | undefined) => {
    if (canal === "MESERO") return "bg-purple-100 text-purple-700";
    return "bg-blue-100 text-blue-700";
  };

  if (ventasQ.isError) return <p className="text-sm text-red-600">{getErrorMessage(ventasQ.error)}</p>;

  return (
    <div className="grid gap-4">
      <h2 className="text-2xl font-semibold">Pedidos Cocina</h2>
      <div className="grid gap-3 md:grid-cols-2 xl:grid-cols-3">
        {cards.map((v) => {
          const st = stateMap[v.id] ?? "pendiente";
          const origen = v.canalVenta ?? "DOMICILIO";
          return (
            <article key={v.id} className="card p-4">
              <div className="mb-2 flex items-center justify-between">
                <h3 className="text-lg font-semibold">Pedido #{v.id}</h3>
                <div className="flex items-center gap-2">
                  <span className={`rounded-full px-2 py-0.5 text-xs font-semibold ${badgeStyle(origen)}`}>
                    {origen}
                  </span>
                  <span className="rounded-full bg-pos-accentSoft px-2 py-0.5 text-xs capitalize">{st}</span>
                </div>
              </div>
              <p className="text-sm text-pos-muted">Cliente: {v.clienteNombre || "N/A"}</p>
              {v.direccion && <p className="text-sm text-pos-muted">Dirección: {v.direccion}</p>}
              {origen === "MESERO" && v.usuario && (
                <p className="text-sm font-semibold text-purple-700">Mesero: {v.usuario}</p>
              )}
              <p className="mt-3 text-sm">Observaciones por producto: disponibles en detalle de venta.</p>
              <div className="mt-4 flex gap-2">
                <button className="btn-ghost" onClick={() => setStateMap((prev) => ({ ...prev, [v.id]: "preparando" }))}>
                  Preparando
                </button>
                <button className="btn-primary" onClick={() => setStateMap((prev) => ({ ...prev, [v.id]: "listo" }))}>
                  Marcar listo
                </button>
              </div>
            </article>
          );
        })}
      </div>
    </div>
  );
}
