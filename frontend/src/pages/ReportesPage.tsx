import { useMemo, useState } from "react";
import { useMutation, useQuery } from "@tanstack/react-query";
import { Bar, Doughnut } from "react-chartjs-2";
import { ArcElement, BarElement, CategoryScale, Chart as ChartJS, Legend, LinearScale, Tooltip } from "chart.js";
import { posApi } from "../shared/api/posApi";
import { getErrorMessage, money } from "../shared/utils";

ChartJS.register(ArcElement, Tooltip, Legend, BarElement, CategoryScale, LinearScale);

const tabs = ["Ventas", "Rentabilidad", "Turnos"] as const;

function today() {
  return new Date().toISOString().slice(0, 10);
}

function downloadBlob(blob: Blob, name: string) {
  const url = URL.createObjectURL(blob);
  const a = document.createElement("a");
  a.href = url;
  a.download = name;
  document.body.appendChild(a);
  a.click();
  a.remove();
  URL.revokeObjectURL(url);
}

export function ReportesPage() {
  const [tab, setTab] = useState<(typeof tabs)[number]>("Ventas");
  const [fi, setFi] = useState(today());
  const [ff, setFf] = useState(today());

  const reportQ = useQuery({
    queryKey: ["reportes-ventas", fi, ff],
    queryFn: () => posApi.getReporteVentas(fi, ff)
  });

  const pdfM = useMutation({
    mutationFn: () => posApi.exportVentasPdf(fi, ff),
    onSuccess: (blob) => downloadBlob(blob, `reporte_ventas_${fi}_${ff}.pdf`)
  });
  const xlsM = useMutation({
    mutationFn: () => posApi.exportVentasExcel(fi, ff),
    onSuccess: (blob) => downloadBlob(blob, `reporte_ventas_${fi}_${ff}.xlsx`)
  });

  const payChart = useMemo(() => {
    const r = reportQ.data;
    if (!r) return null;
    return {
      labels: ["Efectivo", "Transferencia"],
      datasets: [
        {
          data: [Number(r.totalEfectivo || 0), Number(r.totalTransferencia || 0)],
          backgroundColor: ["#16a34a", "#0ea5e9"]
        }
      ]
    };
  }, [reportQ.data]);

  const salesChart = useMemo(() => {
    const r = reportQ.data;
    if (!r) return null;
    return {
      labels: ["Bruto", "Descuentos", "Neto"],
      datasets: [
        {
          label: "COP",
          data: [Number(r.totalBruto || 0), Number(r.totalDescuentos || 0), Number(r.totalNeto || 0)],
          backgroundColor: ["#0ea5e9", "#f59e0b", "#16a34a"]
        }
      ]
    };
  }, [reportQ.data]);

  return (
    <div className="grid gap-4">
      <h2 className="text-2xl font-semibold">Reportes</h2>
      <div className="flex flex-wrap gap-2">
        {tabs.map((t) => (
          <button key={t} className={tab === t ? "btn-soft" : "btn-ghost"} onClick={() => setTab(t)}>
            {t}
          </button>
        ))}
      </div>

      <div className="card grid gap-3 p-4 md:grid-cols-5">
        <label className="text-sm">
          Fecha inicio
          <input className="input mt-1" type="date" value={fi} onChange={(e) => setFi(e.target.value)} />
        </label>
        <label className="text-sm">
          Fecha fin
          <input className="input mt-1" type="date" value={ff} onChange={(e) => setFf(e.target.value)} />
        </label>
        <label className="text-sm">
          Turno
          <input className="input mt-1" placeholder="Próximo endpoint" disabled />
        </label>
        <div className="flex items-end gap-2">
          <button className="btn-primary" onClick={() => pdfM.mutate()}>Export PDF</button>
          <button className="btn-ghost" onClick={() => xlsM.mutate()}>Export Excel</button>
        </div>
      </div>

      {reportQ.isError && <p className="text-sm text-red-600">{getErrorMessage(reportQ.error)}</p>}
      {(pdfM.isError || xlsM.isError) && <p className="text-sm text-red-600">{getErrorMessage(pdfM.error || xlsM.error)}</p>}

      <div className="grid gap-4 xl:grid-cols-2">
        <div className="card p-4">
          <h3 className="mb-3 font-semibold">Ventas por día (periodo)</h3>
          {salesChart ? <Bar data={salesChart} /> : <p>Cargando...</p>}
        </div>
        <div className="card p-4">
          <h3 className="mb-3 font-semibold">Ventas por método pago</h3>
          {payChart ? <Doughnut data={payChart} /> : <p>Cargando...</p>}
        </div>
      </div>

      {reportQ.data && (
        <div className="card grid gap-2 p-4 md:grid-cols-4">
          <div><p className="text-sm text-pos-muted">Total ventas</p><p className="font-semibold">{reportQ.data.totalVentas}</p></div>
          <div><p className="text-sm text-pos-muted">Bruto</p><p className="font-semibold">{money.format(reportQ.data.totalBruto || 0)}</p></div>
          <div><p className="text-sm text-pos-muted">Descuentos</p><p className="font-semibold">{money.format(reportQ.data.totalDescuentos || 0)}</p></div>
          <div><p className="text-sm text-pos-muted">Neto</p><p className="font-semibold">{money.format(reportQ.data.totalNeto || 0)}</p></div>
        </div>
      )}
    </div>
  );
}
