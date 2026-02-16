import type {
  ApiError,
  AuthMe,
  CatalogoHoy,
  Categoria,
  PageResponse,
  Producto,
  ReporteVentas,
  Turno,
  UsuarioCreado,
  Venta
} from "../types";

const API_BASE = import.meta.env.VITE_API_BASE ?? "";

function makeHeaders(token?: string): HeadersInit {
  return token
    ? {
        Authorization: `Bearer ${token}`,
        "Content-Type": "application/json"
      }
    : {
        "Content-Type": "application/json"
      };
}

async function parseError(response: Response): Promise<ApiError> {
  try {
    return (await response.json()) as ApiError;
  } catch {
    return { message: `Error HTTP ${response.status}` };
  }
}

async function jsonRequest<T>(path: string, options: RequestInit = {}): Promise<T> {
  const response = await fetch(`${API_BASE}${path}`, options);
  if (!response.ok) {
    throw await parseError(response);
  }
  return (await response.json()) as T;
}

export async function login(username: string, password: string): Promise<string> {
  const data = await jsonRequest<{ token: string }>("/api/v1/auth/login", {
    method: "POST",
    headers: makeHeaders(),
    body: JSON.stringify({ username, password })
  });
  return data.token;
}

export function me(token: string): Promise<AuthMe> {
  return jsonRequest<AuthMe>("/api/v1/auth/me", {
    headers: makeHeaders(token)
  });
}

export function getVentas(
  token: string,
  params: Record<string, string | number | undefined>
): Promise<PageResponse<Venta>> {
  const query = new URLSearchParams();
  Object.entries(params).forEach(([k, v]) => {
    if (v !== undefined && `${v}` !== "") query.set(k, String(v));
  });
  return jsonRequest<PageResponse<Venta>>(`/api/v1/ventas?${query.toString()}`, {
    headers: makeHeaders(token)
  });
}

export function getVentaById(token: string, id: number): Promise<Venta> {
  return jsonRequest<Venta>(`/api/v1/ventas/${id}`, { headers: makeHeaders(token) });
}

export function despachar(token: string, id: number): Promise<Venta> {
  return jsonRequest<Venta>(`/api/v1/ventas/${id}/despachar`, {
    method: "POST",
    headers: makeHeaders(token)
  });
}

export function cancelar(token: string, id: number): Promise<Venta> {
  return jsonRequest<Venta>(`/api/v1/ventas/${id}/cancelar`, {
    method: "POST",
    headers: makeHeaders(token)
  });
}

export function imprimirFactura(token: string, id: number): Promise<Venta> {
  return jsonRequest<Venta>(`/api/v1/ventas/${id}/imprimir-factura`, {
    method: "POST",
    headers: makeHeaders(token)
  });
}

export function imprimirCocina(token: string, id: number): Promise<Venta> {
  return jsonRequest<Venta>(`/api/v1/ventas/${id}/imprimir-cocina`, {
    method: "POST",
    headers: makeHeaders(token)
  });
}

export function actualizarValorDomicilio(token: string, id: number, valorDomicilio: number): Promise<Venta> {
  return jsonRequest<Venta>(`/api/v1/ventas/${id}/valor-domicilio`, {
    method: "PUT",
    headers: makeHeaders(token),
    body: JSON.stringify({ valorDomicilio })
  });
}

type DetalleVenta = { productoId: number; cantidad: number; observacion?: string };

export function crearVenta(
  token: string,
  payload: {
    tipoVenta: "LOCAL" | "DOMICILIO";
    formaPago: "EFECTIVO" | "TRANSFERENCIA";
    clienteNombre?: string;
    telefono?: string;
    direccion?: string;
    descuentoPorcentaje?: number;
    valorDomicilio?: number;
    detalles: DetalleVenta[];
  }
): Promise<Venta> {
  return jsonRequest<Venta>("/api/v1/ventas", {
    method: "POST",
    headers: makeHeaders(token),
    body: JSON.stringify(payload)
  });
}

export function getCatalogoHoy(token: string): Promise<CatalogoHoy> {
  return jsonRequest<CatalogoHoy>("/api/v1/ventas/catalogo-hoy", {
    headers: makeHeaders(token)
  });
}

