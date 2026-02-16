# Frontend POS

## Requisitos
- Node.js 18+

## Ejecutar en desarrollo
```bash
cd frontend
npm install
npm run dev
```

Por defecto usa proxy a `http://localhost:8080` para rutas `/api/*`.

## Variables opcionales
- `VITE_API_BASE`: URL base del backend (ej. `http://localhost:8080`).

Si no se define, usa rutas relativas y el proxy de Vite.

