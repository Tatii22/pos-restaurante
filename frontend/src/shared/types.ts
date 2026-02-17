export type Role = "ADMIN" | "CAJA" | "DOMI";

export type AuthMe = {
  username: string;
  roles: string[];
};

export type ApiError = {
  code?: string;
  message?: string;
  status?: number;
  fieldErrors?: Record<string, string>;
  mensaje?: string;
};

export type Venta = {
  id: number;
  fecha: string;
  tipoVenta: "LOCAL" | "DOMICILIO";
  estado: "EN_PROCESO" | "DESPACHADA" | "CANCELADA" | "ANULADA";
  clienteNombre: string | null;
  telefono: string | null;
  direccion: string | null;
  valorDomicilio: number | null;
  descuentoPorcentaje: number | null;
  descuentoValor: number | null;
  total: number;
  formaPago: "EFECTIVO" | "TRANSFERENCIA";
};

export type PageResponse<T> = {
  content: T[];
  page?: {
    number: number;
    size: number;
    totalElements: number;
    totalPages: number;
  };
};

export type ProductoVenta = {
  id: number;
  nombre: string;
  precio: number;
  agotado: boolean;
};

export type CatalogoHoy = {
  menuDiario: ProductoVenta[];
  siempreDisponibles: ProductoVenta[];
};

export type Producto = {
  id: number;
  nombre: string;
  precio: number;
  activo: boolean;
  categoriaId: number;
  categoriaNombre: string;
  tipoVenta?: "MENU_DIARIO" | "SIEMPRE_DISPONIBLE";
};

export type Categoria = {
  id: number;
  nombre: string;
  activa: boolean;
};

export type Usuario = {
  id: number;
  username: string;
  rol: "ADMIN" | "CAJA" | "DOMI" | string;
  activo: boolean;
};

export type TipoGasto = {
  id: number;
  nombre: string;
};

export type GastoCaja = {
  id: number;
  fecha: string;
  descripcion: string;
  valor: number;
};

export type InventarioDiario = {
  id: number;
  fecha: string;
  productoId: number;
  producto: string;
  stockInicial: number;
  stockActual: number;
  stockMinimo: number;
  agotado: boolean;
};

export type Turno = {
  id: number;
  fechaApertura: string;
  fechaCierre: string | null;
  montoInicial: number;
  totalVentas: number;
  totalGastos: number;
  esperado: number | null;
  faltante: number | null;
  estado: "ABIERTO" | "SIMULADO" | "CERRADO";
  usuario: string;
};

export type ReporteVentas = {
  fechaInicio: string;
  fechaFin: string;
  totalVentas: number;
  totalBruto: number;
  totalDescuentos: number;
  totalNeto: number;
  totalEfectivo: number;
  totalTransferencia: number;
  ventas: Venta[];
};
