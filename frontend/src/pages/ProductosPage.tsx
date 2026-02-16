import { FormEvent, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { posApi } from "../shared/api/posApi";
import { getErrorMessage, money } from "../shared/utils";

export function ProductosPage() {
  const qc = useQueryClient();
  const productsQ = useQuery({ queryKey: ["productos"], queryFn: () => posApi.getProductos() });
  const categoriesQ = useQuery({ queryKey: ["categorias"], queryFn: () => posApi.getCategorias() });

  const [form, setForm] = useState({
    nombre: "",
    precio: "",
    categoriaId: "",
    tipoVenta: "SIEMPRE_DISPONIBLE",
    activo: true
  });

  const createM = useMutation({
    mutationFn: () =>
      posApi.crearProducto({
        nombre: form.nombre,
        precio: Number(form.precio),
        categoriaId: Number(form.categoriaId),
        tipoVenta: form.tipoVenta,
        activo: form.activo
      }),
    onSuccess: () => {
      setForm({ nombre: "", precio: "", categoriaId: "", tipoVenta: "SIEMPRE_DISPONIBLE", activo: true });
      qc.invalidateQueries({ queryKey: ["productos"] });
    }
  });
  const createCatM = useMutation({
    mutationFn: (name: string) => posApi.crearCategoria({ nombre: name, activa: true }),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["categorias"] })
  });
  const toggleCatM = useMutation({
    mutationFn: (payload: { id: number; nombre: string; activa: boolean }) =>
      posApi.actualizarCategoria(payload.id, { nombre: payload.nombre, activa: payload.activa }),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["categorias"] })
  });
  const delCatM = useMutation({
    mutationFn: (id: number) => posApi.eliminarCategoria(id),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["categorias"] })
  });
  const [newCategoria, setNewCategoria] = useState("");

  const toggleM = useMutation({
    mutationFn: (payload: { id: number; name: string; price: number; categoryId: number; active: boolean }) =>
      posApi.actualizarProducto(payload.id, {
        nombre: payload.name,
        precio: payload.price,
        categoriaId: payload.categoryId,
        tipoVenta: "SIEMPRE_DISPONIBLE",
        activo: payload.active
      }),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["productos"] })
  });

  const deleteM = useMutation({
    mutationFn: (id: number) => posApi.eliminarProducto(id),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["productos"] })
  });

  function submit(e: FormEvent) {
    e.preventDefault();
    createM.mutate();
  }

  return (
    <div className="grid gap-4">
      <h2 className="text-2xl font-semibold">Productos</h2>
      <form className="card grid gap-3 p-4 md:grid-cols-5" onSubmit={submit}>
        <input className="input" placeholder="Nombre" value={form.nombre} onChange={(e) => setForm({ ...form, nombre: e.target.value })} required />
        <input className="input" placeholder="Precio" type="number" value={form.precio} onChange={(e) => setForm({ ...form, precio: e.target.value })} required />
        <select className="input" value={form.categoriaId} onChange={(e) => setForm({ ...form, categoriaId: e.target.value })} required>
          <option value="">Categoría</option>
          {(categoriesQ.data || []).map((c) => (
            <option key={c.id} value={c.id}>{c.nombre}</option>
          ))}
        </select>
        <select className="input" value={form.tipoVenta} onChange={(e) => setForm({ ...form, tipoVenta: e.target.value })}>
          <option value="MENU_DIARIO">MENU_DIARIO</option>
          <option value="SIEMPRE_DISPONIBLE">SIEMPRE_DISPONIBLE</option>
        </select>
        <button className="btn-primary">Crear</button>
      </form>

      <div className="card overflow-auto">
        <table className="w-full min-w-[860px] text-sm">
          <thead>
            <tr className="border-b border-pos-border">
              <th className="p-3 text-left">ID</th>
              <th className="p-3 text-left">Nombre</th>
              <th className="p-3 text-left">Precio</th>
              <th className="p-3 text-left">Categoría</th>
              <th className="p-3 text-left">Activo</th>
              <th className="p-3 text-left">Acciones</th>
            </tr>
          </thead>
          <tbody>
            {(productsQ.data || []).map((p) => (
              <tr key={p.id} className="border-b border-pos-border/70">
                <td className="p-3">{p.id}</td>
                <td className="p-3">{p.nombre}</td>
                <td className="p-3">{money.format(p.precio)}</td>
                <td className="p-3">{p.categoriaNombre}</td>
                <td className="p-3">{p.activo ? "Sí" : "No"}</td>
                <td className="p-3">
                  <div className="flex gap-2">
                    <button
                      className="btn-ghost"
                      onClick={() =>
                        toggleM.mutate({
                          id: p.id,
                          name: p.nombre,
                          price: p.precio,
                          categoryId: p.categoriaId,
                          active: !p.activo
                        })
                      }
                    >
                      Toggle Activo
                    </button>
                    <button className="btn-ghost" onClick={() => deleteM.mutate(p.id)}>
                      Eliminar
                    </button>
                  </div>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
      <div className="card p-4">
        <h3 className="mb-3 text-lg font-semibold">Categorías (CRUD básico)</h3>
        <div className="mb-3 flex gap-2">
          <input className="input max-w-sm" placeholder="Nueva categoría" value={newCategoria} onChange={(e) => setNewCategoria(e.target.value)} />
          <button className="btn-primary" onClick={() => newCategoria.trim() && createCatM.mutate(newCategoria.trim())}>
            Crear categoría
          </button>
        </div>
        <div className="grid gap-2 md:grid-cols-2">
          {(categoriesQ.data || []).map((c) => (
            <div key={c.id} className="flex items-center justify-between rounded-xl border border-pos-border p-2">
              <span>{c.nombre} ({c.activa ? "activa" : "inactiva"})</span>
              <div className="flex gap-2">
                <button className="btn-ghost" onClick={() => toggleCatM.mutate({ id: c.id, nombre: c.nombre, activa: !c.activa })}>
                  Toggle
                </button>
                <button className="btn-ghost" onClick={() => delCatM.mutate(c.id)}>
                  Eliminar
                </button>
              </div>
            </div>
          ))}
        </div>
      </div>
      {(productsQ.isError || createM.isError || toggleM.isError || deleteM.isError) && (
        <p className="text-sm text-red-600">
          {getErrorMessage(productsQ.error || createM.error || toggleM.error || deleteM.error)}
        </p>
      )}
    </div>
  );
}
