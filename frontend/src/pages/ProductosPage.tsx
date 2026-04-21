import { FormEvent, useMemo, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { BsPencilSquare, BsToggleOff, BsToggleOn, BsTrash3 } from "react-icons/bs";
import { posApi } from "../shared/api/posApi";
import { formatCurrencyInput, getErrorMessage, money, normalizeCurrencyInput, parseCurrencyInput } from "../shared/utils";

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

      <form className="card grid gap-3 p-4 md:grid-cols-5" onSubmit={submit}>
        <input
          className="input"
          placeholder="Nombre"
          value={form.nombre}
          onChange={(e) => setForm({ ...form, nombre: e.target.value.slice(0, 80) })}
          maxLength={80}
          required
        />
        <input
          className="input"
          placeholder="Precio"
          inputMode="numeric"
          value={formatCurrencyInput(form.precio)}
          onChange={(e) => handlePriceChange(e.target.value, (precio) => setForm({ ...form, precio }))}
          required
        />
        <select className="input" value={form.categoriaId} onChange={(e) => setForm({ ...form, categoriaId: e.target.value })} required>
          <option value="">Categoria</option>
          {(categoriesQ.data || []).map((c) => (
            <option key={c.id} value={c.id}>{c.nombre}</option>
          ))}
        </select>
        <select className="input" value={form.tipoVenta} onChange={(e) => setForm({ ...form, tipoVenta: e.target.value })}>
          <option value="MENU_DIARIO">MENU_DIARIO</option>
          <option value="SIEMPRE_DISPONIBLE">SIEMPRE_DISPONIBLE</option>
        </select>
        <button className="btn-primary" disabled={createM.isPending}>
          {createM.isPending ? "Creando..." : "Crear"}
        </button>
      </form>

      <div className="card p-4">
        <div className="grid gap-2 md:grid-cols-[1fr_260px]">
          <input
            className="input"
            placeholder="Buscar por nombre, categoria o precio..."
            value={buscar}
            onChange={(e) => setBuscar(e.target.value.slice(0, 80))}
            maxLength={80}
          />
          <select className="input" value={filtroTipo} onChange={(e) => setFiltroTipo(e.target.value as "ALL" | "MENU_DIARIO" | "SIEMPRE_DISPONIBLE")}>
            <option value="ALL">Todos los tipos</option>
            <option value="MENU_DIARIO">MENU_DIARIO</option>
            <option value="SIEMPRE_DISPONIBLE">SIEMPRE_DISPONIBLE</option>
          </select>
        </div>
      </div>

      <div className="card md:hidden p-3">
        <div className="grid gap-2">
          {lista.map((p) => (
            <div key={p.id} className="rounded-xl border border-pos-border p-3">
              <p className="font-semibold">{p.nombre}</p>
              <p className="text-xs text-pos-muted">ID: {p.id}</p>
              <p className="text-sm">{money.format(p.precio)}</p>
              <p className="text-sm text-pos-muted">{p.categoriaNombre || "Sin categoria"}</p>
              <p className="text-xs">{p.tipoVenta || "-"}</p>
              <p className="text-xs">{p.activo ? "Activo" : "Inactivo"}</p>
              <div className="mt-2 flex flex-wrap gap-2">
                <button
                  className="btn-ghost inline-flex h-8 w-8 items-center justify-center p-0"
                  title="Editar producto"
                  aria-label="Editar producto"
                  onClick={() => openEdit(p)}
                >
                  <BsPencilSquare size={14} />
                </button>
                <button
                  className="btn-ghost inline-flex h-8 w-8 items-center justify-center p-0"
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
                  {p.activo ? <BsToggleOn size={14} /> : <BsToggleOff size={14} />}
                </button>
                <button
                  className="btn-ghost inline-flex h-8 w-8 items-center justify-center p-0 text-red-600 hover:bg-red-50"
                  title="Eliminar producto"
                  aria-label="Eliminar producto"
                  onClick={() => deleteM.mutate(p.id)}
                >
                  <BsTrash3 size={14} />
                </button>
              </div>
            </div>
          ))}
        </div>
      </div>

      <div className="card hidden overflow-x-auto md:block">
        <table className="w-full min-w-[920px] text-sm">
          <thead>
            <tr className="border-b border-pos-border">
              <th className="p-3 text-left">ID</th>
              <th className="p-3 text-left">Nombre</th>
              <th className="p-3 text-left">Precio</th>
              <th className="p-3 text-left">Categoria</th>
              <th className="p-3 text-left">Tipo</th>
              <th className="p-3 text-left">Activo</th>
              <th className="p-3 text-left">Acciones</th>
            </tr>
          </thead>
          <tbody>
            {lista.map((p) => (
              <tr key={p.id} className="border-b border-pos-border/70">
                <td className="p-3">{p.id}</td>
                <td className="p-3">{p.nombre}</td>
                <td className="p-3">{money.format(p.precio)}</td>
                <td className="p-3">{p.categoriaNombre || "Sin categoria"}</td>
                <td className="p-3">{p.tipoVenta || "-"}</td>
                <td className="p-3">{p.activo ? "Si" : "No"}</td>
                <td className="p-3 whitespace-nowrap">
                  <div className="flex gap-2">
                    <button
                      className="btn-ghost inline-flex h-8 w-8 items-center justify-center p-0"
                      title="Editar producto"
                      aria-label="Editar producto"
                      onClick={() => openEdit(p)}
                    >
                      <BsPencilSquare size={14} />
                    </button>
                    <button
                      className="btn-ghost inline-flex h-8 w-8 items-center justify-center p-0"
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
                      {p.activo ? <BsToggleOn size={14} /> : <BsToggleOff size={14} />}
                    </button>
                    <button
                      className="btn-ghost inline-flex h-8 w-8 items-center justify-center p-0 text-red-600 hover:bg-red-50"
                      title="Eliminar producto"
                      aria-label="Eliminar producto"
                      onClick={() => deleteM.mutate(p.id)}
                    >
                      <BsTrash3 size={14} />
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
            <select
              className="input"
              value={editForm.categoriaId}
              onChange={(e) => setEditForm({ ...editForm, categoriaId: e.target.value })}
              required
            >
              <option value="">Categoria</option>
              {(categoriesQ.data || []).map((c) => (
                <option key={c.id} value={c.id}>{c.nombre}</option>
              ))}
            </select>
            <select
              className="input"
              value={editForm.tipoVenta}
              onChange={(e) => setEditForm({ ...editForm, tipoVenta: e.target.value })}
            >
              <option value="MENU_DIARIO">MENU_DIARIO</option>
              <option value="SIEMPRE_DISPONIBLE">SIEMPRE_DISPONIBLE</option>
            </select>
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
