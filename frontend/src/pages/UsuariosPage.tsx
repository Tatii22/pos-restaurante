import { FormEvent, useState } from "react";
import { useMutation } from "@tanstack/react-query";
import { posApi } from "../shared/api/posApi";
import { getErrorMessage } from "../shared/utils";

export function UsuariosPage() {
  const [form, setForm] = useState({ username: "", password: "", rol: "CAJA" });
  const createM = useMutation({
    mutationFn: () => posApi.crearUsuario(form)
  });

  function submit(e: FormEvent) {
    e.preventDefault();
    createM.mutate();
  }

  return (
    <div className="grid gap-4">
      <h2 className="text-2xl font-semibold">Usuarios</h2>
      <form className="card grid gap-3 p-4 md:grid-cols-4" onSubmit={submit}>
        <input className="input" placeholder="Usuario" value={form.username} onChange={(e) => setForm({ ...form, username: e.target.value })} required />
        <input className="input" placeholder="Contraseña" type="password" value={form.password} onChange={(e) => setForm({ ...form, password: e.target.value })} required />
        <select className="input" value={form.rol} onChange={(e) => setForm({ ...form, rol: e.target.value })}>
          <option value="CAJA">CAJA</option>
          <option value="DOMI">DOMI</option>
          <option value="ADMIN">ADMIN</option>
        </select>
        <button className="btn-primary">Crear usuario</button>
      </form>
      {createM.isSuccess && <p className="text-sm text-green-700">Usuario creado.</p>}
      {createM.isError && <p className="text-sm text-red-600">{getErrorMessage(createM.error)}</p>}
      <div className="card p-4 text-sm text-pos-muted">
        Editar rol y activar/desactivar usuario requieren endpoints backend de actualización/listado de usuarios.
      </div>
    </div>
  );
}
