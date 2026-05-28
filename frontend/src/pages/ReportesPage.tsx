import { useMemo, useState, useRef, useEffect, useCallback } from "react";
import { useMutation, useQuery } from "@tanstack/react-query";
import { Bar, Doughnut } from "react-chartjs-2";
import { ArcElement, BarElement, CategoryScale, Chart as ChartJS, Filler, Legend, LinearScale, LineElement, PointElement, Tooltip } from "chart.js";
import { posApi } from "../shared/api/posApi";
import { getErrorMessage, money } from "../shared/utils";
import { today, startOfMonth, lastDayOfMonth } from "../shared/dateUtils";
import { DateRangePicker } from "../shared/DateRangePicker";

ChartJS.register(ArcElement, BarElement, CategoryScale, LinearScale, LineElement, PointElement, Tooltip, Legend, Filler);

const tabs = ["Ventas", "Turnos", "Cierre de Mes"] as const;

const MONTHS = [
  "Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio",
  "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre"
];

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
  const [fi, setFi] = useState(startOfMonth());
  const [ff, setFf] = useState(today());
  const mesInicio = startOfMonth();
  const mesFin = today();

  const ahora = new Date();
  const [ciMes, setCiMes] = useState({ month: ahora.getMonth(), year: ahora.getFullYear() });
  const [monthOpen, setMonthOpen] = useState(false);
  const [yearOpen, setYearOpen] = useState(false);
  const monthRef = useRef<HTMLDivElement>(null);
  const yearRef = useRef<HTMLDivElement>(null);

  const closeDropdowns = useCallback((e: MouseEvent) => {
    if (monthRef.current && !monthRef.current.contains(e.target as Node)) setMonthOpen(false);
    if (yearRef.current && !yearRef.current.contains(e.target as Node)) setYearOpen(false);
  }, []);
  useEffect(() => { document.addEventListener("mousedown", closeDropdowns); return () => document.removeEventListener("mousedown", closeDropdowns); }, [closeDropdowns]);
  const ciInicio = `${ciMes.year}-${String(ciMes.month + 1).padStart(2, "0")}-01`;
  const ciFin = `${ciMes.year}-${String(ciMes.month + 1).padStart(2, "0")}-${String(lastDayOfMonth(ciMes.year, ciMes.month + 1)).padStart(2, "0")}`;

  const reportMesQ = useQuery({
    queryKey: ["reportes-ventas-mes", mesInicio, mesFin],
    queryFn: () => posApi.getReporteVentas(mesInicio, mesFin)
  });
  const turnosQ = useQuery({
    queryKey: ["reportes-turnos-rango", fi, ff],
    queryFn: () => posApi.getTurnosByRango(fi, ff)
  });
  const sortedTurnos = useMemo(() => [...(turnosQ.data || [])].sort((a, b) => new Date(a.fechaApertura).getTime() - new Date(b.fechaApertura).getTime()), [turnosQ.data]);

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
    onSuccess: (blob) => downloadBlob(blob, `informe_cierre_${MONTHS[ciMes.month].toLowerCase()}_${ciMes.year}.pdf`)
  });

  const ciXlsM = useMutation({
    mutationFn: () => posApi.exportRentabilidadExcel(ciInicio, ciFin),
    onSuccess: (blob) => downloadBlob(blob, `informe_cierre_${MONTHS[ciMes.month].toLowerCase()}_${ciMes.year}.xlsx`)
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

  const trendQ = useQuery({
    queryKey: ["ventas-trend-v2", mesInicio, mesFin],
    queryFn: async () => {
      const start = new Date(mesInicio + "T00:00:00");
      const end = new Date(mesFin + "T00:00:00");
      const days: string[] = [];
      const cursor = new Date(start);
      while (cursor <= end) {
        days.push(cursor.toISOString().slice(0, 10));
        cursor.setDate(cursor.getDate() + 1);
      }
      if (days.length > 62) return { days: [], ventas: [], rents: [] };
      const results = await Promise.all(days.map(async (d) => {
        const [venta, rent] = await Promise.all([
          posApi.getReporteVentas(d, d),
          posApi.getReporteRentabilidad(d, d)
        ]);
        return { venta, rent };
      }));
      return { days, ventas: results.map(r => r.venta), rents: results.map(r => r.rent) };
    },
    enabled: tab === "Ventas"
  });

  const trendChart = useMemo(() => {
    const d = trendQ.data;
    if (!d || d.days.length === 0) return null;
    return {
      labels: d.days.map((day) => String(Number(day.slice(8, 10)))),
      datasets: [
        {
          label: "Dinero que entra",
          data: (d.ventas || []).map((v) => Number(v.recaudoReal || 0)),
          backgroundColor: "#16a34a",
          borderRadius: 4
        },
        {
          label: "Dinero que sale",
          data: (d.rents || []).map((r) => Number(r.totalGastos || 0)),
          backgroundColor: "#dc2626",
          borderRadius: 4
        }
      ]
    };
  }, [trendQ.data]);

  const ciPayChart = useMemo(() => {
    const r = ciRentQ.data;
    if (!r) return null;
    return {
      labels: ["Efectivo", "Transferencia"],
      datasets: [{
        data: [Number(r.totalVentasEfectivo || 0), Number(r.totalVentasTransferencia || 0)],
        backgroundColor: ["#3EB489", "#2A7B5E"],
        borderWidth: 0
      }]
    };
  }, [ciRentQ.data]);

  const ciFlowChart = useMemo(() => {
    const r = ciRentQ.data;
    if (!r) return null;
    return {
      labels: ["Ingresos", "Gastos", "Ganancia"],
      datasets: [{
        label: "Flujo del mes",
        data: [Number(r.recaudoReal || 0), Number(r.totalGastos || 0), Number(r.gananciaNeta || 0)],
        backgroundColor: ["#3EB489", "#dc2626", "#2A7B5E"],
        borderRadius: 4
      }]
    };
  }, [ciRentQ.data]);

  const insights = useMemo(() => {
    const r = reportMesQ.data;
    if (!r) return [];
    const list: { icon: string; text: string; color: string }[] = [];
    const abonos = r.totalAbonos ?? 0;
    const cartera = r.carteraPendiente ?? 0;
    if (abonos > 0) list.push({ icon: "✅", text: `Recuperaste ${money.format(abonos)} en pagos de deudas`, color: "text-emerald-700" });
    if (cartera > 0) list.push({ icon: "⚠️", text: `Hay ${money.format(cartera)} pendientes por cobrar`, color: "text-amber-700" });
    if (!cartera && !abonos && (r.totalVentas ?? 0) > 0) list.push({ icon: "📈", text: "Buen movimiento de ventas este período", color: "text-blue-700" });
    return list;
  }, [reportMesQ.data]);

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

      {(reportMesQ.isError || turnosQ.isError || trendQ.isError || ciPdfM.isError || ciXlsM.isError) && (
        <p className="text-sm text-red-600">{getErrorMessage(reportMesQ.error || turnosQ.error || trendQ.error || ciPdfM.error || ciXlsM.error)}</p>
      )}

      {tab === "Ventas" && (
        <>
          <p className="text-lg font-semibold text-pos-muted">
            {MONTHS[ahora.getMonth()]} {ahora.getFullYear()}
          </p>
          <div className="grid gap-3 grid-cols-2 xl:grid-cols-4">
            <div className="card p-4">
              <div className="flex items-center gap-3">
                <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-full bg-blue-100">
                  <svg className="h-5 w-5 text-blue-600" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
                    <path strokeLinecap="round" strokeLinejoin="round" d="M12 8c-1.657 0-3 .895-3 2s1.343 2 3 2 3 .895 3 2-1.343 2-3 2m0-8c1.11 0 2.08.402 2.599 1M12 8V7m0 1v8m0 0v1m0-1c-1.11 0-2.08-.402-2.599-1M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
                  </svg>
                </div>
                <div className="min-w-0">
                  <p className="truncate text-xs text-pos-muted">Total vendido este período</p>
                  <p className="text-xl font-bold tabular-nums">{money.format(reportMesQ.data?.totalNeto ?? 0)}</p>
                </div>
              </div>
            </div>
            <div className="card p-4">
              <div className="flex items-center gap-3">
                <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-full bg-emerald-100">
                  <svg className="h-5 w-5 text-emerald-600" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
                    <path strokeLinecap="round" strokeLinejoin="round" d="M17 9V7a2 2 0 00-2-2H5a2 2 0 00-2 2v6a2 2 0 002 2h2m2 4h10a2 2 0 002-2v-6a2 2 0 00-2-2H9a2 2 0 00-2 2v6a2 2 0 002 2zm7-5a2 2 0 11-4 0 2 2 0 014 0z" />
                  </svg>
                </div>
                <div className="min-w-0">
                  <p className="truncate text-xs text-pos-muted">Dinero recibido</p>
                  <p className="text-xl font-bold tabular-nums">{money.format(reportMesQ.data?.recaudoReal ?? 0)}</p>
                </div>
              </div>
            </div>
            <div className="card p-4">
              <div className="flex items-center gap-3">
                <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-full bg-amber-100">
                  <svg className="h-5 w-5 text-amber-600" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
                    <path strokeLinecap="round" strokeLinejoin="round" d="M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
                  </svg>
                </div>
                <div className="min-w-0">
                  <p className="truncate text-xs text-pos-muted">Ventas fiadas pendientes</p>
                  <p className="text-xl font-bold tabular-nums">{money.format(reportMesQ.data?.carteraPendiente ?? 0)}</p>
                </div>
              </div>
            </div>
            <div className="card p-4">
              <div className="flex items-center gap-3">
                <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-full bg-violet-100">
                  <svg className="h-5 w-5 text-violet-600" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
                    <path strokeLinecap="round" strokeLinejoin="round" d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2" />
                  </svg>
                </div>
                <div className="min-w-0">
                  <p className="truncate text-xs text-pos-muted">Ventas registradas</p>
                  <p className="text-xl font-bold tabular-nums">{reportMesQ.data?.totalVentas ?? 0}</p>
                </div>
              </div>
            </div>
          </div>

          <div className="grid gap-4 xl:grid-cols-2">
            <div className="card p-4">
              <h3 className="mb-3 text-sm font-semibold">Ventas por método de pago (mes)</h3>
              {payChart ? (
                <div className="mx-auto w-full max-w-[200px]">
                  <Doughnut data={payChart} />
                </div>
              ) : <p className="text-sm text-pos-muted">Cargando...</p>}
            </div>
            <div className="card p-4">
              <h3 className="mb-3 text-sm font-semibold">Tendencia de ventas</h3>
              {trendChart ? (
                <Bar data={trendChart} />
              ) : trendQ.isLoading ? <p className="text-sm text-pos-muted">Cargando...</p> : <p className="text-sm text-pos-muted">Sin datos para el período actual</p>}
            </div>
          </div>

          {insights.length > 0 && (
            <div className="card p-3">
              <div className="flex flex-wrap gap-x-6 gap-y-2 text-sm">
                {insights.map((ins, i) => (
                  <span key={i} className={ins.color}>{ins.icon} {ins.text}</span>
                ))}
              </div>
            </div>
          )}
        </>
      )}

      {tab === "Cierre de Mes" && (
        <>
          {(ciVentasQ.isError || ciRentQ.isError) && (
            <p className="text-sm text-red-600">{getErrorMessage(ciVentasQ.error || ciRentQ.error)}</p>
          )}

          {/* ── Header ─────────────────────────────────────────────── */}
          <div className="flex flex-col gap-3 sm:flex-row sm:items-end sm:justify-between">
            <div>
              <p className="text-xl font-semibold text-pos-text">Resumen financiero de {MONTHS[ciMes.month]} {ciMes.year}</p>
              <p className="mt-0.5 text-sm text-pos-muted">Información consolidada del período seleccionado.</p>
            </div>
            <div className="flex shrink-0 flex-wrap items-center gap-2">
              <div className="relative" ref={monthRef}>
                <button
                  type="button"
                  onClick={() => { setMonthOpen((o) => !o); setYearOpen(false); }}
                  className="flex h-9 items-center gap-1 rounded-lg border border-pos-border bg-pos-card px-3 text-sm font-medium text-pos-text shadow-sm transition-all hover:border-pos-border focus:border-pos-accent focus:outline-none"
                >
                  {MONTHS[ciMes.month]}
                  <svg className={`h-3.5 w-3.5 text-pos-muted transition-transform ${monthOpen ? "rotate-180" : ""}`} fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}><path strokeLinecap="round" strokeLinejoin="round" d="M19.5 8.25l-7.5 7.5-7.5-7.5" /></svg>
                </button>
                {monthOpen && (
                  <div className="absolute right-0 z-20 mt-1 w-full min-w-[140px] overflow-hidden rounded-lg border border-pos-border bg-pos-card shadow-pos">
                    {MONTHS.map((name, i) => (
                      <button
                        key={i}
                        type="button"
                        className={`w-full px-3 py-1.5 text-left text-sm transition-colors ${
                          i === ciMes.month
                            ? "bg-pos-accentSoft font-medium text-pos-forest"
                            : "text-pos-text hover:bg-pos-accentSoft hover:text-pos-forest"
                        }`}
                        onClick={() => { setCiMes((p) => ({ ...p, month: i })); setMonthOpen(false); }}
                      >
                        {name}
                      </button>
                    ))}
                  </div>
                )}
              </div>
              <div className="relative" ref={yearRef}>
                <button
                  type="button"
                  onClick={() => { setYearOpen((o) => !o); setMonthOpen(false); }}
                  className="flex h-9 items-center gap-1 rounded-lg border border-pos-border bg-pos-card px-3 text-sm font-medium text-pos-text shadow-sm transition-all hover:border-pos-border focus:border-pos-accent focus:outline-none"
                >
                  {ciMes.year}
                  <svg className={`h-3.5 w-3.5 text-pos-muted transition-transform ${yearOpen ? "rotate-180" : ""}`} fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}><path strokeLinecap="round" strokeLinejoin="round" d="M19.5 8.25l-7.5 7.5-7.5-7.5" /></svg>
                </button>
                {yearOpen && (
                  <div className="absolute right-0 z-20 mt-1 w-full min-w-[100px] overflow-hidden rounded-lg border border-pos-border bg-pos-card shadow-pos">
                    {Array.from({ length: 5 }, (_, i) => ahora.getFullYear() - 2 + i).map((y) => (
                      <button
                        key={y}
                        type="button"
                        className={`w-full px-3 py-1.5 text-left text-sm transition-colors ${
                          y === ciMes.year
                            ? "bg-pos-accentSoft font-medium text-pos-forest"
                            : "text-pos-text hover:bg-pos-accentSoft hover:text-pos-forest"
                        }`}
                        onClick={() => { setCiMes((p) => ({ ...p, year: y })); setYearOpen(false); }}
                      >
                        {y}
                      </button>
                    ))}
                  </div>
                )}
              </div>
              <button
                className="inline-flex h-9 items-center gap-1.5 rounded-lg border border-pos-border bg-pos-card px-3 py-2 text-sm font-medium text-pos-text shadow-sm transition-all hover:border-emerald-200 hover:bg-emerald-50 hover:shadow disabled:cursor-not-allowed disabled:opacity-50"
                onClick={() => ciPdfM.mutate()}
                disabled={ciPdfM.isPending}
              >
                {ciPdfM.isPending ? (
                  <svg className="h-4 w-4 animate-spin text-pos-muted" fill="none" viewBox="0 0 24 24"><circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" /><path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z" /></svg>
                ) : (
                  <svg className="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={1.5}><path strokeLinecap="round" strokeLinejoin="round" d="M6.72 13.829c-.24.03-.48.062-.72.096m.72-.096a42.415 42.415 0 0110.56 0m-10.56 0L6.34 18m10.94-4.171c.24.03.48.062.72.096m-.72-.096L17.66 18m0 0l.229 2.523a1.125 1.125 0 01-1.12 1.227H7.231c-.662 0-1.18-.568-1.12-1.227L6.34 18m11.318 0h1.091A2.25 2.25 0 0021 15.75V9.456c0-1.081-.768-2.015-1.837-2.175a48.055 48.055 0 00-1.913-.247M6.34 18H5.25A2.25 2.25 0 013 15.75V9.456c0-1.081.768-2.015 1.837-2.175a48.041 48.041 0 011.913-.247m10.5 0a48.536 48.536 0 00-10.5 0m10.5 0V3.375c0-.621-.504-1.125-1.125-1.125h-8.25c-.621 0-1.125.504-1.125 1.125v3.659M18 10.5h.008v.008H18V10.5zm-3 0h.008v.008H15V10.5z" /></svg>
                )}
                {ciPdfM.isPending ? "PDF…" : "PDF"}
              </button>
              <button
                className="inline-flex h-9 items-center gap-1.5 rounded-lg border border-pos-border bg-pos-card px-3 py-2 text-sm font-medium text-pos-text shadow-sm transition-all hover:border-emerald-200 hover:bg-emerald-50 hover:shadow disabled:cursor-not-allowed disabled:opacity-50"
                onClick={() => ciXlsM.mutate()}
                disabled={ciXlsM.isPending}
              >
                {ciXlsM.isPending ? (
                  <svg className="h-4 w-4 animate-spin text-pos-muted" fill="none" viewBox="0 0 24 24"><circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" /><path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z" /></svg>
                ) : (
                  <svg className="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={1.5}><path strokeLinecap="round" strokeLinejoin="round" d="M3.75 9.776c.112-.017.227-.026.344-.026h15.812c.117 0 .232.009.344.026m-16.5 0a2.25 2.25 0 00-1.883 2.542l.857 6a2.25 2.25 0 002.227 1.932H19.05a2.25 2.25 0 002.227-1.932l.857-6a2.25 2.25 0 00-1.883-2.542m-16.5 0V6A2.25 2.25 0 016 3.75h3.879a1.5 1.5 0 011.06.44l2.122 2.12a1.5 1.5 0 001.06.44H18A2.25 2.25 0 0120.25 9v.776" /></svg>
                )}
                {ciXlsM.isPending ? "Excel…" : "Excel"}
              </button>
            </div>
          </div>

          {/* ── Fila 1 — Métricas principales ──────────────────────── */}
          <div className="grid gap-4 md:grid-cols-4">
            <div className="card p-5">
              <div className="flex items-center gap-3">
                <div className="flex h-11 w-11 shrink-0 items-center justify-center rounded-xl bg-pos-accentSoft">
                  <svg className="h-5 w-5 text-pos-forest" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
                    <path strokeLinecap="round" strokeLinejoin="round" d="M12 8c-1.657 0-3 .895-3 2s1.343 2 3 2 3 .895 3 2-1.343 2-3 2m0-8c1.11 0 2.08.402 2.599 1M12 8V7m0 1v8m0 0v1m0-1c-1.11 0-2.08-.402-2.599-1M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
                  </svg>
                </div>
                <div className="min-w-0">
                  <p className="text-xs font-medium uppercase tracking-wide text-pos-muted">Ventas netas</p>
                  <p className="text-2xl font-bold text-pos-text tabular-nums">{money.format(ciVentasQ.data?.totalNeto || 0)}</p>
                </div>
              </div>
            </div>
            <div className="card p-5">
              <div className="flex items-center gap-3">
                <div className="flex h-11 w-11 shrink-0 items-center justify-center rounded-xl bg-pos-accentSoft">
                  <svg className="h-5 w-5 text-pos-forest" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
                    <path strokeLinecap="round" strokeLinejoin="round" d="M17 9V7a2 2 0 00-2-2H5a2 2 0 00-2 2v6a2 2 0 002 2h2m2 4h10a2 2 0 002-2v-6a2 2 0 00-2-2H9a2 2 0 00-2 2v6a2 2 0 002 2zm7-5a2 2 0 11-4 0 2 2 0 014 0z" />
                  </svg>
                </div>
                <div className="min-w-0">
                  <p className="text-xs font-medium uppercase tracking-wide text-pos-muted">Dinero recibido</p>
                  <p className="text-2xl font-bold text-pos-text tabular-nums">{money.format(ciRentQ.data?.recaudoReal || 0)}</p>
                  <p className="text-[10px] text-pos-muted/80">Incluye pagos de ventas y abonos a fiados.</p>
                </div>
              </div>
            </div>
            <div className="card p-5">
              <div className="flex items-center gap-3">
                <div className="flex h-11 w-11 shrink-0 items-center justify-center rounded-xl bg-red-100">
                  <svg className="h-5 w-5 text-red-600" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
                    <path strokeLinecap="round" strokeLinejoin="round" d="M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
                  </svg>
                </div>
                <div className="min-w-0">
                  <p className="text-xs font-medium uppercase tracking-wide text-pos-muted">Gastos</p>
                  <p className="text-2xl font-bold text-pos-text tabular-nums">{money.format(ciRentQ.data?.totalGastos || 0)}</p>
                </div>
              </div>
            </div>
            <div className="relative overflow-hidden rounded-2xl bg-[#50C4A0] p-5 shadow-pos">
              <div className="absolute right-2 top-2 opacity-10">
                <svg className="h-20 w-20 text-white" fill="currentColor" viewBox="0 0 24 24"><path d="M11.944 17.97L4.58 13.62 11.943 24l7.37-10.38-7.372 4.35h.003zM12.056 0L4.69 12.223l7.365 4.354 7.365-4.35L12.056 0z" /></svg>
              </div>
              <div className="relative">
                <div className="flex items-center gap-2">
                  <svg className="h-5 w-5 text-white" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
                    <path strokeLinecap="round" strokeLinejoin="round" d="M13 7h8m0 0v8m0-8l-8 8-4-4-6 6" />
                  </svg>
                  <p className="text-xs font-semibold uppercase tracking-wide text-white">Ganancia neta del mes</p>
                </div>
                <p className="mt-2 text-3xl font-bold text-white tabular-nums">{money.format(ciRentQ.data?.gananciaNeta || 0)}</p>
                <p className="mt-1 text-[11px] leading-relaxed tracking-wide text-white/80">Resultado final después de gastos y movimientos del período.</p>
              </div>
            </div>
          </div>

          {/* ── Fila 2 — Detalles operativos ────────────────────────── */}
          <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-4">
            <div className="rounded-xl border border-pos-border bg-pos-card p-3 shadow-sm">
              <p className="text-[11px] font-medium uppercase tracking-wider text-pos-muted">Ventas realizadas</p>
              <p className="mt-1 text-lg font-semibold text-pos-text tabular-nums">{ciVentasQ.data?.totalVentas ?? "—"}</p>
            </div>
            <div className="rounded-xl border border-pos-border bg-pos-card p-3 shadow-sm">
              <p className="text-[11px] font-medium uppercase tracking-wider text-pos-muted">Efectivo</p>
              <p className="mt-1 text-lg font-semibold text-pos-text tabular-nums">{money.format(ciRentQ.data?.totalVentasEfectivo || 0)}</p>
            </div>
            <div className="rounded-xl border border-pos-border bg-pos-card p-3 shadow-sm">
              <p className="text-[11px] font-medium uppercase tracking-wider text-pos-muted">Transferencia</p>
              <p className="mt-1 text-lg font-semibold text-pos-text tabular-nums">{money.format(ciRentQ.data?.totalVentasTransferencia || 0)}</p>
            </div>
            <div className="rounded-xl border border-pos-border bg-pos-card p-3 shadow-sm">
              <p className="text-[11px] font-medium uppercase tracking-wider text-pos-muted">Descuentos</p>
              <p className="mt-1 text-lg font-semibold text-pos-text tabular-nums">{money.format(ciVentasQ.data?.totalDescuentos || 0)}</p>
            </div>
          </div>

          {/* ── Fila 3 — Gráficos ───────────────────────────────────── */}
          <div className="grid gap-4 md:grid-cols-2">
            <div className="card p-5">
              <h3 className="mb-1 text-sm font-semibold text-pos-text">Distribución de ingresos</h3>
              <p className="mb-4 text-xs text-pos-muted">Método de pago utilizado en el período</p>
              {ciPayChart ? (
                <div className="mx-auto w-full max-w-[220px]">
                  <Doughnut data={ciPayChart} />
                </div>
              ) : ciRentQ.isLoading ? (
                <p className="py-8 text-center text-sm text-pos-muted">Cargando...</p>
              ) : (
                <p className="py-8 text-center text-sm text-pos-muted">Sin datos</p>
              )}
            </div>
            <div className="card p-5">
              <h3 className="mb-1 text-sm font-semibold text-pos-text">Flujo del mes</h3>
              <p className="mb-4 text-xs text-pos-muted">Comparación de ingresos, gastos y ganancia</p>
              {ciFlowChart ? (
                <Bar data={ciFlowChart} />
              ) : ciRentQ.isLoading ? (
                <p className="py-8 text-center text-sm text-pos-muted">Cargando...</p>
              ) : (
                <p className="py-8 text-center text-sm text-pos-muted">Sin datos</p>
              )}
            </div>
          </div>

          {/* ── Fila 4 — Turnos ─────────────────────────────────────── */}
          {ciTurnosQ.data && ciTurnosQ.data.length > 0 && (
            <div className="card p-4">
              <p className="mb-3 text-sm font-semibold text-pos-text">Resumen operativo por turnos</p>
              <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-5">
                <div>
                  <p className="text-[11px] font-medium uppercase tracking-wider text-pos-muted">Turnos</p>
                  <p className="mt-0.5 text-lg font-semibold text-pos-text tabular-nums">{ciTurnosQ.data.length}</p>
                </div>
                <div>
                  <p className="text-[11px] font-medium uppercase tracking-wider text-pos-muted">Recaudo operativo</p>
                  <p className="mt-0.5 text-lg font-semibold text-pos-text tabular-nums">{money.format(ciTurnosQ.data.reduce((a, t) => a + Number(t.recaudoBruto || 0), 0))}</p>
                </div>
                <div>
                  <p className="text-[11px] font-medium uppercase tracking-wider text-pos-muted">Gastos operativos</p>
                  <p className="mt-0.5 text-lg font-semibold text-pos-text tabular-nums">{money.format(ciTurnosQ.data.reduce((a, t) => a + Number(t.totalGastosCombinados || 0), 0))}</p>
                </div>
                <div>
                  <p className="text-[11px] font-medium uppercase tracking-wider text-pos-muted">Total operativo</p>
                  <p className="mt-0.5 text-lg font-semibold text-pos-text tabular-nums">{money.format(ciTurnosQ.data.reduce((a, t) => a + Number(t.totalOperativoNeto || 0), 0))}</p>
                </div>
                <div>
                  <p className="text-[11px] font-medium uppercase tracking-wider text-pos-muted">Diferencia caja</p>
                  <p className="mt-0.5 text-lg font-semibold text-pos-text tabular-nums" style={{ color: ciTurnosQ.data.reduce((a, t) => a + Number(t.diferenciaTotal || 0), 0) === 0 ? "#3EB489" : "#dc2626" }}>
                    {money.format(ciTurnosQ.data.reduce((a, t) => a + Number(t.diferenciaTotal || 0), 0))}
                  </p>
                </div>
              </div>
            </div>
          )}
        </>
      )}

      {tab === "Turnos" && (
        <>
          <div className="grid grid-cols-2 gap-3 md:grid-cols-5">
            <div className="rounded-xl border border-slate-100 bg-white p-4 shadow-sm">
              <p className="text-[11px] font-medium uppercase tracking-wider text-slate-400">Turnos</p>
              <p className="mt-1 text-2xl font-semibold text-slate-800">{sortedTurnos.length}</p>
            </div>
            <div className="rounded-xl border border-slate-100 bg-white p-4 shadow-sm">
              <p className="text-[11px] font-medium uppercase tracking-wider text-slate-400">Recaudo real</p>
              <p className="mt-1 text-2xl font-semibold text-slate-800">{money.format(sortedTurnos.reduce((acc, t) => acc + Number(t.recaudoBruto || 0), 0))}</p>
            </div>
            <div className="rounded-xl border border-slate-100 bg-white p-4 shadow-sm">
              <p className="text-[11px] font-medium uppercase tracking-wider text-slate-400">Gastos caja</p>
              <p className="mt-1 text-2xl font-semibold text-slate-800">{money.format(sortedTurnos.reduce((acc, t) => acc + Number(t.totalGastos || 0), 0))}</p>
            </div>
            <div className="rounded-xl border border-slate-100 bg-white p-4 shadow-sm">
              <p className="text-[11px] font-medium uppercase tracking-wider text-slate-400">Gastos admin</p>
              <p className="mt-1 text-2xl font-semibold text-amber-700">{money.format(sortedTurnos.reduce((acc, t) => acc + Number(t.totalGastosAdmin || 0), 0))}</p>
            </div>
            <div className="rounded-xl border border-emerald-100 bg-emerald-50 p-4 shadow-sm">
              <p className="text-[11px] font-medium uppercase tracking-wider text-emerald-600">Ganancia neta</p>
              <p className="mt-1 text-2xl font-semibold text-emerald-800">{money.format(sortedTurnos.reduce((acc, t) => acc + Number(t.gananciaNeta || 0), 0))}</p>
              <p className="text-[10px] text-emerald-600 mt-0.5">Recaudo − caja − admin</p>
            </div>
          </div>

          <div className="rounded-xl border border-slate-200 bg-white shadow-sm">
            <div className="flex items-center justify-between border-b border-slate-100 px-5 py-3">
              <p className="text-sm font-medium text-slate-700">Histórico de turnos</p>
              <div className="min-w-0 flex-1 sm:max-w-xs">
                <DateRangePicker fi={fi} ff={ff} onRangeChange={(newFi, newFf) => { setFi(newFi); setFf(newFf); }} />
              </div>
            </div>
            {turnosQ.isLoading && (
              <div className="flex items-center justify-center px-5 py-12">
                <div className="flex items-center gap-2 text-sm text-slate-400">
                  <svg className="h-4 w-4 animate-spin" fill="none" viewBox="0 0 24 24"><circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" /><path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z" /></svg>
                  Cargando turnos...
                </div>
              </div>
            )}
            {!turnosQ.isLoading && sortedTurnos.length === 0 && (
              <div className="px-5 py-12 text-center text-sm text-slate-400">No hay turnos en ese rango.</div>
            )}
            {sortedTurnos.length > 0 && (
              <>
                <div className="grid gap-2 p-4 md:hidden">
                  {sortedTurnos.map((t, i) => {
                    const d = Number(t.diferenciaTotal || 0);
                    const dClass = d === 0 ? "text-emerald-600" : d < 0 ? "text-red-500" : "text-amber-500";
                    return (
                      <div key={t.id} className="rounded-xl border border-slate-100 bg-white p-4 text-sm shadow-sm">
                        <div className="flex items-center justify-between">
                          <span className="font-semibold text-slate-800">Turno #{i + 1}</span>
                          <span className={`rounded-md px-2 py-0.5 text-[10px] font-medium uppercase tracking-wide ${t.estado === "CERRADO" ? "bg-emerald-50 text-emerald-700" : t.estado === "ABIERTO" ? "bg-blue-50 text-blue-700" : "bg-amber-50 text-amber-700"}`}>{t.estado}</span>
                        </div>
                        <p className="mt-1 text-xs text-slate-400">{new Date(t.fechaApertura).toLocaleString()}{t.fechaCierre ? ` → ${new Date(t.fechaCierre).toLocaleString()}` : ""} · {t.usuario}</p>
                        <div className="mt-3 grid grid-cols-2 gap-2">
                          <div className="rounded-lg bg-slate-50 p-2">
                            <p className="text-[10px] uppercase tracking-wide text-slate-400">Recaudo</p>
                            <p className="font-medium text-slate-700">{money.format(t.recaudoBruto || 0)}</p>
                          </div>
                          <div className="rounded-lg bg-slate-50 p-2">
                            <p className="text-[10px] uppercase tracking-wide text-slate-400">Gastos</p>
                            <p className="font-medium text-slate-700">{money.format(t.totalGastos || 0)}</p>
                          </div>
                          <div className="rounded-lg bg-slate-50 p-2">
                            <p className="text-[10px] uppercase tracking-wide text-slate-400">Efectivo</p>
                            <p className="font-medium text-slate-700">{money.format(t.efectivoOperativo || 0)}</p>
                          </div>
                          <div className="rounded-lg bg-slate-50 p-2">
                            <p className="text-[10px] uppercase tracking-wide text-slate-400">Transf.</p>
                            <p className="font-medium text-slate-700">{money.format(t.transferenciasOperativas || 0)}</p>
                          </div>
                        </div>
                        <div className="mt-2 flex items-center justify-between rounded-lg bg-slate-50 p-2">
                          <span className="text-[10px] uppercase tracking-wide text-slate-400">Total operativo</span>
                          <span className="font-semibold text-slate-800">{money.format(t.totalOperativoNeto || 0)}</span>
                        </div>
                        <div className="mt-1 flex items-center justify-between rounded-lg p-2">
                          <span className="text-[10px] uppercase tracking-wide text-slate-400">Diferencia</span>
                          <span className={`font-semibold ${dClass}`}>{money.format(d)}</span>
                        </div>
                      </div>
                    );
                  })}
                </div>

                <div className="hidden overflow-x-auto md:block">
                  <table className="w-full text-sm">
                    <thead>
                      <tr className="border-b border-slate-100">
                        <th className="px-4 py-3 text-left text-[11px] font-medium uppercase tracking-wider text-slate-400">Turno</th>
                        <th className="px-4 py-3 text-left text-[11px] font-medium uppercase tracking-wider text-slate-400">Apertura</th>
                        <th className="px-4 py-3 text-left text-[11px] font-medium uppercase tracking-wider text-slate-400">Cierre</th>
                        <th className="px-4 py-3 text-left text-[11px] font-medium uppercase tracking-wider text-slate-300">Usuario</th>
                        <th className="px-4 py-3 text-left text-[11px] font-medium uppercase tracking-wider text-slate-400">Estado</th>
                        <th className="px-4 py-3 text-right text-[11px] font-medium uppercase tracking-wider text-slate-400">Recaudo</th>
                        <th className="px-4 py-3 text-right text-[11px] font-medium uppercase tracking-wider text-slate-400">G. Caja</th>
                        <th className="px-4 py-3 text-right text-[11px] font-medium uppercase tracking-wider text-amber-500">G. Admin</th>
                        <th className="px-4 py-3 text-right text-[11px] font-medium uppercase tracking-wider text-slate-400">Efectivo</th>
                        <th className="px-4 py-3 text-right text-[11px] font-medium uppercase tracking-wider text-slate-400">Transf.</th>
                        <th className="px-4 py-3 text-right text-[11px] font-medium uppercase tracking-wider text-emerald-600">Ganancia</th>
                        <th className="px-4 py-3 text-right text-[11px] font-medium uppercase tracking-wider text-slate-600">Diferencia</th>
                      </tr>
                    </thead>
                    <tbody>
                      {sortedTurnos.map((t, i) => {
                        const dif = Number(t.diferenciaTotal || 0);
                        const difClass = dif === 0 ? "text-emerald-600" : dif < 0 ? "text-red-500" : "text-amber-500";
                        return (
                          <tr key={t.id} className="border-b border-slate-50 transition-colors hover:bg-slate-50/80">
                            <td className="px-4 py-3 font-medium text-slate-800">#{i + 1}</td>
                            <td className="px-4 py-3 text-xs text-slate-500">{new Date(t.fechaApertura).toLocaleString()}</td>
                            <td className="px-4 py-3 text-xs text-slate-500">{t.fechaCierre ? new Date(t.fechaCierre).toLocaleString() : "—"}</td>
                            <td className="px-4 py-3 text-xs text-slate-400">{t.usuario}</td>
                            <td className="px-4 py-3">
                              <span className={`inline-block rounded-md px-2 py-0.5 text-[10px] font-medium ${t.estado === "CERRADO" ? "bg-emerald-50 text-emerald-700" : t.estado === "ABIERTO" ? "bg-blue-50 text-blue-700" : "bg-amber-50 text-amber-700"}`}>{t.estado}</span>
                            </td>
                            <td className="px-4 py-3 text-right tabular-nums text-slate-700">{money.format(t.recaudoBruto || 0)}</td>
                            <td className="px-4 py-3 text-right tabular-nums text-slate-700">{money.format(t.totalGastos || 0)}</td>
                            <td className="px-4 py-3 text-right tabular-nums text-amber-700 font-medium">{money.format(t.totalGastosAdmin || 0)}</td>
                            <td className="px-4 py-3 text-right tabular-nums text-slate-600">{money.format(t.efectivoOperativo || 0)}</td>
                            <td className="px-4 py-3 text-right tabular-nums text-slate-600">{money.format(t.transferenciasOperativas || 0)}</td>
                            <td className="px-4 py-3 text-right tabular-nums font-semibold text-emerald-700">{money.format(t.gananciaNeta || 0)}</td>
                            <td className={`px-4 py-3 text-right tabular-nums font-semibold ${difClass}`}>{money.format(dif)}</td>
                          </tr>
                        );
                      })}
                    </tbody>
                  </table>
                </div>
              </>
            )}
          </div>
        </>
      )}
    </div>
  );
}
