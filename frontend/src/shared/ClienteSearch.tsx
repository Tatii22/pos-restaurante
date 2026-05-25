import { useState, useCallback, useEffect, useRef } from "react";
import type { ClienteSearch as ClienteSearchType, Cliente } from "../types";
import { posApi } from "./api/posApi";

/**
 * Props para el componente ClienteSearch
 * 
 * @property onSelect - Callback cuando se selecciona un cliente
 * @property onCrearNuevo - Callback para crear nuevo cliente (override de API)
 * @property placeholder - Placeholder del input
 * @property label - Label del campo
 * @property allowCreate - Permitir crear cliente inline (default: true)
 * @property autoFocus - Auto-enfocar el input
 * @property className - Clases CSS adicionales
 * @property showDebt - Mostrar deuda en el listado (default: true)
 * @property initialQuery - Query inicial para búsqueda
 */
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

const normalizarTelefono = (tel: string): string => {
  return tel.replace(/\D/g, "").slice(0, 15);
};

const validarTelefono = (tel: string): boolean => {
  const limpio = normalizarTelefono(tel);
  return limpio.length >= 7 && limpio.length <= 15;
};

/**
 * Hook reutilizable para autocomplete de clientes.
 * Maneja debounce (300ms), normalización, llamada a buscarClientesLigero.
 * Usado internamente por ClienteSearch y directamente en formularios con inputs clásicos.
 */
