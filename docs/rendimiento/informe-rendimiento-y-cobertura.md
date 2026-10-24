# Informe de Pruebas de Rendimiento y Cobertura — Botica Q&M

**Curso:** Desarrollo Web Integrado · **Sección:** 38447
**Profesor:** Ramires Pacheco, Luis Enrrique
**Proyecto:** Sistema de Gestión Integral para Botica Q&M
**Semana:** 13 — Rendimiento y actas

---

## 1. Objetivo

Verificar que el sistema soporta la concurrencia esperada de una botica (varios usuarios
cobrando, consultando y registrando a la vez) y medir la cobertura de las pruebas
automatizadas (caja blanca) y de los escenarios funcionales (caja negra).

---

## 2. Pruebas de rendimiento (k6)

Script: [`k6-botica.js`](./k6-botica.js)

### 2.1 Escenario simulado

| Parámetro | Valor |
|---|---|
| Herramienta | k6 (Grafana) |
| Executor | `ramping-vus` |
| Usuarios virtuales (pico) | 30 |
| Duración total | 1 min 30 s (20s rampa + 40s subida + 20s sostenido + 10s bajada) |
| Flujos por iteración | Login → Consulta de ventas → Registro de venta |

### 2.2 Flujos cubiertos

| # | Flujo | Método y ruta | Métrica k6 |
|---|---|---|---|
| 1 | Login de usuario | `POST /api/auth/login` | `duracion_login` |
| 2 | Consulta de ventas | `GET /api/ventas?q=` | `duracion_consulta_ventas` |
| 3 | Registro de venta | `POST /api/ventas` | `duracion_registro_venta` |

### 2.3 Criterios de aceptación (SLA)

| Métrica | Umbral (threshold) | Significado |
|---|---|---|
| `http_req_failed` | < 2 % | Menos del 2 % de peticiones fallidas |
| `http_req_duration` (p95) | < 800 ms | El 95 % de las peticiones responde bajo 800 ms |
| `duracion_login` (p95) | < 900 ms | Login ágil |
| `duracion_consulta_ventas` (p95) | < 600 ms | Consultas rápidas |
| `duracion_registro_venta` (p95) | < 1000 ms | Registro con descuento de stock |
| `errores_negocio` | < 5 % | Baja tasa de errores funcionales |

### 2.4 Cómo ejecutar

```bash
# 1. Levantar el backend
cd backend/botica-api\ \(10\)/botica-api
mvn spring-boot:run

# 2. Ejecutar la prueba (con k6 instalado)
k6 run docs/rendimiento/k6-botica.js

# Con parámetros:
k6 run -e BASE_URL=http://localhost:8080 -e USUARIO=admin@botica.pe -e PASSWORD=admin123 -e LOTE_ID=1 docs/rendimiento/k6-botica.js
```

### 2.5 Resultado esperado

Al finalizar, k6 imprime un resumen con `checks`, `http_req_duration` (avg, p90, p95) y el
estado de cada **threshold** (✓/✗). El script falla (exit code ≠ 0) si algún umbral no se
cumple, lo que permite usarlo en integración continua.

> **Nota:** los valores numéricos concretos dependen del equipo donde se ejecute y del estado
> de la base de datos (Supabase/PostgreSQL), por eso los criterios están expresados como SLA
> y no como cifras cerradas. El registro de ventas contempla `409` (sin stock) como respuesta
> válida del servidor cuando el lote de prueba se agota durante la carga.

---

## 3. Cobertura de pruebas — Backend (JaCoCo)

Reporte generado en `backend/botica-api (10)/botica-api/target/site/jacoco/index.html`.

| Capa | Clase | Cobertura de instrucciones |
|---|---|---|
| Servicio | `VentaService` | alta (registro, anulación, edición, validaciones) |
| Servicio | `CompraService` | alta (entrada de mercadería, anulación, validaciones) |
| Servicio | `AuthService` | 100 % |
| Seguridad | `JwtService`, `JwtFilter` | alta |
| Lotes | `AsignacionFefo` | 100 % |
| Validación | `Validador*`, `RegistroValidadoresAtributos` | alta |
| Dominio/DTO | `Dtos.*`, entidades | cubierto por pruebas |

### 3.1 Tipos de prueba

- **Caja blanca:** pruebas unitarias que recorren cada rama de decisión (stock insuficiente,
  producto desactivado, lote vencido, requiere receta, anulación doble, etc.).
- **Caja negra:** pruebas de integración que ejercitan los endpoints/contratos HTTP
  (`401` sin token, `403` a EMPLEADO, `200` a ADMIN, flujo compra→venta→anulación).

### 3.2 Resultado

```
Tests run: 62, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

> Ejecutar con: `mvn test` (genera el reporte HTML de JaCoCo automáticamente).

---

## 4. Cobertura de pruebas — Frontend (Vitest)

Reporte generado con `npm run test:coverage` (HTML en `frontend/.../coverage/`).

| Archivo | % Stmts | % Branch | % Funcs | % Lines |
|---|---|---|---|---|
| `src/api.js` | 100 | 87.5 | 100 | 100 |
| `src/auth.jsx` | 100 | 100 | 100 | 100 |
| `src/components/ResumenVenta.jsx` | 97.7 | 75 | 100 | 97.7 |
| `src/impresion.js` | 82.4 | 52.6 | 75 | 82.4 |
| `src/pages/Historial.jsx` | 71.3 | 89.7 | 56.3 | 71.3 |

### 4.1 Resultado

```
Test Files  5 passed (5)
     Tests  27 passed (27)
```

Cubre: utilidades de formato, cliente HTTP (token, body, errores, 401, 204), escapado del
comprobante imprimible, componente `ResumenVenta` (carga, detalle, imprimir, cerrar/Escape,
error), tabla `Historial` (filas, estado anulada, búsqueda con debounce, filtros de fecha,
acciones por fila) y el proveedor de autenticación (`login`/`logout`).

> Ejecutar con: `npm test` o `npm run test:coverage`.

---

## 5. Conclusión

- Se automatizaron **62 pruebas de backend** (unitarias + integración + seguridad) y
  **27 pruebas de frontend**, todas en verde.
- La cobertura se concentra en la lógica de negocio crítica (ventas, compras, anulación de
  stock, seguridad y validaciones), que es donde reside el riesgo del sistema.
- El script de k6 permite validar el rendimiento bajo 30 usuarios concurrentes con criterios
  de aceptación objetivos, sirviendo como base de pruebas de regresión de performance.
