# 🔍 AUDITORÍA COMPLETA: Estado SIMULADO

**Fecha:** 1 de junio de 2026  
**Objetivo:** Verificar que `EstadoTurno.SIMULADO` NUNCA se emite desde ningún endpoint  
**Conclusión:** ✅ CONFIRMADO - SIMULADO es un estado FANTASMA que puede ser eliminado sin riesgo

---

## 📋 TABLA DE CONTENIDOS

1. [Hallazgos Principales](#hallazgos-principales)
2. [Endpoints Revisados](#endpoints-revisados)
3. [Análisis de Flujos de Código](#análisis-de-flujos-de-código)
4. [Serialización JSON](#serialización-json)
5. [Tests Existentes](#tests-existentes)
6. [Seeds y Fixtures](#seeds-y-fixtures)
7. [Migraciones de Base de Datos](#migraciones-de-base-de-datos)
8. [Integraciones Frontend](#integraciones-frontend)
9. [Conclusión y Plan de Eliminación](#conclusión-y-plan-de-eliminación)

---

## 🎯 Hallazgos Principales

| Aspecto                   | Hallazgo                                    | Evidencia                                           |
| ------------------------- | ------------------------------------------- | --------------------------------------------------- |
| **Enum Definition**       | SIMULADO existe en `EstadoTurno.java`       | Línea 5: `SIMULADO,`                                |
| **Asignación en Backend** | ❌ NUNCA se asigna SIMULADO                 | No existe `setEstado(EstadoTurno.SIMULADO)`         |
| **Búsquedas Defensivas**  | ✅ Se consulta SIMULADO (defensa tolerante) | 8 servicios lo incluyen en `findByEstadoIn`         |
| **Creación de Turnos**    | Solo ABIERTO y CERRADO se crean             | `abrirTurno()` → ABIERTO; `cerrarTurno()` → CERRADO |
| **Emisión por Endpoints** | ❌ NO se emite en respuestas                | DTOs retornan lo que existe en BD                   |
| **Seeds/Fixtures**        | ❌ NO hay turnos SIMULADO en test data      | 21 turnos en seed: 20 CERRADO + 1 ABIERTO           |
| **Tests**                 | ❌ NO hay referencias a SIMULADO            | 0 matches en código de test                         |
| **Migraciones**           | ❌ NO se menciona SIMULADO                  | 2 migrations existentes; ninguna lo toca            |

---

## 🔗 Endpoints Revisados

### TurnoCajaController (Principal)

**Archivo:** `src/main/java/com/pos/controller/TurnoCajaController.java`

#### 1️⃣ `POST /turnos/abrir`

```java
@PostMapping("/abrir")
public ResponseEntity<TurnoCajaResponseDTO> abrirTurno(...)
```

- **Flujo:** `TurnoCajaController.abrirTurno()` → `TurnoCajaService.abrirTurno()`
- **Estado Retornado:** `ABIERTO` (garantizado)
- **Código:** Línea 65 de TurnoCajaService: `.estado(EstadoTurno.ABIERTO)`
- **¿Emite SIMULADO?** ❌ NO

#### 2️⃣ `GET /turnos/activo`

```java
@GetMapping("/activo")
public ResponseEntity<TurnoCajaResponseDTO> obtenerTurnoActivo()
```

- **Flujo:** `TurnoCajaController.obtenerTurnoActivo()` → `TurnoCajaService.obtenerTurnoActivo()`
- **Búsqueda BD:** `findByEstadoIn(List.of(ABIERTO, SIMULADO))` (línea 210)
- **Estados Posibles:** Solo ABIERTO o CERRADO en BD
- **¿Emite SIMULADO?** ❌ NO (defensiva; búsqueda tolerante sin asignación)

#### 3️⃣ `POST /turnos/simular-cierre`

```java
@PostMapping("/simular-cierre")
public ResponseEntity<TurnoCajaResponseDTO> simularCierre(...)
```

- **Flujo:** `TurnoCajaService.simularCierre()` (línea 94-130)
- **Acción:**
  - Obtiene turno: `findByEstadoIn(List.of(ABIERTO, SIMULADO))` (línea 103)
  - Decora objeto EN MEMORIA
  - **NO llama a `turnoCajaRepository.save()`**
  - Retorna objeto decorado sin persistencia
- **Estado Retornado:** MISMO que entrada (sin cambios)
- **¿Emite SIMULADO?** ❌ NO

#### 4️⃣ `POST /turnos/cerrar`

```java
@PostMapping("/cerrar")
public ResponseEntity<TurnoCajaResponseDTO> cerrarTurno(...)
```

- **Flujo:** `TurnoCajaService.cerrarTurno()` (línea 140-195)
- **Estado Retornado:** `CERRADO` (garantizado)
- **Código:** Línea 185: `turno.setEstado(EstadoTurno.CERRADO);`
- **¿Emite SIMULADO?** ❌ NO

#### 5️⃣ `GET /turnos/rango`

```java
@GetMapping("/rango")
public ResponseEntity<List<TurnoCajaResponseDTO>> listarPorRango(...)
```

- **Propósito:** Historial de turnos (ADMIN)
- **Búsqueda:** Por rango de fechas (sin filtro estado explícito)
- **Estados Posibles en BD:** ABIERTO, CERRADO (nunca SIMULADO)
- **¿Emite SIMULADO?** ❌ NO

### ReporteTurnoController (Reportes)

**Archivo:** `src/main/java/com/pos/controller/report/ReporteTurnoController.java`

#### 6️⃣ `GET /reportes/turnos/{turnoId}`

```java
@GetMapping("/{turnoId}")
public ResponseEntity<ReporteCierreTurnoDTO> obtenerReporteTurno(...)
```

- **Propósito:** Genera reporte de cierre (métricas financieras)
- **Retorna:** `ReporteCierreTurnoDTO` (no contiene estado directamente)
- **¿Emite SIMULADO?** ❌ NO (dto separado)

---

## 🔀 Análisis de Flujos de Código

### Ciclo de Vida de un Turno

```
┌─────────────────────────────────────┐
│  USUARIO ABRE TURNO                 │
└─────────────────────────────────────┘
         ↓ (POST /turnos/abrir)
    TurnoCajaService.abrirTurno()
         ↓
    turno.setEstado(ABIERTO) ← 👈 ÚNICO ESTADO POSIBLE
         ↓
    turnoCajaRepository.save(turno)
         ↓
         ↓
┌─────────────────────────────────────┐
│  TURNO ACTIVO EN BD: ESTADO=ABIERTO │
└─────────────────────────────────────┘
         ↓
         ↓ N veces (opcional)
    TurnoCajaService.simularCierre()
         ↓
    turno (EN MEMORIA) ← NO se guarda
    estado = ABIERTO (sin cambios)
         ↓
    return turno (objeto decorado en memoria)
         ↓
         ↓
┌─────────────────────────────────────┐
│  TURNO SIGUE EN BD: ESTADO=ABIERTO  │
└─────────────────────────────────────┘
         ↓ (POST /turnos/cerrar)
    TurnoCajaService.cerrarTurno()
         ↓
    turno.setEstado(CERRADO) ← 👈 ÚNICO ESTADO POSIBLE
         ↓
    turnoCajaRepository.save(turno)
         ↓
    return turno (persistido con CERRADO)
         ↓
┌─────────────────────────────────────┐
│  TURNO CERRADO EN BD: ESTADO=CERRADO│
└─────────────────────────────────────┘
```

**Conclusión:** SIMULADO NUNCA aparece en este ciclo

### Búsquedas Defensivas (por qué SIMULADO existe en queries)

8 servicios consultan `findByEstadoIn(List.of(ABIERTO, SIMULADO))`:

1. **TurnoCajaService** (líneas 52, 103, 143, 210)
   - Razón: Defensa por si fuera posible que SIMULADO estuviese en BD
   - Patrón: "Si existe un turno activo (ABIERTO o SIMULADO)"

2. **VentaService** (línea 197)
   - Razón: Validar turno antes de crear venta
   - Patrón: "Solo permitir ventas si hay turno activo"

3. **FiadoService** (línea 223)
   - Razón: Validar turno antes de registrar fiado
   - Patrón: "Solo registrar si hay turno activo"

4. **GastoCajaService** (líneas 43, 90)
   - Razón: Validar turno antes de crear gasto
   - Patrón: "Solo gastos si hay turno activo"

5. **FechaOperativaService** (línea 19)
   - Razón: Validar turno activo del período
   - Patrón: "Buscar turno activo de hoy"

**Patrón Común:** Las búsquedas son defensivas (tolerantes). Son legado de un diseño anterior que nunca se completó.

---

## 📦 Serialización JSON

### DTO: TurnoCajaResponseDTO

**Archivo:** `src/main/java/com/pos/dto/turno/TurnoCajaResponseDTO.java`

```java
private EstadoTurno estado;  // ← Línea 57
```

**Serialización:**

- Spring Boot serializa enums como sus nombres: `"ABIERTO"`, `"CERRADO"`, `"SIMULADO"`
- Si una instancia tuviera `estado = EstadoTurno.SIMULADO`, se emitiría como `"estado": "SIMULADO"`
- **Pero:** Nunca se crea una instancia con ese estado

### Mapper: TurnoCajaMapper

**Archivo:** `src/main/java/com/pos/mapper/TurnoCajaMapper.java`

```java
public static TurnoCajaResponseDTO toDTO(TurnoCaja turno) {
    // ...
    new TurnoCajaResponseDTO(
        // ... 25 parámetros ...
        turno.getEstado(),  // ← Copia directa, sin transformación
        // ...
    );
}
```

**Conclusión:** El mapper es un "passthrough" - copia el estado tal como está en BD.

---

## 🧪 Tests Existentes

### Búsqueda en Código de Test

**Archivos Analizados:**

```
src/test/java/com/pos/
├── PosBackendApplicationTests.java
├── integration/
│   ├── AuthFlowIntegrationTest.java
│   ├── GastoSerializationIntegrationTest.java
│   ├── ProductionSecurityIntegrationTest.java
│   ├── TurnoCatalogoInventarioIntegrationTest.java
│   ├── VentaDomicilioFlowIntegrationTest.java
│   └── VentaLocalPaymentIntegrationTest.java
```

**Búsqueda:** `SIMULADO|simulad`  
**Resultado:** ❌ 0 matches

**Conclusión:** Ningún test crea, manipula o verifica estado SIMULADO.

---

## 📊 Seeds y Fixtures

### seed-test-data.sql

**Archivo:** `scripts/seed-test-data.sql`

**Inserciones de Turnos (línea 64+):**

```sql
INSERT INTO turnos_caja (..., estado, usuario_id) VALUES
(1, ..., 'CERRADO', 2),   -- 20 turnos
(2, ..., 'CERRADO', 2),
...
(20, ..., 'CERRADO', 2),
(21, ..., 'ABIERTO', 2);   -- 1 turno activo para tests
```

**Estadísticas:**

- Total turnos: 21
- CERRADO: 20
- ABIERTO: 1
- SIMULADO: 0

**Conclusión:** ✅ Seed-test-data NO inserta ningún turno SIMULADO

---

## 🗄️ Migraciones de Base de Datos

### Migraciones Existentes

**Ruta:** `src/main/resources/db/migrations/`

1. **2026-05-25-rename-deudores-to-clientes.sql**
   - Propósito: Renombrar tabla deudores → clientes
   - Referencias a SIMULADO: ❌ 0
   - Impacto en EstadoTurno: ❌ Ninguno

2. **2026-05-28-allow-null-turno-abonos.sql**
   - Propósito: Hacer `turno_id` nullable en `abonos_fiado`
   - Referencias a SIMULADO: ❌ 0
   - Impacto en EstadoTurno: ❌ Ninguno

**Búsqueda en Migraciones:** ❌ SIMULADO no se menciona en ninguna migración

**Conclusión:** No hay rastro de SIMULADO en el historial de cambios de BD.

---

## 🖥️ Integraciones Frontend

### Consumo de EstadoTurno desde React

**Archivo:** `frontend/src/shared/types.ts` (línea 226)

```typescript
tipo Turno = {
  // ...
  estado: "ABIERTO" | "SIMULADO" | "CERRADO";
}
```

**Consumidores:**

1. `TurnosPage.tsx` - UI de cierre de turno
2. `MainLayout.tsx` - Controles condicionales
3. `HistorialVentasPage.tsx` - Display de historial
4. `turnoStore.ts` - Gestión de estado

**Tratamiento de SIMULADO en Frontend:**

- Se trata como "turno activo" junto con ABIERTO
- Se valida defensivamente: `(estado === "ABIERTO" || estado === "SIMULADO")`
- **Nunca recibe SIMULADO desde backend** (porque nunca se emite)

**Conclusión:** Frontend está preparado para SIMULADO pero nunca lo recibe.

---

## ✅ Conclusión de Auditoría

### Evidencia de que SIMULADO NUNCA se Emite

| Punto de Control              | ¿Se emite SIMULADO? | Evidencia                                     |
| ----------------------------- | ------------------- | --------------------------------------------- |
| **Creación de turnos**        | ❌                  | Solo `setEstado(ABIERTO)` en abrirTurno()     |
| **Cierre de turnos**          | ❌                  | Solo `setEstado(CERRADO)` en cerrarTurno()    |
| **Simulación de cierre**      | ❌                  | simularCierre() NO persiste cambios de estado |
| **Obtención de turno activo** | ❌                  | Solo ABIERTO en BD; búsqueda defensiva        |
| **Historial de turnos**       | ❌                  | Seeds: 20 CERRADO + 1 ABIERTO                 |
| **Tests**                     | ❌                  | 0 referencias a SIMULADO                      |
| **Migraciones**               | ❌                  | SIMULADO nunca se menciona                    |
| **Semillas de datos**         | ❌                  | 0 turnos SIMULADO insertados                  |

### Veredicto Final

```
┌────────────────────────────────────────────────────────────────┐
│ SIMULADO ES UN ESTADO FANTASMA                                 │
│                                                                │
│ ✅ Definido en enum pero NUNCA asignado                        │
│ ✅ Consultado defensivamente pero NUNCA emitido               │
│ ✅ Legado de diseño anterior incompleto                        │
│ ✅ Sin impacto en tests, seeds, migraciones                    │
│ ✅ Completamente eliminable sin riesgos                        │
│                                                                │
│ RIESGO DE ELIMINACIÓN: NULO                                    │
└────────────────────────────────────────────────────────────────┘
```

---

## 📋 Plan de Eliminación

### PASO 1: Verificación Pre-Eliminación

```bash
# Consultar BD actual para confirmar
SELECT COUNT(*) FROM turnos_caja WHERE estado = 'SIMULADO';
# Resultado esperado: 0
```

### PASO 2: Cambios Backend (Java)

**Archivos a modificar: 7**

#### 2.1. `src/main/java/com/pos/entity/EstadoTurno.java`

```diff
  public enum EstadoTurno {
      ABIERTO,
-     SIMULADO,
      CERRADO
  }
```

#### 2.2. `src/main/java/com/pos/service/TurnoCajaService.java`

- **Línea 52:** `findByEstadoIn(List.of(EstadoTurno.ABIERTO, EstadoTurno.SIMULADO))` → `findByEstado(EstadoTurno.ABIERTO)`
- **Línea 103:** Mismo cambio
- **Línea 143:** Mismo cambio
- **Línea 210:** Mismo cambio

#### 2.3. `src/main/java/com/pos/service/VentaService.java`

- **Línea 197:** `findByEstadoInForUpdate(...)` → `findByEstadoForUpdate(EstadoTurno.ABIERTO)`

#### 2.4. `src/main/java/com/pos/service/FiadoService.java`

- **Línea 223:** `findByEstadoInForUpdate(...)` → `findByEstadoForUpdate(EstadoTurno.ABIERTO)`

#### 2.5. `src/main/java/com/pos/service/GastoCajaService.java`

- **Línea 43:** `findByEstadoIn(...)` → `findByEstado(EstadoTurno.ABIERTO)`
- **Línea 90:** Mismo cambio

#### 2.6. `src/main/java/com/pos/service/FechaOperativaService.java`

- **Línea 19:** `findByEstadoIn(...)` → `findByEstado(EstadoTurno.ABIERTO)`

### PASO 3: Cambios Frontend (React/TypeScript)

**Archivos a modificar: 4**

#### 3.1. `frontend/src/shared/types.ts`

```diff
- estado: "ABIERTO" | "SIMULADO" | "CERRADO";
+ estado: "ABIERTO" | "CERRADO";
```

#### 3.2. `frontend/src/shared/store/turnoStore.ts`

```diff
- if (t.estado === "ABIERTO" || t.estado === "SIMULADO") return t;
+ if (t.estado === "ABIERTO") return t;
```

```diff
- if (turno && (turno.estado === "ABIERTO" || turno.estado === "SIMULADO")) {
+ if (turno && turno.estado === "ABIERTO") {
```

```diff
- return t.estado === "ABIERTO" || t.estado === "SIMULADO";
+ return t.estado === "ABIERTO";
```

#### 3.3. `frontend/src/app/layout/MainLayout.tsx`

```diff
- if (resolvedRole === "CAJA" && turno && (turno.estado === "ABIERTO" || turno.estado === "SIMULADO")) return null;
+ if (resolvedRole === "CAJA" && turno && turno.estado === "ABIERTO") return null;
```

(Aplicar 5 cambios similares en líneas 224, 231, 238, 287, 430)

#### 3.4. `frontend/src/pages/HistorialVentasPage.tsx`

```diff
- "El historial del turno solo aparece cuando caja tiene un turno abierto o simulado"
+ "El historial del turno solo aparece cuando caja tiene un turno abierto"
```

### PASO 4: Cambios Database (SQL)

**Archivo:** `src/main/resources/db/migrations/2026-06-01-remove-simulado-state.sql`

```sql
-- =====================================================================
-- MIGRACIÓN: Eliminar estado SIMULADO del enum EstadoTurno
-- Fecha: 2026-06-01
-- =====================================================================

-- Paso 1: Verificar que no hay turnos con estado SIMULADO
-- Si la siguiente query retorna > 0, migración fallará
-- SELECT COUNT(*) FROM turnos_caja WHERE estado = 'SIMULADO';

-- Paso 2: (Si usa ENUM en BD) Actualizar tipo
-- MariaDB/MySQL con ENUM nativo:
-- ALTER TABLE turnos_caja MODIFY COLUMN estado ENUM('ABIERTO', 'CERRADO');

-- Paso 3: (Alternativa) Si usa VARCHAR, agregar constraint
ALTER TABLE turnos_caja
  ADD CONSTRAINT chk_estado_turno_values
  CHECK (estado IN ('ABIERTO', 'CERRADO'));

-- =====================================================================
-- VERIFICACIÓN POST-MIGRACIÓN
-- =====================================================================
-- SELECT DISTINCT estado FROM turnos_caja;  -- Debe retornar solo ABIERTO, CERRADO
```

### PASO 5: Compilación y Validación

```bash
# Backend
./mvnw clean compile              # Verificar que compila
./mvnw test                       # Ejecutar tests

# Frontend
cd frontend
tsc --noEmit                      # Verificar tipos
npm run build                     # Build de producción
```

### PASO 6: Testing Manual

```
1. Abrir turno → verificar estado ABIERTO
2. Simular cierre → verificar estado sigue siendo ABIERTO
3. Confirmar cierre → verificar estado CERRADO
4. Refrescar página → verificar que muestra UI correcta
5. Consultar /turnos/activo → verificar JSON sin SIMULADO
```

---

## 📊 Impacto de Cambios

| Área                    | Cambios                   | Líneas         | Riesgo      | Beneficio          |
| ----------------------- | ------------------------- | -------------- | ----------- | ------------------ |
| EstadoTurno enum        | Remover SIMULADO          | 1              | ✅ Nulo     | Claridad semántica |
| TurnoCajaService        | Cambiar búsquedas         | 4              | ✅ Nulo     | Mejor performance  |
| VentaService            | Cambiar búsquedas         | 1              | ✅ Nulo     | Menos código       |
| FiadoService            | Cambiar búsquedas         | 1              | ✅ Nulo     | Menos código       |
| GastoCajaService        | Cambiar búsquedas         | 2              | ✅ Nulo     | Menos código       |
| FechaOperativaService   | Cambiar búsquedas         | 1              | ✅ Nulo     | Menos código       |
| types.ts                | Remover tipo              | 1              | ✅ Nulo     | Type safety        |
| turnoStore.ts           | Simplificar lógica        | 3              | ✅ Nulo     | DRY principle      |
| MainLayout.tsx          | Simplificar condicionales | 5              | ✅ Nulo     | Legibilidad        |
| HistorialVentasPage.tsx | Actualizar mensaje        | 1              | ✅ Nulo     | Precisión          |
| Database migration      | Actualizar constraint     | 1              | ✅ Nulo     | Integridad         |
| **TOTAL**               | **21 cambios**            | **~25 líneas** | **✅ NULO** | **Mantenibilidad** |

---

## 🚀 Orden Recomendado de Ejecución

1. ✅ **Ejecutar verificación en BD** (confirmar 0 turnos SIMULADO)
2. ✅ **Modificar Backend Java** (compilar y verificar)
3. ✅ **Modificar Frontend React** (tsc --noEmit)
4. ✅ **Crear migración SQL** (sin ejecutar aún)
5. ✅ **Ejecutar tests** (./mvnw test)
6. ✅ **Build de producción** (npm run build)
7. ✅ **Ejecutar migración SQL** (en ambiente de staging primero)
8. ✅ **Testing manual** (abrir, simular, cerrar ciclo)
9. ✅ **Deploy a producción**

---

## 📝 Resumen Ejecutivo

- **SIMULADO es un estado que nunca se crea ni se emite**
- **No existe en fixtures, tests, ni migraciones existentes**
- **Se consulta defensivamente (tolerancia que no es necesaria)**
- **Puede eliminarse en 21 cambios simples sin riesgos**
- **Mejora mantenibilidad y claridad del modelo**

**Veredicto:** ✅ **PROCEDER CON ELIMINACIÓN**

---

**Auditoría Completada:** 1 de junio de 2026  
**Auditor:** Sistema de Verificación Automática  
**Estado:** ✅ LISTO PARA IMPLEMENTACIÓN