export function useClienteAutocomplete(query: string) {
  const [results, setResults] = useState<ClienteSearchType[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const debounceRef = useRef<ReturnType<typeof setTimeout> | null>(null);

  const buscar = useCallback(async (q: string) => {
    if (!q.trim()) {
      setResults([]);
      return;
    }
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
    if (!query.trim()) {
      setResults([]);
      return;
    }
    debounceRef.current = setTimeout(() => buscar(query), 300);
    return () => {
      if (debounceRef.current) clearTimeout(debounceRef.current);
    };
  }, [query, buscar]);

  return { results, loading, error, buscar };
}

/**
 * ClienteSearch - Componente reutilizable para búsqueda y selección de clientes.
 * 
 * Características:
 * - Búsqueda rápida por nombre o teléfono
 * - Ordenamiento inteligente (exacta > parcial > nombre)
 * - Normalización de teléfono
 * - Creación inline de clientes
 * - Indicador de deuda
  * - Reutilizable en VentasPage, DomiciliosPage, ClientesPage
 * 
 * Ejemplo de uso:
 * 
 * ```tsx
 * const handleSelectCliente = (cliente: ClienteSearch) => {
 *   console.log(cliente.id, cliente.nombre, cliente.telefono);
 * };
 * 
 * <ClienteSearch
 *   onSelect={handleSelectCliente}
 *   label="Cliente"
 *   placeholder="Buscar o crear cliente..."
 *   showDebt={true}
 * />
 * ```
 */
export function ClienteSearch({
  onSelect,
  onCrearNuevo,
  placeholder = "Buscar cliente por nombre o teléfono...",
  label,
  allowCreate = true,
  autoFocus = false,
  className = "",
  showDebt = true,
  initialQuery = ""
}: ClienteSearchProps) {
  const [searchInput, setSearchInput] = useState(initialQuery);
  const [mostrarDropdown, setMostrarDropdown] = useState(false);
  const [mostrarFormCreacion, setMostrarFormCreacion] = useState(false);
  const [nombreNuevo, setNombreNuevo] = useState("");
  const [telefonoNuevo, setTelefonoNuevo] = useState("");
  const [creandoCliente, setCreandoCliente] = useState(false);
  const [formError, setFormError] = useState<string | null>(null);

  const inputRef = useRef<HTMLInputElement>(null);

  const { results: sugerencias, loading: cargando, error: searchError } = useClienteAutocomplete(searchInput);

  const handleSeleccionarCliente = (cliente: ClienteSearchType) => {
    onSelect(cliente);
    setSearchInput("");
    setMostrarDropdown(false);
    setMostrarFormCreacion(false);
    setNombreNuevo("");
    setTelefonoNuevo("");
    setFormError(null);
  };

  const handleCrearCliente = async (e: React.FormEvent) => {
    e.preventDefault();

    const nombre = nombreNuevo.trim();
    const telefono = normalizarTelefono(telefonoNuevo);

    if (!nombre) {
      setFormError("El nombre es obligatorio");
      return;
    }

    if (!validarTelefono(telefonoNuevo)) {
      setFormError("El teléfono debe tener entre 7 y 15 dígitos");
      return;
    }

    setCreandoCliente(true);
    setFormError(null);

    try {
      let nuevoCliente: ClienteSearchType | Cliente;
      
      if (onCrearNuevo) {
        nuevoCliente = await onCrearNuevo(nombre, telefono);
      } else {
        // Crear vía API
        nuevoCliente = await posApi.crearCliente({ nombre, telefono });
      }

      // Convertir a ClienteSearchType si es necesario
      const clienteSearch: ClienteSearchType = {
        id: nuevoCliente.id,
        nombre: nuevoCliente.nombre,
        telefono: nuevoCliente.telefono,
        direccionPredeterminada: nuevoCliente.direccionPredeterminada,
        deudaActual: "deudaActual" in nuevoCliente ? nuevoCliente.deudaActual : 0,
        tieneDeuda: "tieneDeuda" in nuevoCliente ? nuevoCliente.tieneDeuda : false
      };

      handleSeleccionarCliente(clienteSearch);
    } catch (err) {
      setFormError(
        err instanceof Error
          ? err.message.includes("duplicate") || err.message.includes("ya está")
            ? "Este teléfono ya está registrado"
            : err.message
          : "Error al crear cliente"
      );
    } finally {
      setCreandoCliente(false);
    }
  };

  const formatearDeuda = (deuda: number): string => {
    return `$${deuda.toLocaleString("es-CO", { maximumFractionDigits: 0 })}`;
  };

  return (
    <div className={`relative ${className}`}>
      {label && (
        <label className="block text-sm font-medium text-gray-700 mb-1">
          {label}
        </label>
      )}

      <div className="relative">
        <input
          ref={inputRef}
          type="text"
          value={searchInput}
          onChange={(e) => setSearchInput(e.target.value)}
          onFocus={() => searchInput && setMostrarDropdown(true)}
          onBlur={() => {
            // Retrasar cierre para permitir click en sugerencias
            setTimeout(() => setMostrarDropdown(false), 200);
          }}
          autoFocus={autoFocus}
          placeholder={placeholder}
          className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500 text-sm"
        />

        {cargando && (
          <div className="absolute right-3 top-2.5 text-gray-500">
            <span className="inline-block w-4 h-4 border-2 border-gray-300 border-t-blue-500 rounded-full animate-spin" />
          </div>
        )}
      </div>

      {(formError || searchError) && (
        <div className="mt-1 text-sm text-red-600 bg-red-50 px-2 py-1 rounded">
          {formError || searchError}
        </div>
      )}

      {/* Dropdown de sugerencias */}
      {mostrarDropdown && sugerencias.length > 0 && (
        <div className="absolute z-10 w-full bg-white border border-gray-300 rounded-md shadow-lg mt-1 max-h-64 overflow-y-auto">
          <ul className="py-1">
            {sugerencias.map((cliente) => (
              <li key={cliente.id}>
                <button
                  type="button"
                  onClick={() => handleSeleccionarCliente(cliente)}
                  className="w-full text-left px-3 py-2 hover:bg-blue-50 focus:outline-none focus:bg-blue-100 text-sm transition-colors"
                >
                  <div className="font-medium text-gray-900">
                    {cliente.nombre}
                  </div>
                  <div className="text-xs text-gray-500 flex justify-between items-center">
                    <span>{cliente.telefono}</span>
                    {showDebt && cliente.tieneDeuda && (
                      <span className="font-semibold text-red-600">
                        Debe: {formatearDeuda(cliente.deudaActual)}
                      </span>
                    )}
                  </div>
                  {cliente.direccionPredeterminada && (
                    <div className="text-xs text-gray-400 truncate">
                      {cliente.direccionPredeterminada}
                    </div>
                  )}
                </button>
              </li>
            ))}
          </ul>
        </div>
      )}

      {/* Mensaje si no hay resultados */}
      {mostrarDropdown && !cargando && sugerencias.length === 0 && searchInput.trim() && (
        <div className="absolute z-10 w-full bg-white border border-gray-300 rounded-md shadow-lg mt-1 p-3">
          <div className="text-sm text-gray-600 mb-3">
            No se encontraron clientes para "{searchInput}"
          </div>

          {allowCreate && !mostrarFormCreacion && (
            <button
              type="button"
              onClick={() => {
                setMostrarFormCreacion(true);
                setFormError(null);
                // Detectar si es teléfono o nombre
                if (normalizarTelefono(searchInput).length >= 7) {
                  setTelefonoNuevo(normalizarTelefono(searchInput));
                  setNombreNuevo("");
                } else {
                  setNombreNuevo(searchInput);
                  setTelefonoNuevo("");
                }
              }}
              className="w-full px-2 py-1 text-xs bg-blue-500 text-white rounded hover:bg-blue-600 transition-colors"
            >
              + Crear nuevo cliente
            </button>
          )}
        </div>
      )}

      {/* Formulario de creación inline */}
      {mostrarFormCreacion && (
        <div className="absolute z-20 w-full bg-white border border-gray-300 rounded-md shadow-lg mt-1 p-3 space-y-2">
          <div>
            <label className="text-xs text-gray-700 block mb-1">Nombre</label>
            <input
              type="text"
              value={nombreNuevo}
              onChange={(e) => setNombreNuevo(e.target.value)}
              placeholder="Nombre del cliente"
              className="w-full px-2 py-1 border border-gray-300 rounded text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
              autoFocus
            />
          </div>

          <div>
            <label className="text-xs text-gray-700 block mb-1">Teléfono</label>
            <input
              type="tel"
              value={telefonoNuevo}
              onChange={(e) => setTelefonoNuevo(normalizarTelefono(e.target.value))}
              placeholder="Ej: 3123456789"
              className="w-full px-2 py-1 border border-gray-300 rounded text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
            />
            {telefonoNuevo && !validarTelefono(telefonoNuevo) && (
              <div className="text-xs text-red-600 mt-1">
                Debe tener entre 7 y 15 dígitos
              </div>
            )}
          </div>

          <div className="flex gap-2">
            <button
              type="submit"
              onClick={handleCrearCliente}
              disabled={!nombreNuevo.trim() || !validarTelefono(telefonoNuevo) || creandoCliente}
              className="flex-1 px-2 py-1 text-xs bg-green-500 text-white rounded hover:bg-green-600 disabled:bg-gray-300 disabled:cursor-not-allowed transition-colors"
            >
              {creandoCliente ? "Creando..." : "Crear"}
            </button>
            <button
              type="button"
              onClick={() => {
                setMostrarFormCreacion(false);
                setNombreNuevo("");
                setTelefonoNuevo("");
                setFormError(null);
              }}
              className="flex-1 px-2 py-1 text-xs bg-gray-300 text-gray-700 rounded hover:bg-gray-400 transition-colors"
            >
              Cancelar
            </button>
          </div>
        </div>
      )}
    </div>
  );
}
