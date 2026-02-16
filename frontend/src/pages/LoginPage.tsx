import { FormEvent, useState } from "react";
import { useMutation } from "@tanstack/react-query";
import { useNavigate } from "react-router-dom";
import { posApi } from "../shared/api/posApi";
import { useAuthStore } from "../shared/store/authStore";
import { getErrorMessage, normalizeRole } from "../shared/utils";

export function LoginPage() {
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const navigate = useNavigate();
  const { setAuth } = useAuthStore();

  const mutation = useMutation({
    mutationFn: async () => {
      const login = await posApi.login(username, password);
      localStorage.setItem("pos_token", login.token);
      const me = await posApi.me();
      return { token: login.token, me };
    },
    onSuccess: ({ token, me }) => {
      setAuth({ token, username: me.username, role: normalizeRole(me.roles) });
      navigate("/dashboard", { replace: true });
    }
  });

  function onSubmit(e: FormEvent) {
    e.preventDefault();
    mutation.mutate();
  }

  return (
    <div className="grid min-h-screen place-items-center p-4">
      <div className="card w-full max-w-md p-6">
        <h1 className="mb-1 text-2xl font-bold">Restaurant POS</h1>
        <p className="mb-4 text-sm text-pos-muted">Inicia sesión con tu usuario</p>
        <form onSubmit={onSubmit} className="grid gap-3">
          <label className="text-sm">
            Usuario
            <input className="input mt-1" value={username} onChange={(e) => setUsername(e.target.value)} required />
          </label>
          <label className="text-sm">
            Contraseña
            <input
              className="input mt-1"
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              required
            />
          </label>
          <button className="btn-primary mt-2" disabled={mutation.isPending}>
            {mutation.isPending ? "Ingresando..." : "Ingresar"}
          </button>
        </form>
        {mutation.isError && <p className="mt-3 text-sm text-red-600">{getErrorMessage(mutation.error)}</p>}
      </div>
    </div>
  );
}
