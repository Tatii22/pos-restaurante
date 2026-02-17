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

ChartJS.register(CategoryScale, LinearScale, PointElement, LineElement, Tooltip, Legend);

function dateYmd(deltaDays: number): string {
  const d = new Date();
  d.setDate(d.getDate() - deltaDays);
  return d.toISOString().slice(0, 10);
}

function startOfMonthYmd(): string {
  const d = new Date();
  d.setDate(1);
  return d.toISOString().slice(0, 10);
}

export function DashboardPage() {
  const today = dateYmd(0);
  const monthStart = startOfMonthYmd();
  const reportQ = useQuery({
    queryKey: ["dashboard-report-month", monthStart, today],
    queryFn: () => posApi.getReporteVentas(monthStart, today)
  });

  const chartQ = useQuery({
    queryKey: ["dashboard-7days"],
    queryFn: async () => {
      const days = Array.from({ length: 7 }, (_, i) => dateYmd(6 - i));
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

  const resumen = reportQ.data;

  if (reportQ.isError || chartQ.isError) {
    return <p className="text-sm text-red-600">{getErrorMessage(reportQ.error || chartQ.error)}</p>;
  }

  return (
    <div className="grid gap-4">
      <h2 className="text-2xl font-semibold">Panel administrativo</h2>

      <div className="grid gap-3 md:grid-cols-2 xl:grid-cols-4">
        <div className="card p-4">
          <p className="text-sm text-pos-muted">Ventas del mes</p>
          <p className="text-2xl font-bold">{resumen?.totalVentas ?? 0}</p>
        </div>
        <div className="card p-4">
          <p className="text-sm text-pos-muted">Total neto del mes</p>
          <p className="text-2xl font-bold">{money.format(resumen?.totalNeto ?? 0)}</p>
        </div>
        <div className="card p-4">
          <p className="text-sm text-pos-muted">Efectivo</p>
          <p className="text-2xl font-bold">{money.format(resumen?.totalEfectivo ?? 0)}</p>
        </div>
        <div className="card p-4">
          <p className="text-sm text-pos-muted">Transferencia</p>
          <p className="text-2xl font-bold">{money.format(resumen?.totalTransferencia ?? 0)}</p>
        </div>
      </div>

      <div className="grid gap-4 xl:grid-cols-[2fr_1fr]">
        <div className="card p-4">
          <h3 className="mb-3 text-lg font-semibold">Tendencia de ventas (ultimos 7 dias)</h3>
          {lineData ? <Line data={lineData} /> : <p className="text-sm text-pos-muted">Cargando grafico...</p>}
        </div>

        <div className="card p-4">
          <h3 className="mb-3 text-lg font-semibold">Resumen del mes</h3>
          <div className="space-y-2 text-sm">
            <p className="flex justify-between"><span className="text-pos-muted">Bruto</span><span className="font-semibold">{money.format(resumen?.totalBruto ?? 0)}</span></p>
            <p className="flex justify-between"><span className="text-pos-muted">Descuentos</span><span className="font-semibold">{money.format(resumen?.totalDescuentos ?? 0)}</span></p>
            <p className="flex justify-between"><span className="text-pos-muted">Neto</span><span className="font-semibold">{money.format(resumen?.totalNeto ?? 0)}</span></p>
          </div>
        </div>
      </div>
    </div>
  );
}
