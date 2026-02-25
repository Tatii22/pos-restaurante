import { useMemo, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { posApi } from "../shared/api/posApi";
import { useTurnoStore } from "../shared/store/turnoStore";
import { useAuthStore } from "../shared/store/authStore";
import { getErrorMessage, money } from "../shared/utils";

function todayYmd() {
  return new Date().toISOString().slice(0, 10);
}

function monthStartYmd() {
  const d = new Date();
  d.setDate(1);
  return d.toISOString().slice(0, 10);
}

export function GastosPage() {
  const qc = useQueryClient();
  const { role } = useAuthStore();
  const { isAbierto, setTurno } = useTurnoStore();
  const [descripcion, setDescripcion] = useState("");
  const [monto, setMonto] = useState("0");
  const [tipoGastoId, setTipoGastoId] = useState<number | "">("");
  const [fechaAdmin, setFechaAdmin] = useState(() => todayYmd());
  const [fechaInicio, setFechaInicio] = useState(() => monthStartYmd());
  const [fechaFin, setFechaFin] = useState(() => todayYmd());
  const esAdmin = role === "ADMIN";

  const tiposQ = useQuery({
    queryKey: ["tipos-gasto"],
    queryFn: () => posApi.getTiposGasto()
  });
  const gastosQ = useQuery({
    queryKey: ["gastos-caja-rango", fechaInicio, fechaFin],
    queryFn: () => posApi.getGastosCajaByRango(fechaInicio, fechaFin),
    enabled: true
  });
  const gastosAdminQ = useQuery({
    queryKey: ["gastos-admin-rango", fechaInicio, fechaFin],
    queryFn: () => posApi.getGastosAdminByRango(fechaInicio, fechaFin),
    enabled: esAdmin
  });

  const createM = useMutation({
    mutationFn: () =>
      posApi.registrarGastoCaja({
        descripcion: descripcion.trim(),
        monto: Number(monto),
        tipoGastoId: Number(tipoGastoId)
      }),
    onSuccess: () => {
      setDescripcion("");
      setMonto("0");
      setTipoGastoId("");
      qc.invalidateQueries({ queryKey: ["turno-activo-layout"] });
      qc.invalidateQueries({ queryKey: ["historial-turno-ventas"] });
      qc.invalidateQueries({ queryKey: ["gastos-caja-rango"] });
      posApi.getTurnoActivo().then((t) => setTurno(t)).catch(() => {});
    }
  });
  const createAdminM = useMutation({
    mutationFn: () =>
      posApi.registrarGastoAdmin({
        fecha: fechaAdmin,
        descripcion: descripcion.trim(),
        monto: Number(monto),
        tipoGastoId: Number(tipoGastoId)
      }),
    onSuccess: () => {
      setDescripcion("");
      setMonto("0");
      setTipoGastoId("");
      qc.invalidateQueries({ queryKey: ["gastos-admin-rango"] });
      qc.invalidateQueries({ queryKey: ["gastos-caja-rango"] });
    }
  });
  const deleteM = useMutation({
    mutationFn: (payload: { id: number; origen: "CAJA" | "ADMIN" }) =>
      payload.origen === "CAJA" ? posApi.eliminarGastoCaja(payload.id) : posApi.eliminarGastoAdmin(payload.id),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["gastos-admin-rango"] });
      qc.invalidateQueries({ queryKey: ["gastos-caja-rango"] });
      qc.invalidateQueries({ queryKey: ["dashboard-rent-month"] });
      qc.invalidateQueries({ queryKey: ["reportes-rentabilidad"] });
      qc.invalidateQueries({ queryKey: ["reportes-rentabilidad-mes"] });
    }
  });

  const disabled = esAdmin ? false : !isAbierto();
  const registrarPending = esAdmin ? createAdminM.isPending : createM.isPending;
  const registrarError = esAdmin ? createAdminM.error : createM.error;
  const historialLoading = esAdmin ? gastosAdminQ.isLoading || gastosQ.isLoading : gastosQ.isLoading;
  const historialData = useMemo(() => {
    if (!esAdmin) {
      return (gastosQ.data || []).map((g) => ({
        id: g.id,
        fecha: g.fecha,
        descripcion: g.descripcion,
        monto: Number(g.valor || 0),
        tipoNombre: "-",
        origen: "CAJA" as const
      }));
    }
    const caja = (gastosQ.data || []).map((g) => ({
      id: g.id,
      fecha: g.fecha,
      descripcion: g.descripcion,
      monto: Number(g.valor || 0),
      tipoNombre: "-",
      origen: "CAJA" as const
    }));
    const admin = (gastosAdminQ.data || []).map((g) => ({
      id: g.id,
      fecha: g.fecha,
      descripcion: g.descripcion,
      monto: Number(g.monto || 0),
      tipoNombre: g.tipo?.nombre || "-",
      origen: "ADMIN" as const
    }));
    return [...caja, ...admin].sort(
      (a, b) => new Date(b.fecha).getTime() - new Date(a.fecha).getTime()
    );
  }, [esAdmin, gastosQ.data, gastosAdminQ.data]);
  const historialError = esAdmin ? gastosAdminQ.error || gastosQ.error : gastosQ.error;

  return (
    <div className="mx-auto grid w-full max-w-4xl gap-4">
      <h2 className="text-2xl font-semibold">{esAdmin ? "Gastos Administrativos" : "Gastos de Caja"}</h2>

      {!esAdmin && !isAbierto() && (
        <div className="rounded-xl border border-yellow-300 bg-yellow-50 p-3 text-sm text-yellow-800">
          Solo puedes registrar gastos con turno en estado ABIERTO.
        </div>
      )}
      {esAdmin && (
        <div className="rounded-xl border border-blue-200 bg-blue-50 p-3 text-sm text-blue-800">
          Los gastos de ADMIN se registran por fecha y no afectan turnos de caja.
        </div>
      )}

      <div className="card p-4">
        <div className="grid gap-3">
          {esAdmin && (
            <label className="text-sm">
              Fecha
              <input
                className="input mt-1"
                type="date"
                value={fechaAdmin}
                onChange={(e) => setFechaAdmin(e.target.value)}
              />
            </label>
          )}
          <label className="text-sm">
            Tipo de gasto
            <select
              className="input mt-1"
              value={tipoGastoId}
              onChange={(e) => setTipoGastoId(e.target.value ? Number(e.target.value) : "")}
              disabled={disabled || tiposQ.isLoading}
            >
              <option value="">Selecciona...</option>
              {(tiposQ.data || []).map((t) => (
                <option key={t.id} value={t.id}>
                  {t.nombre}
                </option>
              ))}
            </select>
          </label>

          <label className="text-sm">
            Monto
            <input
              className="input mt-1"
              value={monto}
              onChange={(e) => setMonto(e.target.value)}
              inputMode="decimal"
              disabled={disabled}
            />
          </label>

          <label className="text-sm">
            Concepto / Descripcion
            <input
              className="input mt-1"
              value={descripcion}
              onChange={(e) => setDescripcion(e.target.value)}
              disabled={disabled}
            />
          </label>

          <button
            className="btn-primary"
            onClick={() => (esAdmin ? createAdminM.mutate() : createM.mutate())}
            disabled={disabled || registrarPending || !descripcion.trim() || !Number(monto) || !tipoGastoId}
          >
            {registrarPending ? "Registrando..." : "Registrar Gasto"}
          </button>
        </div>
      </div>

      <div className="card p-4">
        <h3 className="mb-3 text-lg font-semibold">
          {esAdmin ? "Historial global de gastos (ADMIN + CAJA)" : "Historial de gastos de caja"}
        </h3>
        <div className="mb-3 grid gap-2 md:grid-cols-2">
          <label className="text-sm">
            Fecha inicio
            <input className="input mt-1" type="date" value={fechaInicio} onChange={(e) => setFechaInicio(e.target.value)} />
          </label>
          <label className="text-sm">
            Fecha fin
            <input className="input mt-1" type="date" value={fechaFin} onChange={(e) => setFechaFin(e.target.value)} />
          </label>
        </div>
        {historialLoading && (
          <p className="text-sm text-pos-muted">Cargando gastos...</p>
        )}
        {!historialLoading && historialData.length === 0 && (
          <p className="text-sm text-pos-muted">
            {esAdmin ? "Aun no hay gastos administrativos en ese rango." : "Aun no hay gastos de caja en ese rango."}
          </p>
        )}
        {historialData.length > 0 && (
          <>
            <div className="grid gap-2 md:hidden">
              {historialData.map((g) => (
                <div key={`${g.origen}-${g.id}`} className="rounded-xl border border-pos-border p-3">
                  <p className="text-xs text-pos-muted">{new Date(g.fecha).toLocaleString()}</p>
                  {esAdmin && <p className="text-xs">Origen: {g.origen}</p>}
                  {esAdmin && <p className="text-xs">Tipo: {g.tipoNombre}</p>}
                  <p className="font-semibold text-red-700">{money.format(g.monto)}</p>
                  <p className="text-sm">{g.descripcion}</p>
                  {esAdmin && (
                    <div className="mt-2">
                      <button
                        className="btn-ghost bg-red-100 text-red-700 hover:bg-red-200"
                        onClick={() => deleteM.mutate({ id: g.id, origen: g.origen })}
                        disabled={deleteM.isPending}
                      >
                        Eliminar
                      </button>
                    </div>
                  )}
                </div>
              ))}
            </div>
            <div className="hidden overflow-x-auto md:block">
              <table className="w-full min-w-[520px] text-sm">
              <thead>
                <tr className="border-b border-pos-border">
                  <th className="p-2 text-left">Fecha</th>
                  {esAdmin && <th className="p-2 text-left">Origen</th>}
                  {esAdmin && <th className="p-2 text-left">Tipo de gasto</th>}
                  <th className="p-2 text-left">Monto</th>
                  <th className="p-2 text-left">Concepto / Descripcion</th>
                  {esAdmin && <th className="p-2 text-left">Acciones</th>}
                </tr>
              </thead>
              <tbody>
                {historialData.map((g) => (
                  <tr key={`${g.origen}-${g.id}`} className="border-b border-pos-border/70">
                    <td className="p-2">{new Date(g.fecha).toLocaleString()}</td>
                    {esAdmin && <td className="p-2">{g.origen}</td>}
                    {esAdmin && <td className="p-2">{g.tipoNombre}</td>}
                    <td className="p-2 font-semibold text-red-700">{money.format(g.monto)}</td>
                    <td className="p-2">{g.descripcion}</td>
                    {esAdmin && (
                      <td className="p-2 whitespace-nowrap">
                        <button
                          className="btn-ghost bg-red-100 text-red-700 hover:bg-red-200"
                          onClick={() => deleteM.mutate({ id: g.id, origen: g.origen })}
                          disabled={deleteM.isPending}
                        >
                          Eliminar
                        </button>
                      </td>
                    )}
                  </tr>
                ))}
              </tbody>
              </table>
            </div>
          </>
        )}
      </div>

      {(tiposQ.isError || registrarError || historialError || deleteM.isError) && (
        <p className="text-sm text-red-600">{getErrorMessage(tiposQ.error || registrarError || historialError || deleteM.error)}</p>
      )}
    </div>
  );
}
