import { FormEvent, useMemo, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { BsPencilSquare, BsTrash3 } from "react-icons/bs";
import { posApi } from "../shared/api/posApi";
import { getErrorMessage } from "../shared/utils";

export function CategoriasPage() {
  const qc = useQueryClient();
  const [nombre, setNombre] = useState("");
  const [buscar, setBuscar] = useState("");
  const [editandoId, setEditandoId] = useState<number | null>(null);
  const [nombreEditar, setNombreEditar] = useState("");

  const categoriasQ = useQuery({ queryKey: ["categorias"], queryFn: () => posApi.getCategorias() });

  const crearM = useMutation({
    mutationFn: () => posApi.crearCategoria({ nombre: nombre.trim(), activa: true }),
    onSuccess: () => {
      setNombre("");
      qc.invalidateQueries({ queryKey: ["categorias"] });
    }
  });

  const actualizarM = useMutation({
    mutationFn: (payload: { id: number; nombre: string; activa: boolean }) =>
      posApi.actualizarCategoria(payload.id, { nombre: payload.nombre.trim(), activa: payload.activa }),
    onSuccess: () => {
      setEditandoId(null);
      setNombreEditar("");
      qc.invalidateQueries({ queryKey: ["categorias"] });
      qc.invalidateQueries({ queryKey: ["productos"] });
    }
  });

  const eliminarM = useMutation({
    mutationFn: (id: number) => posApi.eliminarCategoria(id),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["categorias"] });
      qc.invalidateQueries({ queryKey: ["productos"] });
    }
  });

  const categoriasFiltradas = useMemo(() => {
    const term = buscar.trim().toLowerCase();
    if (!term) return categoriasQ.data || [];
    return (categoriasQ.data || []).filter((c) => c.nombre.toLowerCase().includes(term));
  }, [categoriasQ.data, buscar]);

  function submitCrear(e: FormEvent) {
    e.preventDefault();
    if (!nombre.trim()) return;
    crearM.mutate();
  }

  return (
    <div className="grid gap-4">
      <h2 className="text-2xl font-semibold">Categorías</h2>

      <form className="card grid gap-3 p-4 md:grid-cols-[1fr_180px]" onSubmit={submitCrear}>
        <input
          className="input h-11"
          placeholder="Nombre de categoría"
          value={nombre}
          onChange={(e) => setNombre(e.target.value.slice(0, 60))}
          maxLength={60}
          required
        />
        <button className="btn-primary h-11 border border-pos-accent" disabled={crearM.isPending || !nombre.trim()}>
          {crearM.isPending ? "Creando..." : "Crear categoría"}
        </button>
      </form>

      <div className="card p-4">
        <div className="mb-3">
          <input
            className="input w-full"
            placeholder="Buscar categoría..."
            value={buscar}
            onChange={(e) => setBuscar(e.target.value.slice(0, 60))}
            maxLength={60}
          />
        </div>

        <div className="grid gap-2">
          {categoriasFiltradas.map((c) => (
            <div key={c.id} className="flex items-center gap-3 rounded-xl border border-pos-border p-3">
              {editandoId === c.id ? (
                <input
                  className="input flex-1"
                  value={nombreEditar}
                  onChange={(e) => setNombreEditar(e.target.value.slice(0, 60))}
                  maxLength={60}
                />
              ) : (
                <div className="flex-1">
                  <p className="font-medium">{c.nombre}</p>
                  <p className="text-xs text-pos-muted">{c.activa ? "Activa" : "Inactiva"}</p>
                </div>
              )}

              {editandoId === c.id ? (
                <button
                  className="btn-soft"
                  onClick={() => actualizarM.mutate({ id: c.id, nombre: nombreEditar, activa: c.activa })}
                  disabled={actualizarM.isPending || !nombreEditar.trim()}
                >
                  Guardar
                </button>
              ) : (
                <div className="flex items-center gap-4">
                  <button
                    className="btn-ghost inline-flex h-8 w-8 items-center justify-center p-0"
                    title="Editar categoría"
                    aria-label="Editar categoría"
                    onClick={() => {
                      setEditandoId(c.id);
                      setNombreEditar(c.nombre);
                    }}
                  >
                    <BsPencilSquare size={14} />
                  </button>
                  <button
                    className="inline-flex h-8 w-8 items-center justify-center p-0 text-red-400 hover:text-red-600"
                    title="Eliminar categoría"
                    aria-label="Eliminar categoría"
                    onClick={() => eliminarM.mutate(c.id)}
                    disabled={eliminarM.isPending}
                  >
                    <BsTrash3 size={14} />
                  </button>
                  <button
                    className={`relative h-5 w-9 rounded-full transition-colors flex-shrink-0 ${c.activa ? "bg-green-500" : "bg-gray-300"}`}
                    title={c.activa ? "Desactivar categoría" : "Activar categoría"}
                    aria-label={c.activa ? "Desactivar categoría" : "Activar categoría"}
                    onClick={() => actualizarM.mutate({ id: c.id, nombre: c.nombre, activa: !c.activa })}
                    disabled={actualizarM.isPending}
                  >
                    <span
                      className={`absolute left-0.5 top-0.5 h-4 w-4 rounded-full bg-white shadow transition-transform ${c.activa ? "translate-x-4" : "translate-x-0"}`}
                    />
                  </button>
                </div>
              )}
            </div>
          ))}
        </div>

        {!categoriasQ.isLoading && categoriasFiltradas.length === 0 && (
          <p className="mt-2 text-sm text-pos-muted">No hay categorías para mostrar.</p>
        )}
      </div>

      {(categoriasQ.isError || crearM.isError || actualizarM.isError || eliminarM.isError) && (
        <p className="text-sm text-red-600">
          {getErrorMessage(categoriasQ.error || crearM.error || actualizarM.error || eliminarM.error)}
        </p>
      )}
    </div>
  );
}
