import { useState, useCallback, useEffect, useRef } from "react";
import type { Deudor } from "../types";
import { posApi } from "./api/posApi";

export type ClienteSearchProps = {
  onClienteSeleccionado: (cliente: Deudor) => void;
  onCrearNuevo?: (nombre: string, telefono: string) => void;
  placeholder?: string;
  label?: string;
  permitirCrearInline?: boolean;
  autoFocus?: boolean;
  className?: string;
};

const normalizarTelefono = (tel: string): string => {
  return tel.replace(/\D/g, "").slice(0, 15);
};

const validarTelefono = (tel: string): boolean => {
  const limpio = normalizarTelefono(tel);
  return limpio.length >= 7 && limpio.length <= 15;
};

/**
 * ClienteSearch
 * Componente reutilizable para búsqueda y selección de clientes (deudores).
 *
 * Características:
 * - Búsqueda por nombre o teléfono con debounce
 * - Normalización de teléfono (solo dígitos)
 * - Validación de formato
 * - Opción para crear cliente inline
 * - Integración con API de fiados
 */
export function ClienteSearch({
  onClienteSeleccionado,
  onCrearNuevo,
  placeholder = "Buscar cliente por nombre o teléfono...",
  label,
  permitirCrearInline = true,
  autoFocus = false,
  className = ""
}: ClienteSearchProps) {
  const [searchInput, setSearchInput] = useState("");
  const [sugerencias, setSugerencias] = useState<Deudor[]>([]);
  const [cargando, setCargando] = useState(false);
  const [mostrarDropdown, setMostrarDropdown] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [mostrarFormCreacion, setMostrarFormCreacion] = useState(false);
  const [nombreNuevo, setNombreNuevo] = useState("");
  const [telefonoNuevo, setTelefonoNuevo] = useState("");
  const [creandoCliente, setCreandoCliente] = useState(false);

  const debounceTimeoutRef = useRef<ReturnType<typeof setTimeout> | null>(null);

  // Debounce de búsqueda
  const realizarBusqueda = useCallback(
    async (q: string) => {
      if (!q.trim()) {
        setSugerencias([]);
        setMostrarDropdown(false);
        return;
      }

      setCargando(true);
      setError(null);
      try {
        const resultados = await posApi.buscarClientes(q);
        setSugerencias(resultados || []);
        setMostrarDropdown(true);
      } catch (err) {
        setError(err instanceof Error ? err.message : "Error en búsqueda");
        setSugerencias([]);
      } finally {
        setCargando(false);
      }
    },
    []
  );

  useEffect(() => {
    if (debounceTimeoutRef.current) {
      clearTimeout(debounceTimeoutRef.current);
    }

    if (!searchInput.trim()) {
      setSugerencias([]);
      setMostrarDropdown(false);
      return;
    }

    debounceTimeoutRef.current = setTimeout(() => {
      realizarBusqueda(searchInput);
    }, 300);

    return () => {
      if (debounceTimeoutRef.current) {
        clearTimeout(debounceTimeoutRef.current);
      }
    };
  }, [searchInput, realizarBusqueda]);

  const handleSeleccionarCliente = (cliente: Deudor) => {
    onClienteSeleccionado(cliente);
    setSearchInput("");
    setSugerencias([]);
    setMostrarDropdown(false);
    setError(null);
    setMostrarFormCreacion(false);
    setNombreNuevo("");
    setTelefonoNuevo("");
  };

  const handleCrearCliente = async (e: React.FormEvent) => {
    e.preventDefault();

    const nombre = nombreNuevo.trim();
    const telefono = normalizarTelefono(telefonoNuevo);

    if (!nombre) {
      setError("El nombre es obligatorio");
      return;
    }

    if (!validarTelefono(telefonoNuevo)) {
      setError("El teléfono debe tener entre 7 y 15 dígitos");
      return;
    }

    setCreandoCliente(true);
    setError(null);

    try {
      if (onCrearNuevo) {
        onCrearNuevo(nombre, telefono);
      } else {
        // Crear vía API
        const nuevoCliente = await posApi.crearDeudor({ nombre, telefono });
        handleSeleccionarCliente(nuevoCliente);
      }

      // Limpiar formulario
      setNombreNuevo("");
      setTelefonoNuevo("");
      setMostrarFormCreacion(false);
      setSearchInput("");
      setSugerencias([]);
      setMostrarDropdown(false);
    } catch (err) {
      setError(
        err instanceof Error
          ? err.message.includes("duplicate")
            ? "Este teléfono ya está registrado"
            : err.message
          : "Error al crear cliente"
      );
    } finally {
      setCreandoCliente(false);
    }
  };

  const formatearDeudor = (deudor: Deudor): string => {
    const deuda = deudor.deudaTotal > 0 ? ` (Debe: $${deudor.deudaTotal.toFixed(0)})` : "";
    return `${deudor.nombre} - ${deudor.telefono}${deuda}`;
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

      {error && (
        <div className="mt-1 text-sm text-red-600 bg-red-50 px-2 py-1 rounded">
          {error}
        </div>
      )}

      {/* Dropdown de sugerencias */}
      {mostrarDropdown && sugerencias.length > 0 && (
        <div className="absolute z-10 w-full bg-white border border-gray-300 rounded-md shadow-lg mt-1 max-h-64 overflow-y-auto">
          <ul className="py-1">
            {sugerencias.map((deudor) => (
              <li key={deudor.id}>
                <button
                  type="button"
                  onClick={() => handleSeleccionarCliente(deudor)}
                  className="w-full text-left px-3 py-2 hover:bg-blue-50 focus:outline-none focus:bg-blue-100 text-sm"
                >
                  <div className="font-medium text-gray-900">
                    {deudor.nombre}
                  </div>
                  <div className="text-xs text-gray-500">
                    {deudor.telefono}
                    {deudor.deudaTotal > 0 && (
                      <span className="ml-2 font-semibold text-red-600">
                        Debe: ${deudor.deudaTotal.toFixed(0)}
                      </span>
                    )}
                  </div>
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

          {permitirCrearInline && !mostrarFormCreacion && (
            <button
              type="button"
              onClick={() => {
                setMostrarFormCreacion(true);
                // Detectar si es teléfono o nombre
                if (normalizarTelefono(searchInput).length >= 7) {
                  setTelefonoNuevo(normalizarTelefono(searchInput));
                  setNombreNuevo("");
                } else {
                  setNombreNuevo(searchInput);
                  setTelefonoNuevo("");
                }
              }}
              className="w-full px-2 py-1 text-xs bg-blue-500 text-white rounded hover:bg-blue-600"
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
              className="flex-1 px-2 py-1 text-xs bg-green-500 text-white rounded hover:bg-green-600 disabled:bg-gray-300 disabled:cursor-not-allowed"
            >
              {creandoCliente ? "Creando..." : "Crear"}
            </button>
            <button
              type="button"
              onClick={() => {
                setMostrarFormCreacion(false);
                setNombreNuevo("");
                setTelefonoNuevo("");
              }}
              className="flex-1 px-2 py-1 text-xs bg-gray-300 text-gray-700 rounded hover:bg-gray-400"
            >
              Cancelar
            </button>
          </div>
        </div>
      )}
    </div>
  );
}
