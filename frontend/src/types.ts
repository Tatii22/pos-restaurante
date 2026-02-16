export type ApiError = {
  timestamp?: string;
  status?: number;
  code?: string;
  message?: string;
  path?: string;
  fieldErrors?: Record<string, string>;
  mensaje?: string;
};

export type AuthMe = {
  username: string;
  roles: string[];
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
  page: {
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
};

export type Categoria = {
  id: number;
  nombre: string;
  activa: boolean;
};

export type UsuarioCreado = {
  id: number;
  username: string;
  rol: string;
  activo: boolean;
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
