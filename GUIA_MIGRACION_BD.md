# Guía de Migración de Base de Datos: Deudores → Clientes

## Estado Actual
✅ **Backend**: Compilación exitosa - Todos los cambios aplicados  
✅ **Frontend**: Build exitoso (npm run build completado)  
⏳ **Base de Datos**: PENDIENTE - Requiere ejecución manual del script SQL

## Resumen de Cambios Realizados

### Backend Java
- ✅ Entity `Cliente.java` renombrada desde `Deudor.java`
- ✅ Repository `ClienteRepository.java` con queries correctas
- ✅ Service `FiadoService.java` actualizado a nomenclatura Cliente
- ✅ Controller `FiadoController.java` con endpoints `/fiados/clientes` y legacy endpoints marcados `@Deprecated`
- ✅ DTOs usando registros: `ClienteResponseDTO`, `ClienteCreateDTO`, `ClienteSearchDTO`, `ClienteDetalleDTO`
- ✅ Maven build: **BUILD SUCCESS** (16.494 segundos)

### Frontend React/TypeScript
- ✅ `posApi.ts`: Métodos legacy removidos, nuevos métodos activos (`getClientes`, `getClienteById`, etc.)
- ✅ `ClientesPage.tsx`: Refactor completo a nomenclatura Cliente, State hooks corregidos
- ✅ `DomiciliosPage.tsx`: Payload de mutaciones actualizado a `clienteId`
- ✅ `HistorialVentasPage.tsx`: Llamadas API actualizadas, etiquetas de UI renovadas
- ✅ `MainLayout.tsx`: Navegación, queries y mutation payloads migraciones
- ✅ `VentasPage.tsx`: Estructura JSX de "Modo fiado" corregida
- ✅ Frontend build: **ÉXITO** (vite build completado)

---

## PASO 1: Preparación (CRÍTICO - Hacer PRIMERO)

### ⚠️ HACER BACKUP INMEDIATAMENTE

```bash
# Desde tu cliente MySQL/MariaDB
SHOW DATABASES;

# Backup de la BD actual (reemplaza 'pos_restaurante' con tu nombre real)
mysqldump -h [HOST] -u [USERNAME] -p pos_restaurante > backup_antes_migracion.sql
```

**Almacena el backup en un lugar seguro. Si algo falla, podrás restaurar.**

---

## PASO 2: Conectar a la Base de Datos

### Opción A: MySQL Command Line (Recomendado)

```bash
# Conectar a la BD
mysql -h [HOST] -u [USERNAME] -p

# Dentro del cliente MySQL, selecciona la BD
USE pos_restaurante;
```

### Opción B: MySQL Workbench o DBeaver
1. Abre tu cliente SQL preferido
2. Conecta con tus credenciales de BD
3. Selecciona la base de datos `pos_restaurante`

---

## PASO 3: Ejecutar la Migración SQL

### 📋 Script SQL Ubicado En:
```
src/main/resources/db/migrations/2026-05-25-rename-deudores-to-clientes.sql
```

### Pasos de Ejecución:

**1. Verifica el estado ANTES de migrar:**
```sql
-- Ver estructura actual
SHOW TABLES;
SHOW CREATE TABLE deudores;
SHOW CREATE TABLE ventas;
SHOW CREATE TABLE abonos_fiado;

-- Contar datos
SELECT COUNT(*) as total_deudores FROM deudores;
SELECT COUNT(*) as total_ventas_con_deudor FROM ventas WHERE deudor_id IS NOT NULL;
SELECT COUNT(*) as total_abonos FROM abonos_fiado;
```

**2. Copia el contenido completo del archivo SQL:**
- Abre: `src/main/resources/db/migrations/2026-05-25-rename-deudores-to-clientes.sql`
- Copia TODO el contenido desde `START TRANSACTION;` hasta `COMMIT;`

**3. Pega en tu cliente MySQL y ejecuta:**
```sql
-- Pega aquí todo el contenido del archivo
START TRANSACTION;
RENAME TABLE deudores TO clientes;
-- ... resto del script ...
COMMIT;
```

**4. Verifica el estado DESPUÉS de migrar:**
```sql
-- Ver nueva estructura
SHOW TABLES;
SHOW CREATE TABLE clientes;
SHOW CREATE TABLE ventas;
SHOW CREATE TABLE abonos_fiado;

-- Contar datos (deben ser iguales a antes)
SELECT COUNT(*) as total_clientes FROM clientes;
SELECT COUNT(*) as total_ventas_con_cliente FROM ventas WHERE cliente_id IS NOT NULL;
SELECT COUNT(*) as total_abonos FROM abonos_fiado;

-- Verificar integridad referencial
SELECT 
    v.id as venta_id, 
    v.cliente_id, 
    c.nombre as cliente_nombre
FROM ventas v
LEFT JOIN clientes c ON v.cliente_id = c.id
WHERE v.cliente_id IS NOT NULL
LIMIT 5;

SELECT 
    a.id as abono_id,
    a.cliente_id,
    c.nombre as cliente_nombre
FROM abonos_fiado a
LEFT JOIN clientes c ON a.cliente_id = c.id
LIMIT 5;
```

---

## PASO 4: Validación Post-Migración (En la Aplicación)

### A. Detener y Limpiar

