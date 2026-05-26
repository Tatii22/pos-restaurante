import { useState, useCallback, useEffect, useRef } from "react";
import type { ClienteSearch as ClienteSearchType, Cliente } from "../types";
import { posApi } from "./api/posApi";

export type ClienteSearchProps = {
  onSelect: (cliente: ClienteSearchType) => void;
  onCrearNuevo?: (nombre: string, telefono: string) => Promise<ClienteSearchType | Cliente> | ClienteSearchType | Cliente;
  placeholder?: string;
  label?: string;
  allowCreate?: boolean;
  autoFocus?: boolean;
  className?: string;
  showDebt?: boolean;
  initialQuery?: string;
};

const normalizarTelefono = (tel: string): string => tel.replace(/\D/g, "").slice(0, 15);
const validarTelefono = (tel: string): boolean => {
  const limpio = normalizarTelefono(tel);
  return limpio.length >= 7 && limpio.length <= 15;
};

export function useClienteAutocomplete(query: string) {
  const [results, setResults] = useState<ClienteSearchType[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const debounceRef = useRef<ReturnType<typeof setTimeout> | null>(null);

  const buscar = useCallback(async (q: string) => {
    if (!q.trim()) { setResults([]); return; }
    setLoading(true);
    setError(null);
    try {
      const res = await posApi.buscarClientesLigero(q);
      setResults(res || []);
    } catch (e) {
      setError(e instanceof Error ? e.message : "Error en búsqueda");
      setResults([]);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    if (debounceRef.current) clearTimeout(debounceRef.current);
    if (!query.trim()) { setResults([]); return; }
    debounceRef.current = setTimeout(() => buscar(query), 300);
    return () => { if (debounceRef.current) clearTimeout(debounceRef.current); };
  }, [query, buscar]);

  return { results, loading, error };
}

export function ClienteSearch({
  onSelect,
  onCrearNuevo,
  placeholder = "Buscar cliente por nombre o teléfono...",
  label,
  allowCreate = true,
  autoFocus = false,
  className = "",
  showDebt = true,
  initialQuery = "",
}: ClienteSearchProps) {
  const [searchInput, setSearchInput] = useState(initialQuery);
  // Controlamos la visibilidad del panel con un ref de "interacción activa"
  // para no depender del blur nativo que falla en móvil.
  const [panelAbierto, setPanelAbierto] = useState(false);
  const [mostrarFormCreacion, setMostrarFormCreacion] = useState(false);
  const [nombreNuevo, setNombreNuevo] = useState("");
  const [telefonoNuevo, setTelefonoNuevo] = useState("");
  const [creandoCliente, setCreandoCliente] = useState(false);
  const [formError, setFormError] = useState<string | null>(null);

  const contenedorRef = useRef<HTMLDivElement>(null);
  const { results: sugerencias, loading: cargando, error: searchError } = useClienteAutocomplete(searchInput);

   // Abre el panel cada vez que hay texto en el input
   useEffect(() => {
     if (searchInput.trim() && !panelAbierto) setPanelAbierto(true);
     else if (!searchInput.trim()) { setPanelAbierto(false); setMostrarFormCreacion(false); }
   }, [searchInput, panelAbierto]);

  // Cierra el panel cuando el usuario hace clic fuera del componente completo
  useEffect(() => {
    function handleClickFuera(e: MouseEvent) {
      if (contenedorRef.current && !contenedorRef.current.contains(e.target as Node)) {
        setPanelAbierto(false);
        setMostrarFormCreacion(false);
      }
    }
    document.addEventListener("mousedown", handleClickFuera);
    return () => document.removeEventListener("mousedown", handleClickFuera);
  }, []);

  const handleSeleccionarCliente = (cliente: ClienteSearchType) => {
    onSelect(cliente);
    setSearchInput("");
    setPanelAbierto(false);
    setMostrarFormCreacion(false);
    setNombreNuevo("");
    setTelefonoNuevo("");
    setFormError(null);
  };

  const handleCrearCliente = async (e: React.FormEvent) => {
    e.preventDefault();
    const nombre = nombreNuevo.trim();
    const telefono = normalizarTelefono(telefonoNuevo);
    if (!nombre) { setFormError("El nombre es obligatorio"); return; }
    if (!validarTelefono(telefonoNuevo)) { setFormError("El teléfono debe tener entre 7 y 15 dígitos"); return; }

    setCreandoCliente(true);
    setFormError(null);
    try {
      let nuevoCliente: ClienteSearchType | Cliente;
      if (onCrearNuevo) {
        nuevoCliente = await onCrearNuevo(nombre, telefono);
      } else {
        nuevoCliente = await posApi.crearCliente({ nombre, telefono });
      }
      const clienteSearch: ClienteSearchType = {
        id: nuevoCliente.id,
        nombre: nuevoCliente.nombre,
        telefono: nuevoCliente.telefono,
        direccionPredeterminada: nuevoCliente.direccionPredeterminada ?? null,
        deudaActual: "deudaActual" in nuevoCliente ? nuevoCliente.deudaActual : 0,
        tieneDeuda: "tieneDeuda" in nuevoCliente ? nuevoCliente.tieneDeuda : false,
      };
      handleSeleccionarCliente(clienteSearch);
    } catch (err) {
      const msg = err instanceof Error ? err.message : "Error al crear cliente";
      setFormError(
        msg.toLowerCase().includes("duplicate") || msg.includes("ya está")
          ? "Este teléfono ya está registrado"
          : msg
      );
    } finally {
      setCreandoCliente(false);
    }
  };

  const fmt = (n: number) => `$${n.toLocaleString("es-CO", { maximumFractionDigits: 0 })}`;

  const hayResultados = sugerencias.length > 0;
  const hayTexto = searchInput.trim().length > 0;
  const mostrarPanel = panelAbierto && hayTexto;

  return (
    <div ref={contenedorRef} className={`relative ${className}`}>
      {label && (
        <label className="block text-sm font-medium text-gray-700 mb-1">{label}</label>
      )}

      <div className="relative">
         <input
           type="text"
           value={searchInput}
           onChange={(e) => setSearchInput(e.target.value)}
           autoFocus={autoFocus}
           placeholder={placeholder}
           className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500 text-sm"
         />
        {cargando && (
          <div className="absolute right-3 top-2.5 text-gray-400">
            <span className="inline-block w-4 h-4 border-2 border-gray-200 border-t-blue-500 rounded-full animate-spin" />
          </div>
        )}
      </div>

      {(formError || searchError) && (
        <div className="mt-1 text-sm text-red-600 bg-red-50 px-2 py-1 rounded">
          {formError || searchError}
        </div>
      )}

      {mostrarPanel && (
        <div className="absolute z-30 w-full bg-white border border-gray-300 rounded-md shadow-lg mt-1 max-h-72 overflow-y-auto">
          {/* Lista de sugerencias */}
          {hayResultados && (
            <ul className="py-1">
              {sugerencias.map((cliente) => (
                <li key={cliente.id}>
                  {/* onMouseDown en lugar de onClick para que dispare antes del blur */}
                  <button
                    type="button"
                    onMouseDown={(e) => { e.preventDefault(); handleSeleccionarCliente(cliente); }}
                    className="w-full text-left px-3 py-2 hover:bg-blue-50 focus:bg-blue-100 focus:outline-none text-sm transition-colors"
                  >
                    <div className="font-medium text-gray-900">{cliente.nombre}</div>
                    <div className="text-xs text-gray-500 flex justify-between items-center">
                      <span>{cliente.telefono}</span>
                      {showDebt && cliente.tieneDeuda && (
                        <span className="font-semibold text-red-600">Debe: {fmt(cliente.deudaActual)}</span>
                      )}
                    </div>
                    {cliente.direccionPredeterminada && (
                      <div className="text-xs text-gray-400 truncate">{cliente.direccionPredeterminada}</div>
                    )}
                  </button>
                </li>
              ))}
            </ul>
          )}

          {/* Sin resultados */}
          {!cargando && !hayResultados && (
            <div className="p-3">
              <p className="text-sm text-gray-500 mb-2">Sin resultados para "{searchInput}"</p>
              {allowCreate && !mostrarFormCreacion && (
                <button
                  type="button"
                  onMouseDown={(e) => {
                    e.preventDefault();
                    setMostrarFormCreacion(true);
                    setFormError(null);
                    const soloDigitos = normalizarTelefono(searchInput);
                    if (soloDigitos.length >= 7) {
                      setTelefonoNuevo(soloDigitos);
                      setNombreNuevo("");
                    } else {
                      setNombreNuevo(searchInput.trim());
                      setTelefonoNuevo("");
                    }
                  }}
                  className="w-full px-2 py-1.5 text-xs bg-blue-500 text-white rounded hover:bg-blue-600 transition-colors"
                >
                  + Crear nuevo cliente
                </button>
              )}
            </div>
          )}

          {/* Formulario de creación inline */}
          {mostrarFormCreacion && (
            <div className="p-3 border-t border-gray-100 space-y-2">
              <p className="text-xs font-semibold text-gray-700">Nuevo cliente</p>
              <div>
                <label className="text-xs text-gray-600 block mb-0.5">Nombre *</label>
                <input
                  type="text"
                  value={nombreNuevo}
                  onChange={(e) => setNombreNuevo(e.target.value)}
                  placeholder="Nombre del cliente"
                  autoFocus
                  className="w-full px-2 py-1 border border-gray-300 rounded text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
                />
              </div>
              <div>
                <label className="text-xs text-gray-600 block mb-0.5">Teléfono *</label>
                <input
                  type="tel"
                  value={telefonoNuevo}
                  onChange={(e) => setTelefonoNuevo(normalizarTelefono(e.target.value))}
                  placeholder="Ej: 3123456789"
                  className="w-full px-2 py-1 border border-gray-300 rounded text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
                />
                {telefonoNuevo && !validarTelefono(telefonoNuevo) && (
                  <p className="text-xs text-red-600 mt-0.5">Debe tener entre 7 y 15 dígitos</p>
                )}
              </div>
              {formError && <p className="text-xs text-red-600">{formError}</p>}
              <div className="flex gap-2">
                <button
                  type="button"
                  onMouseDown={(e) => { e.preventDefault(); handleCrearCliente(e); }}
                  disabled={!nombreNuevo.trim() || !validarTelefono(telefonoNuevo) || creandoCliente}
                  className="flex-1 px-2 py-1 text-xs bg-green-500 text-white rounded hover:bg-green-600 disabled:bg-gray-300 disabled:cursor-not-allowed transition-colors"
                >
                  {creandoCliente ? "Creando..." : "Crear"}
                </button>
                <button
                  type="button"
                  onMouseDown={(e) => {
                    e.preventDefault();
                    setMostrarFormCreacion(false);
                    setNombreNuevo("");
                    setTelefonoNuevo("");
                    setFormError(null);
                  }}
                  className="flex-1 px-2 py-1 text-xs bg-gray-200 text-gray-700 rounded hover:bg-gray-300 transition-colors"
                >
                  Cancelar
                </button>
              </div>
            </div>
          )}
        </div>
      )}
    </div>
  );
}
