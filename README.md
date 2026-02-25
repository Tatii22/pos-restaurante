# POS Restaurante

Backend y frontend para un sistema POS de restaurante con control de ventas, turnos de caja, inventario, gastos, reportes y exportaciones.

## Stack
- Backend: Java 17, Spring Boot 3.5, Spring Security (JWT), Spring Data JPA
- Base de datos: MySQL (runtime), H2 (tests)
- Frontend: React 18, TypeScript, Vite, React Query, Zustand
- Build: Maven Wrapper + npm

## Estructura del proyecto
- `src/main/java`: API backend (controladores, servicios, seguridad, repositorios)
- `src/main/resources`: configuraciones (`application*.yaml`)
- `src/test`: pruebas de integracion y contexto
- `frontend`: aplicacion web React
- `.github/workflows/ci.yml`: pipeline CI (backend + frontend en paralelo)

## API y seguridad
- Prefijo global API: `/api/v1` (ver `WebApiConfig`)
- Login publico: `POST /api/v1/auth/login`
- Resto de endpoints: autenticados con token Bearer JWT
- Swagger/OpenAPI:
  - No prod: habilitado
  - Prod: bloqueado y deshabilitado
- CORS configurable por `app.cors.allowed-origins`

## Requisitos
- Java 17
- Node.js 20+ (recomendado)
- npm 10+ (recomendado)
- MySQL 8+ para desarrollo/produccion

## Configuracion por perfil

### `application.yaml` (base)
Variables requeridas:
- `DB_URL`
- `DB_USERNAME`
- `DB_PASSWORD`
- `JWT_SECRET` (Base64, longitud segura)

Variables opcionales:
- `JPA_DDL_AUTO` (default: `validate`)
- `JPA_SHOW_SQL` (default: `false`)
- `JWT_EXPIRATION_MS` (default: `36000000`)
- `CORS_ALLOWED_ORIGINS` (default: `http://localhost:5173`)

### `application-local.yaml`
Configuracion local de ejemplo:
- MySQL en `localhost:3309/restaurante_db`
- `ddl-auto: update`
- `show-sql: true`

### `application-prod.yaml`
- `ddl-auto: validate`
- `show-sql: false`
- `open-in-view: false`
- `springdoc` deshabilitado
- `CORS_ALLOWED_ORIGINS` obligatorio

## Ejecucion local

### 1) Backend
```bash
# PowerShell / Bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

Backend por defecto en `http://localhost:8080`.

### 2) Frontend
```bash
cd frontend
npm install
npm run dev
```

Frontend por defecto en `http://localhost:5173` con proxy de `/api` hacia `http://localhost:8080`.

## Build para produccion

### Backend
```bash
./mvnw -DskipTests package
```

Artefacto generado en `target/`.

### Frontend
```bash
cd frontend
npm ci
npm run build
```

Salida en `frontend/dist/`.

## Pruebas
```bash
./mvnw clean test
```

Pruebas actuales:
- `AuthFlowIntegrationTest`
- `ProductionSecurityIntegrationTest`
- `VentaDomicilioFlowIntegrationTest`
- `PosBackendApplicationTests`

## CI (GitHub Actions)
Workflow: `.github/workflows/ci.yml`

Se ejecuta en `push` y `pull_request` con dos jobs paralelos:
- `backend`: `mvn -B clean test` + `mvn -B -DskipTests package`
- `frontend`: `npm ci --prefix frontend` + `npm --prefix frontend run build`

Incluye cache de Maven y npm.

## Notas de operacion
- `open-in-view` esta deshabilitado en base/prod/test para evitar consultas fuera de capa de servicio.
- Si cambias variables sensibles (`JWT_SECRET`, DB credentials), actualiza secretos del entorno de despliegue.
- No subir logs de ejecucion (`spring-run*.log`) ni secretos al repositorio.

## Troubleshooting rapido
- Error de DB al iniciar backend: valida `DB_URL`, usuario, contrasena y puerto MySQL.
- Error 403/401 en endpoints: revisa token Bearer y rol del usuario.
- CORS bloqueado: revisa `CORS_ALLOWED_ORIGINS`.
- Frontend no conecta al backend: verifica proxy de Vite o URL base de API.
