import { useMemo, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { posApi } from "../shared/api/posApi";
import { getErrorMessage, money } from "../shared/utils";
import { useAuthStore } from "../shared/store/authStore";

export function MenuDiaPage() {
  const { role } = useAuthStore();
  const qc = useQueryClient();
  const [productoId, setProductoId] = useState<number | "">("");
  const [stockInicial, setStockInicial] = useState("10");

  const productosQ = useQuery({ queryKey: ["productos-menu-dia"], queryFn: () => posApi.getProductos() });
  const inventarioQ = useQuery({ queryKey: ["inventario-dia"], queryFn: () => posApi.getInventarioDiario(), retry: false });

  const menuActivo = inventarioQ.isSuccess;

  const crearMenuM = useMutation({
    mutationFn: () => posApi.crearMenuDiario(),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["inventario-dia"] });
      qc.invalidateQueries({ queryKey: ["catalogo-hoy"] });
    }
  });

  const agregarM = useMutation({
    mutationFn: () => posApi.crearInventarioDiario(Number(productoId), Number(stockInicial)),
    onSuccess: () => {
      setProductoId("");
      setStockInicial("10");
      qc.invalidateQueries({ queryKey: ["inventario-dia"] });
      qc.invalidateQueries({ queryKey: ["catalogo-hoy"] });
    }
  });

  const menuProductos = useMemo(() => {
    const productos = productosQ.data || [];
    return productos.filter((p) => p.activo && p.tipoVenta === "MENU_DIARIO");
  }, [productosQ.data]);

  const disponiblesParaAgregar = useMemo(() => {
    const usados = new Set((inventarioQ.data || []).map((i) => i.producto));
    return menuProductos.filter((p) => !usados.has(p.nombre));
  }, [menuProductos, inventarioQ.data]);

  if (productosQ.isError) return <p className="text-sm text-red-600">{getErrorMessage(productosQ.error)}</p>;

  return (
    <div className="grid gap-4">
      <h2 className="text-2xl font-semibold">Menu del Dia</h2>
      {role !== "CAJA" && (
        <div className="rounded-xl border border-yellow-300 bg-yellow-50 p-3 text-sm text-yellow-800">
          Solo CAJA puede crear menu y agregar productos en backend.
        </div>
      )}

      <div className="card p-4">
        <div className="mb-3 flex items-center justify-between">
          <div>
            <p className="text-sm text-pos-muted">Estado del menu de hoy</p>
            <p className="text-lg font-semibold">{menuActivo ? "ACTIVO" : "NO CREADO"}</p>
          </div>
          {!menuActivo && role === "CAJA" && (
            <button className="btn-primary" onClick={() => crearMenuM.mutate()} disabled={crearMenuM.isPending}>
              {crearMenuM.isPending ? "Creando..." : "Crear Menu del Dia"}
            </button>
          )}
        </div>

        {menuActivo && role === "CAJA" && (
          <div className="mb-4 grid gap-2 rounded-xl border border-pos-border bg-gray-50 p-3 md:grid-cols-[1fr_160px_140px]">
            <select
              className="input"
              value={productoId}
              onChange={(e) => setProductoId(e.target.value ? Number(e.target.value) : "")}
            >
              <option value="">Selecciona producto MENU_DIARIO...</option>
              {disponiblesParaAgregar.map((p) => (
                <option key={p.id} value={p.id}>
                  {p.nombre} - {money.format(p.precio)}
                </option>
              ))}
            </select>
            <input
              className="input"
              inputMode="numeric"
              value={stockInicial}
              onChange={(e) => setStockInicial(e.target.value)}
              placeholder="Stock inicial"
            />
            <button
              className="btn-soft"
              onClick={() => agregarM.mutate()}
              disabled={agregarM.isPending || !productoId || Number(stockInicial) <= 0}
            >
              Agregar
            </button>
          </div>
        )}

        <div className="grid gap-2 md:grid-cols-2 xl:grid-cols-3">
          {(inventarioQ.data || []).map((i) => (
            <div key={i.id} className="rounded-xl border border-pos-border bg-white p-3">
              <p className="font-semibold">{i.producto}</p>
              <p className="text-sm text-pos-muted">Stock inicial: {i.stockInicial}</p>
              <p className="text-sm text-pos-muted">Disponible: {i.stockActual}</p>
              <p className={i.agotado ? "text-xs text-red-600" : "text-xs text-green-700"}>
                {i.agotado ? "Agotado" : "Disponible"}
              </p>
            </div>
          ))}
        </div>

        {!inventarioQ.isLoading && (inventarioQ.data || []).length === 0 && (
          <p className="mt-2 text-sm text-pos-muted">No hay productos agregados al menu de hoy.</p>
        )}

        {(crearMenuM.isError || agregarM.isError || (inventarioQ.isError && role === "CAJA")) && (
          <p className="mt-3 text-sm text-red-600">
            {getErrorMessage(crearMenuM.error || agregarM.error || inventarioQ.error)}
          </p>
        )}
      </div>
    </div>
  );
}

