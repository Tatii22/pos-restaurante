import { FormEvent, useMemo, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { BsPencilSquare, BsToggleOff, BsToggleOn, BsTrash3 } from "react-icons/bs";
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
      <h2 className="text-2xl font-semibold">Categorias</h2>

      <form className="card grid gap-3 p-4 md:grid-cols-[1fr_180px]" onSubmit={submitCrear}>
        <input
          className="input"
          placeholder="Nombre de categoria"
          value={nombre}
          onChange={(e) => setNombre(e.target.value.slice(0, 60))}
          maxLength={60}
          required
        />
        <button className="btn-primary" disabled={crearM.isPending || !nombre.trim()}>
          {crearM.isPending ? "Creando..." : "Crear categoria"}
        </button>
      </form>

      <div className="card p-4">
        <div className="mb-3">
          <input
            className="input max-w-md"
            placeholder="Buscar categoria..."
            value={buscar}
            onChange={(e) => setBuscar(e.target.value.slice(0, 60))}
            maxLength={60}
          />
        </div>

        <div className="grid gap-2">
          {categoriasFiltradas.map((c) => (
            <div key={c.id} className="grid gap-2 rounded-xl border border-pos-border p-3 md:grid-cols-[1fr_auto_auto_auto] md:items-center">
              {editandoId === c.id ? (
                <input
                  className="input"
                  value={nombreEditar}
                  onChange={(e) => setNombreEditar(e.target.value.slice(0, 60))}
                  maxLength={60}
                />
              ) : (
                <div>
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
                <button
                  className="btn-ghost inline-flex h-8 w-8 items-center justify-center p-0"
                  title="Editar categoria"
                  aria-label="Editar categoria"
                  onClick={() => {
                    setEditandoId(c.id);
                    setNombreEditar(c.nombre);
                  }}
                >
                  <BsPencilSquare size={14} />
                </button>
              )}

              <button
                className="btn-ghost inline-flex h-8 w-8 items-center justify-center p-0"
                title={c.activa ? "Desactivar categoria" : "Activar categoria"}
                aria-label={c.activa ? "Desactivar categoria" : "Activar categoria"}
                onClick={() => actualizarM.mutate({ id: c.id, nombre: c.nombre, activa: !c.activa })}
                disabled={actualizarM.isPending}
              >
                {c.activa ? <BsToggleOn size={14} /> : <BsToggleOff size={14} />}
              </button>

              <button
                className="btn-ghost inline-flex h-8 w-8 items-center justify-center p-0 text-red-600 hover:bg-red-50"
                title="Eliminar categoria"
                aria-label="Eliminar categoria"
                onClick={() => eliminarM.mutate(c.id)}
                disabled={eliminarM.isPending}
              >
                <BsTrash3 size={14} />
              </button>
            </div>
          ))}
        </div>

        {!categoriasQ.isLoading && categoriasFiltradas.length === 0 && (
          <p className="mt-2 text-sm text-pos-muted">No hay categorias para mostrar.</p>
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
