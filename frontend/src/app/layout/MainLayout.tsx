import {
  Bell,
  Boxes,
  History,
  LayoutDashboard,
  LogOut,
  Settings,
  ShoppingCart,
  Truck,
  UserCircle,
  Users,
  Wallet,
  X
} from "lucide-react";
import { useEffect, useMemo, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { NavLink, Outlet, useNavigate } from "react-router-dom";
import clsx from "clsx";
import { useAuthStore } from "../../shared/store/authStore";
import type { Role } from "../../shared/types";
import { useTurnoStore } from "../../shared/store/turnoStore";
import { posApi } from "../../shared/api/posApi";
import { getErrorMessage, money } from "../../shared/utils";

type MenuItem = {
  to: string;
  label: string;
  roles: Role[];
  icon: JSX.Element;
};

const items: MenuItem[] = [
  { to: "/dashboard", label: "Dashboard", roles: ["ADMIN"], icon: <LayoutDashboard size={16} /> },
  { to: "/ventas", label: "Ventas", roles: ["CAJA", "DOMI"], icon: <ShoppingCart size={16} /> },
  { to: "/historial", label: "Historial", roles: ["CAJA"], icon: <History size={16} /> },
  { to: "/domicilios", label: "Domicilios", roles: ["CAJA", "DOMI"], icon: <Truck size={16} /> },
  { to: "/gastos", label: "Gastos", roles: ["CAJA"], icon: <Wallet size={16} /> },
  { to: "/reportes", label: "Reportes", roles: ["ADMIN"], icon: <LayoutDashboard size={16} /> },
  { to: "/categorias", label: "Categorias", roles: ["ADMIN"], icon: <Boxes size={16} /> },
  { to: "/productos", label: "Productos", roles: ["ADMIN"], icon: <LayoutDashboard size={16} /> },
  { to: "/usuarios", label: "Usuarios", roles: ["ADMIN"], icon: <Users size={16} /> },
  { to: "/configuracion", label: "Configuracion", roles: ["ADMIN"], icon: <Settings size={16} /> }
];

export function MainLayout() {
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const { role, username, clearAuth } = useAuthStore();
  const { turno, setTurno, clearTurno } = useTurnoStore();
  const [showAlerts, setShowAlerts] = useState(false);

  const [montoInicial, setMontoInicial] = useState("");
  const [productoId, setProductoId] = useState<number | "">("");
  const [stockInicial, setStockInicial] = useState("10");
  const [menuSetupLocked, setMenuSetupLocked] = useState(false);
  const [dismissedAlerts, setDismissedAlerts] = useState<Record<number, number>>({});
  const [restockQtyByAlert, setRestockQtyByAlert] = useState<Record<number, string>>({});

  const menu = items.filter((i) => role && i.roles.includes(role));
  const menuSetupKey = useMemo(() => {
    if (!turno) return "";
    return `menu-setup-locked:${username}:turno:${turno.id}:apertura:${turno.fechaApertura}`;
  }, [username, turno?.id, turno?.fechaApertura]);

  const turnoActivoQ = useQuery({
    queryKey: ["turno-activo-layout", role],
    queryFn: () => posApi.getTurnoActivo(),
    enabled: role === "CAJA",
    retry: false
  });

  const inventarioQ = useQuery({
    queryKey: ["inventario-arranque-caja", role],
    queryFn: () => posApi.getInventarioDiario(),
    enabled: role === "CAJA" && !!turno,
    retry: false
  });

  const productosQ = useQuery({
    queryKey: ["productos-arranque-caja", role],
    queryFn: () => posApi.getProductos(),
    enabled: role === "CAJA"
  });

  const openTurnoM = useMutation({
    mutationFn: () => posApi.abrirTurno(Number(montoInicial)),
    onSuccess: (data) => {
      setTurno(data);
      queryClient.invalidateQueries({ queryKey: ["inventario-arranque-caja"] });
      queryClient.invalidateQueries({ queryKey: ["catalogo-alertas-caja"] });
    }
  });

  const addMenuItemM = useMutation({
    mutationFn: async () => {
      try {
        await posApi.crearInventarioDiario(Number(productoId), Number(stockInicial));
      } catch (error) {
        const msg = getErrorMessage(error);
        const lower = msg.toLowerCase();
        if (!lower.includes("no hay men")) {
          throw error;
        }
        await posApi.crearMenuDiario();
        await posApi.crearInventarioDiario(Number(productoId), Number(stockInicial));
      }
    },
    onSuccess: () => {
      setProductoId("");
      setStockInicial("10");
      queryClient.invalidateQueries({ queryKey: ["inventario-arranque-caja"] });
      queryClient.invalidateQueries({ queryKey: ["catalogo-alertas-caja"] });
      queryClient.invalidateQueries({ queryKey: ["catalogo-hoy"] });
      queryClient.invalidateQueries({ queryKey: ["inventario-ventas"] });
    }
  });

  const restockM = useMutation({
    mutationFn: ({ id, cantidad }: { id: number; cantidad: number }) => posApi.reabastecerInventario(id, cantidad),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["inventario-arranque-caja"] });
      queryClient.invalidateQueries({ queryKey: ["catalogo-alertas-caja"] });
      queryClient.invalidateQueries({ queryKey: ["inventario-ventas"] });
      queryClient.invalidateQueries({ queryKey: ["catalogo-hoy"] });
    }
  });

  useEffect(() => {
    if (role !== "CAJA") return;
    if (turnoActivoQ.data === undefined) return;
    if (turnoActivoQ.data) {
      setTurno(turnoActivoQ.data);
      return;
    }
    clearTurno();
  }, [role, turnoActivoQ.data, setTurno, clearTurno]);

  useEffect(() => {
    if (role !== "CAJA" || !turno) return;
    if (!menuSetupKey) return;
    const alreadyUnlocked = sessionStorage.getItem(menuSetupKey) === "0";
    setMenuSetupLocked(!alreadyUnlocked);
  }, [role, turno?.id, menuSetupKey]);

  const turnoClass =
    turno?.estado === "ABIERTO"
      ? "bg-green-100 text-green-700"
      : turno?.estado === "SIMULADO"
        ? "bg-yellow-100 text-yellow-700"
        : "bg-red-100 text-red-700";

  const lowStockAlerts = (inventarioQ.data || []).filter(
    (i) => i.stockActual <= i.stockMinimo && dismissedAlerts[i.id] === undefined
  );

  useEffect(() => {
    if (!inventarioQ.data || Object.keys(dismissedAlerts).length === 0) return;
    // Rehabilita alerta si:
    // 1) ya salió del nivel crítico (reabastecido), o
    // 2) sigue crítico pero bajó más el stock desde que se ocultó.
    setDismissedAlerts((prev) => {
      let changed = false;
      const next = { ...prev };
      Object.entries(prev).forEach(([idRaw, stockWhenDismissed]) => {
        const id = Number(idRaw);
        const item = inventarioQ.data.find((i) => i.id === id);
        if (!item) {
          delete next[id];
          changed = true;
          return;
        }
        if (item.stockActual > item.stockMinimo || item.stockActual < stockWhenDismissed) {
          delete next[id];
          changed = true;
        }
      });
      return changed ? next : prev;
    });
  }, [inventarioQ.data, dismissedAlerts]);

  useEffect(() => {
    // Al cerrar turno se limpia el estado de notificaciones.
    if (role !== "CAJA") return;
    if (turno && turno.estado !== "CERRADO") return;
    setShowAlerts(false);
    setDismissedAlerts({});
    setRestockQtyByAlert({});
  }, [role, turno]);
  const needsTurno = role === "CAJA" && !turno;
  const needsMenu = role === "CAJA" && !!turno && menuSetupLocked;
  const blockCajaNavigation = needsTurno || needsMenu;

  const productosDisponiblesMenu = useMemo(() => {
    const inventarioProductoIds = new Set((inventarioQ.data || []).map((i) => i.productoId));
    return (productosQ.data || []).filter(
      (p) => p.activo && p.tipoVenta === "MENU_DIARIO" && !inventarioProductoIds.has(p.id)
    );
  }, [productosQ.data, inventarioQ.data]);
  const montoInicialValido = Number(montoInicial) > 0;

  return (
    <div className="min-h-screen">
      <header className="sticky top-0 z-20 border-b border-pos-border bg-white">
        <div className="flex items-center justify-between px-4 py-3">
          <div className="flex items-center gap-3 text-sm">
            <span className="text-lg font-bold">Restaurant POS</span>
            <span className="font-semibold">{username}</span>
            {role === "CAJA" && (
              <span className={clsx("rounded-full px-2 py-1 text-xs font-semibold", turnoClass)}>
                Turno: {turno?.estado ?? "CERRADO"}
              </span>
            )}
          </div>
          <div className="flex items-center gap-2">
            {role === "CAJA" && (
              <button className="btn-soft" onClick={() => navigate("/turnos")} disabled={needsTurno}>
                {turno?.estado === "ABIERTO" || turno?.estado === "SIMULADO" ? "Cerrar turno" : "Abrir turno"}
              </button>
            )}
            {role === "CAJA" && (
              <button className="btn-soft relative" onClick={() => setShowAlerts((v) => !v)}>
                <Bell size={16} />
                {lowStockAlerts.length > 0 && (
                  <span className="absolute -right-1 -top-1 rounded-full bg-red-500 px-1.5 text-[10px] font-bold text-white">
                    {lowStockAlerts.length}
                  </span>
                )}
              </button>
            )}
            <button className="btn-ghost" onClick={clearAuth}>
              <LogOut size={16} className="mr-1" />
              Salir
            </button>
          </div>
        </div>

        {!blockCajaNavigation && (
          <nav className="flex gap-2 overflow-x-auto border-t border-pos-border px-4 py-2">
            {menu.map((item) => (
              <NavLink
                key={item.to}
                to={item.to}
                className={({ isActive }) =>
                  clsx(
                    "inline-flex shrink-0 items-center gap-2 rounded-xl border px-3 py-2 text-sm",
                    isActive ? "border-pos-accent bg-pos-accentSoft text-pos-text" : "border-pos-border bg-white hover:bg-gray-50"
                  )
                }
              >
                {item.icon}
                {item.label}
              </NavLink>
            ))}
          </nav>
        )}
      </header>

      {showAlerts && role === "CAJA" && (
        <div className="fixed right-4 top-20 z-40 w-[360px] max-w-[95vw] rounded-2xl border border-pos-border bg-white p-4 shadow-pos">
          <div className="mb-2 flex items-center justify-between">
            <h4 className="font-semibold">Alertas de inventario</h4>
            <div className="flex gap-1">
              {lowStockAlerts.length > 0 && (
                <button
                  className="btn-ghost px-2 py-1 text-xs"
                  onClick={() =>
                    setDismissedAlerts((prev) => ({
                      ...prev,
                      ...Object.fromEntries(lowStockAlerts.map((a) => [a.id, a.stockActual]))
                    }))
                  }
                >
                  Borrar
                </button>
              )}
              <button className="btn-ghost p-1" onClick={() => setShowAlerts(false)}>
                <X size={14} />
              </button>
            </div>
          </div>
          {lowStockAlerts.length === 0 && (
            <p className="text-sm text-pos-muted">No hay productos en nivel minimo o agotados.</p>
          )}
          <div className="grid gap-2">
            {lowStockAlerts.map((a) => (
              <div key={a.id} className="rounded-xl border border-red-200 bg-red-50 p-2">
                <p className="text-sm font-semibold">{a.producto}</p>
                <p className="text-xs text-red-700">Se esta acabando: {a.stockActual}/{a.stockInicial}</p>
                <div className="mt-2 flex items-center gap-2">
                  <input
                    className="input h-8"
                    value={restockQtyByAlert[a.id] ?? "5"}
                    inputMode="numeric"
                    onChange={(e) => setRestockQtyByAlert((prev) => ({ ...prev, [a.id]: e.target.value }))}
                  />
                  <button
                    className="btn-soft h-8 px-2 text-xs"
                    onClick={() => restockM.mutate({ id: a.id, cantidad: Number(restockQtyByAlert[a.id] ?? "5") })}
                    disabled={restockM.isPending || Number(restockQtyByAlert[a.id] ?? "5") <= 0}
                  >
                    Agregar stock
                  </button>
                  <button
                    className="btn-ghost h-8 px-2 text-xs"
                    onClick={() => setDismissedAlerts((prev) => ({ ...prev, [a.id]: a.stockActual }))}
                  >
                    Ocultar
                  </button>
                </div>
              </div>
            ))}
          </div>
        </div>
      )}

      <main className="mx-auto w-full max-w-7xl p-4 md:p-6">
        {needsTurno && (
          <div className="grid min-h-[70vh] place-items-center">
            <div className="card w-full max-w-xl p-6 text-center">
              <p className="text-xs uppercase tracking-wide text-pos-muted">Inicio de jornada</p>
              <h2 className="mt-2 text-2xl font-bold">Debes abrir turno para continuar</h2>
              <p className="mt-3 text-sm text-pos-muted">Hasta abrir turno no se muestran las opciones de caja.</p>
              <div className="mt-4 grid gap-2">
                <input
                  className="input"
                  value={montoInicial}
                  onChange={(e) => setMontoInicial(e.target.value)}
                  inputMode="decimal"
                  placeholder="Monto inicial"
                />
                {!montoInicialValido && (
                  <p className="text-xs text-orange-700">El monto inicial debe ser mayor a 0.</p>
                )}
                <button
                  className="btn-primary py-3 text-base"
                  onClick={() => openTurnoM.mutate()}
                  disabled={openTurnoM.isPending || !montoInicialValido}
                >
                  {openTurnoM.isPending ? "Abriendo..." : "Abrir turno"}
                </button>
                {openTurnoM.isError && <p className="text-sm text-red-600">{getErrorMessage(openTurnoM.error)}</p>}
              </div>
            </div>
          </div>
        )}

        {needsMenu && !needsTurno && (
          <div className="grid min-h-[70vh] place-items-center">
            <div className="card w-full max-w-3xl p-6">
              <p className="text-xs uppercase tracking-wide text-pos-muted">Inicio de jornada</p>
              <h2 className="mt-2 text-2xl font-bold">Configura el menu del dia</h2>
              <p className="mt-2 text-sm text-pos-muted">
                Agrega los platos de menu diario con su cantidad inicial. Luego se habilitan las opciones de caja.
              </p>

              <div className="mt-4 grid gap-2 md:grid-cols-[1fr_160px_140px]">
                <select className="input" value={productoId} onChange={(e) => setProductoId(e.target.value ? Number(e.target.value) : "")}>
                  <option value="">Selecciona producto...</option>
                  {productosDisponiblesMenu.map((p) => (
                    <option key={p.id} value={p.id}>
                      {p.nombre} - {money.format(Number(p.precio))}
                    </option>
                  ))}
                </select>
                <input
                  className="input"
                  inputMode="numeric"
                  value={stockInicial}
                  onChange={(e) => setStockInicial(e.target.value)}
                  placeholder="Cantidad"
                />
                <button
                  className="btn-primary"
                  onClick={() => addMenuItemM.mutate()}
                  disabled={!productoId || Number(stockInicial) <= 0 || addMenuItemM.isPending}
                >
                  Agregar
                </button>
              </div>

              {productosDisponiblesMenu.length === 0 && (
                <p className="mt-2 text-sm text-pos-muted">
                  No hay mas productos activos para agregar al menu de hoy.
                </p>
              )}

              <div className="mt-4 grid gap-2 md:grid-cols-2">
                {(inventarioQ.data || []).map((i) => (
                  <div key={i.id} className="rounded-xl border border-pos-border bg-white p-3">
                    <p className="font-semibold">{i.producto}</p>
                    <p className="text-sm text-pos-muted">Inicial: {i.stockInicial}</p>
                    <p className="text-sm text-pos-muted">Disponible: {i.stockActual}</p>
                  </div>
                ))}
              </div>

              {(addMenuItemM.isError || inventarioQ.isError || productosQ.isError) && (
                <p className="mt-3 text-sm text-red-600">
                  {getErrorMessage(addMenuItemM.error || inventarioQ.error || productosQ.error)}
                </p>
              )}

              <div className="mt-4 flex justify-end">
                <button
                  className="btn-primary"
                  disabled={(inventarioQ.data?.length || 0) === 0}
                  onClick={() => {
                    if (menuSetupKey) {
                      sessionStorage.setItem(menuSetupKey, "0");
                    }
                    setMenuSetupLocked(false);
                    queryClient.invalidateQueries({ queryKey: ["catalogo-hoy"] });
                    queryClient.invalidateQueries({ queryKey: ["inventario-ventas"] });
                    queryClient.refetchQueries({ queryKey: ["catalogo-hoy"], type: "active" });
                    queryClient.refetchQueries({ queryKey: ["inventario-ventas"], type: "active" });
                  }}
                >
                  Finalizar menu del dia
                </button>
              </div>
            </div>
          </div>
        )}

        {!blockCajaNavigation && <Outlet />}
      </main>
    </div>
  );
}
