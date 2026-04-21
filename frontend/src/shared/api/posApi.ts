import { http } from "./http";
import type {
  AdminConfig,
  AuthMe,
  CatalogoHoy,
  Categoria,
  GastoCaja,
  GastoAdmin,
  InventarioDiario,
  PageResponse,
  Producto,
  ReporteCierreTurno,
  ReporteVentas,
  ReporteRentabilidad,
  TipoGasto,
  Turno,
  Usuario,
  Venta,
  VentaDetalle
} from "../types";

export const posApi = {
  login: async (username: string, password: string) => {
    const { data } = await http.post<{ token: string }>("/api/v1/auth/login", { username, password });
    return data;
  },
  me: async () => {
    const { data } = await http.get<AuthMe>("/api/v1/auth/me");
    return data;
  },
  getVentas: async (params: Record<string, string | number | undefined>) => {
    const { data } = await http.get<PageResponse<Venta>>("/api/v1/ventas", { params });
    return data;
  },
  getVentaById: async (id: number) => {
    const { data } = await http.get<VentaDetalle>(`/api/v1/ventas/${id}`);
    return data;
  },
  crearVenta: async (payload: unknown) => {
    const { data } = await http.post<Venta>("/api/v1/ventas", payload);
    return data;
  },
  despachar: async (
    id: number,
    payload?: { formaPago?: "EFECTIVO" | "TRANSFERENCIA"; pagoEfectivo?: number; pagoTransferencia?: number }
  ) => {
    const { data } = await http.post<Venta>(`/api/v1/ventas/${id}/despachar`, payload ?? {});
    return data;
  },
  cancelar: async (id: number) => {
    const { data } = await http.post<Venta>(`/api/v1/ventas/${id}/cancelar`);
    return data;
  },
  anular: async (id: number, payload?: { motivo?: string }) => {
    const { data } = await http.post<Venta>(`/api/v1/ventas/${id}/anular`, payload ?? {});
    return data;
  },
  imprimirCocina: async (id: number) => {
    const { data } = await http.post<Venta>(`/api/v1/ventas/${id}/imprimir-cocina`);
    return data;
  },
  imprimirCocinaPreview: async (payload: {
    clienteNombre?: string;
    paraLlevar?: boolean;
    detalles: Array<{ productoId: number; cantidad: number; observacion?: string }>;
  }) => {
    await http.post("/api/v1/ventas/imprimir-cocina-preview", payload);
  },
  imprimirFactura: async (id: number) => {
    const { data } = await http.post<Venta>(`/api/v1/ventas/${id}/imprimir-factura`);
    return data;
  },
  actualizarValorDomicilio: async (id: number, valorDomicilio: number) => {
    const { data } = await http.put<Venta>(`/api/v1/ventas/${id}/valor-domicilio`, { valorDomicilio });
    return data;
  },
  catalogoHoy: async () => {
    const { data } = await http.get<CatalogoHoy>("/api/v1/ventas/catalogo-hoy");
    return data;
  },
  abrirTurno: async (montoInicial: number) => {
    const { data } = await http.post<Turno>("/api/v1/turnos/abrir", { montoInicial });
    return data;
  },
  getTurnoActivo: async () => {
    const { data } = await http.get<Turno | null>("/api/v1/turnos/activo");
    return data;
  },
  getTurnosByRango: async (fechaInicio: string, fechaFin: string) => {
    const { data } = await http.get<Turno[]>("/api/v1/turnos/rango", {
      params: { fechaInicio, fechaFin }
    });
    return data;
  },
  crearMenuDiario: async () => {
    const { data } = await http.post("/api/v1/menu-diario");
    return data;
  },
  crearInventarioDiario: async (productoId: number, stockInicial: number) => {
    const { data } = await http.post("/api/v1/inventario-diario", null, {
      params: { productoId, stockInicial }
    });
    return data;
  },
  getInventarioDiario: async () => {
    const { data } = await http.get<InventarioDiario[]>("/api/v1/inventario-diario");
    return data;
  },
  reabastecerInventario: async (id: number, cantidad: number) => {
    const { data } = await http.post<InventarioDiario>(`/api/v1/inventario-diario/${id}/reabastecer`, null, {
      params: { cantidad }
    });
    return data;
  },
  simularCierre: async (efectivoContado: number) => {
    const { data } = await http.post<Turno>("/api/v1/turnos/simular-cierre", { efectivoContado });
    return data;
  },
  cerrarTurno: async (montoFinal: number) => {
    const { data } = await http.post<Turno>("/api/v1/turnos/cerrar", { montoFinal });
    return data;
  },
  getReporteVentas: async (fechaInicio: string, fechaFin: string) => {
    const { data } = await http.get<ReporteVentas>("/api/v1/reportes/ventas", {
      params: { fechaInicio, fechaFin }
    });
    return data;
  },
  getReporteRentabilidad: async (fechaInicio: string, fechaFin: string) => {
    const { data } = await http.get<ReporteRentabilidad>("/api/v1/reportes/rentabilidad", {
      params: { fechaInicio, fechaFin }
    });
    return data;
  },
  getReporteTurno: async (turnoId: number) => {
    const { data } = await http.get<ReporteCierreTurno>(`/api/v1/reportes/turnos/${turnoId}`);
    return data;
  },
  getProductos: async () => {
    const { data } = await http.get<Producto[]>("/api/v1/productos");
    return data;
  },
  crearProducto: async (payload: unknown) => {
    const { data } = await http.post<Producto>("/api/v1/productos", payload);
    return data;
  },
  actualizarProducto: async (id: number, payload: unknown) => {
    const { data } = await http.put<Producto>(`/api/v1/productos/${id}`, payload);
    return data;
  },
  eliminarProducto: async (id: number) => {
    await http.delete(`/api/v1/productos/${id}`);
  },
  getCategorias: async () => {
    const { data } = await http.get<Categoria[]>("/api/v1/categorias");
    return data;
  },
  crearCategoria: async (payload: unknown) => {
    const { data } = await http.post<Categoria>("/api/v1/categorias", payload);
    return data;
  },
  actualizarCategoria: async (id: number, payload: unknown) => {
    const { data } = await http.put<Categoria>(`/api/v1/categorias/${id}`, payload);
    return data;
  },
  eliminarCategoria: async (id: number) => {
    await http.delete(`/api/v1/categorias/${id}`);
  },
  crearUsuario: async (payload: unknown) => {
    const { data } = await http.post("/api/v1/usuarios", payload);
    return data;
  },
  getUsuarios: async () => {
    const { data } = await http.get<Usuario[]>("/api/v1/usuarios");
    return data;
  },
  actualizarUsuario: async (
    id: number,
    payload: { username: string; rol: string; activo: boolean; password?: string }
  ) => {
    const { data } = await http.put<Usuario>(`/api/v1/usuarios/${id}`, payload);
    return data;
  },
  eliminarUsuario: async (id: number) => {
    await http.delete(`/api/v1/usuarios/${id}`);
  },
  getTiposGasto: async () => {
    const { data } = await http.get<TipoGasto[]>("/api/v1/tipos-gasto");
    return data;
  },
  registrarGastoCaja: async (payload: {
    descripcion: string;
    montoEfectivo: number;
    montoTransferencia: number;
    tipoGastoId: number;
  }) => {
    const { data } = await http.post("/api/v1/gastos-caja", payload);
    return data;
  },
  getGastosCaja: async () => {
    const { data } = await http.get<GastoCaja[]>("/api/v1/gastos-caja");
    return data;
  },
  getGastosCajaByRango: async (inicio: string, fin: string) => {
    const { data } = await http.get<GastoCaja[]>("/api/v1/gastos-caja/rango", {
      params: { inicio, fin }
    });
    return data;
  },
  eliminarGastoCaja: async (id: number) => {
    await http.delete(`/api/v1/gastos-caja/${id}`);
  },
  registrarGastoAdmin: async (payload: {
    fecha: string;
    descripcion: string;
    montoEfectivo: number;
    montoTransferencia: number;
    tipoGastoId: number;
  }) => {
    const { data } = await http.post<GastoAdmin>("/api/v1/gastos-admin", payload);
    return data;
  },
  getGastosAdminByFecha: async (fecha: string) => {
    const { data } = await http.get<GastoAdmin[]>(`/api/v1/gastos-admin/fecha/${fecha}`);
    return data;
  },
  getGastosAdminByRango: async (inicio: string, fin: string) => {
    const { data } = await http.get<GastoAdmin[]>("/api/v1/gastos-admin/rango", {
      params: { inicio, fin }
    });
    return data;
  },
  eliminarGastoAdmin: async (id: number) => {
    await http.delete(`/api/v1/gastos-admin/${id}`);
  },
  exportVentasPdf: async (fechaInicio: string, fechaFin: string) => {
    const { data } = await http.get("/api/v1/export/ventas/pdf", {
      params: { fechaInicio, fechaFin },
      responseType: "blob"
    });
    return data as Blob;
  },
  exportVentasExcel: async (fechaInicio: string, fechaFin: string) => {
    const { data } = await http.get("/api/v1/export/ventas/excel", {
      params: { fechaInicio, fechaFin },
      responseType: "blob"
    });
    return data as Blob;
  },
  exportRentabilidadPdf: async (fechaInicio: string, fechaFin: string) => {
    const { data } = await http.get("/api/v1/export/rentabilidad/pdf", {
      params: { fechaInicio, fechaFin },
      responseType: "blob"
    });
    return data as Blob;
  },
  exportRentabilidadExcel: async (fechaInicio: string, fechaFin: string) => {
    const { data } = await http.get("/api/v1/export/rentabilidad/excel", {
      params: { fechaInicio, fechaFin },
      responseType: "blob"
    });
    return data as Blob;
  },
  getAdminConfig: async () => {
    const { data } = await http.get<AdminConfig>("/api/v1/configuracion");
    return data;
  },
  saveAdminConfig: async (payload: AdminConfig) => {
    const { data } = await http.put<AdminConfig>("/api/v1/configuracion", payload);
    return data;
  }
};
