// src/types.ts — fuente única de verdad para todos los tipos del frontend.
// shared/types.ts es un alias que re-exporta desde aquí para compatibilidad.

export type Role = "ADMIN" | "CAJA" | "DOMI";

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
  paraLlevar?: boolean | null;
  estado: "EN_PROCESO" | "DESPACHADA" | "CANCELADA" | "ANULADA";
  clienteNombre: string | null;
  telefono: string | null;
  direccion: string | null;
  valorDomicilio: number | null;
  descuentoPorcentaje: number | null;
  descuentoValor: number | null;
  total: number;
  formaPago: "EFECTIVO" | "TRANSFERENCIA" | "FIADO";
  pagoEfectivo?: number | null;
  pagoTransferencia?: number | null;
  condicionPago?: "CONTADO" | "FIADO";
  saldoPendiente?: number | null;
  clienteId?: number | null;
};

export type VentaDetalleItem = {
  productoId: number | null;
  productoNombre: string;
  cantidad: number;
  precioUnitario: number;
  subtotal: number;
  observacion: string | null;
};

export type VentaDetalle = Venta & {
  pagoEfectivo: number;
  pagoTransferencia: number;
  condicionPago: "CONTADO" | "FIADO";
  saldoPendiente: number;
  clienteId?: number | null;
  fechaAnulacion: string | null;
  motivoAnulacion: string | null;
  detalles: VentaDetalleItem[];
};

export type Cliente = {
  id: number;
  nombre: string;
  telefono: string;
  direccionPredeterminada?: string | null;
  notas?: string | null;
  activo: boolean;
  tieneDeuda?: boolean;
  deudaTotal: number;
  ventasPendientes: number;
};

/**
 * DTO ligero devuelto por /clientes/buscar-ligero.
 * Usado en el componente ClienteSearch para autocomplete.
 */
export type ClienteSearch = {
  id: number;
  nombre: string;
  telefono: string;
  direccionPredeterminada?: string | null;
  deudaActual: number;
  tieneDeuda: boolean;
};

export type AbonoFiado = {
  id: number;
  fecha: string;
  monto: number;
  montoEfectivo: number;
  montoTransferencia: number;
  formaPago: "EFECTIVO" | "TRANSFERENCIA";
  observacion: string | null;
  usuario: string;
  turnoId: number | null;
  /** Efectivo que el cajero debe devolver al cliente. Cero si no aplica. */
  cambioEfectivo?: number;
};

export type ClienteDetalle = {
  id: number;
  nombre: string;
  telefono: string;
  direccionPredeterminada?: string | null;
  notas?: string | null;
  tieneDeuda?: boolean;
  deudaTotal: number;
  ventasPendientes: Venta[];
  abonos: AbonoFiado[];
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
  categoriaNombre?: string;
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

export type UsuarioCreado = {
  id: number;
  username: string;
  rol: "ADMIN" | "CAJA" | "DOMI" | string;
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
  montoEfectivo?: number | null;
  montoTransferencia?: number | null;
};

export type GastoAdmin = {
  id: number;
  fecha: string;
  descripcion: string;
  monto: number;
  montoEfectivo?: number | null;
  montoTransferencia?: number | null;
  tipoGasto?: string;
  usuario?: string;
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
  recaudoBruto: number;
  totalVentas?: number | null;
  totalGastos: number;
  totalGastosAdmin?: number | null;
  totalGastosCombinados?: number | null;
  gananciaNeta?: number | null;
  esperado: number | null;
  faltante: number | null;
  transferenciasNetas?: number | null;
  totalOperativoTurno?: number | null;
  efectivoOperativo?: number | null;
  transferenciasOperativas?: number | null;
  totalOperativoNeto?: number | null;
  efectivoContado?: number | null;
  transferenciasVerificadas?: number | null;
  diferenciaEfectivo?: number | null;
  diferenciaTransferencias?: number | null;
  totalVerificado?: number | null;
  diferenciaTotal?: number | null;
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
  totalAbonos?: number;
  totalAbonosEfectivo?: number;
  totalAbonosTransferencia?: number;
  totalVentasContado?: number;
  totalVentasFiadas?: number;
  totalMontoContado?: number;
  totalMontoFiado?: number;
  carteraGenerada?: number;
  carteraPendiente?: number;
  recaudoReal?: number;
  ventas: Venta[];
};

export type GastoReporte = {
  id: number;
  fecha: string;
  descripcion: string;
  monto: number;
  origen: "CAJA" | "ADMIN" | string;
};

export type ReporteRentabilidad = {
  fechaInicio: string;
  fechaFin: string;
  totalVentas: number;
  totalVentasEfectivo?: number;
  totalVentasTransferencia?: number;
  totalVentasComerciales?: number;
  ventasContado?: number;
  ventasFiadas?: number;
  carteraGenerada?: number;
  recaudoReal?: number;
  totalGastos: number;
  totalGastosEfectivo?: number;
  totalGastosTransferencia?: number;
  gananciaNeta: number;
  ventas: Venta[];
  gastos: GastoReporte[];
};

export type ReporteCierreTurno = {
  turnoId: number;
  apertura: string;
  cierre: string | null;
  totalVentas: number;
  totalEfectivo: number;
  totalTransferencia: number;
  totalGastos: number;
  totalGastosEfectivo: number;
  totalGastosTransferencia: number;
  gananciaEfectivo: number;
  gananciaTransferencia: number;
  netoEnCaja: number;
  cajaFisicaEsperada?: number;
  transferenciasNetas?: number;
  totalOperativoTurno?: number;
  cajaContada?: number | null;
  diferenciaCaja?: number | null;
  totalAbonos?: number;
  totalAbonosEfectivo?: number;
  totalAbonosTransferencia?: number;
  ventas: Venta[];
  gastos: GastoCaja[];
};

export type AdminConfig = {
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