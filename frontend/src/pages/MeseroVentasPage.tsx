import { useEffect, useMemo, useRef, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { BsCake2, BsCupHot, BsCupStraw, BsGrid, BsTrash, BsXLg } from "react-icons/bs";
import { posApi } from "../shared/api/posApi";
import { formatCurrencyInput, getErrorMessage, money, normalizeCurrencyInput, parseCurrencyInput } from "../shared/utils";
import { useAuthStore } from "../shared/store/authStore";

import type { Turno } from "../types";

type CartItem = { id: number; nombre: string; precio: number; cantidad: number; observacion: string };

type ViewProduct = {
  id: number;
  nombre: string;
  precio: number;
  agotado: boolean;
  categoria: string;
  stockInicial?: number;
  stockActual?: number;
  stockMinimo?: number;
};

function ForkKnifeIcon({ size = 16, className = "" }: { size?: number; className?: string }) {
  return (
    <svg xmlns="http://www.w3.org/2000/svg" width={size} height={size} fill="currentColor" className={className} viewBox="0 0 16 16">
      <path d="M13 .5c0-.276-.226-.506-.498-.465-1.703.257-2.94 2.012-3 8.462a.5.5 0 0 0 .498.5c.56.01 1 .13 1 1.003v5.5a.5.5 0 0 0 .5.5h1a.5.5 0 0 0 .5-.5zM4.25 0a.25.25 0 0 1 .25.25v5.122a.128.128 0 0 0 .256.006l.233-5.14A.25.25 0 0 1 5.24 0h.522a.25.25 0 0 1 .25.238l.233 5.14a.128.128 0 0 0 .256-.006V.25A.25.25 0 0 1 6.75 0h.29a.5.5 0 0 1 .498.458l.423 5.07a1.69 1.69 0 0 1-1.059 1.711l-.053.022a.92.92 0 0 0-.58.884L6.47 15a.971.971 0 1 1-1.942 0l.202-6.855a.92.92 0 0 0-.58-.884l-.053-.022a1.69 1.69 0 0 1-1.059-1.712L3.462.458A.5.5 0 0 1 3.96 0z" />
    </svg>
  );
}

function PatchPlusIcon({ size = 16, className = "" }: { size?: number; className?: string }) {
  return (
    <svg xmlns="http://www.w3.org/2000/svg" width={size} height={size} fill="currentColor" className={className} viewBox="0 0 16 16">
      <path fillRule="evenodd" d="M8 5.5a.5.5 0 0 1 .5.5v1.5H10a.5.5 0 0 1 0 1H8.5V10a.5.5 0 0 1-1 0V8.5H6a.5.5 0 0 1 0-1h1.5V6a.5.5 0 0 1 .5-.5"/>
      <path d="m10.273 2.513-.921-.944.715-.698.622.637.89-.011a2.89 2.89 0 0 1 2.924 2.924l-.01.89.636.622a2.89 2.89 0 0 1 0 4.134l-.637.622.011.89a2.89 2.89 0 0 1-2.924 2.924l-.89-.01-.622.636a2.89 2.89 0 0 1-4.134 0l-.622-.637-.89.011a2.89 2.89 0 0 1-2.924-2.924l.01-.89-.636-.622a2.89 2.89 0 0 1 0-4.134l.637-.622-.011-.89a2.89 2.89 0 0 1 2.924-2.924l.89.01.622-.636a2.89 2.89 0 0 1 4.134 0l-.715.698a1.89 1.89 0 0 0-2.704 0l-.92.944-1.32-.016a1.89 1.89 0 0 0-1.911 1.912l.016 1.318-.944.921a1.89 1.89 0 0 0 0 2.704l.944.92-.016 1.32a1.89 1.89 0 0 0 1.912 1.911l1.318-.016.921.944a1.89 1.89 0 0 0 2.704 0l.92-.944 1.32.016a1.89 1.89 0 0 0 1.911-1.912l-.016-1.318.944-.921a1.89 1.89 0 0 0 0-2.704l-.944-.92.016-1.32a1.89 1.89 0 0 0-1.912-1.911z"/>
    </svg>
  );
}

function categoryIcon(name: string) {
  const key = name.toLowerCase();
  if (key.includes("coffee") || key.includes("cafe")) return <BsCupHot size={16} />;
  if (key.includes("beverage") || key.includes("bebida")) return <BsCupStraw size={16} />;
  if (key.includes("food") || key.includes("plato") || key.includes("almuerzo") || key.includes("comida")) return <ForkKnifeIcon />;
  if (key.includes("adicional") || key.includes("extra")) return <PatchPlusIcon />;
  if (key.includes("appetizer") || key.includes("entrada")) return <PatchPlusIcon />;
  if (key.includes("baker") || key.includes("pan") || key.includes("bakery")) return <BsCake2 size={16} />;
  return <BsGrid size={16} />;
}

function productAccent(name: string): string {
  const n = name.toLowerCase();
  if (n.includes("cafe") || n.includes("coffee")) return "from-amber-100 to-orange-50";
  if (n.includes("pizza") || n.includes("burger") || n.includes("hamb")) return "from-red-100 to-orange-50";
  if (n.includes("jugo") || n.includes("soda") || n.includes("gaseosa")) return "from-cyan-100 to-blue-50";
  return "from-gray-100 to-slate-50";
}

function categoryCardIcon(name: string): JSX.Element {
  const key = name.toLowerCase();
  if (key.includes("coffee") || key.includes("cafe")) return <BsCupHot size={34} />;
  if (key.includes("beverage") || key.includes("bebida")) return <BsCupStraw size={34} />;
  if (key.includes("food") || key.includes("plato") || key.includes("almuerzo") || key.includes("comida")) return <ForkKnifeIcon size={34} className="leading-none" />;
  if (key.includes("adicional") || key.includes("extra")) return <PatchPlusIcon size={34} className="leading-none" />;
  if (key.includes("appetizer") || key.includes("entrada")) return <PatchPlusIcon size={34} className="leading-none" />;
  if (key.includes("baker") || key.includes("pan") || key.includes("bakery")) return <BsCake2 size={34} />;
  return <ForkKnifeIcon size={34} className="leading-none" />;
}

export function MeseroVentasPage() {
  const { role } = useAuthStore();
  const qc = useQueryClient();

  const turnoActivoQ = useQuery({
    queryKey: ["turno-activo-mesero"],
    queryFn: () => posApi.getTurnoActivo(),
    retry: false,
    refetchInterval: 30000
  });

  const bloqueado = !turnoActivoQ.data;

  const [search, setSearch] = useState("");
  const [selectedCategory, setSelectedCategory] = useState("ALL");
  const [cart, setCart] = useState<CartItem[]>([]);
  const [showPayModal, setShowPayModal] = useState(false);
  const [checkoutMode, setCheckoutMode] = useState<"SALON" | "LLEVAR">("SALON");
  const [transferAmount, setTransferAmount] = useState("0");
  const [cashAmount, setCashAmount] = useState("0");
  const [activeCalcField, setActiveCalcField] = useState<"TRANSFERENCIA" | "EFECTIVO">("EFECTIVO");
  const [clienteNombre, setClienteNombre] = useState("");

  const [showSuccessToast, setShowSuccessToast] = useState(false);
  const [stockWarn, setStockWarn] = useState<string | null>(null);
  const [validationWarns, setValidationWarns] = useState<string[]>([]);
  const [orderCode, setOrderCode] = useState(() => String(Math.floor(Math.random() * 9000) + 1000));

  const mountedRef = useRef(true);
  const successToastTimerRef = useRef<ReturnType<typeof setTimeout> | null>(null);
  const stockWarnTimerRef = useRef<ReturnType<typeof setTimeout> | null>(null);
  const validationWarnTimerRef = useRef<ReturnType<typeof setTimeout> | null>(null);

  useEffect(() => {
    mountedRef.current = true;
    return () => {
      mountedRef.current = false;
      if (successToastTimerRef.current) clearTimeout(successToastTimerRef.current);
      if (stockWarnTimerRef.current) clearTimeout(stockWarnTimerRef.current);
      if (validationWarnTimerRef.current) clearTimeout(validationWarnTimerRef.current);
    };
  }, []);

  const catalogQ = useQuery({ queryKey: ["catalogo-hoy"], queryFn: () => posApi.catalogoHoy() });
  const inventarioQ = useQuery({
    queryKey: ["inventario-ventas"],
    queryFn: () => posApi.getInventarioDiario(),
    retry: false
  });

  const createSale = useMutation({
    mutationFn: (payload: unknown) => posApi.crearVenta(payload),
    onSuccess: () => {
      setCart([]);
      setClienteNombre("");
      setOrderCode(String(Math.floor(Math.random() * 9000) + 1000));
      setShowPayModal(false);
      setCheckoutMode("SALON");
      setTransferAmount("0");
      setCashAmount("0");
      if (successToastTimerRef.current) clearTimeout(successToastTimerRef.current);
      setShowSuccessToast(true);
      successToastTimerRef.current = setTimeout(() => {
        if (mountedRef.current) setShowSuccessToast(false);
      }, 2200);
      qc.invalidateQueries({ queryKey: ["catalogo-hoy"] });
      qc.invalidateQueries({ queryKey: ["inventario-ventas"] });
      qc.invalidateQueries({ queryKey: ["turno-activo-layout"] });
      qc.invalidateQueries({ queryKey: ["historial-turno-ventas"] });
    }
  });

  const printKitchenPreviewM = useMutation({
    mutationFn: (payload: {
      clienteNombre?: string;
      paraLlevar?: boolean;
      detalles: Array<{ productoId: number; cantidad: number; observacion?: string }>;
    }) => posApi.imprimirCocinaPreview(payload)
  });

  const products = useMemo(() => {
    const invMap = new Map((inventarioQ.data || []).map((i) => [i.productoId, i]));
    const menu = (catalogQ.data?.menuDiario || []).map<ViewProduct>((p) => ({
      id: p.id,
      nombre: p.nombre,
      precio: Number(p.precio),
      agotado: p.agotado,
      categoria: p.categoriaNombre || "Sin categoria",
      stockInicial: invMap.get(p.id)?.stockInicial,
      stockActual: invMap.get(p.id)?.stockActual,
      stockMinimo: invMap.get(p.id)?.stockMinimo
    }));
    const siempre = (catalogQ.data?.siempreDisponibles || []).map<ViewProduct>((p) => ({
      id: p.id,
      nombre: p.nombre,
      precio: Number(p.precio),
      agotado: p.agotado,
      categoria: p.categoriaNombre || "Sin categoria"
    }));
    return [...menu, ...siempre];
  }, [catalogQ.data, inventarioQ.data]);

  const categories = useMemo(() => {
    const fromProductos = products.map((p) => p.categoria);
    const uniques = Array.from(new Set([...fromProductos])).sort();
    return ["ALL", ...uniques];
  }, [products]);

  const filteredProducts = useMemo(() => {
    return products
      .filter((p) => selectedCategory === "ALL" || p.categoria === selectedCategory)
      .filter((p) => p.nombre.toLowerCase().includes(search.toLowerCase()));
  }, [products, selectedCategory, search]);

  const subtotal = useMemo(() => cart.reduce((sum, item) => sum + item.precio * item.cantidad, 0), [cart]);
  const total = subtotal;
  const totalItems = cart.length;
  const totalPieces = cart.reduce((sum, item) => sum + item.cantidad, 0);

  function cleanText(value: string, maxLen: number): string {
    return value.slice(0, maxLen);
  }

  function addProduct(product: ViewProduct) {
    if (bloqueado || product.agotado) return;
    setCart((prev) => {
      const existing = prev.find((x) => x.id === product.id);
      const maxStock = typeof product.stockActual === "number" ? product.stockActual : Number.MAX_SAFE_INTEGER;
      const currentQty = existing?.cantidad ?? 0;
      if (maxStock <= 0 || currentQty >= maxStock) {
        if (stockWarnTimerRef.current) clearTimeout(stockWarnTimerRef.current);
        setStockWarn(`No hay mas stock para ${product.nombre}`);
        stockWarnTimerRef.current = setTimeout(() => {
          if (mountedRef.current) setStockWarn(null);
        }, 2000);
        return prev;
      }
      if (!existing) {
        return [...prev, { id: product.id, nombre: product.nombre, precio: product.precio, cantidad: 1, observacion: "" }];
      }
      return prev.map((x) => (x.id === product.id ? { ...x, cantidad: x.cantidad + 1 } : x));
    });
  }

  function removeItem(itemId: number) {
    setCart((prev) => prev.filter((x) => x.id !== itemId));
  }

  function handleCurrencyChange(
    value: string,
    setValue: (next: string) => void
  ) {
    const result = normalizeCurrencyInput(value, { maxDigits: 9, allowZero: true });
    if (result.value !== null) {
      setValue(result.value || "0");
    }
  }

  const transfer = parseCurrencyInput(transferAmount);
  const cash = parseCurrencyInput(cashAmount);
  const paid = transfer + cash;
  const remaining = Math.max(0, total - paid);
  const change = Math.max(0, paid - total);

  function resolveFormaPagoFrom(transferValue: number, cashValue: number): "EFECTIVO" | "TRANSFERENCIA" {
    if (transferValue > 0 && cashValue === 0) return "TRANSFERENCIA";
    if (cashValue > 0 && transferValue === 0) return "EFECTIVO";
    return cashValue >= transferValue ? "EFECTIVO" : "TRANSFERENCIA";
  }

  function buildSalePayload(transferValue: number, cashValue: number) {
    const formaPago = resolveFormaPagoFrom(transferValue, cashValue);
    return {
      tipoVenta: "LOCAL" as const,
      formaPago,
      pagoEfectivo: cashValue,
      pagoTransferencia: transferValue,
      paraLlevar: checkoutMode === "LLEVAR",
      descuentoPorcentaje: 0,
      clienteNombre: clienteNombre.trim() || undefined,
      detalles: cart.map((i) => ({ productoId: i.id, cantidad: i.cantidad, observacion: i.observacion }))
    };
  }

  function pushCalcValue(token: string) {
    const update = (curr: string) => {
      if (token === "C") return "0";
      if (token === "<") return curr.length <= 1 ? "0" : curr.slice(0, -1);
      if (curr === "0") return token;
      return curr + token;
    };
    if (activeCalcField === "TRANSFERENCIA") {
      setTransferAmount((prev) => update(prev));
    } else {
      setCashAmount((prev) => update(prev));
    }
  }

  async function setExactPayment() {
    const remainingNow = Math.max(0, total - (parseCurrencyInput(transferAmount) + parseCurrencyInput(cashAmount)));
    const transferNow = parseCurrencyInput(transferAmount);
    const cashNow = parseCurrencyInput(cashAmount);
    if (bloqueado || cart.length === 0 || createSale.isPending) return;

    let transferFinal = transferNow;
    let cashFinal = cashNow;

    if (remainingNow > 0) {
      if (activeCalcField === "TRANSFERENCIA") {
        transferFinal = transferNow + remainingNow;
        setTransferAmount(String(transferFinal));
      } else {
        cashFinal = cashNow + remainingNow;
        setCashAmount(String(cashFinal));
      }
    }

    try {
      await createSale.mutateAsync(buildSalePayload(transferFinal, cashFinal));
    } catch (err: any) {
      setValidationWarns([getErrorMessage(err)]);
    }
  }

  async function registerSale() {
    if (bloqueado || cart.length === 0) return;
    if (remaining > 0) {
      setValidationWarns(["El pago es insuficiente para completar la venta"]);
      return;
    }
    try {
      await createSale.mutateAsync(buildSalePayload(transfer, cash));
    } catch (err: any) {
      setValidationWarns([getErrorMessage(err)]);
    }
  }

  async function startCheckout(mode: "SALON" | "LLEVAR") {
    if (bloqueado || cart.length === 0) return;
    setCheckoutMode(mode);
    try {
      await printKitchenPreviewM.mutateAsync({
        clienteNombre: clienteNombre.trim() || undefined,
        paraLlevar: mode === "LLEVAR",
        detalles: cart.map((i) => ({ productoId: i.id, cantidad: i.cantidad, observacion: i.observacion }))
      });
    } catch {
      // Si falla la impresión, igual permitimos continuar
    }
    setShowPayModal(true);
    setActiveCalcField("EFECTIVO");
  }

  const hasError = catalogQ.isError || createSale.isError || turnoActivoQ.isError;
  const pageErrorMessage = hasError
    ? getErrorMessage(catalogQ.error || createSale.error || turnoActivoQ.error)
    : "";
  const hasUsableData = products.length > 0 || !!catalogQ.data;
  const showInlineError = hasError && !(pageErrorMessage === "Acceso denegado" && hasUsableData);

  if (turnoActivoQ.isLoading) {
    return (
      <div className="grid min-h-[70vh] place-items-center">
        <p className="text-sm text-pos-muted">Cargando...</p>
      </div>
    );
  }

  if (bloqueado && turnoActivoQ.isSuccess) {
    return (
      <div className="grid min-h-[70vh] place-items-center">
        <div className="card w-full max-w-xl p-6 text-center">
          <h2 className="mt-2 text-2xl font-bold">No hay turno activo</h2>
          <p className="mt-3 text-sm text-pos-muted">
            Debe haber un turno de caja activo para poder registrar ventas.
          </p>
        </div>
      </div>
    );
  }

  return (
    <div className="grid gap-4 xl:grid-cols-[130px_1fr_390px]">
      {bloqueado && (
        <div className="xl:col-span-3 rounded-xl border border-yellow-300 bg-yellow-50 p-3 text-sm text-yellow-800">
          Ventas bloqueadas: no hay turno ACTIVO de caja para operar.
        </div>
      )}

      <aside className="card p-3">
        <p className="mb-2 text-xs uppercase tracking-wide text-pos-muted">Categorias</p>
        <div className="grid max-h-[70vh] gap-2 overflow-y-auto pr-1">
          {categories.map((c) => {
            const selected = c === selectedCategory;
            return (
              <button
                key={c}
                className={`flex w-full min-w-0 items-center gap-2 rounded-xl border px-2 py-2 text-left text-sm ${
                  selected ? "border-orange-300 bg-orange-50 text-orange-700" : "border-pos-border hover:bg-gray-50"
                }`}
                onClick={() => setSelectedCategory(c)}
              >
                {c === "ALL" ? <BsGrid size={16} /> : categoryIcon(c)}
                <span className="truncate">{c === "ALL" ? "Todas" : c}</span>
              </button>
            );
          })}
        </div>
      </aside>

      <section className="card p-4">
        <h2 className="mb-3 text-xl font-semibold">Seleccion de Productos</h2>
        <div className="mb-3">
          <input
            className="input max-w-sm"
            placeholder="Buscar producto..."
            value={search}
            onChange={(e) => setSearch(cleanText(e.target.value, 80))}
            maxLength={80}
          />
        </div>

        <div className="grid grid-cols-2 gap-3 md:grid-cols-3 2xl:grid-cols-4">
          {filteredProducts.map((p) => {
            const qty = cart.find((x) => x.id === p.id)?.cantidad || 0;
            const canAddMore = !bloqueado && !p.agotado && (typeof p.stockActual !== "number" || qty < p.stockActual);
            return (
              <button
                key={p.id}
                className={`rounded-2xl border bg-white p-2 text-left shadow-sm transition ${
                  qty > 0 ? "border-orange-300 ring-1 ring-orange-200" : "border-pos-border"
                } ${p.agotado || bloqueado ? "opacity-60" : "hover:-translate-y-0.5"}`}
                onClick={() => addProduct(p)}
                disabled={!canAddMore}
              >
                <div className={`relative h-28 rounded-xl bg-gradient-to-br ${productAccent(p.nombre)} p-3`}>
                  <div className="text-xs font-semibold text-gray-600">{p.categoria}</div>
                  <div className="mt-4 text-gray-700">{categoryCardIcon(p.categoria)}</div>
                  {qty > 0 && (
                    <span className="absolute right-2 top-2 rounded-full bg-orange-500 px-2 py-0.5 text-xs font-semibold text-white">
                      {qty}
                    </span>
                  )}
                </div>
                <div className="px-1 pb-1 pt-2">
                  <p className="truncate font-semibold">{p.nombre}</p>
                  <p className="text-sm text-pos-muted">{money.format(p.precio)}</p>
                  {typeof p.stockActual === "number" && (
                    <p className="mt-1 text-xs font-semibold text-pos-muted">
                      Stock: {p.stockActual}
                    </p>
                  )}
                  {typeof p.stockActual === "number" && p.stockActual <= 5 && (
                    <p className="mt-1 text-xs font-semibold text-orange-700">
                      Alerta: la comida se esta acabando
                    </p>
                  )}
                  {typeof p.stockActual === "number" && qty >= p.stockActual && (
                    <p className="mt-1 text-xs font-semibold text-red-700">
                      Limite por inventario alcanzado
                    </p>
                  )}
                </div>
              </button>
            );
          })}
        </div>
      </section>

      <section className="card p-4">
        <div className="mb-2 flex items-center justify-between">
          <h3 className="text-lg font-semibold">Resumen del Pedido</h3>
          <span className="rounded-lg bg-gray-100 px-2 py-1 text-xs">#{orderCode}</span>
        </div>

        <div className="mb-3 grid grid-cols-1 gap-2">
          <div className="relative">
            <label className="text-sm">
              Nombre <span className="text-pos-muted">(opcional)</span>
              <input
                className="input mt-1"
                value={clienteNombre}
                onChange={(e) => setClienteNombre(cleanText(e.target.value, 60))}
                placeholder="Ej: Juan Perez"
                maxLength={60}
              />
            </label>
          </div>
        </div>

        <div className="max-h-72 space-y-2 overflow-auto pr-1">
          {cart.map((item) => (
            <div key={item.id} className="rounded-xl border border-pos-border p-2">
              <div className="flex items-center justify-between">
                <p className="font-medium">{item.nombre}</p>
                <div className="flex items-center gap-1">
                  <p className="text-sm">{money.format(item.precio * item.cantidad)}</p>
                  <button
                    className="btn-ghost inline-flex h-8 w-8 items-center justify-center p-0 text-red-600 hover:bg-red-50"
                    onClick={() => removeItem(item.id)}
                    title="Quitar producto"
                    aria-label="Quitar producto"
                  >
                    <BsTrash size={14} />
                  </button>
                </div>
              </div>
              <p className="text-xs text-pos-muted">
                {item.cantidad} x {money.format(item.precio)}
              </p>
              <input
                className="input mt-2"
                placeholder="Observacion"
                value={item.observacion}
                onChange={(e) =>
                  setCart((prev) => prev.map((x) => (x.id === item.id ? { ...x, observacion: cleanText(e.target.value, 120) } : x)))
                }
                maxLength={120}
              />
            </div>
          ))}
        </div>

        <div className="mt-3 border-t border-pos-border pt-3">
          <div className="mt-2 text-sm text-pos-muted">
            <p>Items: {totalItems}</p>
            <p>Piezas: {totalPieces}</p>
            <p>Subtotal: {money.format(subtotal)}</p>
          </div>
          <p className="mt-2 text-3xl font-bold">{money.format(total)}</p>
        </div>

        <div className="mt-4 grid gap-2">
          <button
            className="btn-primary bg-green-600 hover:bg-green-700"
            disabled={createSale.isPending || printKitchenPreviewM.isPending}
            onClick={() => startCheckout("SALON")}
          >
            COBRAR
          </button>
          <button
            className="btn-soft border border-green-300 bg-green-50 text-green-700 hover:bg-green-100"
            disabled={createSale.isPending || printKitchenPreviewM.isPending}
            onClick={() => startCheckout("LLEVAR")}
          >
            COBRAR PARA LLEVAR
          </button>
        </div>

        {showInlineError && (
          <p className="mt-3 text-sm text-red-600">{pageErrorMessage}</p>
        )}
      </section>

      {showSuccessToast && (
        <div className="fixed right-4 top-20 z-50 flex items-center gap-3 rounded-xl border border-green-300 bg-green-50 px-4 py-3 text-sm font-semibold text-green-700 shadow-pos">
          <span>Venta exitosa</span>
          <button className="ml-auto btn-ghost p-0.5" onClick={() => setShowSuccessToast(false)}>
            <BsXLg size={12} />
          </button>
        </div>
      )}

      {stockWarn && (
        <div className="fixed right-4 top-36 z-50 flex items-center gap-3 rounded-xl border border-orange-300 bg-orange-50 px-4 py-3 text-sm font-semibold text-orange-700 shadow-pos">
          <span>{stockWarn}</span>
          <button className="ml-auto btn-ghost p-0.5" onClick={() => setStockWarn(null)}>
            <BsXLg size={12} />
          </button>
        </div>
      )}

      {validationWarns.length > 0 && (
        <div className="fixed right-4 top-52 z-50 max-w-md rounded-xl border border-orange-300 bg-orange-50 px-4 py-3 text-sm text-orange-700 shadow-pos">
          <div className="flex items-start justify-between gap-2">
            <p className="font-semibold">No se puede continuar por estas validaciones:</p>
            <button className="btn-ghost p-0.5 shrink-0" onClick={() => setValidationWarns([])}>
              <BsXLg size={12} />
            </button>
          </div>
          <ul className="mt-1 list-disc pl-5">
            {validationWarns.map((message) => (
              <li key={message}>{message}</li>
            ))}
          </ul>
        </div>
      )}

      {showPayModal && (
        <div className="fixed inset-0 z-40 flex items-center justify-center bg-black/40 p-4">
          <div className="card w-full max-w-lg p-5">
            <div className="mb-3 flex items-center justify-between">
              <h4 className="font-semibold">Cobro de la venta</h4>
              <button className="btn-ghost p-1" onClick={() => setShowPayModal(false)}>
                <BsXLg size={14} />
              </button>
            </div>

            <div className="grid gap-2 rounded-xl border border-pos-border bg-gray-50 p-3 text-sm">
              <p>Total venta: <span className="font-semibold">{money.format(total)}</span></p>
              <p>Pagado ahora: <span className="font-semibold text-green-700">{money.format(paid)}</span></p>
              <p>Saldo pendiente: <span className="font-semibold text-orange-700">{money.format(remaining)}</span></p>
              {change > 0 && <p>Vueltos: <span className="font-semibold text-green-700">{money.format(change)}</span></p>}
            </div>

            <div className="mt-3 grid gap-2 md:grid-cols-2">
              <div>
                <button
                  className={activeCalcField === "TRANSFERENCIA" ? "btn-soft w-full border border-green-300 bg-green-50 text-green-700" : "btn-ghost w-full"}
                  onClick={() => setActiveCalcField("TRANSFERENCIA")}
                >
                  Transferencia
                </button>
                <input
                  className="input mt-2"
                  value={formatCurrencyInput(transferAmount)}
                  onChange={(e) => handleCurrencyChange(e.target.value, setTransferAmount)}
                  inputMode="numeric"
                  maxLength={9}
                />
              </div>
              <div>
                <button
                  className={activeCalcField === "EFECTIVO" ? "btn-soft w-full border border-green-300 bg-green-50 text-green-700" : "btn-ghost w-full"}
                  onClick={() => setActiveCalcField("EFECTIVO")}
                >
                  Efectivo
                </button>
                <input
                  className="input mt-2"
                  value={formatCurrencyInput(cashAmount)}
                  onChange={(e) => handleCurrencyChange(e.target.value, setCashAmount)}
                  inputMode="numeric"
                  maxLength={9}
                />
              </div>
            </div>

            <div className="mt-3 grid grid-cols-3 gap-2">
              <button className="btn-soft col-span-3" onClick={setExactPayment}>
                Pagar exacto ({money.format(remaining)})
              </button>
              {["7", "8", "9", "4", "5", "6", "1", "2", "3", "000", "0", "<"].map((k) => (
                <button key={k} className="btn-ghost" onClick={() => pushCalcValue(k)}>
                  {k}
                </button>
              ))}
              <button className="btn-ghost col-span-3" onClick={() => pushCalcValue("C")}>
                Limpiar
              </button>
            </div>

            <div className="mt-3 flex gap-2">
              <button className="btn-ghost flex-1" onClick={() => setShowPayModal(false)}>
                Cancelar
              </button>
              <button
                className="btn-primary flex-1"
                disabled={createSale.isPending}
                onClick={registerSale}
              >
                {remaining > 0 ? "Pagar parcial" : "Confirmar cobro"}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
