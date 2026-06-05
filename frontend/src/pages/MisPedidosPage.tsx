import { useQuery } from "@tanstack/react-query";
import { posApi } from "../shared/api/posApi";
import { getErrorMessage, money } from "../shared/utils";
import { useAuthStore } from "../shared/store/authStore";
import type { Venta } from "../types";

function estadoEntregaCajaLabel(venta: Venta): { text: string; className: string } {
  if (venta.estadoEntregaCaja === "PENDIENTE") {
    return { text: "PENDIENTE", className: "bg-yellow-100 text-yellow-800" };
  }
  if (venta.estadoEntregaCaja === "ENTREGADO") {
    return { text: "ENTREGADO", className: "bg-green-100 text-green-800" };
  }
  return { text: "SIN ESTADO", className: "bg-gray-100 text-gray-700" };
}

function estadoVentaLabel(estado: Venta["estado"]): { text: string; className: string } {
  switch (estado) {
    case "EN_PROCESO":
      return { text: "EN PROCESO", className: "bg-blue-100 text-blue-800" };
    case "DESPACHADA":
      return { text: "DESPACHADA", className: "bg-green-100 text-green-800" };
    case "CANCELADA":
      return { text: "CANCELADA", className: "bg-gray-100 text-gray-700" };
    case "ANULADA":
      return { text: "ANULADA", className: "bg-red-100 text-red-800" };
    default:
      return { text: estado || "SIN ESTADO", className: "bg-gray-100 text-gray-700" };
  }
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

export function MisPedidosPage() {
  const usernameActual = useAuthStore((s) => s.username);

  const pendientesQ = useQuery({
    queryKey: ["mis-pedidos-pendientes"],
    queryFn: () => posApi.getPendientesMeseros(),
    refetchInterval: 30000
  });

  if (pendientesQ.isLoading) {
    return (
      <div className="grid min-h-[50vh] place-items-center">
        <p className="text-sm text-pos-muted">Cargando pedidos...</p>
      </div>
    );
  }

  if (pendientesQ.isError) {
    return (
      <div className="card p-4">
        <p className="text-sm text-red-600">{getErrorMessage(pendientesQ.error)}</p>
      </div>
    );
  }

  const ventas = (pendientesQ.data || []).filter((venta) => {
    if (!usernameActual) return true;
    return venta.usuario === usernameActual;
  });

  return (
    <div className="grid gap-4">
      <section className="card p-4">
        <h1 className="text-xl font-semibold">Mis Pedidos</h1>
        <p className="mt-1 text-sm text-pos-muted">
          {ventas.length} pedido{ventas.length !== 1 ? "s" : ""} creado{ventas.length !== 1 ? "s" : ""} en el turno actual
        </p>
      </section>

      {ventas.length === 0 && (
        <section className="card p-4">
          <p className="text-sm text-pos-muted">Aun no tienes pedidos en el turno actual.</p>
        </section>
      )}

      <section className="grid gap-3 md:hidden">
        {ventas.map((venta) => {
          const etiquetaEntrega = estadoEntregaCajaLabel(venta);
          const etiquetaVenta = estadoVentaLabel(venta.estado);
          return (
            <div key={venta.id} className="card p-3">
              <div className="flex items-start justify-between gap-2">
                <div>
                  <p className="font-semibold">Venta #{venta.id}</p>
                  <p className="text-xs text-pos-muted">{fechaHora(venta.fecha)}</p>
                </div>
                <div className="flex flex-col items-end gap-1">
                  <span className={`rounded-full px-2 py-1 text-xs font-semibold ${etiquetaVenta.className}`}>{etiquetaVenta.text}</span>
                  <span className={`rounded-full px-2 py-1 text-xs font-semibold ${etiquetaEntrega.className}`}>{etiquetaEntrega.text}</span>
                </div>
              </div>
              {venta.clienteNombre && (
                <p className="mt-2 text-sm">Cliente: {venta.clienteNombre}</p>
              )}
              <div className="mt-2 space-y-1 text-sm">
                <p>Total: <span className="font-semibold">{money.format(venta.total)}</span></p>
                <p>Forma de pago: {venta.formaPago}</p>
                {venta.paraLlevar && <p>Para llevar: Si</p>}
              </div>
            </div>
          );
        })}
      </section>

      <section className="card hidden overflow-x-auto md:block">
        {ventas.length === 0 ? (
          <p className="p-4 text-sm text-pos-muted">Aun no tienes pedidos en el turno actual.</p>
        ) : (
          <table className="w-full min-w-[700px] table-fixed text-sm">
            <thead>
              <tr className="border-b border-pos-border">
                <th className="w-16 p-3 text-left">#</th>
                <th className="w-36 p-3 text-left">Fecha</th>
                <th className="w-32 p-3 text-left">Cliente</th>
                <th className="w-24 p-3 text-left">Total</th>
                <th className="w-28 p-3 text-left">Pago</th>
                <th className="w-20 p-3 text-left">LLev.</th>
                <th className="w-32 p-3 text-left">Estado</th>
                <th className="w-32 p-3 text-left">Entrega Caja</th>
              </tr>
            </thead>
            <tbody>
              {ventas.map((venta) => {
                const etiquetaEntrega = estadoEntregaCajaLabel(venta);
                const etiquetaVenta = estadoVentaLabel(venta.estado);
                return (
                  <tr key={venta.id} className="border-b border-pos-border hover:bg-gray-50">
                    <td className="p-3 font-semibold">{venta.id}</td>
                    <td className="p-3">{fechaHora(venta.fecha)}</td>
                    <td className="p-3">{venta.clienteNombre || "-"}</td>
                    <td className="p-3 font-semibold">{money.format(venta.total)}</td>
                    <td className="p-3">{venta.formaPago}</td>
                    <td className="p-3">{venta.paraLlevar ? "Si" : "No"}</td>
                    <td className="p-3">
                      <span className={`rounded-full px-2 py-1 text-xs font-semibold ${etiquetaVenta.className}`}>{etiquetaVenta.text}</span>
                    </td>
                    <td className="p-3">
                      <span className={`rounded-full px-2 py-1 text-xs font-semibold ${etiquetaEntrega.className}`}>{etiquetaEntrega.text}</span>
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        )}
      </section>
    </div>
  );
}
