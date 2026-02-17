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

function startOfMonth() {
  const d = new Date();
  d.setDate(1);
  return d.toISOString().slice(0, 10);
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
  const mesInicio = startOfMonth();
  const mesFin = today();

  const reportQ = useQuery({
    queryKey: ["reportes-ventas", fi, ff],
    queryFn: () => posApi.getReporteVentas(fi, ff)
  });

  const reportMesQ = useQuery({
    queryKey: ["reportes-ventas-mes", mesInicio, mesFin],
    queryFn: () => posApi.getReporteVentas(mesInicio, mesFin)
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
    const r = reportMesQ.data;
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
  }, [reportMesQ.data]);

  const salesChart = useMemo(() => {
    const r = reportMesQ.data;
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
  }, [reportMesQ.data]);

  const margen = useMemo(() => {
    const r = reportQ.data;
    if (!r || !r.totalBruto) return 0;
    return Math.round((Number(r.totalNeto || 0) / Number(r.totalBruto || 1)) * 100);
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
          <input className="input mt-1" placeholder="Filtro disponible en siguiente endpoint" disabled />
        </label>
        <div className="flex items-end gap-2">
          <button className="btn-primary" onClick={() => pdfM.mutate()} disabled={pdfM.isPending}>Export PDF</button>
          <button className="btn-ghost" onClick={() => xlsM.mutate()} disabled={xlsM.isPending}>Export Excel</button>
        </div>
      </div>

      {reportQ.isError && <p className="text-sm text-red-600">{getErrorMessage(reportQ.error)}</p>}
      {(pdfM.isError || xlsM.isError) && <p className="text-sm text-red-600">{getErrorMessage(pdfM.error || xlsM.error)}</p>}

      {tab === "Ventas" && (
        <>
          <div className="grid gap-4 xl:grid-cols-2">
            <div className="card p-4">
              <h3 className="mb-3 font-semibold">Ventas del mes</h3>
              {salesChart ? <Bar data={salesChart} /> : <p>Cargando...</p>}
            </div>
            <div className="card p-4">
              <h3 className="mb-3 font-semibold">Ventas por metodo de pago (mes)</h3>
              {payChart ? (
                <div className="mx-auto w-full max-w-[240px]">
                  <Doughnut data={payChart} />
                </div>
              ) : <p>Cargando...</p>}
            </div>
          </div>
          {reportMesQ.data && (
            <div className="card grid gap-2 p-4 md:grid-cols-4">
              <div><p className="text-sm text-pos-muted">Total ventas (mes)</p><p className="font-semibold">{reportMesQ.data.totalVentas}</p></div>
              <div><p className="text-sm text-pos-muted">Bruto (mes)</p><p className="font-semibold">{money.format(reportMesQ.data.totalBruto || 0)}</p></div>
              <div><p className="text-sm text-pos-muted">Descuentos (mes)</p><p className="font-semibold">{money.format(reportMesQ.data.totalDescuentos || 0)}</p></div>
              <div><p className="text-sm text-pos-muted">Neto (mes)</p><p className="font-semibold">{money.format(reportMesQ.data.totalNeto || 0)}</p></div>
            </div>
          )}
        </>
      )}

      {tab === "Rentabilidad" && (
        <div className="grid gap-4 md:grid-cols-3">
          <div className="card p-4">
            <p className="text-sm text-pos-muted">Ingresos brutos</p>
            <p className="text-2xl font-bold">{money.format(reportQ.data?.totalBruto || 0)}</p>
          </div>
          <div className="card p-4">
            <p className="text-sm text-pos-muted">Neto</p>
            <p className="text-2xl font-bold">{money.format(reportQ.data?.totalNeto || 0)}</p>
          </div>
          <div className="card p-4">
            <p className="text-sm text-pos-muted">Margen neto estimado</p>
            <p className="text-2xl font-bold">{margen}%</p>
          </div>
        </div>
      )}

      {tab === "Turnos" && (
        <div className="card p-4 text-sm text-pos-muted">
          Resumen por turnos: pendiente de endpoint dedicado en backend para historico de turnos.
        </div>
      )}
    </div>
  );
}
