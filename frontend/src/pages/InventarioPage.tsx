import { useQuery } from "@tanstack/react-query";
import { posApi } from "../shared/api/posApi";
import { getErrorMessage } from "../shared/utils";

export function InventarioPage() {
  const menuQ = useQuery({ queryKey: ["inventario-menu"], queryFn: () => posApi.catalogoHoy() });
  if (menuQ.isError) return <p className="text-sm text-red-600">{getErrorMessage(menuQ.error)}</p>;

  return (
    <div className="grid gap-4">
      <h2 className="text-2xl font-semibold">Inventario</h2>
      <div className="card p-4">
        <p className="mb-3 text-sm text-pos-muted">Vista rápida de disponibilidad (menu del día)</p>
        <div className="grid gap-2 md:grid-cols-2 xl:grid-cols-3">
          {(menuQ.data?.menuDiario || []).map((p) => (
            <div key={p.id} className="rounded-xl border border-pos-border p-3">
              <p className="font-medium">{p.nombre}</p>
              <p className={p.agotado ? "text-xs text-red-600" : "text-xs text-green-700"}>
                {p.agotado ? "Agotado" : "Disponible"}
              </p>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}
