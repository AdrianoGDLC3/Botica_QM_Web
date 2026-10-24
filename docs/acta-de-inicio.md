# ACTA DE INICIO DEL PROYECTO

**Sistema de Gestión Integral para Botica Q&M**

---

## 1. Datos generales

| Campo | Detalle |
|---|---|
| **Curso** | Desarrollo Web Integrado |
| **Sección** | 38447 |
| **Profesor** | Ramires Pacheco, Luis Enrrique |
| **Proyecto** | Sistema de Gestión Integral para Botica Q&M |
| **Fecha de inicio** | Semana 11 del ciclo |
| **Fecha de emisión del acta** | Semana 13 |

## 2. Integrantes del equipo

| N.º | Apellidos y nombres | Rol en el equipo |
|---|---|---|
| 1 | Guzmán de la Cruz, Adriano Arturo | Desarrollo backend / base de datos |
| 2 | Roque Limachi, Michael | Desarrollo frontend / interfaz |
| 3 | Romero Arrascue, Roymer | Pruebas / documentación |

## 3. Antecedentes y justificación

La Botica Q&M requiere un sistema que integre la venta al público (POS) con el control de
inventario por lotes y fechas de vencimiento, la gestión de compras a proveedores y la
emisión de comprobantes. La operación manual actual dificulta el control de stock, el
seguimiento de vencimientos (crítico en farmacia) y la generación de reportes de gestión.

## 4. Objetivo general

Desarrollar e implantar un sistema web integral que permita a la botica registrar ventas y
compras, controlar el inventario por lotes aplicando la política **FEFO** (First-Expired,
First-Out) y ofrecer un tablero de indicadores para la toma de decisiones.

## 5. Objetivos específicos

1. Registrar ventas con generación automática de comprobantes (boleta/factura) por serie.
2. Registrar compras con entrada de mercadería y creación/actualización de lotes.
3. Controlar el stock por lote y alertar sobre vencimientos y stock bajo.
4. Permitir la anulación de ventas/compras con reversión automática del stock.
5. Ofrecer un Dashboard de Gestión con KPIs y gráficos.
6. Garantizar la seguridad mediante autenticación JWT y control de roles.
7. Asegurar la calidad mediante pruebas automatizadas y de rendimiento.

## 6. Alcance

### 6.1 Incluido

- Módulos de Autenticación, Usuarios, Productos, Lotes, Laboratorios, Proveedores.
- Módulos de Ventas (POS), Compras e Historiales con búsqueda y filtros por fecha.
- Dashboard de Gestión con KPIs y gráficos (Recharts).
- Edición de cabecera y anulación lógica con reversión de stock.
- Diseño responsive (PC, tablet, móvil) con menú de navegación adaptativo.

### 6.2 Excluido

- Facturación electrónica con validez tributaria (SUNAT).
- Aplicación móvil nativa.
- Integración con pasarelas de pago en línea.

## 7. Arquitectura y tecnologías

| Capa | Tecnología |
|---|---|
| Backend | Java 17 · Spring Boot 3.5 · Spring Security (JWT) · Spring Data JPA |
| Base de datos | PostgreSQL (SQL del esquema versionado con scripts) |
| Frontend | React 18 · Vite · React Router · Recharts |
| Pruebas | JUnit 5 · Mockito · AssertJ · H2 · JaCoCo · Vitest · Testing Library · k6 |

**Patrones de diseño aplicados:** Builder (Venta/Compra/Lote), Abstract Factory + Decorator
(comprobantes), Strategy (FEFO, validadores de atributos), Observer (notificación de stock
bajo), Adapter (entidades de contacto), Singleton (configuración de negocio), Prototype
(duplicado de productos).

## 8. Metodología de trabajo

- Desarrollo incremental por semanas (11 a 15), con entregables verificables.
- Control de versiones con repositorio Git.
- Verificación de compilación y pruebas tras cada incremento.

## 9. Cronograma (semanas 11–15)

| Semana | Entregable |
|---|---|
| 11 | Diseño responsive y menú adaptativo |
| 12 | Dashboard de gestión y CRUD completo (edición/anulación) |
| 13 | Suite de pruebas automatizadas, rendimiento y actas |
| 14 | Chat de reglas/FAQ y mejoras de usabilidad |
| 15 | Entrega final, manuales y sustentación |

## 10. Entregables

1. Código fuente del backend y frontend.
2. Scripts SQL del esquema y migraciones.
3. Suite de pruebas automatizadas (backend y frontend).
4. Script e informe de pruebas de rendimiento (k6) y cobertura.
5. Actas de inicio y de conformidad.
6. Manual de usuario y documentación técnica.

## 11. Firmas

| Integrante | Firma |
|---|---|
| Guzmán de la Cruz, Adriano Arturo | _____________________ |
| Roque Limachi, Michael | _____________________ |
| Romero Arrascue, Roymer | _____________________ |

**Profesor:** Ramires Pacheco, Luis Enrrique — _____________________
