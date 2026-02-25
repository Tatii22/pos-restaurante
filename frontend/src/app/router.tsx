import { createBrowserRouter, Navigate } from "react-router-dom";
import { MainLayout } from "./layout/MainLayout";
import { LoginPage } from "../pages/LoginPage";
import { useAuthStore } from "../shared/store/authStore";
import type { Role } from "../shared/types";
import { DashboardPage } from "../pages/DashboardPage";
import { VentasPage } from "../pages/VentasPage";
import { DomiciliosPage } from "../pages/DomiciliosPage";
import { TurnosPage } from "../pages/TurnosPage";
import { ReportesPage } from "../pages/ReportesPage";
import { InventarioPage } from "../pages/InventarioPage";
import { ProductosPage } from "../pages/ProductosPage";
import { MenuDiaPage } from "../pages/MenuDiaPage";
import { UsuariosPage } from "../pages/UsuariosPage";
import { ConfiguracionPage } from "../pages/ConfiguracionPage";
import { GastosPage } from "../pages/GastosPage";
import { HistorialVentasPage } from "../pages/HistorialVentasPage";
import { CategoriasPage } from "../pages/CategoriasPage";

function Protected({ children, roles }: { children: JSX.Element; roles?: Role[] }) {
  const { token, role } = useAuthStore();
  if (!token) return <Navigate to="/login" replace />;
  if (roles && role && !roles.includes(role)) {
    if (role === "CAJA") return <Navigate to="/ventas" replace />;
    if (role === "DOMI") return <Navigate to="/ventas" replace />;
    return <Navigate to="/dashboard" replace />;
  }
  return children;
}

function HomeRedirect() {
  const { role } = useAuthStore();
  if (role === "ADMIN") return <Navigate to="/dashboard" replace />;
  return <Navigate to="/ventas" replace />;
}

export const router = createBrowserRouter([
  { path: "/login", element: <LoginPage /> },
  {
    path: "/",
    element: (
      <Protected>
        <MainLayout />
      </Protected>
    ),
    children: [
      { index: true, element: <HomeRedirect /> },
      { path: "dashboard", element: <Protected roles={["ADMIN"]}><DashboardPage /></Protected> },
      { path: "ventas", element: <Protected roles={["CAJA", "DOMI"]}><VentasPage /></Protected> },
      { path: "historial", element: <Protected roles={["CAJA"]}><HistorialVentasPage /></Protected> },
      { path: "domicilios", element: <Protected roles={["CAJA", "DOMI"]}><DomiciliosPage /></Protected> },
      { path: "turnos", element: <Protected roles={["CAJA"]}><TurnosPage /></Protected> },
      { path: "reportes", element: <Protected roles={["ADMIN"]}><ReportesPage /></Protected> },
      { path: "inventario", element: <Protected roles={["ADMIN"]}><InventarioPage /></Protected> },
      { path: "gastos", element: <Protected roles={["CAJA", "ADMIN"]}><GastosPage /></Protected> },
      { path: "productos", element: <Protected roles={["ADMIN"]}><ProductosPage /></Protected> },
      { path: "categorias", element: <Protected roles={["ADMIN"]}><CategoriasPage /></Protected> },
      { path: "menu-dia", element: <Protected roles={["ADMIN"]}><MenuDiaPage /></Protected> },
      { path: "usuarios", element: <Protected roles={["ADMIN"]}><UsuariosPage /></Protected> },
      { path: "configuracion", element: <Protected roles={["ADMIN"]}><ConfiguracionPage /></Protected> }
    ]
  }
], ({
  future: {
    v7_startTransition: true
  }
} as unknown) as Parameters<typeof createBrowserRouter>[1]);
