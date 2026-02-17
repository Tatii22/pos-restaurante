import { http } from "./http";
import type {
  AuthMe,
  CatalogoHoy,
  Categoria,
  GastoCaja,
  InventarioDiario,
  PageResponse,
  Producto,
  ReporteVentas,
  TipoGasto,
  Turno,
  Usuario,
  Venta
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
    const { data } = await http.get<Venta>(`/api/v1/ventas/${id}`);
    return data;
  },
  crearVenta: async (payload: unknown) => {
    const { data } = await http.post<Venta>("/api/v1/ventas", payload);
    return data;
  },
  despachar: async (id: number) => {
    const { data } = await http.post<Venta>(`/api/v1/ventas/${id}/despachar`);
    return data;
  },
  cancelar: async (id: number) => {
    const { data } = await http.post<Venta>(`/api/v1/ventas/${id}/cancelar`);
    return data;
  },
  anular: async (id: number) => {
    const { data } = await http.post<Venta>(`/api/v1/ventas/${id}/anular`);
    return data;
  },
  imprimirCocina: async (id: number) => {
    const { data } = await http.post<Venta>(`/api/v1/ventas/${id}/imprimir-cocina`);
    return data;
  },
  imprimirCocinaPreview: async (payload: { clienteNombre?: string; detalles: Array<{ productoId: number; cantidad: number; observacion?: string }> }) => {
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
    const { data } = await http.post(`/api/v1/inventario-diario/${id}/reabastecer`, null, {
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
  registrarGastoCaja: async (payload: { descripcion: string; monto: number; tipoGastoId: number }) => {
    const { data } = await http.post("/api/v1/gastos-caja", payload);
    return data;
  },
  getGastosCaja: async () => {
    const { data } = await http.get<GastoCaja[]>("/api/v1/gastos-caja");
    return data;
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
  }
};
