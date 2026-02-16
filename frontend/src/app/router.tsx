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

function Protected({ children, roles }: { children: JSX.Element; roles?: Role[] }) {
  const { token, role } = useAuthStore();
  if (!token) return <Navigate to="/login" replace />;
  if (roles && role && !roles.includes(role)) return <Navigate to={role === "CAJA" ? "/ventas" : "/dashboard"} replace />;
  return children;
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
      { index: true, element: <Navigate to="/ventas" replace /> },
      { path: "dashboard", element: <Protected roles={["ADMIN", "DOMI"]}><DashboardPage /></Protected> },
      { path: "ventas", element: <Protected roles={["CAJA"]}><VentasPage /></Protected> },
      { path: "historial", element: <Protected roles={["CAJA"]}><HistorialVentasPage /></Protected> },
      { path: "domicilios", element: <Protected roles={["CAJA", "DOMI"]}><DomiciliosPage /></Protected> },
      { path: "turnos", element: <Protected roles={["CAJA"]}><TurnosPage /></Protected> },
      { path: "reportes", element: <Protected roles={["ADMIN"]}><ReportesPage /></Protected> },
      { path: "inventario", element: <Protected roles={["ADMIN"]}><InventarioPage /></Protected> },
      { path: "gastos", element: <Protected roles={["CAJA"]}><GastosPage /></Protected> },
      { path: "productos", element: <Protected roles={["ADMIN"]}><ProductosPage /></Protected> },
      { path: "menu-dia", element: <Protected roles={["ADMIN"]}><MenuDiaPage /></Protected> },
      { path: "usuarios", element: <Protected roles={["ADMIN"]}><UsuariosPage /></Protected> },
      { path: "configuracion", element: <Protected roles={["ADMIN"]}><ConfiguracionPage /></Protected> }
    ]
  }
]);
