import { useEffect, useMemo, useState } from "react";

type AdminConfig = {
  negocioNombre: string;
  negocioNit: string;
  negocioTelefono: string;
  negocioDireccion: string;
  ticketEncabezado: string;
  ticketPie: string;
  imprimirFacturaAuto: boolean;
  imprimirCocinaAuto: boolean;
  tamanoFuenteTicket: "SMALL" | "NORMAL" | "LARGE";
};

const STORAGE_KEY = "pos_admin_config_v1";

const defaults: AdminConfig = {
  negocioNombre: "Restaurant POS",
  negocioNit: "",
  negocioTelefono: "",
  negocioDireccion: "",
  ticketEncabezado: "Gracias por su compra",
  ticketPie: "Vuelve pronto",
  imprimirFacturaAuto: true,
  imprimirCocinaAuto: true,
  tamanoFuenteTicket: "NORMAL"
};

export function ConfiguracionPage() {
  const [config, setConfig] = useState<AdminConfig>(defaults);
  const [msg, setMsg] = useState<string>("");

  useEffect(() => {
    const raw = localStorage.getItem(STORAGE_KEY);
    if (!raw) return;
    try {
      const parsed = JSON.parse(raw) as Partial<AdminConfig>;
      const merged: AdminConfig = {
        ...defaults,
        ...parsed
      };
      setConfig(merged);
    } catch {
      // Ignora config corrupta.
    }
  }, []);

  function save() {
    const payload: AdminConfig = {
      ...config,
      negocioNombre: config.negocioNombre.trim() || defaults.negocioNombre,
      ticketEncabezado: config.ticketEncabezado.trim(),
      ticketPie: config.ticketPie.trim(),
      negocioNit: config.negocioNit.trim(),
      negocioTelefono: config.negocioTelefono.trim(),
      negocioDireccion: config.negocioDireccion.trim()
    };
    localStorage.setItem(STORAGE_KEY, JSON.stringify(payload));
    setConfig(payload);
    setMsg("Configuracion guardada.");
    setTimeout(() => setMsg(""), 1800);
  }

  function reset() {
    localStorage.removeItem(STORAGE_KEY);
    setConfig(defaults);
    setMsg("Configuracion restablecida.");
    setTimeout(() => setMsg(""), 1800);
  }

  return (
    <div className="grid gap-4">
      <h2 className="text-2xl font-semibold">Configuracion</h2>

      <section className="card p-4">
        <h3 className="mb-3 text-lg font-semibold">Negocio</h3>
        <div className="grid gap-3 md:grid-cols-2">
          <label className="text-sm">
            Nombre comercial
            <input
              className="input mt-1"
              value={config.negocioNombre}
              onChange={(e) => setConfig((prev) => ({ ...prev, negocioNombre: e.target.value.slice(0, 80) }))}
              maxLength={80}
            />
          </label>
          <label className="text-sm">
            NIT
            <input
              className="input mt-1"
              value={config.negocioNit}
              onChange={(e) => setConfig((prev) => ({ ...prev, negocioNit: e.target.value.slice(0, 30) }))}
              maxLength={30}
            />
          </label>
          <label className="text-sm">
            Telefono
            <input
              className="input mt-1"
              value={config.negocioTelefono}
              onChange={(e) => setConfig((prev) => ({ ...prev, negocioTelefono: e.target.value.slice(0, 20) }))}
              maxLength={20}
            />
          </label>
          <label className="text-sm">
            Direccion
            <input
              className="input mt-1"
              value={config.negocioDireccion}
              onChange={(e) => setConfig((prev) => ({ ...prev, negocioDireccion: e.target.value.slice(0, 120) }))}
              maxLength={120}
            />
          </label>
        </div>
      </section>

      <section className="card p-4">
        <h3 className="mb-3 text-lg font-semibold">Impresion</h3>
        <div className="grid gap-3 md:grid-cols-2">
          <label className="text-sm">
            Encabezado de ticket
            <input
              className="input mt-1"
              value={config.ticketEncabezado}
              onChange={(e) => setConfig((prev) => ({ ...prev, ticketEncabezado: e.target.value.slice(0, 100) }))}
              maxLength={100}
            />
          </label>
          <label className="text-sm">
            Pie de ticket
            <input
              className="input mt-1"
              value={config.ticketPie}
              onChange={(e) => setConfig((prev) => ({ ...prev, ticketPie: e.target.value.slice(0, 100) }))}
              maxLength={100}
            />
          </label>
          <label className="text-sm">
            Tamano fuente ticket
            <select
              className="input mt-1"
              value={config.tamanoFuenteTicket}
              onChange={(e) =>
                setConfig((prev) => ({
                  ...prev,
                  tamanoFuenteTicket: e.target.value as AdminConfig["tamanoFuenteTicket"]
                }))
              }
            >
              <option value="SMALL">SMALL</option>
              <option value="NORMAL">NORMAL</option>
              <option value="LARGE">LARGE</option>
            </select>
          </label>
          <div className="grid gap-2 pt-6 text-sm">
            <label className="inline-flex items-center gap-2">
              <input
                type="checkbox"
                checked={config.imprimirFacturaAuto}
                onChange={(e) => setConfig((prev) => ({ ...prev, imprimirFacturaAuto: e.target.checked }))}
              />
              Imprimir factura automaticamente
            </label>
            <label className="inline-flex items-center gap-2">
              <input
                type="checkbox"
                checked={config.imprimirCocinaAuto}
                onChange={(e) => setConfig((prev) => ({ ...prev, imprimirCocinaAuto: e.target.checked }))}
              />
              Imprimir cocina automaticamente
            </label>
          </div>
        </div>
      </section>

      <section className="card p-4">
        <h3 className="mb-3 text-lg font-semibold">Sistema</h3>
        <div className="grid gap-2 text-sm text-pos-muted">
          <p>Modo configuracion: Local (frontend).</p>
          <p>API base actual: {import.meta.env.VITE_API_BASE || "mismo origen"}</p>
          <p>Estas opciones se guardan por navegador mientras se crean endpoints backend.</p>
        </div>
      </section>

      <div className="flex flex-wrap gap-2">
        <button className="btn-primary" onClick={save}>
          Guardar configuracion
        </button>
        <button className="btn-ghost" onClick={reset}>
          Restablecer
        </button>
        {msg && <p className="self-center text-sm text-green-700">{msg}</p>}
      </div>
    </div>
  );
}
