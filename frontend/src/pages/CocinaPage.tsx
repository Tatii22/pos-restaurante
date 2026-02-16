import { useMemo, useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { posApi } from "../shared/api/posApi";
import { getErrorMessage } from "../shared/utils";

type KitchenState = "pendiente" | "preparando" | "listo";

export function CocinaPage() {
  const [stateMap, setStateMap] = useState<Record<number, KitchenState>>({});
  const ventasQ = useQuery({
    queryKey: ["kitchen-ventas"],
    queryFn: () => posApi.getVentas({ tipoVenta: "DOMICILIO", estado: "EN_PROCESO", page: 0, size: 60 })
  });

  const cards = useMemo(() => ventasQ.data?.content ?? [], [ventasQ.data]);

  if (ventasQ.isError) return <p className="text-sm text-red-600">{getErrorMessage(ventasQ.error)}</p>;

  return (
    <div className="grid gap-4">
      <h2 className="text-2xl font-semibold">Pedidos Cocina</h2>
      <div className="grid gap-3 md:grid-cols-2 xl:grid-cols-3">
        {cards.map((v) => {
          const st = stateMap[v.id] ?? "pendiente";
          return (
            <article key={v.id} className="card p-4">
              <div className="mb-2 flex items-center justify-between">
                <h3 className="text-lg font-semibold">Pedido #{v.id}</h3>
                <span className="rounded-full bg-pos-accentSoft px-2 py-1 text-xs capitalize">{st}</span>
              </div>
              <p className="text-sm text-pos-muted">Cliente: {v.clienteNombre || "N/A"}</p>
              <p className="text-sm text-pos-muted">Dirección: {v.direccion || "N/A"}</p>
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
