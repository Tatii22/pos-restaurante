import { useMemo, useState } from "react";
import { useMutation, useQuery } from "@tanstack/react-query";
import { Bar, Doughnut } from "react-chartjs-2";
import { ArcElement, BarElement, CategoryScale, Chart as ChartJS, Legend, LinearScale, Tooltip } from "chart.js";
import { posApi } from "../shared/api/posApi";
import { getErrorMessage, money } from "../shared/utils";

ChartJS.register(ArcElement, Tooltip, Legend, BarElement, CategoryScale, LinearScale);

const tabs = ["Ventas", "Rentabilidad", "Turnos", "Cierre de Mes"] as const;

const MONTHS = [
  "Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio",
  "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre"
];

function today() {
  return new Date().toISOString().slice(0, 10);
}

function startOfMonth() {
  const d = new Date();
  d.setDate(1);
  return d.toISOString().slice(0, 10);
}

function lastDayOfMonth(year: number, month: number) {
  return new Date(year, month, 0).getDate();
}

function mesKey(year: number, month: number) {
  return `${year}-${String(month).padStart(2, "0")}`;
}

function parseMesKey(key: string) {
  const [y, m] = key.split("-").map(Number);
  return { year: y, month: m };
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

  const ahora = new Date();
  const [mesSel, setMesSel] = useState(mesKey(ahora.getFullYear(), ahora.getMonth() + 1));
  const { year: mesYear, month: mesNum } = parseMesKey(mesSel);
  const ciInicio = `${mesYear}-${String(mesNum).padStart(2, "0")}-01`;
  const ciFin = `${mesYear}-${String(mesNum).padStart(2, "0")}-${String(lastDayOfMonth(mesYear, mesNum)).padStart(2, "0")}`;

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

  const ciVentasQ = useQuery({
    queryKey: ["reportes-ventas-ci", ciInicio, ciFin],
    queryFn: () => posApi.getReporteVentas(ciInicio, ciFin),
    enabled: tab === "Cierre de Mes"
  });
  const ciRentQ = useQuery({
    queryKey: ["reportes-rentabilidad-ci", ciInicio, ciFin],
    queryFn: () => posApi.getReporteRentabilidad(ciInicio, ciFin),
    enabled: tab === "Cierre de Mes"
  });
  const ciTurnosQ = useQuery({
    queryKey: ["reportes-turnos-ci", ciInicio, ciFin],
    queryFn: () => posApi.getTurnosByRango(ciInicio, ciFin),
    enabled: tab === "Cierre de Mes"
  });

  const ciPdfM = useMutation({
    mutationFn: () => posApi.exportRentabilidadPdf(ciInicio, ciFin),
    onSuccess: (blob) => downloadBlob(blob, `cierre_mes_${ciInicio}_${ciFin}.pdf`)
  });
  const ciXlsM = useMutation({
    mutationFn: () => posApi.exportRentabilidadExcel(ciInicio, ciFin),
    onSuccess: (blob) => downloadBlob(blob, `cierre_mes_${ciInicio}_${ciFin}.xlsx`)
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
              <div><p className="text-sm text-pos-muted">Recaudo real (mes)</p><p className="font-semibold">{money.format(reportMesQ.data.recaudoReal || 0)}</p></div>
              <div><p className="text-sm text-pos-muted">Cartera generada</p><p className="font-semibold">{money.format(reportMesQ.data.carteraGenerada || 0)}</p></div>
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
             <p className="text-sm text-pos-muted">Ventas realizadas</p>
             <p className="text-2xl font-bold">{money.format(rentQ.data?.totalVentas || 0)}</p>
           </div>
           <div className="card p-4">
             <p className="text-sm text-pos-muted">Ingresos recibidos</p>
             <p className="text-2xl font-bold">{money.format(rentQ.data?.recaudoReal || 0)}</p>
           </div>
           <div className="card p-4">
             <p className="text-sm text-pos-muted">Gastos registrados</p>
             <p className="text-2xl font-bold">{money.format(rentQ.data?.totalGastos || 0)}</p>
           </div>
           <div className="card p-4 border-2 border-emerald-300 bg-emerald-50">
             <p className="text-sm text-emerald-700 font-medium">Balance final del turno</p>
             <p className="text-3xl font-bold text-emerald-800">{money.format(rentQ.data?.gananciaNeta || 0)}</p>
           </div>
          <div className="card p-4">
            <p className="text-sm text-pos-muted">Margen neto (%)</p>
            <p className="text-2xl font-bold">{margen}%</p>
          </div>
        </div>
      )}

      {tab === "Cierre de Mes" && (
        <>
          <div className="card grid gap-3 p-4 md:grid-cols-4">
            <label className="text-sm">
              Mes
              <select className="input mt-1" value={mesSel} onChange={(e) => setMesSel(e.target.value)}>
                {Array.from({ length: 12 }, (_, i) => {
                  const y = ahora.getFullYear();
                  const m = i + 1;
                  const key = mesKey(y, m);
                  return <option key={key} value={key}>{MONTHS[i]} {y}</option>;
                })}
                {Array.from({ length: 12 }, (_, i) => {
                  const y = ahora.getFullYear() - 1;
                  const m = i + 1;
                  const key = mesKey(y, m);
                  return <option key={key} value={key}>{MONTHS[i]} {y}</option>;
                })}
              </select>
            </label>
            <div className="flex flex-wrap items-end gap-2">
              <button className="btn-primary" onClick={() => ciPdfM.mutate()} disabled={ciPdfM.isPending}>Export PDF </button>
              <button className="btn-ghost" onClick={() => ciXlsM.mutate()} disabled={ciXlsM.isPending}>Export Excel </button>
            </div>
          </div>

          {(ciVentasQ.isError || ciRentQ.isError) && (
            <p className="text-sm text-red-600">{getErrorMessage(ciVentasQ.error || ciRentQ.error)}</p>
          )}

          <div className="grid gap-4 md:grid-cols-4">
            <div className="card p-4">
              <p className="text-sm text-pos-muted">Ventas realizadas</p>
              <p className="text-2xl font-bold">{ciVentasQ.data?.totalVentas ?? "—"}</p>
              <p className="text-xs text-pos-muted mt-1">Total de ventas del mes</p>
            </div>
            <div className="card p-4">
              <p className="text-sm text-pos-muted">Total bruto</p>
              <p className="text-2xl font-bold">{money.format(ciVentasQ.data?.totalBruto || 0)}</p>
            </div>
            <div className="card p-4">
              <p className="text-sm text-pos-muted">Descuentos</p>
              <p className="text-2xl font-bold">{money.format(ciVentasQ.data?.totalDescuentos || 0)}</p>
            </div>
            <div className="card p-4">
              <p className="text-sm text-pos-muted">Total neto</p>
              <p className="text-2xl font-bold">{money.format(ciVentasQ.data?.totalNeto || 0)}</p>
            </div>
          </div>

          <div className="grid gap-4 md:grid-cols-2">
            <div className="card p-4">
              <p className="text-sm text-pos-muted">Ingresos recibidos</p>
              <p className="text-2xl font-bold">{money.format(ciRentQ.data?.recaudoReal || 0)}</p>
              <div className="mt-2 text-xs text-pos-muted space-x-3">
                <span>Efectivo: <span className="font-medium">{money.format(ciRentQ.data?.totalVentasEfectivo || 0)}</span></span>
                <span>Transferencia: <span className="font-medium">{money.format(ciRentQ.data?.totalVentasTransferencia || 0)}</span></span>
              </div>
            </div>
            <div className="card p-4">
              <p className="text-sm text-pos-muted">Gastos registrados</p>
              <p className="text-2xl font-bold text-red-600">{money.format(ciRentQ.data?.totalGastos || 0)}</p>
              <div className="mt-2 text-xs text-pos-muted space-x-3">
                <span>Efectivo: <span className="font-medium">{money.format(ciRentQ.data?.totalGastosEfectivo || 0)}</span></span>
                <span>Transferencia: <span className="font-medium">{money.format(ciRentQ.data?.totalGastosTransferencia || 0)}</span></span>
              </div>
            </div>
          </div>

          <div className="card p-4 border-2 border-emerald-300 bg-emerald-50">
            <p className="text-sm text-emerald-700 font-medium">GANANCIA NETA DEL MES</p>
            <p className="text-4xl font-bold text-emerald-800">{money.format(ciRentQ.data?.gananciaNeta || 0)}</p>
          </div>

          {ciTurnosQ.data && ciTurnosQ.data.length > 0 && (
            <div className="card p-4">
              <p className="text-sm font-medium text-pos-muted mb-2">Turnos del mes</p>
              <div className="grid gap-2 md:grid-cols-4">
                <div><p className="text-xs text-pos-muted">Total turnos</p><p className="font-semibold">{ciTurnosQ.data.length}</p></div>
                <div><p className="text-xs text-pos-muted">Suma ventas</p><p className="font-semibold">{money.format(ciTurnosQ.data.reduce((a, t) => a + Number(t.totalVentas || 0), 0))}</p></div>
                <div><p className="text-xs text-pos-muted">Suma gastos</p><p className="font-semibold">{money.format(ciTurnosQ.data.reduce((a, t) => a + Number(t.totalGastos || 0), 0))}</p></div>
                <div><p className="text-xs text-pos-muted">Total operativo neto</p><p className="font-semibold">{money.format(ciTurnosQ.data.reduce((a, t) => a + Number(t.totalOperativoNeto || 0), 0))}</p></div>
              </div>
            </div>
          )}
        </>
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
              <p className="text-sm text-pos-muted">Total neto operativo acumulado</p>
              <p className="font-semibold">
                {money.format((turnosQ.data || []).reduce((acc, t) => acc + Number(t.totalOperativoNeto || 0), 0))}
              </p>
            </div>
          </div>

            <div className="card p-4">
            <div className="mb-3 text-sm font-medium text-pos-muted">Histórico de turnos · Métricas operativas reales</div>
            {turnosQ.isLoading && <p className="text-sm text-pos-muted">Cargando turnos...</p>}
            {!turnosQ.isLoading && (turnosQ.data?.length || 0) === 0 && (
              <p className="text-sm text-pos-muted">No hay turnos en ese rango.</p>
            )}
            {(turnosQ.data?.length || 0) > 0 && (
              <div className="grid gap-2 md:hidden">
                {(turnosQ.data || []).map((t) => {
                  const d = Number(t.diferenciaTotal || 0);
                  const dClass = d === 0 ? "text-emerald-600" : d < 0 ? "text-red-600" : "text-amber-600";
                  return (
                    <div key={t.id} className="rounded-xl border border-pos-border p-3 text-sm">
                      <div className="flex items-center justify-between">
                        <span className="font-semibold">Turno #{t.id}</span>
                        <span className="text-[10px] uppercase tracking-wider text-pos-muted">{t.estado}</span>
                      </div>
                      <p className="text-xs text-pos-muted">{t.usuario} · {new Date(t.fechaApertura).toLocaleString()}{t.fechaCierre ? ` → ${new Date(t.fechaCierre).toLocaleString()}` : ""}</p>
                      <div className="mt-2 grid grid-cols-2 gap-x-4 gap-y-1 text-xs">
                        <div>Ventas: <span className="font-medium">{money.format(t.totalVentas || 0)}</span></div>
                        <div>Gastos: <span className="font-medium">{money.format(t.totalGastos || 0)}</span></div>
                        <div>Efectivo (neto op.): <span className="font-medium">{money.format(t.efectivoOperativo || 0)}</span></div>
                        <div>Transferencias (neto op.): <span className="font-medium">{money.format(t.transferenciasOperativas || 0)}</span></div>
                        <div>Total operativo: <span className="font-medium">{money.format(t.totalOperativoNeto || 0)}</span></div>
                        <div className="col-span-2">Diferencia (cierre): <span className={`font-semibold ${dClass}`}>{money.format(d)}</span></div>
                      </div>
                    </div>
                  );
                })}
              </div>
            )}
            {(turnosQ.data?.length || 0) > 0 && (
              <div className="hidden overflow-x-auto md:block">
              <table className="w-full min-w-[1080px] text-sm">
                <thead>
                  <tr className="border-b border-pos-border text-xs uppercase tracking-wider text-pos-muted">
                    <th className="p-2 text-left w-12">Turno</th>
                    <th className="p-2 text-left">Apertura</th>
                    <th className="p-2 text-left">Cierre</th>
                    <th className="p-2 text-left">Usuario</th>
                    <th className="p-2 text-left">Estado</th>
                    <th className="p-2 text-right">Total ventas</th>
                    <th className="p-2 text-right">Total gastos</th>
                    <th className="p-2 text-right">Efectivo</th>
                    <th className="p-2 text-right">Transferencias</th>
                    <th className="p-2 text-right">Total</th>
                    <th className="p-2 text-right">Diferencia</th>
                  </tr>
                </thead>
                <tbody>
                  {(turnosQ.data || []).map((t) => {
                    const dif = Number(t.diferenciaTotal || 0);
                    const difClass = dif === 0 ? "text-emerald-600" : dif < 0 ? "text-red-600" : "text-amber-600";
                    return (
                      <tr key={t.id} className="border-b border-pos-border/70 hover:bg-pos-bg/50">
                        <td className="p-2 font-medium">#{t.id}</td>
                        <td className="p-2 text-xs">{new Date(t.fechaApertura).toLocaleString()}</td>
                        <td className="p-2 text-xs">{t.fechaCierre ? new Date(t.fechaCierre).toLocaleString() : "—"}</td>
                        <td className="p-2 text-xs">{t.usuario}</td>
                        <td className="p-2">
                          <span className={`inline-block rounded px-1.5 py-0.5 text-[10px] ${t.estado === "CERRADO" ? "bg-emerald-100 text-emerald-700" : t.estado === "ABIERTO" ? "bg-blue-100 text-blue-700" : "bg-amber-100 text-amber-700"}`}>
                            {t.estado}
                          </span>
                        </td>
                        <td className="p-2 text-right tabular-nums">{money.format(t.totalVentas || 0)}</td>
                        <td className="p-2 text-right tabular-nums">{money.format(t.totalGastos || 0)}</td>
                        <td className="p-2 text-right tabular-nums">{money.format(t.efectivoOperativo || 0)}</td>
                        <td className="p-2 text-right tabular-nums">{money.format(t.transferenciasOperativas || 0)}</td>
                        <td className="p-2 text-right tabular-nums font-medium">{money.format(t.totalOperativoNeto || 0)}</td>
                        <td className={`p-2 text-right tabular-nums font-semibold ${difClass}`}>{money.format(dif)}</td>
                      </tr>
                    );
                  })}
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
