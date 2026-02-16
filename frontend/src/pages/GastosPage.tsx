import { useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { posApi } from "../shared/api/posApi";
import { useTurnoStore } from "../shared/store/turnoStore";
import { getErrorMessage, money } from "../shared/utils";

export function GastosPage() {
  const qc = useQueryClient();
  const { isAbierto, setTurno } = useTurnoStore();
  const [descripcion, setDescripcion] = useState("");
  const [monto, setMonto] = useState("0");
  const [tipoGastoId, setTipoGastoId] = useState<number | "">("");

  const tiposQ = useQuery({
    queryKey: ["tipos-gasto"],
    queryFn: () => posApi.getTiposGasto()
  });
  const gastosQ = useQuery({
    queryKey: ["gastos-caja"],
    queryFn: () => posApi.getGastosCaja(),
    enabled: isAbierto()
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
      qc.invalidateQueries({ queryKey: ["gastos-caja"] });
      posApi.getTurnoActivo().then((t) => setTurno(t)).catch(() => {});
    }
  });

  const disabled = !isAbierto();

  return (
    <div className="mx-auto grid w-full max-w-4xl gap-4">
      <h2 className="text-2xl font-semibold">Gastos de Caja</h2>

      {!isAbierto() && (
        <div className="rounded-xl border border-yellow-300 bg-yellow-50 p-3 text-sm text-yellow-800">
          Solo puedes registrar gastos con turno en estado ABIERTO.
        </div>
      )}

      <div className="card p-4">
        <div className="grid gap-3">
          <label className="text-sm">
            Concepto
            <input
              className="input mt-1"
              value={descripcion}
              onChange={(e) => setDescripcion(e.target.value)}
              disabled={disabled}
            />
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

          <button
            className="btn-primary"
            onClick={() => createM.mutate()}
            disabled={disabled || createM.isPending || !descripcion.trim() || !Number(monto) || !tipoGastoId}
          >
            {createM.isPending ? "Registrando..." : "Registrar Gasto"}
          </button>
        </div>
      </div>

      <div className="card p-4">
        <h3 className="mb-3 text-lg font-semibold">Historial de gastos del turno</h3>
        {!isAbierto() && <p className="text-sm text-pos-muted">Abre turno para ver el historial de gastos.</p>}
        {isAbierto() && gastosQ.isLoading && <p className="text-sm text-pos-muted">Cargando gastos...</p>}
        {isAbierto() && !gastosQ.isLoading && (gastosQ.data?.length || 0) === 0 && (
          <p className="text-sm text-pos-muted">Aun no hay gastos registrados en este turno.</p>
        )}
        {isAbierto() && (gastosQ.data?.length || 0) > 0 && (
          <div className="overflow-auto">
            <table className="w-full min-w-[520px] text-sm">
              <thead>
                <tr className="border-b border-pos-border">
                  <th className="p-2 text-left">Fecha</th>
                  <th className="p-2 text-left">Descripcion</th>
                  <th className="p-2 text-left">Valor</th>
                </tr>
              </thead>
              <tbody>
                {(gastosQ.data || []).map((g) => (
                  <tr key={g.id} className="border-b border-pos-border/70">
                    <td className="p-2">{new Date(g.fecha).toLocaleString()}</td>
                    <td className="p-2">{g.descripcion}</td>
                    <td className="p-2 font-semibold text-red-700">{money.format(g.valor)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {(tiposQ.isError || createM.isError || gastosQ.isError) && (
        <p className="text-sm text-red-600">{getErrorMessage(tiposQ.error || createM.error || gastosQ.error)}</p>
      )}
    </div>
  );
}
