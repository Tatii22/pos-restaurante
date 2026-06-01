import { FormEvent, useMemo, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { BsPencilSquare, BsTrash3 } from "react-icons/bs";
import { posApi } from "../shared/api/posApi";
import { formatCurrencyInput, getErrorMessage, money, normalizeCurrencyInput, parseCurrencyInput } from "../shared/utils";
import { CustomSelect } from "../shared/CustomSelect";

function formatTipo(tipo?: string | null) {
  if (!tipo) return "-";
  const map: Record<string, string> = {
    SIEMPRE_DISPONIBLE: "Siempre disponible",
    MENU_DIARIO: "Menú diario"
  };
  return map[tipo] || tipo;
}

export function ProductosPage() {
  const qc = useQueryClient();
  const productsQ = useQuery({ queryKey: ["productos"], queryFn: () => posApi.getProductos() });
  const categoriesQ = useQuery({ queryKey: ["categorias"], queryFn: () => posApi.getCategorias() });
  const [buscar, setBuscar] = useState("");
  const [filtroTipo, setFiltroTipo] = useState<"ALL" | "MENU_DIARIO" | "SIEMPRE_DISPONIBLE">("ALL");
  const [editOpen, setEditOpen] = useState(false);
  const [editForm, setEditForm] = useState({
    id: 0,
    nombre: "",
    precio: "",
    categoriaId: "",
    tipoVenta: "SIEMPRE_DISPONIBLE",
    activo: true
  });

  const [form, setForm] = useState({
    nombre: "",
    precio: "",
    categoriaId: "",
    tipoVenta: "SIEMPRE_DISPONIBLE",
    activo: true
  });

  function handlePriceChange(
    value: string,
    setter: (next: string) => void
  ) {
    const result = normalizeCurrencyInput(value, { maxDigits: 9, allowZero: false });
    if (result.value !== null) {
      setter(result.value);
    }
  }

  const createM = useMutation({
    mutationFn: () =>
      posApi.crearProducto({
        nombre: form.nombre.trim(),
        precio: parseCurrencyInput(form.precio),
        categoriaId: Number(form.categoriaId),
        tipoVenta: form.tipoVenta,
        activo: form.activo
      }),
    onSuccess: () => {
      setForm({ nombre: "", precio: "", categoriaId: "", tipoVenta: "SIEMPRE_DISPONIBLE", activo: true });
      qc.invalidateQueries({ queryKey: ["productos"] });
    }
  });

  const updateM = useMutation({
    mutationFn: (payload: { id: number; name: string; price: number; categoryId: number; type: string; active: boolean }) =>
      posApi.actualizarProducto(payload.id, {
        nombre: payload.name,
        precio: payload.price,
        categoriaId: payload.categoryId,
        tipoVenta: payload.type,
        activo: payload.active
      }),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["productos"] })
  });

  const deleteM = useMutation({
    mutationFn: (id: number) => posApi.eliminarProducto(id),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["productos"] })
  });

  const lista = useMemo(() => {
    const term = buscar.trim().toLowerCase();
    return (productsQ.data || []).filter((p) => {
      const pasaTexto =
        !term ||
        p.nombre.toLowerCase().includes(term) ||
        (p.categoriaNombre || "").toLowerCase().includes(term) ||
        String(p.precio).includes(term);
      const pasaTipo = filtroTipo === "ALL" || (p.tipoVenta || "SIEMPRE_DISPONIBLE") === filtroTipo;
      return pasaTexto && pasaTipo;
    });
  }, [productsQ.data, buscar, filtroTipo]);

  function submit(e: FormEvent) {
    e.preventDefault();
    if (!form.nombre.trim() || parseCurrencyInput(form.precio) <= 0 || !form.categoriaId) return;
    createM.mutate();
  }

  function openEdit(producto: {
    id: number;
    nombre: string;
    precio: number;
    categoriaId: number;
    tipoVenta?: "MENU_DIARIO" | "SIEMPRE_DISPONIBLE";
    activo: boolean;
  }) {
    setEditForm({
      id: producto.id,
      nombre: producto.nombre,
      precio: String(Math.trunc(producto.precio)),
      categoriaId: String(producto.categoriaId),
      tipoVenta: producto.tipoVenta || "SIEMPRE_DISPONIBLE",
      activo: producto.activo
    });
    setEditOpen(true);
  }

  function submitEdit(e: FormEvent) {
    e.preventDefault();
    if (!editForm.nombre.trim() || parseCurrencyInput(editForm.precio) <= 0 || !editForm.categoriaId) return;
    updateM.mutate(
      {
        id: editForm.id,
        name: editForm.nombre.trim(),
        price: parseCurrencyInput(editForm.precio),
        categoryId: Number(editForm.categoriaId),
        type: editForm.tipoVenta,
        active: editForm.activo
      },
      {
        onSuccess: () => {
          setEditOpen(false);
        }
      }
    );
  }

  return (
    <div className="grid gap-4">
      <h2 className="text-2xl font-semibold">Productos</h2>

      <form className="bg-white rounded-2xl shadow-sm p-4 grid gap-3 md:grid-cols-[1fr_140px_160px_160px_260px]" onSubmit={submit}>
        <input
          className="input h-11"
          placeholder="Nombre"
          value={form.nombre}
          onChange={(e) => setForm({ ...form, nombre: e.target.value.slice(0, 80) })}
          maxLength={80}
          required
        />
        <input
          className="input h-11"
          placeholder="Precio"
          inputMode="numeric"
          value={formatCurrencyInput(form.precio)}
          onChange={(e) => handlePriceChange(e.target.value, (precio) => setForm({ ...form, precio }))}
          required
        />
        <CustomSelect
          value={form.categoriaId}
          onChange={(v) => setForm({ ...form, categoriaId: v })}
          options={[{ value: "", label: "Categoría" }, ...(categoriesQ.data || []).map((c) => ({ value: String(c.id), label: c.nombre }))]}
          required
          className="h-11"
        />
        <CustomSelect
          value={form.tipoVenta}
          onChange={(v) => setForm({ ...form, tipoVenta: v })}
          options={[
            { value: "MENU_DIARIO", label: "Menú diario" },
            { value: "SIEMPRE_DISPONIBLE", label: "Siempre disponible" }
          ]}
          className="h-11"
        />
        <button className="btn-primary h-11 border border-pos-accent" disabled={createM.isPending}>
          {createM.isPending ? "Creando..." : "Crear"}
        </button>
      </form>

      <div className="card p-4">
        <div className="grid gap-2 md:grid-cols-[1fr_260px]">
          <input
            className="input h-11"
            placeholder="Buscar por nombre, categoría o precio..."
            value={buscar}
            onChange={(e) => setBuscar(e.target.value.slice(0, 80))}
            maxLength={80}
          />
          <CustomSelect
            value={filtroTipo}
            onChange={(v) => setFiltroTipo(v as "ALL" | "MENU_DIARIO" | "SIEMPRE_DISPONIBLE")}
            options={[
              { value: "ALL", label: "Todos los tipos" },
              { value: "MENU_DIARIO", label: "Menú diario" },
              { value: "SIEMPRE_DISPONIBLE", label: "Siempre disponible" }
            ]}
            className="h-11"
          />
        </div>
      </div>

      <div className="card md:hidden p-3">
        <div className="grid gap-2">
          {lista.map((p) => (
            <div key={p.id} className="rounded-xl border border-pos-border p-3">
              <p className="font-semibold">{p.nombre}</p>
              <p className="text-sm">{money.format(p.precio)}</p>
              <p className="text-sm text-pos-muted">{p.categoriaNombre || "Sin categoría"}</p>
              <p className="text-xs">{formatTipo(p.tipoVenta)}</p>
              <div className="mt-2 flex items-center gap-4">
                <button
                  className="btn-ghost inline-flex h-8 w-8 items-center justify-center p-0"
                  title="Editar producto"
                  aria-label="Editar producto"
                  onClick={() => openEdit(p)}
                >
                  <BsPencilSquare size={14} />
                </button>
                <button
                  className="inline-flex h-8 w-8 items-center justify-center p-0 text-red-400 hover:text-red-600"
                  title="Eliminar producto"
                  aria-label="Eliminar producto"
                  onClick={() => deleteM.mutate(p.id)}
                >
                  <BsTrash3 size={14} />
                </button>
                <button
                  className={`relative h-5 w-9 rounded-full transition-colors flex-shrink-0 ${p.activo ? "bg-green-500" : "bg-gray-300"}`}
                  title={p.activo ? "Desactivar producto" : "Activar producto"}
                  aria-label={p.activo ? "Desactivar producto" : "Activar producto"}
                  onClick={() =>
                    updateM.mutate({
                      id: p.id,
                      name: p.nombre,
                      price: p.precio,
                      categoryId: p.categoriaId,
                      type: p.tipoVenta || "SIEMPRE_DISPONIBLE",
                      active: !p.activo
                    })
                  }
                >
                  <span
                    className={`absolute left-0.5 top-0.5 h-4 w-4 rounded-full bg-white shadow transition-transform ${p.activo ? "translate-x-4" : "translate-x-0"}`}
                  />
                </button>
              </div>
            </div>
          ))}
        </div>
      </div>

      <div className="card hidden overflow-x-auto md:block">
        <table className="w-full min-w-[920px] text-sm">
          <thead className="bg-gray-50">
            <tr>
              <th className="pl-6 pr-4 py-3 text-left text-xs font-semibold uppercase text-pos-muted">Nombre</th>
              <th className="px-4 py-3 text-center text-xs font-semibold uppercase text-pos-muted">Precio</th>
              <th className="px-4 py-3 text-center text-xs font-semibold uppercase text-pos-muted">Categoría</th>
              <th className="px-4 py-3 text-center text-xs font-semibold uppercase text-pos-muted">Tipo</th>
              <th className="pl-4 pr-6 py-3 text-center text-xs font-semibold uppercase text-pos-muted">Acciones</th>
            </tr>
          </thead>
          <tbody>
            {lista.map((p) => (
              <tr key={p.id} className="border-t border-pos-border">
                <td className="py-4 pl-6 pr-4">{p.nombre}</td>
                <td className="py-4 px-4 text-center font-semibold">{money.format(p.precio)}</td>
                <td className="py-4 px-4 text-center">{p.categoriaNombre || "Sin categoría"}</td>
                <td className="py-4 px-4 text-center">{formatTipo(p.tipoVenta)}</td>
                <td className="py-4 pl-4 pr-6 text-center whitespace-nowrap">
                  <div className="flex items-center justify-center gap-4">
                    <button
                      className="btn-ghost inline-flex h-8 w-8 items-center justify-center p-0"
                      title="Editar producto"
                      aria-label="Editar producto"
                      onClick={() => openEdit(p)}
                    >
                      <BsPencilSquare size={14} />
                    </button>
                    <button
                      className="inline-flex h-8 w-8 items-center justify-center p-0 text-red-400 hover:text-red-600"
                      title="Eliminar producto"
                      aria-label="Eliminar producto"
                      onClick={() => deleteM.mutate(p.id)}
                    >
                      <BsTrash3 size={14} />
                    </button>
                    <button
                      className={`relative h-5 w-9 rounded-full transition-colors flex-shrink-0 ${p.activo ? "bg-green-500" : "bg-gray-300"}`}
                      title={p.activo ? "Desactivar producto" : "Activar producto"}
                      aria-label={p.activo ? "Desactivar producto" : "Activar producto"}
                      onClick={() =>
                        updateM.mutate({
                          id: p.id,
                          name: p.nombre,
                          price: p.precio,
                          categoryId: p.categoriaId,
                          type: p.tipoVenta || "SIEMPRE_DISPONIBLE",
                          active: !p.activo
                        })
                      }
                    >
                      <span
                        className={`absolute left-0.5 top-0.5 h-4 w-4 rounded-full bg-white shadow transition-transform ${p.activo ? "translate-x-4" : "translate-x-0"}`}
                      />
                    </button>
                  </div>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      {!productsQ.isLoading && lista.length === 0 && <p className="text-sm text-pos-muted">No hay productos para mostrar.</p>}

      {(productsQ.isError || categoriesQ.isError || createM.isError || updateM.isError || deleteM.isError) && (
        <p className="text-sm text-red-600">
          {getErrorMessage(productsQ.error || categoriesQ.error || createM.error || updateM.error || deleteM.error)}
        </p>
      )}

      {editOpen && (
        <div className="fixed inset-0 z-40 flex items-center justify-center bg-black/40 p-4">
          <form className="card w-full max-w-xl grid gap-3 p-5" onSubmit={submitEdit}>
            <h3 className="text-lg font-semibold">Editar producto</h3>
            <input
              className="input"
              placeholder="Nombre"
              value={editForm.nombre}
              onChange={(e) => setEditForm({ ...editForm, nombre: e.target.value.slice(0, 80) })}
              maxLength={80}
              required
            />
            <input
              className="input"
              placeholder="Precio"
              inputMode="numeric"
              value={formatCurrencyInput(editForm.precio)}
              onChange={(e) => handlePriceChange(e.target.value, (precio) => setEditForm({ ...editForm, precio }))}
              required
            />
            <CustomSelect
              value={editForm.categoriaId}
              onChange={(v) => setEditForm({ ...editForm, categoriaId: v })}
              options={[{ value: "", label: "Categoría" }, ...(categoriesQ.data || []).map((c) => ({ value: String(c.id), label: c.nombre }))]}
              required
              className="h-11"
            />
            <CustomSelect
              value={editForm.tipoVenta}
              onChange={(v) => setEditForm({ ...editForm, tipoVenta: v })}
              options={[
                { value: "MENU_DIARIO", label: "Menú diario" },
                { value: "SIEMPRE_DISPONIBLE", label: "Siempre disponible" }
              ]}
              className="h-11"
            />
            <label className="inline-flex items-center gap-2 text-sm">
              <input
                type="checkbox"
                checked={editForm.activo}
                onChange={(e) => setEditForm({ ...editForm, activo: e.target.checked })}
              />
              Producto activo
            </label>
            <div className="mt-1 flex gap-2">
              <button type="button" className="btn-ghost flex-1" onClick={() => setEditOpen(false)}>
                Cancelar
              </button>
              <button className="btn-primary flex-1" disabled={updateM.isPending}>
                {updateM.isPending ? "Guardando..." : "Guardar cambios"}
              </button>
            </div>
          </form>
        </div>
      )}
    </div>
  );
}
