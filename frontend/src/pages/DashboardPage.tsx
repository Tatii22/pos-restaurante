import { useMemo } from "react";
import { useQuery } from "@tanstack/react-query";
import { Line } from "react-chartjs-2";
import {
  CategoryScale,
  Chart as ChartJS,
  Legend,
  LineElement,
  LinearScale,
  PointElement,
  Tooltip
} from "chart.js";
import { posApi } from "../shared/api/posApi";
import { getErrorMessage, money } from "../shared/utils";
import { useAuthStore } from "../shared/store/authStore";

ChartJS.register(CategoryScale, LinearScale, PointElement, LineElement, Tooltip, Legend);

function formatDate(delta: number): string {
  const date = new Date();
  date.setDate(date.getDate() - delta);
  return date.toISOString().slice(0, 10);
}

export function DashboardPage() {
  const { role } = useAuthStore();
  const today = formatDate(0);
  const reportQ = useQuery({
    queryKey: ["dashboard-report", today],
    queryFn: () => posApi.getReporteVentas(today, today)
  });

  const chartQ = useQuery({
    queryKey: ["dashboard-7days"],
    queryFn: async () => {
      const days = Array.from({ length: 7 }, (_, i) => formatDate(6 - i));
      const values = await Promise.all(days.map((d) => posApi.getReporteVentas(d, d)));
      return { days, values };
    }
  });

  const lineData = useMemo(() => {
    if (!chartQ.data) return null;
    return {
      labels: chartQ.data.days,
      datasets: [
        {
          label: "Ventas netas",
          data: chartQ.data.values.map((v) => Number(v.totalNeto || 0)),
          borderColor: "#16a34a",
          backgroundColor: "rgba(22,163,74,0.2)"
        }
      ]
    };
  }, [chartQ.data]);

  if (reportQ.isError || chartQ.isError) {
    return <p className="text-sm text-red-600">{getErrorMessage(reportQ.error || chartQ.error)}</p>;
  }

  return (
    <div className="grid gap-4">
      <h2 className="text-2xl font-semibold">Dashboard</h2>
      <div className="grid gap-3 md:grid-cols-4">
        <div className="card p-4">
          <p className="text-sm text-pos-muted">Ventas del día</p>
          <p className="text-2xl font-bold">{reportQ.data?.totalVentas ?? 0}</p>
        </div>
        <div className="card p-4">
          <p className="text-sm text-pos-muted">Caja actual</p>
          <p className="text-2xl font-bold">{money.format(reportQ.data?.totalNeto ?? 0)}</p>
        </div>
        <div className="card p-4">
          <p className="text-sm text-pos-muted">Efectivo</p>
          <p className="text-2xl font-bold">{money.format(reportQ.data?.totalEfectivo ?? 0)}</p>
        </div>
        <div className="card p-4">
          <p className="text-sm text-pos-muted">Pedidos pendientes</p>
          <p className="text-2xl font-bold">{role === "DOMI" ? "-" : "Operativo"}</p>
        </div>
      </div>

      <div className="grid gap-4 xl:grid-cols-[2fr_1fr]">
        <div className="card p-4">
          <h3 className="mb-3 text-lg font-semibold">Ventas últimos 7 días</h3>
          {lineData ? <Line data={lineData} /> : <p>Cargando gráfico...</p>}
        </div>

        <div className="card p-4">
          <h3 className="mb-3 text-lg font-semibold">Menu del Día Panel</h3>
          <MenuPanel />
        </div>
      </div>
    </div>
  );
}

function MenuPanel() {
  const catalogQ = useQuery({
    queryKey: ["catalogo-dashboard"],
    queryFn: () => posApi.catalogoHoy()
  });

  if (catalogQ.isError) return <p className="text-sm text-red-600">{getErrorMessage(catalogQ.error)}</p>;
  if (!catalogQ.data) return <p>Cargando...</p>;

  return (
    <div className="grid gap-2">
      {(catalogQ.data.menuDiario || []).slice(0, 8).map((p) => (
        <div key={p.id} className="rounded-xl border border-pos-border bg-white p-2 text-sm">
          <div className="font-medium">{p.nombre}</div>
          <div className="text-pos-muted">{money.format(p.precio)}</div>
          <div className={p.agotado ? "text-xs text-red-600" : "text-xs text-green-700"}>
            {p.agotado ? "No disponible" : "Disponible"}
          </div>
        </div>
      ))}
    </div>
  );
}