```bash
cd pos-backend

# Limpiar cache de compilación
./mvnw.cmd clean

# Opcional: Limpiar caché de Gradle/Maven
rm -rf ~/.m2/repository  # Linux/Mac
rmdir /s %USERPROFILE%\.m2\repository  # Windows (si usas Maven)
```

### B. Configurar ddl-auto a VALIDATE

Edita: `src/main/resources/application.yaml` o `application-local.yaml`

```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: validate  # Cambia a 'validate' para verificar esquema sin hacer cambios
```

### C. Compilar y Ejecutar

```bash
# Compilar
./mvnw.cmd clean compile

# Si compila sin errores, ejecuta la aplicación
./mvnw.cmd spring-boot:run
```

**Esperado:**
- ✅ Compilación exitosa sin cambios de esquema
- ✅ Aplicación levanta sin errores JPA/Hibernate
- ✅ Logs no muestran errores de mapeo de entidades

---

## PASO 5: Validación de Funcionalidad (Testing Manual)

Una vez que la aplicación está corriendo:

### Frontend (React/TypeScript)
```bash
cd frontend

# Build final
npm run build

# Resultado esperado: Build success sin errores TypeScript
```

### Verificar Funcionalidades Clave

1. **Página de Clientes**
   - Navega a → `/clientes` (menú lateral: "Clientes")
   - Debe listar clientes frecuentes (anteriormente "deudores")
   - Prueba crear un nuevo cliente
   - Prueba registrar un abono

2. **Crear Venta Fiada**
   - Ve a → `/ventas`
   - Activa "Modo fiado"
   - Selecciona un cliente
   - Registra la venta
   - Verifica en `/clientes` que la deuda se actualice

3. **Domicilios**
   - Ve a → `/domicilios`
   - Intenta crear un pedido con "Marcar como fiado"
   - Verifica que se asocie correctamente al cliente

4. **Historial de Ventas**
   - Ve a → `/historial-ventas`
   - Filtra por "Fiado"
   - Abre detalle de una venta fiada
   - Debe mostrar abonos del cliente correctamente

---

## PROBLEMA: ¿Qué hacer si algo falla?

### Síntoma: "Table 'deudores' doesn't exist"
**Causa:** La migración SQL no se ejecutó correctamente
**Solución:**
1. Restaura el backup: `mysql < backup_antes_migracion.sql`
2. Revisa el nombre real de las foreign keys con `SHOW CREATE TABLE`
3. Reemplaza en el script SQL los nombres reales y vuelve a ejecutar

### Síntoma: "Hibernate validation error - unexpected column"
**Causa:** La BD se migró pero la app sigue buscando `deudor_id`
**Solución:**
1. Verifica que todos los DTOs y Entities del backend sean de "Cliente"
2. Limpia caché: `./mvnw.cmd clean`
3. Recompila: `./mvnw.cmd compile`
4. Vuelve a ejecutar

### Síntoma: Frontend muestra "Deudores" en lugar de "Clientes"
**Causa:** Caché o compilación antigua
**Solución:**
1. Limpia: `cd frontend && rm -rf node_modules dist .next`
2. Reinstala: `npm install`
3. Recompila: `npm run build`
4. Reinicia dev server: `npm run dev`

---

## CAMBIOS DE CONFIGURACIÓN RECOMENDADOS

### Después de Validar Completamente

```yaml
# application.yaml - Post-validación (SOLO después de que todo funcione)
spring:
  jpa:
    hibernate:
      ddl-auto: validate  # Mant ener en VALIDATE en producción
```

### Limpiar Endpoints Deprecated (Futuro)

Cuando estés 100% seguro de que todo funciona y el frontend está completamente migrado:

```java
// FiadoController.java - ELIMINAR después de 2026-Q3
@Deprecated(since = "1.0", forRemoval = true, 
           message = "Usar /clientes en lugar de /deudores")
@GetMapping("/deudores")  // ← ELIMINAR ESTA FUNCIÓN
public ResponseEntity<List<ClienteResponseDTO>> getDeudores(...) { }
```

---

## CRONOGRAMA SUGERIDO

| Tarea | Cuándo | Quién |
|-------|--------|------|
| Hacer Backup | ANTES de todo | Dev/DBA |
| Ejecutar SQL | Inmediatamente | Dev/DBA |
| Validar BD | Después de SQL | Dev |
| Recompilar Backend | Después de validar BD | Dev |
| Recompilar Frontend | Después de Backend OK | Dev |
| Testing Manual | Después de ambas compilaciones | QA/Dev |
| Deprecar endpoints | 2026-Q3 | Dev |

---

## Información de Contacto / Support

Si encuentras problemas:

1. **Verifica el backup está intacto**
2. **Revisa los nombres de las foreign keys con SHOW CREATE TABLE**
3. **Limpia caché: `./mvnw.cmd clean && rm -rf frontend/node_modules frontend/dist`**
4. **Compila desde cero: `./mvnw.cmd clean compile` y `npm install && npm run build`**

---

## Resumen Final

```
✅ Backend compilado exitosamente
✅ Frontend buildado exitosamente
⏳ Base de datos - Esperando ejecución manual del script SQL
📋 Siguiente: Ejecuta el script SQL, valida, y prueba funcionalidades
```

---

**Documento Generado:** 2026-05-25  
**Versión:** 1.0  
**Estado del Refactor:** ~95% Completo (Solo falta ejecución de BD)
