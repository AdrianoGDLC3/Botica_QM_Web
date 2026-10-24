# ACTA DE CONFORMIDAD

**Sistema de Gestión Integral para Botica Q&M**

---

## 1. Datos generales

| Campo | Detalle |
|---|---|
| **Curso** | Desarrollo Web Integrado |
| **Sección** | 38447 |
| **Profesor** | Ramires Pacheco, Luis Enrrique |
| **Proyecto** | Sistema de Gestión Integral para Botica Q&M |
| **Fecha de emisión del acta** | Semana 13 |

## 2. Integrantes del equipo

| N.º | Apellidos y nombres |

|---|---|
| 1 | Guzmán de la Cruz, Adriano Arturo |
| 2 | Roque Limachi, Michael |
| 3 | Romero Arrascue, Roymer |

## 3. Objeto del acta

Dejar constancia de la **conformidad** de los entregables desarrollados y verificados a la
fecha, correspondientes a las semanas 11 a 13 del proyecto, según los criterios de aceptación
definidos en el Acta de Inicio.

## 4. Entregables verificados

| # | Entregable | Semana | Estado |
|---|---|---|---|
| 1 | Diseño responsive y menú adaptativo (PC/tablet/móvil) | 11 | ✅ Conforme |
| 2 | Dashboard de Gestión con KPIs y gráficos (Recharts) | 12 | ✅ Conforme |
| 3 | Endpoint `GET /api/dashboard/resumen` | 12 | ✅ Conforme |
| 4 | Búsqueda y filtros por fecha en ventas y compras | 12 | ✅ Conforme |
| 5 | Edición de cabecera de ventas y compras (Actualizar) | 12 | ✅ Conforme |
| 6 | Anulación lógica con reversión de stock (Eliminar) | 12 | ✅ Conforme |
| 7 | Suite de pruebas backend (JUnit 5 + Mockito + H2) | 13 | ✅ Conforme |
| 8 | Pruebas de seguridad (401 sin token, 403 a EMPLEADO) | 13 | ✅ Conforme |
| 9 | Suite de pruebas frontend (Vitest + Testing Library) | 13 | ✅ Conforme |
| 10 | Cobertura (JaCoCo y Vitest) e informe | 13 | ✅ Conforme |
| 11 | Prueba de rendimiento (k6) e informe | 13 | ✅ Conforme |
| 12 | Actas de inicio y de conformidad | 13 | ✅ Conforme |

## 5. Verificación técnica

### 5.1 Compilación y pruebas

```
# Backend
mvn test   →  Tests run: 62, Failures: 0, Errors: 0, Skipped: 0  ·  BUILD SUCCESS

# Frontend
npm test        →  Test Files 5 passed · Tests 27 passed
npm run build   →  build OK
```

### 5.2 Cobertura de caja blanca

- Backend: reporte JaCoCo en `target/site/jacoco/index.html`; la lógica de negocio crítica
  (`VentaService`, `CompraService`, `AuthService`, `JwtService`, validadores, `AsignacionFefo`)
  queda cubierta por pruebas de ramas.
- Frontend: reporte Vitest; `api.js` y `auth.jsx` al 100 %, `ResumenVenta` 97.7 %,
  `impresion.js` 82 %, `Historial.jsx` 71 %.

### 5.3 Cobertura de caja negra

- Integración end-to-end sobre H2: flujo **compra → venta → anulación** con reversión de
  stock, validación de stock insuficiente y anulación de compra con mercadería ya vendida.
- Seguridad: `401` sin token, `403` a rol EMPLEADO en rutas de administración, `200` con rol
  autorizado e invalidación de tokens corruptos.

### 5.4 Rendimiento

- Script k6 (`docs/rendimiento/k6-botica.js`) con 30 usuarios virtuales concurrentes sobre
  login, consulta y registro de ventas, con umbrales (p95 < 800 ms, errores < 2 %).

## 6. Criterios de aceptación

| Criterio | Cumplido |
|---|---|
| Los módulos funcionales operan según lo especificado | ✅ |
| El control de stock responde correctamente a ventas, compras y anulaciones | ✅ |
| El sistema restringe el acceso según rol (ADMIN/EMPLEADO) | ✅ |
| Las pruebas automatizadas pasan en su totalidad | ✅ |
| Se cuenta con evidencia de cobertura y rendimiento | ✅ |

## 7. Observaciones

- La anulación es **lógica** (no se borran registros): preserva el historial contable y revierte
  el stock de forma transaccional.
- La edición de movimientos está limitada a la **cabecera** (cliente/proveedor, comprobante,
  pago); los importes y líneas no se modifican para no alterar el stock ya descontado.
- Las migraciones de base de datos se ejecutan mediante scripts SQL versionados en `sql/`.

## 8. Conclusión

El equipo declara que los entregables de las semanas 11 a 13 cumplen con los objetivos y
criterios de aceptación establecidos, encontrándose aptos para continuar con las siguientes
etapas del proyecto (semanas 14 y 15).

## 9. Firmas

| Integrante | Firma |
|---|---|
| Guzmán de la Cruz, Adriano Arturo | _____________________ |
| Roque Limachi, Michael | _____________________ |
| Romero Arrascue, Roymer | _____________________ |

**Profesor:** Ramires Pacheco, Luis Enrrique — _____________________
