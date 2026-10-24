# Manual TÃ©cnico y Memoria Descriptiva â€” Sistema de GestiÃ³n Integral Botica Q&M

**Curso:** Desarrollo Web Integrado Â· **SecciÃ³n:** 38447
**Profesor:** Ramires Pacheco, Luis Enrrique
**Proyecto:** Sistema de GestiÃ³n Integral para Botica Q&M
**Stack:** Spring Boot 3.5 (Java 17) + React 18 (Vite)

---

## Ãndice

1. [DescripciÃ³n general](#1-descripciÃ³n-general)
2. [Arquitectura del sistema](#2-arquitectura-del-sistema)
3. [TecnologÃ­as](#3-tecnologÃ­as)
4. [Estructura del cÃ³digo fuente](#4-estructura-del-cÃ³digo-fuente)
5. [Modelo Entidad-RelaciÃ³n (ER)](#5-modelo-entidad-relaciÃ³n-er)
6. [API REST â€” Endpoints](#6-api-rest--endpoints)
7. [Seguridad](#7-seguridad)
8. [Patrones de diseÃ±o aplicados](#8-patrones-de-diseÃ±o-aplicados)
9. [MÃ³dulo FAQ / chatbot](#9-mÃ³dulo-faq--chatbot)
10. [Pruebas y calidad](#10-pruebas-y-calidad)
11. [Despliegue y configuraciÃ³n](#11-despliegue-y-configuraciÃ³n)

---

## 1. DescripciÃ³n general

Botica Q&M es un sistema web que integra el punto de venta (POS) con el control de inventario
por **lotes** y fechas de vencimiento (polÃ­tica **FEFO**), la gestiÃ³n de compras a proveedores y
un dashboard gerencial. EstÃ¡ compuesto por:

- **Backend REST** en Spring Boot (Java 17) con persistencia JPA sobre PostgreSQL.
- **Frontend SPA** en React 18 + Vite, con enrutamiento y grÃ¡ficos.

El objetivo es reemplazar la operaciÃ³n manual por un flujo digital trazable, con control de stock
y vencimientos â€”crÃ­tico en una farmaciaâ€” y reportes para la toma de decisiones.

---

## 2. Arquitectura del sistema

Se usa una arquitectura **clienteâ€“servidor en capas**:

```
â”Œâ”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€ Frontend (React + Vite) â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”
â”‚  pages/      â†’ pantallas (Dashboard, POS, Historial, Compras, Productos, Ayudaâ€¦)    â”‚
â”‚  components/ â†’ UI reutilizable (CrudPage, ResumenVenta, Chatbot, AsistenteFaq)      â”‚
â”‚  api.js      â†’ cliente HTTP (Bearer token)      auth.jsx â†’ contexto de sesiÃ³n        â”‚
â””â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”¬â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”˜
                                          â”‚  HTTP / JSON  (+ JWT)
â”Œâ”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â–¼â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”
â”‚                       Backend (Spring Boot) â€” API REST /api                          â”‚
â”‚  web/        â†’ @RestController (capa HTTP, DTOs de entrada/salida)                   â”‚
â”‚  security/   â†’ JwtFilter + JwtService (autenticaciÃ³n stateless)                      â”‚
â”‚  service/    â†’ lÃ³gica de negocio y transacciones (VentaService, CompraServiceâ€¦)      â”‚
â”‚  repo/       â†’ Spring Data JPA (acceso a datos)                                      â”‚
â”‚  domain/     â†’ entidades JPA (Venta, Compra, Producto, Lote â€¦)                        â”‚
â”‚  dto/        â†’ records de transporte (Dtos)                                          â”‚
â”‚  error/      â†’ manejo global de errores ({ "message": "..." })                        â”‚
â””â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”¬â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”˜
                                          â”‚  JDBC (Hibernate)
                                    â”Œâ”€â”€â”€â”€â”€â–¼â”€â”€â”€â”€â”€â”
                                    â”‚ PostgreSQLâ”‚
                                    â””â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”˜
```

**Responsabilidades por capa**

| Capa | Responsabilidad |
|---|---|
| `web` | Expone endpoints, valida entrada (`@Valid`) y devuelve DTOs. Sin lÃ³gica de negocio. |
| `service` | Reglas de negocio, cÃ¡lculo de totales, control de stock y transacciones. |
| `repo` | Consultas a base de datos (Spring Data JPA + consultas nativas cuando se requiere). |
| `domain` | Mapeo objeto-relacional (entidades JPA). |
| `security` | AutenticaciÃ³n por token JWT y autorizaciÃ³n por rol. |

El **frontend** consume la API mediante `fetch` (mÃ³dulo `api.js`), aÃ±adiendo el token en la
cabecera `Authorization: Bearer <token>` y centralizando el manejo de errores.

---

## 3. TecnologÃ­as

### Backend
| TecnologÃ­a | VersiÃ³n | Uso |
|---|---|---|
| Java | 17 | Lenguaje |
| Spring Boot | 3.5.0 | Framework base |
| Spring Web | 3.5 | API REST |
| Spring Data JPA | 3.5 | Persistencia |
| Spring Security | 3.5 | AutenticaciÃ³n/autorizaciÃ³n |
| Spring Validation | 3.5 | ValidaciÃ³n de DTOs (Jakarta Bean Validation) |
| JJWT | 0.12.6 | GeneraciÃ³n/validaciÃ³n de tokens JWT |
| Lombok | (gestionada) | ReducciÃ³n de cÃ³digo repetitivo |
| PostgreSQL Driver | (gestionada) | Conector JDBC |
| H2 | (gestionada) | Base en memoria para pruebas |
| JUnit 5 + Mockito + AssertJ | (gestionada) | Pruebas |
| JaCoCo | 0.8.13 | Cobertura de cÃ³digo |

### Frontend
| TecnologÃ­a | VersiÃ³n | Uso |
|---|---|---|
| React | 18.3 | Biblioteca de UI |
| Vite | 5.4 | Bundler / servidor de desarrollo |
| React Router | 6.28 | Enrutamiento SPA |
| Recharts | 3.x | GrÃ¡ficos del Dashboard |
| Vitest | 2.1 | Pruebas unitarias/componentes |
| Testing Library | 16.x | Pruebas de componentes React |
| jsdom | 25.x | Entorno DOM para pruebas |

---

## 4. Estructura del cÃ³digo fuente

### Backend (`backend/botica-api (10)/botica-api`)

```
src/main/java/com/botica/
â”œâ”€â”€ adapter/        â†’ Adapter de entidades de contacto (Lab/Proveedor)
â”œâ”€â”€ builder/        â†’ Builders de Venta, Compra, Lote
â”œâ”€â”€ comprobante/    â†’ Abstract Factory + Decorator de comprobantes
â”œâ”€â”€ config/         â†’ SecurityConfig, ConfiguracionNegocio
â”œâ”€â”€ domain/         â†’ Entidades JPA
â”œâ”€â”€ dto/            â†’ Dtos (records de entrada/salida)
â”œâ”€â”€ error/          â†’ ApiException + GlobalExceptionHandler
â”œâ”€â”€ inventario/     â†’ Observer de stock bajo
â”œâ”€â”€ lotes/          â†’ AsignaciÃ³n FEFO
â”œâ”€â”€ repo/           â†’ Repositorios Spring Data
â”œâ”€â”€ security/       â†’ JwtFilter, JwtService
â”œâ”€â”€ service/        â†’ LÃ³gica de negocio (incl. FaqService)
â”œâ”€â”€ util/           â†’ Utilidades (Tiempo)
â”œâ”€â”€ validacion/     â†’ Strategy de validaciÃ³n de atributos
â””â”€â”€ web/            â†’ Controladores REST (incl. FaqController)
src/main/resources/application.yml
sql/                â†’ Scripts de migraciÃ³n versionados
src/test/â€¦          â†’ Pruebas (72 en total)
```

### Frontend (`frontend/botica-frontend (9)/botica-frontend`)

```
src/
â”œâ”€â”€ api.js          â†’ Cliente HTTP + helpers (money, fmtDateâ€¦)
â”œâ”€â”€ auth.jsx        â†’ Contexto de autenticaciÃ³n (login/logout)
â”œâ”€â”€ impresion.js    â†’ GeneraciÃ³n del comprobante imprimible
â”œâ”€â”€ App.jsx         â†’ Rutas, menÃº y Layout (incluye el widget Chatbot)
â”œâ”€â”€ index.css       â†’ Estilos y diseÃ±o responsive
â”œâ”€â”€ components/     â†’ CrudPage, ResumenVenta, Chatbot, AsistenteFaq
â”œâ”€â”€ pages/          â†’ Dashboard, NuevaVenta, Historial*, Compras, Productos, Ayuda
â””â”€â”€ __tests__/      â†’ Pruebas (33 en total)
```

---

## 5. Modelo Entidad-RelaciÃ³n (ER)

### Diagrama (notaciÃ³n textual)

```
USUARIO (id, nombre, dni, telefono, correo, password, rol, activo)
   â”‚ 1                    â”‚ 1
   â”‚                      â”‚
   â”‚ N (cliente)          â”‚ N (empleado/usuario)
   â–¼                      â–¼
VENTA (id, cliente_id?, empleado_id, fecha_venta, tipo_comprobante,
       numero_comprobante, metodo_pago, total, receta_presentada,
       receta_numero, receta_medico, anulado, motivo_anulacion, fecha_anulacion)
   â”‚ 1
   â”‚ N
VENTA_DETALLE (id, venta_id, lote_id, cantidad, precio_venta)

PROVEEDOR (id, nombre, ruc, contacto, telefono, correo, direccion, activo)
   â”‚ 1
   â”‚ N
COMPRA (id, proveedor_id, usuario_id, fecha_compra, tipo_comprobante,
        numero_comprobante, total, anulado, motivo_anulacion, fecha_anulacion)
   â”‚ 1
   â”‚ N
COMPRA_DETALLE (id, compra_id, producto_id, laboratorio_id, numero_lote,
                fecha_vencimiento, cantidad, unidades_por_caja, precio_compra)

PRODUCTO (id, nombre, descripcion, categoria, requiere_receta, atributos jsonb,
          unidad_venta, precio_venta, activo)
   â”‚ N                      â”‚ 1
   â”‚                        â”‚ N
   â”‚ M                      â–¼
   â–¼                LOTE (id, producto_id, laboratorio_id, numero_lote,
PRODUCTO_LABORATORIO       fecha_vencimiento, stock_actual)
  (producto_id, laboratorio_id)
   â–²
   â”‚ M
LABORATORIO (id, nombre, pais, ruc, contacto, telefono, correo, direccion, activo)

SERIE_COMPROBANTE (tipo, serie, correlativo)   -- correlativo por serie (boleta/factura)
```

### Entidades principales

| Entidad | DescripciÃ³n | Claves forÃ¡neas |
|---|---|---|
| **Usuario** | Personal y clientes. `rol` âˆˆ {ADMINISTRADOR, EMPLEADO}. | â€” |
| **Producto** | ArtÃ­culo vendible. `atributos` en `jsonb` (campos por categorÃ­a). | â€” |
| **Laboratorio** | Fabricante. RelaciÃ³n **N:M** con Producto. | â€” |
| **Lote** | Partida con vencimiento y stock (en unidades). | `producto_id`, `laboratorio_id` |
| **Venta** | Cabecera de venta. `anulado` para baja lÃ³gica. | `cliente_id` (opc.), `empleado_id` |
| **VentaDetalle** | LÃ­nea de venta. | `venta_id`, `lote_id` |
| **Proveedor** | A quiÃ©n se compra. | â€” |
| **Compra** | Cabecera de compra. `anulado` para baja lÃ³gica. | `proveedor_id`, `usuario_id` |
| **CompraDetalle** | LÃ­nea de compra (cajas Ã— unidades por caja). | `compra_id`, `producto_id`, `laboratorio_id` |
| **SerieComprobante** | Correlativo por tipo de comprobante. | â€” |

### Reglas de negocio sobre el modelo

- El **stock vive por lote** y siempre estÃ¡ en la **unidad de venta** del producto.
- Una **compra** ingresa `cantidad Ã— unidades_por_caja` unidades al lote indicado.
- Una **venta** descuenta unidades del lote (con **FEFO**: primero el de vencimiento mÃ¡s prÃ³ximo).
- **Anular** una venta devuelve el stock; **anular** una compra lo descuenta (falla si insuficiente).
- Los movimientos usan **baja lÃ³gica** (`anulado`) para preservar el historial contable.

---

## 6. API REST â€” Endpoints

> Base: `http://localhost:8080/api`. Todas las rutas (salvo `/auth/**`) requieren token JWT.
> El servidor responde errores como `{ "message": "..." }`.

### AutenticaciÃ³n
| MÃ©todo | Ruta | Rol | DescripciÃ³n |
|---|---|---|---|
| POST | `/auth/login` | pÃºblico | Inicia sesiÃ³n; devuelve `{ token, usuario }` |

### Usuarios
| MÃ©todo | Ruta | Rol | DescripciÃ³n |
|---|---|---|---|
| GET | `/usuarios` | ADMIN | Lista todos los usuarios |
| GET | `/usuarios/clientes` | ADMIN, EMPLEADO | Lista de clientes (para el POS) |
| POST | `/usuarios` | ADMIN | Crea usuario |
| PUT | `/usuarios/{id}` | ADMIN | Actualiza usuario |
| DELETE | `/usuarios/{id}` | ADMIN | Baja lÃ³gica |

### CatÃ¡logos
| MÃ©todo | Ruta | Rol | DescripciÃ³n |
|---|---|---|---|
| GET | `/productos` | ADMIN, EMPLEADO | Lista productos |
| POST/PUT/DELETE | `/productos` `/productos/{id}` | ADMIN | Alta/ediciÃ³n/baja |
| POST | `/productos/{id}/duplicar` | ADMIN | Duplica como plantilla |
| GET | `/laboratorios` Â· `/proveedores` | ADMIN, EMPLEADO | Listan |
| POST/PUT/DELETE | `/laboratorios` Â· `/proveedores` (y `/â€¦/{id}/definitivo`) | ADMIN | GestiÃ³n |
| GET | `/lotes` Â· `/lotes/disponibles?productoId=` | ADMIN, EMPLEADO | Lotes / disponibles (FEFO) |
| PUT | `/lotes/{id}` | ADMIN | Corrige laboratorio/vencimiento |

### Movimientos
| MÃ©todo | Ruta | Rol | DescripciÃ³n |
|---|---|---|---|
| GET | `/ventas?desde&hasta&q` | ADMIN, EMPLEADO | Lista con filtros |
| GET | `/ventas/{id}` | ADMIN, EMPLEADO | Detalle |
| POST | `/ventas` | ADMIN, EMPLEADO | Registra venta (calcula total y descuenta stock) |
| PUT | `/ventas/{id}` | ADMIN, EMPLEADO | Edita cabecera |
| PUT | `/ventas/{id}/anular` | ADMIN, EMPLEADO | Anula y revierte stock (motivo obligatorio) |
| GET | `/compras?desde&hasta&q` | ADMIN, EMPLEADO | Lista con filtros |
| GET | `/compras/{id}` | ADMIN, EMPLEADO | Detalle |
| POST | `/compras` | ADMIN, EMPLEADO | Registra compra (crea/suma lotes) |
| PUT | `/compras/{id}` | ADMIN, EMPLEADO | Edita cabecera |
| PUT | `/compras/{id}/anular` | ADMIN, EMPLEADO | Anula y descuenta stock |

### Dashboard y FAQ
| MÃ©todo | Ruta | Rol | DescripciÃ³n |
|---|---|---|---|
| GET | `/dashboard/resumen` | ADMIN, EMPLEADO | KPIs y series para los grÃ¡ficos |
| GET | `/faq` | ADMIN, EMPLEADO | Todo el catÃ¡logo de preguntas |
| GET | `/faq/categorias` | ADMIN, EMPLEADO | CategorÃ­as con nÂº de preguntas |
| GET | `/faq/sugeridas` | ADMIN, EMPLEADO | Preguntas destacadas |
| GET | `/faq/buscar?q=` | ADMIN, EMPLEADO | BÃºsqueda por palabra clave |
| GET | `/faq/{id}` | ADMIN, EMPLEADO | Respuesta de una pregunta (404 si no existe) |

### Ejemplo de peticiÃ³n

```bash
# Login
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"correo":"admin@botica.pe","password":"admin123"}'

# Registrar una venta
curl -X POST http://localhost:8080/api/ventas \
  -H "Authorization: Bearer <TOKEN>" -H "Content-Type: application/json" \
  -d '{"tipoComprobante":"BOLETA","metodoPago":"EFECTIVO",
       "recetaPresentada":false,"items":[{"loteId":1,"cantidad":2}]}'
```

---

## 7. Seguridad

- **AutenticaciÃ³n stateless con JWT.** `POST /auth/login` valida credenciales (BCrypt) y emite un
  token firmado (HMAC). El `JwtFilter` valida el token en cada peticiÃ³n y carga el usuario.
- **AutorizaciÃ³n por rol** en `SecurityConfig`:
  - Sin token â†’ `401 Unauthorized`.
  - Token sin permiso â†’ `403 Forbidden`.
  - `/usuarios/**`: solo ADMIN (salvo `GET /usuarios/clientes`).
  - CatÃ¡logos: **lectura** ADMIN+EMPLEADO, **escritura** solo ADMIN.
  - `/ventas`, `/compras`, `/dashboard`, `/faq`: ADMIN+EMPLEADO.
- **CORS** configurable por `CORS_ORIGINS`.
- Las contraseÃ±as se almacenan **cifradas** (BCrypt).

---

## 8. Patrones de diseÃ±o aplicados

| PatrÃ³n | DÃ³nde | PropÃ³sito |
|---|---|---|
| **Builder** | `builder/` (Venta, Compra, Lote) | ConstrucciÃ³n paso a paso de agregados complejos |
| **Abstract Factory + Decorator** | `comprobante/` | Familias de comprobantes (boleta/factura) y decoradores |
| **Strategy** | `lotes/` (FEFO), `validacion/` | Algoritmos de selecciÃ³n y validaciÃ³n intercambiables |
| **Observer** | `inventario/` | NotificaciÃ³n de stock bajo |
| **Adapter** | `adapter/` | Unificar Laboratorio y Proveedor en una salida comÃºn |
| **Singleton** | `config/ConfiguracionNegocio` | ConfiguraciÃ³n Ãºnica de negocio |
| **Prototype** | `domain/Prototipo` + `Producto.clonar()` | Duplicar productos como plantilla |
| **MVC / capas** | `web`/`service`/`repo` | SeparaciÃ³n de responsabilidades |
| **DTO** | `dto/Dtos` (records) | Desacoplar la API del modelo de datos |

> **Documentos detallados (Semana 15):**
> - [`patrones-de-diseno.md`](./patrones-de-diseno.md) â€” explicaciÃ³n completa de cada patrÃ³n
>   (problema, archivo y justificaciÃ³n).
> - [`codificacion-modulos-clave.md`](./codificacion-modulos-clave.md) â€” recorrido **lÃ­nea a lÃ­nea**
>   de FEFO, anulaciÃ³n con reversiÃ³n de stock, emisiÃ³n de comprobantes y seguridad JWT.

---

## 9. MÃ³dulo FAQ / chatbot

**DecisiÃ³n de diseÃ±o:** el catÃ¡logo de preguntas vive **en cÃ³digo** (`FaqService`), no en la base
de datos. Son preguntas estÃ¡ticas de ayuda al personal, no datos transaccionales, por lo que no
requieren tabla ni migraciÃ³n. Ventajas: despliegue simple y sin migraciones.

- **CategorÃ­as:** Ventas, Inventario y lotes, Compras, Productos, Usuarios y acceso, Reportes.
- **BÃºsqueda:** insensible a mayÃºsculas y **tildes** (`Normalizer`), con **puntaje de relevancia**
  (enunciado > etiquetas > categorÃ­a > respuesta) para devolver primero lo mÃ¡s pertinente.
- **Frontend:** componente `AsistenteFaq` reutilizado por el **widget flotante** (`Chatbot`) y por
  la **vista `/ayuda`**. Incluye estados de inicio/categorÃ­a/pregunta/bÃºsqueda y *debounce* de 250 ms.

---

## 10. Pruebas y calidad

### Backend (JUnit 5 + Mockito + AssertJ + H2 + JaCoCo)
- **Unitarias (caja blanca):** servicios (`VentaService`, `CompraService`, `AuthService`,
  `FaqService`), `AsignacionFefo`, `JwtService`, validaciones.
- **IntegraciÃ³n (caja negra):** flujo `compra â†’ venta â†’ anulaciÃ³n` sobre H2 en modo PostgreSQL.
- **Seguridad:** `401` sin token, `403` por rol, `200` autorizado.

```
mvn test   â†’  Tests run: 72, Failures: 0, Errors: 0, Skipped: 0  Â·  BUILD SUCCESS
Reporte de cobertura: target/site/jacoco/index.html
```

### Frontend (Vitest + Testing Library)
- Utilidades (`api.js`, `impresion.js`), componentes (`ResumenVenta`, `Historial`, `Chatbot`/
  `AsistenteFaq`) y contexto de autenticaciÃ³n.

```
npm test   â†’  Test Files 6 passed Â· Tests 33 passed
Cobertura: npm run test:coverage  (coverage/)
```

### Rendimiento
Script k6 (`docs/rendimiento/k6-botica.js`): 30 usuarios virtuales concurrentes sobre login,
consulta y registro de ventas, con umbrales p95 < 800 ms y errores < 2 %.

---

## 11. Despliegue y configuraciÃ³n

### Variables de entorno (backend)
| Variable | DescripciÃ³n | Ejemplo |
|---|---|---|
| `DB_URL` | URL JDBC de PostgreSQL | `jdbc:postgresql://<HOST>:<PUERTO>/<BASE_DE_DATOS>` |
| `DB_USER` | Usuario de BD | `postgres` |
| `DB_PASSWORD` | ContraseÃ±a de BD | `â€¢â€¢â€¢â€¢` |
| `JWT_SECRET` | Clave de firma (â‰¥ 32 caracteres) | `cambia-esta-clave...` |
| `CORS_ORIGINS` | OrÃ­genes permitidos | `http://localhost:5173` |

### Variables de entorno (frontend)
| Variable | DescripciÃ³n | Ejemplo |
|---|---|---|
| `VITE_API_URL` | URL base de la API | `http://localhost:8080/api` |

### Esquema de base de datos
El esquema se versiona con scripts SQL en `sql/` (Hibernate usa `ddl-auto: none`). Los cambios de
entidades se distribuyen como **migraciones** que deben ejecutarse sobre la base
(por ejemplo `migracion_venta_compra_anulacion.sql`).

> Para el detalle de instalaciÃ³n y ejecuciÃ³n, consulta [`guia-del-ejecutable.md`](./guia-del-ejecutable.md).
> Para el uso funcional, consulta [`manual-de-usuario.md`](./manual-de-usuario.md).

