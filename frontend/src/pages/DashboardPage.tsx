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
  const rentQ = useQuery({
    queryKey: ["dashboard-rent-month", monthStart, today],
    queryFn: () => posApi.getReporteRentabilidad(monthStart, today)
  });

  const chartQ = useQuery({
    queryKey: ["dashboard-month-trend", monthStart, today],
    queryFn: async () => {
      const start = new Date(monthStart);
      const end = new Date(today);
      const days: string[] = [];
      const cursor = new Date(start);
      while (cursor <= end) {
        days.push(cursor.toISOString().slice(0, 10));
        cursor.setDate(cursor.getDate() + 1);
      }
      const [ventas, gastos] = await Promise.all([
        Promise.all(days.map((d) => posApi.getReporteVentas(d, d))),
        Promise.all(days.map((d) => posApi.getReporteRentabilidad(d, d)))
      ]);
      return { days, ventas, gastos };
    }
  });

  const lineData = useMemo(() => {
    if (!chartQ.data) return null;
    return {
      labels: chartQ.data.days.map((d) => String(Number(d.slice(8, 10)))),
      datasets: [
        {
          label: "Ventas netas",
          data: chartQ.data.ventas.map((v) => Number(v.totalNeto || 0)),
          borderColor: "#16a34a",
          backgroundColor: "rgba(22,163,74,0.2)"
        },
        {
          label: "Gastos",
          data: chartQ.data.gastos.map((g) => Number(g.totalGastos || 0)),
          borderColor: "#ef4444",
          backgroundColor: "rgba(239,68,68,0.2)"
        }
      ]
    };
  }, [chartQ.data]);
  const monthLabel = useMemo(() => {
    const raw = new Date(`${monthStart}T00:00:00`).toLocaleDateString("es-CO", { month: "long" });
    return raw.charAt(0).toUpperCase() + raw.slice(1);
  }, [monthStart]);

  const resumen = reportQ.data;
  const rent = rentQ.data;

  if (reportQ.isError || chartQ.isError || rentQ.isError) {
    return <p className="text-sm text-red-600">{getErrorMessage(reportQ.error || chartQ.error || rentQ.error)}</p>;
  }

  return (
    <div className="grid gap-4">
      <h2 className="text-2xl font-semibold">Panel administrativo</h2>

      <div className="grid gap-3 md:grid-cols-2 xl:grid-cols-5">
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
        <div className="card p-4">
          <p className="text-sm text-pos-muted">Gastos globales (mes)</p>
          <p className="text-2xl font-bold">{money.format(rent?.totalGastos ?? 0)}</p>
        </div>
      </div>

      <div className="grid gap-4 xl:grid-cols-[2fr_1fr]">
        <div className="card p-4">
          <h3 className="mb-3 text-lg font-semibold">Tendencia del mes</h3>
          {lineData ? (
            <>
              <Line data={lineData} />
              <p className="mt-2 text-center text-sm text-pos-muted">{monthLabel}</p>
            </>
          ) : <p className="text-sm text-pos-muted">Cargando grafico...</p>}
        </div>

        <div className="card p-4">
          <h3 className="mb-3 text-lg font-semibold">Resumen del mes</h3>
          <div className="space-y-2 text-sm">
            <p className="flex justify-between"><span className="text-pos-muted">Bruto</span><span className="font-semibold">{money.format(resumen?.totalBruto ?? 0)}</span></p>
            <p className="flex justify-between"><span className="text-pos-muted">Descuentos</span><span className="font-semibold">{money.format(resumen?.totalDescuentos ?? 0)}</span></p>
            <p className="flex justify-between"><span className="text-pos-muted">Neto</span><span className="font-semibold">{money.format(resumen?.totalNeto ?? 0)}</span></p>
            <p className="flex justify-between"><span className="text-pos-muted">Gastos globales</span><span className="font-semibold">{money.format(rent?.totalGastos ?? 0)}</span></p>
            <p className="flex justify-between"><span className="text-pos-muted">Ganancia neta</span><span className="font-semibold">{money.format(rent?.gananciaNeta ?? 0)}</span></p>
          </div>
        </div>
      </div>
    </div>
  );
}
