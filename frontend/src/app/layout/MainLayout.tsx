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
import { flushSync } from "react-dom";
import { useAuthStore } from "../../shared/store/authStore";
import type { Role } from "../../shared/types";
import { useTurnoStore } from "../../shared/store/turnoStore";
import { posApi } from "../../shared/api/posApi";
import { getErrorMessage, money, normalizeRole } from "../../shared/utils";

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
  { to: "/gastos", label: "Gastos", roles: ["CAJA", "ADMIN"], icon: <Wallet size={16} /> },
  { to: "/reportes", label: "Reportes", roles: ["ADMIN"], icon: <LayoutDashboard size={16} /> },
  { to: "/categorias", label: "Categorias", roles: ["ADMIN"], icon: <Boxes size={16} /> },
  { to: "/productos", label: "Productos", roles: ["ADMIN"], icon: <LayoutDashboard size={16} /> },
  { to: "/usuarios", label: "Usuarios", roles: ["ADMIN"], icon: <Users size={16} /> },
  { to: "/configuracion", label: "Configuracion", roles: ["ADMIN"], icon: <Settings size={16} /> }
];

export function MainLayout() {
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const { token, role, username, setAuth, clearAuth } = useAuthStore();
  const { turno, setTurno, clearTurno } = useTurnoStore();
  const [showAlerts, setShowAlerts] = useState(false);

  const [montoInicial, setMontoInicial] = useState("");
  const [productoId, setProductoId] = useState<number | "">("");
  const [stockInicial, setStockInicial] = useState("10");
  const [menuSetupLocked, setMenuSetupLocked] = useState(false);
  const [dismissedAlerts, setDismissedAlerts] = useState<Record<number, number>>({});
  const [restockQtyByAlert, setRestockQtyByAlert] = useState<Record<number, string>>({});

  const sessionQ = useQuery({
    queryKey: ["auth-me-layout", token],
    queryFn: () => posApi.me(),
    enabled: !!token,
    retry: false
  });
  const resolvedRole = sessionQ.data ? normalizeRole(sessionQ.data.roles) : role;
  const isCajaSessionReady = !!token && sessionQ.isSuccess && resolvedRole === "CAJA";

  useEffect(() => {
    if (!token) return;
    if (!sessionQ.data) return;
    const normalized = normalizeRole(sessionQ.data.roles);
    if (normalized !== role || sessionQ.data.username !== username) {
      setAuth({ token, username: sessionQ.data.username, role: normalized });
    }
  }, [token, role, username, sessionQ.data, setAuth]);

  useEffect(() => {
    if (!token) return;
    if (!sessionQ.isError) return;
    clearTurno();
    clearAuth();
    navigate("/login", { replace: true });
  }, [token, sessionQ.isError, clearTurno, clearAuth, navigate]);

  const menu = items.filter((i) => resolvedRole && i.roles.includes(resolvedRole));

  const turnoActivoQ = useQuery({
    queryKey: ["turno-activo-layout", resolvedRole],
    queryFn: () => posApi.getTurnoActivo(),
    enabled: isCajaSessionReady,
    retry: false
  });

  const inventarioQ = useQuery({
    queryKey: ["inventario-arranque-caja", resolvedRole],
    queryFn: () => posApi.getInventarioDiario(),
    enabled: isCajaSessionReady && !!turno,
    retry: false
  });

  const productosQ = useQuery({
    queryKey: ["productos-arranque-caja", resolvedRole],
    queryFn: () => posApi.getProductos(),
    enabled: isCajaSessionReady
  });

  const openTurnoM = useMutation({
    mutationFn: () => posApi.abrirTurno(Number(montoInicial)),
    onSuccess: (data) => {
      setTurno(data);
      setMenuSetupLocked(true);
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
    mutationFn: ({ id, cantidad }: { id: number; cantidad: number; stockAntes: number }) => posApi.reabastecerInventario(id, cantidad),
    onMutate: (variables) => {
      const prevArranque = queryClient.getQueryData<typeof inventarioQ.data>(["inventario-arranque-caja", role]);
      const prevVentas = queryClient.getQueryData<typeof inventarioQ.data>(["inventario-ventas"]);
      queryClient.setQueryData(["inventario-arranque-caja", role], (current: typeof inventarioQ.data) =>
        (current || []).map((item) =>
          item.id === variables.id ? { ...item, stockActual: item.stockActual + variables.cantidad, agotado: false } : item
        )
      );
      queryClient.setQueryData(["inventario-ventas"], (current: typeof inventarioQ.data) =>
        (current || []).map((item) =>
          item.id === variables.id ? { ...item, stockActual: item.stockActual + variables.cantidad, agotado: false } : item
        )
      );
      return { prevArranque, prevVentas };
    },
    onSuccess: (updated, variables) => {
      queryClient.setQueryData(["inventario-arranque-caja", role], (current: typeof inventarioQ.data) =>
        (current || []).map((item) => (item.id === updated.id ? updated : item))
      );
      queryClient.setQueryData(["inventario-ventas"], (current: typeof inventarioQ.data) =>
        (current || []).map((item) => (item.id === updated.id ? updated : item))
      );
      queryClient.invalidateQueries({ queryKey: ["inventario-arranque-caja"] });
      queryClient.invalidateQueries({ queryKey: ["catalogo-alertas-caja"] });
      queryClient.invalidateQueries({ queryKey: ["inventario-ventas"] });
      queryClient.invalidateQueries({ queryKey: ["catalogo-hoy"] });
    },
    onError: (_error, variables, context) => {
      setDismissedAlerts((prev) => {
        const next = { ...prev };
        delete next[variables.id];
        return next;
      });
      if (context?.prevArranque) {
        queryClient.setQueryData(["inventario-arranque-caja", role], context.prevArranque);
      }
      if (context?.prevVentas) {
        queryClient.setQueryData(["inventario-ventas"], context.prevVentas);
      }
    }
  });

  function hideAlertNow(id: number, stockActual: number) {
    flushSync(() => {
      setDismissedAlerts((prev) => ({ ...prev, [id]: stockActual }));
      setRestockQtyByAlert((prev) => {
        const next = { ...prev };
        delete next[id];
        return next;
      });
    });
  }

  useEffect(() => {
    if (resolvedRole !== "CAJA") return;
    if (turnoActivoQ.data === undefined) return;
    if (turnoActivoQ.data) {
      setTurno(turnoActivoQ.data);
      return;
    }
    clearTurno();
  }, [resolvedRole, turnoActivoQ.data, setTurno, clearTurno]);

  const turnoClass =
    turno?.estado === "ABIERTO"
      ? "bg-green-100 text-green-700"
      : turno?.estado === "SIMULADO"
        ? "bg-yellow-100 text-yellow-700"
        : "bg-red-100 text-red-700";

  const lowStockAlerts = (inventarioQ.data || []).filter((i) => {
    if (i.stockActual > i.stockMinimo) {
      return false;
    }
    const stockWhenDismissed = dismissedAlerts[i.id];
    if (stockWhenDismissed === undefined) {
      return true;
    }
    return i.stockActual < stockWhenDismissed;
  });

  useEffect(() => {
    if (!inventarioQ.data || Object.keys(dismissedAlerts).length === 0) return;
    // Reinicia ciclo de notificación cuando el stock sube por encima del mínimo.
    // Mantiene "oculto" mientras siga crítico y no baje más que el punto ocultado.
    setDismissedAlerts((prev) => {
      let changed = false;
      const next = { ...prev };
      Object.entries(prev).forEach(([idRaw]) => {
        const id = Number(idRaw);
        const item = inventarioQ.data.find((i) => i.id === id);
        if (!item || item.stockActual > item.stockMinimo) {
          delete next[id];
          changed = true;
        }
      });
      return changed ? next : prev;
    });
  }, [inventarioQ.data, dismissedAlerts]);

  useEffect(() => {
    // Al cerrar turno se limpia el estado de notificaciones.
    if (resolvedRole !== "CAJA") return;
    if (turno && turno.estado !== "CERRADO") return;
    setShowAlerts(false);
    setDismissedAlerts({});
    setRestockQtyByAlert({});
  }, [resolvedRole, turno]);

  useEffect(() => {
    if (!showAlerts) return;
    if (lowStockAlerts.length > 0) return;
    setShowAlerts(false);
  }, [lowStockAlerts.length, showAlerts]);

  const needsTurno = resolvedRole === "CAJA" && !turno;
  const needsMenu = resolvedRole === "CAJA" && !!turno && menuSetupLocked;
  const blockCajaNavigation = needsTurno || needsMenu;

  const productosDisponiblesMenu = useMemo(() => {
    const inventarioProductoIds = new Set((inventarioQ.data || []).map((i) => i.productoId));
    return (productosQ.data || []).filter(
      (p) => p.activo && p.tipoVenta === "MENU_DIARIO" && !inventarioProductoIds.has(p.id)
    );
  }, [productosQ.data, inventarioQ.data]);
  const montoInicialValido = Number(montoInicial) > 0;
  const logoutButtonClass =
    resolvedRole === "DOMI"
      ? "inline-flex w-full items-center justify-center gap-1 rounded-xl border border-red-200 bg-red-50 px-2 py-2 text-xs font-semibold text-red-700 hover:bg-red-100 md:hidden"
      : "btn-ghost w-full whitespace-nowrap px-2 py-2 text-xs sm:w-auto sm:px-3 sm:text-sm";
  const logoutButtonDesktopDomiClass =
    "hidden items-center justify-center gap-1 rounded-xl border border-red-200 bg-red-50 px-3 py-2 text-sm font-semibold text-red-700 hover:bg-red-100 md:inline-flex";
  const isDomiMobileNav = resolvedRole === "DOMI";

  return (
    <div className="min-h-screen overflow-x-hidden">
      <header className="sticky top-0 z-20 overflow-x-hidden border-b border-pos-border bg-white">
        <div className="grid grid-cols-1 gap-2 px-2 py-2 sm:flex sm:items-center sm:justify-between sm:px-4 sm:py-3">
          <div className="grid min-w-0 grid-cols-[minmax(0,1.2fr)_minmax(0,0.85fr)_auto_auto] items-center gap-1 overflow-hidden text-[11px] sm:flex sm:items-center sm:gap-2 sm:text-sm">
            <span className="min-w-0 truncate text-sm font-bold sm:shrink-0 sm:text-lg">Restaurant POS</span>
            <span className="min-w-0 truncate text-right font-semibold sm:max-w-[220px] sm:text-left">{username}</span>
            {resolvedRole === "DOMI" && (
              <button className={logoutButtonClass} onClick={clearAuth}>
                <LogOut size={16} className="mr-1" />
                Salir
              </button>
            )}
            {resolvedRole === "CAJA" && (
              <>
                <span className={clsx("rounded-full px-1.5 py-1 text-[10px] font-semibold whitespace-nowrap sm:px-2 sm:text-xs", turnoClass)}>
                  Turno: {turno?.estado ?? "CERRADO"}
                </span>
                <button className="btn-soft relative h-7 w-7 shrink-0 p-0 sm:hidden" onClick={() => setShowAlerts((v) => !v)}>
                  <Bell size={14} />
                  {lowStockAlerts.length > 0 && (
                    <span className="absolute -right-1 -top-1 rounded-full bg-red-500 px-1 text-[9px] font-bold text-white">
                      {lowStockAlerts.length}
                    </span>
                  )}
                </button>
              </>
            )}
          </div>
          <div className="grid min-w-0 grid-cols-2 gap-2 sm:flex sm:flex-wrap sm:items-center">
            {resolvedRole === "CAJA" && (
              <>
                <button className="btn-soft relative hidden h-9 w-9 shrink-0 p-0 sm:inline-flex sm:items-center sm:justify-center" onClick={() => setShowAlerts((v) => !v)}>
                  <Bell size={14} />
                  {lowStockAlerts.length > 0 && (
                    <span className="absolute -right-1 -top-1 rounded-full bg-red-500 px-1 text-[9px] font-bold text-white">
                      {lowStockAlerts.length}
                    </span>
                  )}
                </button>
                <button className="btn-soft w-full whitespace-nowrap px-2 py-2 text-xs sm:w-auto sm:px-3 sm:text-sm" onClick={() => navigate("/turnos")} disabled={needsTurno}>
                  {turno?.estado === "ABIERTO" || turno?.estado === "SIMULADO" ? "Cerrar turno" : "Abrir turno"}
                </button>
              </>
            )}
            {resolvedRole === "DOMI" && (
              <button className={logoutButtonDesktopDomiClass} onClick={clearAuth}>
                <LogOut size={16} className="mr-1" />
                Salir
              </button>
            )}
            {resolvedRole !== "DOMI" && (
              <button className={logoutButtonClass} onClick={clearAuth}>
                <LogOut size={16} className="mr-1" />
                Salir
              </button>
            )}
          </div>
        </div>

        {!blockCajaNavigation && (
          <div className="w-full max-w-[100vw] overflow-x-hidden border-t border-pos-border">
            <nav
              className={clsx(
                "grid gap-2 px-2 py-2 sm:inline-flex sm:px-4",
                isDomiMobileNav && "grid-cols-2 bg-gray-50/80"
              )}
              style={{ gridTemplateColumns: `repeat(${Math.max(menu.length, 1)}, minmax(0, 1fr))` }}
            >
              {menu.map((item) => (
                <NavLink
                  key={item.to}
                  to={item.to}
                  className={({ isActive }) =>
                    clsx(
                      "inline-flex min-w-0 items-center justify-center gap-1 rounded-xl border px-2 py-2 text-xs text-center sm:shrink-0 sm:gap-2 sm:px-3 sm:text-sm",
                      isDomiMobileNav && "min-h-[44px] gap-1 rounded-xl px-2 py-2 text-[11px] font-semibold shadow-sm",
                      isActive ? "border-pos-accent bg-pos-accentSoft text-pos-text" : "border-pos-border bg-white hover:bg-gray-50"
                    )
                  }
                >
                  {item.icon}
                  {item.label}
                </NavLink>
              ))}
            </nav>
          </div>
        )}
      </header>

      {showAlerts && resolvedRole === "CAJA" && (
        <div className="fixed right-4 top-20 z-40 w-[360px] max-w-[95vw] rounded-2xl border border-pos-border bg-white p-4 shadow-pos">
          <div className="mb-2 flex items-center justify-between">
            <h4 className="font-semibold">Alertas de inventario</h4>
            <div className="flex gap-1">
              {lowStockAlerts.length > 0 && (
                <button
                  className="btn-ghost px-2 py-1 text-xs"
                  onClick={() =>
                    flushSync(() =>
                      setDismissedAlerts((prev) => ({
                        ...prev,
                        ...Object.fromEntries(lowStockAlerts.map((a) => [a.id, a.stockActual]))
                      }))
                    )
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
                    onClick={() => {
                      const cantidad = Number(restockQtyByAlert[a.id] ?? "5");
                      if (!Number.isFinite(cantidad) || cantidad <= 0) return;
                      hideAlertNow(a.id, a.stockActual + cantidad);
                      restockM.mutate({
                        id: a.id,
                        cantidad,
                        stockAntes: a.stockActual
                      });
                    }}
                    disabled={restockM.isPending || Number(restockQtyByAlert[a.id] ?? "5") <= 0}
                  >
                    Agregar stock
                  </button>
                  <button
                    className="btn-ghost h-8 px-2 text-xs"
                    onClick={() => hideAlertNow(a.id, a.stockActual)}
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