export function getProductos(token: string): Promise<Producto[]> {
  return jsonRequest<Producto[]>("/api/v1/productos", { headers: makeHeaders(token) });
}

export function crearProducto(
  token: string,
  payload: { nombre: string; precio: number; activo: boolean; categoriaId: number; tipoVenta: string }
): Promise<Producto> {
  return jsonRequest<Producto>("/api/v1/productos", {
    method: "POST",
    headers: makeHeaders(token),
    body: JSON.stringify(payload)
  });
}

export function actualizarProducto(
  token: string,
  id: number,
  payload: { nombre: string; precio: number; activo: boolean; categoriaId: number; tipoVenta: string }
): Promise<Producto> {
  return jsonRequest<Producto>(`/api/v1/productos/${id}`, {
    method: "PUT",
    headers: makeHeaders(token),
    body: JSON.stringify(payload)
  });
}

export async function eliminarProducto(token: string, id: number): Promise<void> {
  const response = await fetch(`${API_BASE}/api/v1/productos/${id}`, {
    method: "DELETE",
    headers: makeHeaders(token)
  });
  if (!response.ok) throw await parseError(response);
}

export function getCategorias(token: string): Promise<Categoria[]> {
  return jsonRequest<Categoria[]>("/api/v1/categorias", { headers: makeHeaders(token) });
}

export function crearCategoria(token: string, payload: { nombre: string; activa: boolean }): Promise<Categoria> {
  return jsonRequest<Categoria>("/api/v1/categorias", {
    method: "POST",
    headers: makeHeaders(token),
    body: JSON.stringify(payload)
  });
}

export function actualizarCategoria(
  token: string,
  id: number,
  payload: { nombre: string; activa: boolean }
): Promise<Categoria> {
  return jsonRequest<Categoria>(`/api/v1/categorias/${id}`, {
    method: "PUT",
    headers: makeHeaders(token),
    body: JSON.stringify(payload)
  });
}

export async function eliminarCategoria(token: string, id: number): Promise<void> {
  const response = await fetch(`${API_BASE}/api/v1/categorias/${id}`, {
    method: "DELETE",
    headers: makeHeaders(token)
  });
  if (!response.ok) throw await parseError(response);
}

export function crearUsuario(
  token: string,
  payload: { username: string; password: string; rol: "ADMIN" | "CAJA" | "DOMI" }
): Promise<UsuarioCreado> {
  return jsonRequest<UsuarioCreado>("/api/v1/usuarios", {
    method: "POST",
    headers: makeHeaders(token),
    body: JSON.stringify(payload)
  });
}

export function abrirTurno(token: string, montoInicial: number): Promise<Turno> {
  return jsonRequest<Turno>("/api/v1/turnos/abrir", {
    method: "POST",
    headers: makeHeaders(token),
    body: JSON.stringify({ montoInicial })
  });
}

export function simularCierre(token: string, efectivoContado: number): Promise<Turno> {
  return jsonRequest<Turno>("/api/v1/turnos/simular-cierre", {
    method: "POST",
    headers: makeHeaders(token),
    body: JSON.stringify({ efectivoContado })
  });
}

export function cerrarTurno(token: string, montoFinal: number): Promise<Turno> {
  return jsonRequest<Turno>("/api/v1/turnos/cerrar", {
    method: "POST",
    headers: makeHeaders(token),
    body: JSON.stringify({ montoFinal })
  });
}

export function getReporteVentas(token: string, fechaInicio: string, fechaFin: string): Promise<ReporteVentas> {
  return jsonRequest<ReporteVentas>(
    `/api/v1/reportes/ventas?fechaInicio=${encodeURIComponent(fechaInicio)}&fechaFin=${encodeURIComponent(fechaFin)}`,
    { headers: makeHeaders(token) }
  );
}

export async function descargarArchivo(token: string, path: string, filename: string): Promise<void> {
  const response = await fetch(`${API_BASE}${path}`, { headers: makeHeaders(token) });
  if (!response.ok) throw await parseError(response);
  const blob = await response.blob();
  const url = window.URL.createObjectURL(blob);
  const anchor = document.createElement("a");
  anchor.href = url;
  anchor.download = filename;
  document.body.appendChild(anchor);
  anchor.click();
  anchor.remove();
  window.URL.revokeObjectURL(url);
}
