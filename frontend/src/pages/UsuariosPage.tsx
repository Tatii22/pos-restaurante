import { FormEvent, useMemo, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { BsPencilSquare, BsToggleOff, BsToggleOn, BsTrash3 } from "react-icons/bs";
import { posApi } from "../shared/api/posApi";
import { getErrorMessage } from "../shared/utils";
import { CustomSelect } from "../shared/CustomSelect";

type EditState = {
  id: number;
  username: string;
  rol: string;
  activo: boolean;
  password: string;
};

export function UsuariosPage() {
  const qc = useQueryClient();
  const [form, setForm] = useState({ username: "", password: "", rol: "CAJA" });
  const [buscar, setBuscar] = useState("");
  const [editOpen, setEditOpen] = useState(false);
  const [edit, setEdit] = useState<EditState | null>(null);

  const usuariosQ = useQuery({
    queryKey: ["usuarios-admin"],
    queryFn: () => posApi.getUsuarios()
  });

  const createM = useMutation({
    mutationFn: () =>
      posApi.crearUsuario({
        username: form.username.trim(),
        password: form.password,
        rol: form.rol
      }),
    onSuccess: () => {
      setForm({ username: "", password: "", rol: "CAJA" });
      qc.invalidateQueries({ queryKey: ["usuarios-admin"] });
    }
  });

  const updateM = useMutation({
    mutationFn: (payload: EditState) =>
      posApi.actualizarUsuario(payload.id, {
        username: payload.username.trim(),
        rol: payload.rol,
        activo: payload.activo,
        password: payload.password.trim() ? payload.password : undefined
      }),
    onSuccess: () => {
      setEditOpen(false);
      setEdit(null);
      qc.invalidateQueries({ queryKey: ["usuarios-admin"] });
    }
  });

  const deleteM = useMutation({
    mutationFn: (id: number) => posApi.eliminarUsuario(id),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["usuarios-admin"] })
  });

  const usernameValido = form.username.trim().length >= 3 && form.username.trim().length <= 100;
  const passwordValida = form.password.length >= 6;

  const usuariosFiltrados = useMemo(() => {
    const term = buscar.trim().toLowerCase();
    const base = (usuariosQ.data || []).filter((u) => u.rol !== "ADMIN");
    if (!term) return base;
    return base.filter(
      (u) => u.username.toLowerCase().includes(term) || String(u.id).includes(term) || u.rol.toLowerCase().includes(term)
    );
  }, [usuariosQ.data, buscar]);

  function submit(e: FormEvent) {
    e.preventDefault();
    if (!usernameValido || !passwordValida) return;
    createM.mutate();
  }

  function openEdit(u: { id: number; username: string; rol: string; activo: boolean }) {
    setEdit({
      id: u.id,
      username: u.username,
      rol: u.rol,
      activo: !!u.activo,
      password: ""
    });
    setEditOpen(true);
  }

  function submitEdit(e: FormEvent) {
    e.preventDefault();
    if (!edit) return;
              const editUsernameValido = edit.username.trim().length >= 3 && edit.username.trim().length <= 100;
    if (!editUsernameValido) return;
    if (edit.password && edit.password.length < 6) return;
    updateM.mutate(edit);
  }

  return (
    <div className="grid gap-4">
      <h2 className="text-2xl font-semibold">Usuarios</h2>

      <form className="card grid gap-3 p-4 md:grid-cols-4" onSubmit={submit}>
        <label className="text-sm">
          Usuario
          <input
            className="input mt-1"
            placeholder="ej: caja.noche"
            value={form.username}
            onChange={(e) => setForm({ ...form, username: e.target.value.slice(0, 100) })}
            required
          />
          {form.username.length > 0 && !usernameValido && (
            <p className="mt-1 text-xs text-orange-700">El usuario debe tener al menos 3 caracteres.</p>
          )}
        </label>

        <label className="text-sm">
          Contrasena
          <input
            className="input mt-1"
            placeholder="Minimo 6 caracteres"
            type="password"
            value={form.password}
            onChange={(e) => setForm({ ...form, password: e.target.value.slice(0, 60) })}
            required
          />
          {form.password.length > 0 && !passwordValida && (
            <p className="mt-1 text-xs text-orange-700">La contrasena debe tener al menos 6 caracteres.</p>
          )}
        </label>

        <label className="text-sm">
          Rol
          <CustomSelect
            value={form.rol}
            onChange={(v) => setForm({ ...form, rol: v })}
            options={[
              { value: "CAJA", label: "CAJA" },
              { value: "DOMI", label: "DOMI" },
              { value: "MESERO", label: "MESERO" }
            ]}
            className="mt-1"
          />
        </label>

        <div className="flex items-end">
          <button className="btn-primary w-full" disabled={createM.isPending || !usernameValido || !passwordValida}>
            {createM.isPending ? "Creando..." : "Crear usuario"}
          </button>
        </div>
      </form>

      <div className="card p-4">
        <input
          className="input max-w-md"
          placeholder="Buscar usuario por id, nombre o rol..."
          value={buscar}
          onChange={(e) => setBuscar(e.target.value.slice(0, 40))}
          maxLength={40}
        />
      </div>

      <div className="card p-3 md:hidden">
        <div className="grid gap-2">
          {usuariosFiltrados.map((u) => (
            <div key={u.id} className="rounded-xl border border-pos-border p-3">
              <p className="font-semibold">{u.username}</p>
              <p className="text-xs text-pos-muted">ID: {u.id}</p>
              <p className="text-sm">Rol: {u.rol}</p>
              <p className="text-sm">Estado: {u.activo ? "Activo" : "Inactivo"}</p>
              <div className="mt-2 flex items-center gap-1">
                <button
                  className="btn-ghost inline-flex h-7 w-7 items-center justify-center p-0"
                  title="Editar usuario"
                  aria-label="Editar usuario"
                  onClick={() => openEdit(u)}
                  disabled={u.rol === "ADMIN"}
                >
                  <BsPencilSquare size={14} />
                </button>
                <button
                  className="btn-ghost inline-flex h-7 w-7 items-center justify-center p-0"
                  title={u.activo ? "Desactivar usuario" : "Activar usuario"}
                  aria-label={u.activo ? "Desactivar usuario" : "Activar usuario"}
                  onClick={() =>
                    updateM.mutate({
                      id: u.id,
                      username: u.username,
                      rol: u.rol,
                      activo: !u.activo,
                      password: ""
                    })
                  }
                  disabled={u.rol === "ADMIN"}
                >
                  {u.activo ? <BsToggleOn size={14} /> : <BsToggleOff size={14} />}
                </button>
                <button
                  className="btn-ghost inline-flex h-7 w-7 items-center justify-center p-0 text-red-600 hover:bg-red-50"
                  title="Eliminar usuario"
                  aria-label="Eliminar usuario"
                  onClick={() => deleteM.mutate(u.id)}
                  disabled={u.rol === "ADMIN"}
                >
                  <BsTrash3 size={14} />
                </button>
              </div>
            </div>
          ))}
        </div>
      </div>

      <div className="card hidden overflow-x-auto md:block">
        <table className="w-full min-w-[760px] text-sm">
          <thead className="bg-gray-50">
            <tr>
              <th className="pl-6 pr-4 py-3 text-left text-xs font-semibold uppercase text-pos-muted">ID</th>
              <th className="px-4 py-3 text-left text-xs font-semibold uppercase text-pos-muted">Usuario</th>
              <th className="px-4 py-3 text-center text-xs font-semibold uppercase text-pos-muted">Rol</th>
              <th className="px-4 py-3 text-center text-xs font-semibold uppercase text-pos-muted">Estado</th>
              <th className="pl-4 pr-6 py-3 text-center text-xs font-semibold uppercase text-pos-muted">Acciones</th>
            </tr>
          </thead>
          <tbody>
            {usuariosFiltrados.map((u) => (
              <tr key={u.id} className="border-t border-pos-border">
                <td className="py-4 pl-6 pr-4">{u.id}</td>
                <td className="py-4 px-4 truncate" title={u.username}>{u.username}</td>
                <td className="py-4 px-4 text-center">{u.rol}</td>
                <td className="py-4 px-4 text-center">{u.activo ? "Activo" : "Inactivo"}</td>
                <td className="py-4 pl-4 pr-6 text-center whitespace-nowrap">
                  <div className="flex items-center justify-center gap-4">
                    <button
                      className="btn-ghost inline-flex h-7 w-7 items-center justify-center p-0"
                      title="Editar usuario"
                      aria-label="Editar usuario"
                      onClick={() => openEdit(u)}
                      disabled={u.rol === "ADMIN"}
                    >
                      <BsPencilSquare size={14} />
                    </button>
                    <button
                      className="btn-ghost inline-flex h-7 w-7 items-center justify-center p-0"
                      title={u.activo ? "Desactivar usuario" : "Activar usuario"}
                      aria-label={u.activo ? "Desactivar usuario" : "Activar usuario"}
                      onClick={() =>
                        updateM.mutate({
                          id: u.id,
                          username: u.username,
                          rol: u.rol,
                          activo: !u.activo,
                          password: ""
                        })
                      }
                      disabled={u.rol === "ADMIN"}
                    >
                      {u.activo ? <BsToggleOn size={14} /> : <BsToggleOff size={14} />}
                    </button>
                    <button
                      className="btn-ghost inline-flex h-7 w-7 items-center justify-center p-0 text-red-600 hover:bg-red-50"
                      title="Eliminar usuario"
                      aria-label="Eliminar usuario"
                      onClick={() => deleteM.mutate(u.id)}
                      disabled={u.rol === "ADMIN"}
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

      {!usuariosQ.isLoading && usuariosFiltrados.length === 0 && (
        <p className="text-sm text-pos-muted">No hay usuarios para mostrar.</p>
      )}

      {(usuariosQ.isError || createM.isError || updateM.isError || deleteM.isError) && (
        <p className="text-sm text-red-600">{getErrorMessage(usuariosQ.error || createM.error || updateM.error || deleteM.error)}</p>
      )}

      {editOpen && edit && (
        <div className="fixed inset-0 z-40 flex items-center justify-center bg-black/40 p-4">
          <form className="card w-full max-w-xl grid gap-3 p-5" onSubmit={submitEdit}>
            <h3 className="text-lg font-semibold">Editar usuario</h3>
            <label className="text-sm">
              Usuario
              <input
                className="input mt-1"
                value={edit.username}
                onChange={(e) => setEdit({ ...edit, username: e.target.value.slice(0, 100) })}
                required
              />
            </label>
            <label className="text-sm">
              Rol
              <CustomSelect
                value={edit.rol}
                onChange={(v) => setEdit({ ...edit, rol: v })}
                options={[
                  { value: "CAJA", label: "CAJA" },
                  { value: "DOMI", label: "DOMI" },
                  { value: "MESERO", label: "MESERO" }
                ]}
                className="mt-1"
              />
            </label>
            <label className="text-sm">
              Nueva contrasena (opcional)
              <input
                className="input mt-1"
                type="password"
                value={edit.password}
                onChange={(e) => setEdit({ ...edit, password: e.target.value.slice(0, 60) })}
                placeholder="Deja vacio para no cambiar"
              />
            </label>
            <label className="inline-flex items-center gap-2 text-sm">
              <input
                type="checkbox"
                checked={edit.activo}
                onChange={(e) => setEdit({ ...edit, activo: e.target.checked })}
              />
              Usuario activo
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
