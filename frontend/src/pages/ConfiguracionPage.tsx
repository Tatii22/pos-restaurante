export function ConfiguracionPage() {
  return (
    <div className="grid gap-4">
      <h2 className="text-2xl font-semibold">Configuración</h2>
      <div className="card p-4">
        <p className="mb-2 font-medium">Ajustes del sistema POS</p>
        <ul className="list-disc pl-5 text-sm text-pos-muted">
          <li>Nombre del restaurante</li>
          <li>Formato tickets térmicos</li>
          <li>Políticas de descuento</li>
          <li>Parámetros de impresión</li>
        </ul>
      </div>
    </div>
  );
}
