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
  const rentQ = useQuery({
    queryKey: ["reportes-rentabilidad", fi, ff],
    queryFn: () => posApi.getReporteRentabilidad(fi, ff)
  });

  const reportMesQ = useQuery({
    queryKey: ["reportes-ventas-mes", mesInicio, mesFin],
    queryFn: () => posApi.getReporteVentas(mesInicio, mesFin)
  });
  const rentMesQ = useQuery({
    queryKey: ["reportes-rentabilidad-mes", mesInicio, mesFin],
    queryFn: () => posApi.getReporteRentabilidad(mesInicio, mesFin)
  });
  const turnosQ = useQuery({
    queryKey: ["reportes-turnos-rango", fi, ff],
    queryFn: () => posApi.getTurnosByRango(fi, ff)
  });

  const pdfM = useMutation({
    mutationFn: () => posApi.exportRentabilidadPdf(fi, ff),
    onSuccess: (blob) => downloadBlob(blob, `reporte_rentabilidad_${fi}_${ff}.pdf`)
  });
  const xlsM = useMutation({
    mutationFn: () => posApi.exportRentabilidadExcel(fi, ff),
    onSuccess: (blob) => downloadBlob(blob, `reporte_rentabilidad_${fi}_${ff}.xlsx`)
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
    const r = rentQ.data;
    const ventas = Number(r?.totalVentas || 0);
    if (!r || ventas <= 0) return 0;
    return Math.round((Number(r.gananciaNeta || 0) / ventas) * 100);
  }, [rentQ.data]);

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

      <div className="card grid gap-3 p-4 md:grid-cols-4">
        <label className="text-sm">
          Fecha inicio
          <input className="input mt-1" type="date" value={fi} onChange={(e) => setFi(e.target.value)} />
        </label>
        <label className="text-sm">
          Fecha fin
          <input className="input mt-1" type="date" value={ff} onChange={(e) => setFf(e.target.value)} />
        </label>
        <div className="flex flex-wrap items-end gap-2">
          <button className="btn-primary" onClick={() => pdfM.mutate()} disabled={pdfM.isPending}>Export PDF </button>
          <button className="btn-ghost" onClick={() => xlsM.mutate()} disabled={xlsM.isPending}>Export Excel </button>
        </div>
      </div>

      {(reportQ.isError || rentQ.isError || reportMesQ.isError || rentMesQ.isError || turnosQ.isError) && (
        <p className="text-sm text-red-600">{getErrorMessage(reportQ.error || rentQ.error || reportMesQ.error || rentMesQ.error || turnosQ.error)}</p>
      )}
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
          {rentMesQ.data && (
            <div className="card grid gap-2 p-4 md:grid-cols-2">
              <div><p className="text-sm text-pos-muted">Gastos globales (mes)</p><p className="font-semibold">{money.format(rentMesQ.data.totalGastos || 0)}</p></div>
              <div><p className="text-sm text-pos-muted">Ganancia neta global (mes)</p><p className="font-semibold">{money.format(rentMesQ.data.gananciaNeta || 0)}</p></div>
            </div>
          )}
        </>
      )}

      {tab === "Rentabilidad" && (
        <div className="grid gap-4 md:grid-cols-3">
          <div className="card p-4">
            <p className="text-sm text-pos-muted">Ingresos (ventas)</p>
            <p className="text-2xl font-bold">{money.format(rentQ.data?.totalVentas || 0)}</p>
          </div>
          <div className="card p-4">
            <p className="text-sm text-pos-muted">Gastos globales</p>
            <p className="text-2xl font-bold">{money.format(rentQ.data?.totalGastos || 0)}</p>
          </div>
          <div className="card p-4">
            <p className="text-sm text-pos-muted">Ganancia neta</p>
            <p className="text-2xl font-bold">{money.format(rentQ.data?.gananciaNeta || 0)}</p>
          </div>
          <div className="card p-4">
            <p className="text-sm text-pos-muted">Margen neto (%)</p>
            <p className="text-2xl font-bold">{margen}%</p>
          </div>
        </div>
      )}

      {tab === "Turnos" && (
        <>
          <div className="card grid gap-2 p-4 md:grid-cols-4">
            <div>
              <p className="text-sm text-pos-muted">Turnos</p>
              <p className="font-semibold">{turnosQ.data?.length || 0}</p>
            </div>
            <div>
              <p className="text-sm text-pos-muted">Total ventas</p>
              <p className="font-semibold">
                {money.format((turnosQ.data || []).reduce((acc, t) => acc + Number(t.totalVentas || 0), 0))}
              </p>
            </div>
            <div>
              <p className="text-sm text-pos-muted">Total gastos</p>
              <p className="font-semibold">
                {money.format((turnosQ.data || []).reduce((acc, t) => acc + Number(t.totalGastos || 0), 0))}
              </p>
            </div>
            <div>
              <p className="text-sm text-pos-muted">Faltante acumulado</p>
              <p className="font-semibold">
                {money.format((turnosQ.data || []).reduce((acc, t) => acc + Number(t.faltante || 0), 0))}
              </p>
            </div>
          </div>

          <div className="card p-4">
            {turnosQ.isLoading && <p className="text-sm text-pos-muted">Cargando turnos...</p>}
            {!turnosQ.isLoading && (turnosQ.data?.length || 0) === 0 && (
              <p className="text-sm text-pos-muted">No hay turnos en ese rango.</p>
            )}
            {(turnosQ.data?.length || 0) > 0 && (
              <div className="grid gap-2 md:hidden">
                {(turnosQ.data || []).map((t) => (
                  <div key={t.id} className="rounded-xl border border-pos-border p-3">
                    <p className="font-semibold">Turno #{t.id}</p>
                    <p className="text-xs text-pos-muted">Usuario: {t.usuario}</p>
                    <p className="text-xs text-pos-muted">Estado: {t.estado}</p>
                    <p className="text-xs">Apertura: {new Date(t.fechaApertura).toLocaleString()}</p>
                    <p className="text-xs">Cierre: {t.fechaCierre ? new Date(t.fechaCierre).toLocaleString() : "-"}</p>
                    <p className="text-xs">Inicial: {money.format(t.montoInicial || 0)}</p>
                    <p className="text-xs">Ventas: {money.format(t.totalVentas || 0)}</p>
                    <p className="text-xs">Gastos: {money.format(t.totalGastos || 0)}</p>
                    <p className="text-xs">Esperado: {money.format(t.esperado || 0)}</p>
                    <p className="text-xs">Faltante: {money.format(t.faltante || 0)}</p>
                  </div>
                ))}
              </div>
            )}
            {(turnosQ.data?.length || 0) > 0 && (
              <div className="hidden overflow-x-auto md:block">
              <table className="w-full min-w-[900px] text-sm">
                <thead>
                  <tr className="border-b border-pos-border">
                    <th className="p-2 text-left">Turno</th>
                    <th className="p-2 text-left">Apertura</th>
                    <th className="p-2 text-left">Cierre</th>
                    <th className="p-2 text-left">Usuario</th>
                    <th className="p-2 text-left">Estado</th>
                    <th className="p-2 text-left">Monto inicial</th>
                    <th className="p-2 text-left">Total ventas</th>
                    <th className="p-2 text-left">Total gastos</th>
                    <th className="p-2 text-left">Esperado</th>
                    <th className="p-2 text-left">Faltante</th>
                  </tr>
                </thead>
                <tbody>
                  {(turnosQ.data || []).map((t) => (
                    <tr key={t.id} className="border-b border-pos-border/70">
                      <td className="p-2">#{t.id}</td>
                      <td className="p-2">{new Date(t.fechaApertura).toLocaleString()}</td>
                      <td className="p-2">{t.fechaCierre ? new Date(t.fechaCierre).toLocaleString() : "-"}</td>
                      <td className="p-2">{t.usuario}</td>
                      <td className="p-2">{t.estado}</td>
                      <td className="p-2">{money.format(t.montoInicial || 0)}</td>
                      <td className="p-2">{money.format(t.totalVentas || 0)}</td>
                      <td className="p-2">{money.format(t.totalGastos || 0)}</td>
                      <td className="p-2">{money.format(t.esperado || 0)}</td>
                      <td className="p-2">{money.format(t.faltante || 0)}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
              </div>
            )}
          </div>
        </>
      )}
    </div>
  );
}
