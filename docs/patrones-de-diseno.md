# Patrones de Arquitectura e Ingeniería de Software — Botica Q&M

**Curso:** Desarrollo Web Integrado · **Sección:** 38447
**Profesor:** Ramires Pacheco, Luis Enrrique
**Proyecto:** Sistema de Gestión Integral para Botica Q&M
**Documento:** Explicación detallada de los patrones aplicados (Semana 15)

---

## Índice

1. [Introducción](#1-introducción)
2. [Arquitectura en capas (MVC)](#2-arquitectura-en-capas-mvc)
3. [DAO / Repository](#3-dao--repository)
4. [DTO (Data Transfer Object)](#4-dto-data-transfer-object)
5. [Factory y Abstract Factory](#5-factory-y-abstract-factory)
6. [Builder](#6-builder)
7. [Strategy](#7-strategy)
8. [Observer](#8-observer)
9. [Adapter](#9-adapter)
10. [Prototype](#10-prototype)
11. [Singleton](#11-singleton)
12. [Inyección de Dependencias (DI)](#12-inyección-de-dependencias-di)
13. [Decorator](#13-decorator)
14. [Tabla resumen](#14-tabla-resumen)

---

## 1. Introducción

El proyecto aplica de forma deliberada y documentada varios **patrones de diseño** y de
**arquitectura** de la ingeniería de software. El objetivo no es "usar patrones por usarlos",
sino resolver problemas concretos de la botica (control de vencimientos, consistencia de stock,
numeración de comprobantes, extensibilidad de categorías) con soluciones **probadas,
mantenibles y extensibles**.

Cada patrón se describe a continuación con: el **problema** que resuelve, **dónde** está
implementado (archivo), y **por qué** esa elección mejora el diseño.

---

## 2. Arquitectura en capas (MVC)

**Problema:** mezclar el acceso a datos, la lógica de negocio y la presentación produce código
imposible de probar y mantener.

**Solución:** separación estricta en capas. El backend sigue el patrón **MVC** adaptado a una API
REST (el "View" es el JSON; el "Controller" orquesta HTTP; el "Model" son dominio + servicios).

```
Petición HTTP
     │
     ▼
┌──────────────┐   com.botica.web       (VentaController, FaqController…)
│ Controller   │   → valida entrada, delega, devuelve DTO
└──────┬───────┘
       ▼
┌──────────────┐   com.botica.service   (VentaService, CompraService…)
│ Service      │   → reglas de negocio, transacciones
└──────┬───────┘
       ▼
┌──────────────┐   com.botica.repo      (VentaRepository, LoteRepository…)
│ Repository   │   → acceso a datos (JPA)
└──────┬───────┘
       ▼
┌──────────────┐   com.botica.domain    (Venta, Producto, Lote…)
│ Domain (M)   │   → entidades JPA
└──────────────┘
```

**Evidencia en el código:**

- **Controller:** `web/VentaController.java` no contiene lógica de negocio: delega en
  `VentaService` y aplica `@Valid` sobre los DTO de entrada.
- **Service:** `service/VentaService.registrar()` concentra las reglas (validación de stock,
  recorte de FEFO, cálculo de total, generación de comprobante).
- **Repository:** `repo/VentaRepository` expone solo consultas.
- **Domain:** las entidades (`domain/`) no conocen ni HTTP ni JSON.

**Beneficios:** cada capa se prueba de forma aislada (los tests de servicio usan *mocks* de repos),
y cambiar PostgreSQL por otra base solo afecta a `repo`.

---

## 3. DAO / Repository

**Problema:** escribir JDBC/SQL a mano en cada operación es repetitivo y propenso a errores.

**Solución:** patrón **Repository** implementado con **Spring Data JPA**. Las interfaces extienden
`JpaRepository` y Spring genera la implementación en tiempo de ejecución.

**Evidencia en el código:**

- `repo/LoteRepository.java:15` — `interface LoteRepository extends JpaRepository<Lote, Long>`,
  obtiene gratis `save`, `findById`, `findAll`, `count`, etc.
- **Consultas derivadas y JPQL** personalizadas cuando se necesita:
  - `LoteRepository.disponibles()` (línea 22) usa `join fetch` para evitar el problema *N+1*.
  - `LoteRepository.bloquear()` (línea 37) añade `@Lock(PESSIMISTIC_WRITE)` → `SELECT … FOR UPDATE`,
    esencial para que **ventas concurrentes no descuadren el stock**.
- **Consulta nativa** para un *upsert* atómico: `SerieComprobanteRepository.incrementarYObtener()`
  (línea 13) usa `ON CONFLICT … RETURNING`.
- **Especificaciones dinámicas:** `JpaSpecificationExecutor<Venta>` permite construir los filtros
  de búsqueda (fechas + texto) sin multiplicar métodos. Ver `VentaService.filtros()` (líneas 135-154).

**Beneficios:** el "cómo se accede a los datos" queda encapsulado en una capa; el resto del
sistema programa contra interfaces.

---

## 4. DTO (Data Transfer Object)

**Problema:** exponer las entidades JPA directamente en la API filtra el modelo interno, causa
*serialización perezosa* (LazyInitializationException) y acopla el contrato HTTP al esquema de BD.

**Solución:** objetos de transferencia inmutables (record de Java) en `dto/Dtos.java`, separados
en **Request** y **Response**.

**Evidencia en el código (`dto/Dtos.java`):**

| DTO | Rol |
|---|---|
| `LoginReq`, `VentaReq`, `CompraReq`, `ProductoReq` | Entrada (con validaciones Jakarta: `@NotBlank`, `@NotNull`, `@Min`) |
| `LoginRes`, `VentaRes`, `LoteRes`, `FaqRes` | Salida (lo que ve el frontend) |
| `VentaActualizarReq`, `AnularReq` | Entrada para operaciones específicas |

El mapeo entidad→DTO vive en métodos `res(...)` (por ejemplo `VentaService.res()` línea 229), de
modo que la lógica de "qué se muestra" está centralizada y el frontend recibe exactamente los
campos que necesita (nombres ya resueltos, no ids crudos).

**Beneficios:** contrato estable y desacoplado; validación declarativa en la frontera del sistema.

---

## 5. Factory y Abstract Factory

**Problema:** la emisión de comprobantes tiene dos "familias" coherentes (boleta/factura) y cada
una combina un tipo, una serie y un mecanismo de numeración. Crear eso a mano en `VentaService`
lo llenaría de condicionales y acoplaría la lógica de negocio a los detalles de cada comprobante.

**Solución:** **Abstract Factory** en el paquete `comprobante/`.

- `GeneradorComprobante` (interfaz) → produce el siguiente número.
- `FabricaComprobante` (abstract factory) → crea la **familia** de productos coherentes:
  `DescriptorComprobante` + `GeneradorComprobante`.
- Implementaciones concretas: `FabricaBoleta` (serie `B001`), `FabricaFactura` (serie `F001`),
  heredando de `FabricaComprobanteSerie`.
- El **cliente de la fábrica** es `GeneradorComprobanteFactory` (`@Component`): Spring le inyecta
  **todas** las fábricas y las indexa por tipo en un `Map` (líneas 23-26). `VentaService` pide
  `comprobantes.paraTipo(tipo)` (VentaService.java:118) **sin saber cuántas fábricas existen**.

**Por qué Abstract Factory y no simple Factory:** porque garantiza que el descriptor y el
generador provengan de la **misma** familia; es imposible armar un generador de la serie `F001`
con el descriptor de una `BOLETA`.

**Extensibilidad:** agregar `NOTA_CREDITO` = crear otra `@Component FabricaComprobante`. Ni la
fábrica central ni `VentaService` cambian (principio **Open/Closed**). Si dos fábricas declaran el
mismo tipo, la app **falla al arrancar** (fail-fast), no a mitad de una venta.

---

## 6. Builder

**Problema:** `Venta`, `Compra` y `Lote` tienen muchos campos, algunos obligatorios y otros
opcionales, más líneas de detalle. Un constructor gigante es ilegible y fácil de equivocar.

**Solución:** patrón **Builder** en `builder/`, con validación en `build()`.

**Evidencia en el código:**

- `builder/VentaBuilder.java`: setters fluidos que devuelven `this` (líneas 34-50); el método
  `build()` (líneas 52-71) valida los obligatorios (`empleado`, `fechaVenta`, `tipoComprobante`) y
  **enlaza cada detalle con su cabecera** (`d.setVenta(v)`).
- Uso en `VentaService.registrar()`: la cabecera se arma paso a paso (líneas 63-69) y los detalles
  se agregan dentro del bucle; la entidad se crea una sola vez al final.
- También existen `VentaDetalleBuilder`, `CompraBuilder`, `CompraDetalleBuilder` y `LoteBuilder`.

**Beneficios:** construcción clara y ordenada, datos opcionales normalizados (vacío→null vía
`Textos.opcional`), y un único punto de validación.

---

## 7. Strategy

**Problema:** "¿qué lote despacho primero?" es una política que puede cambiar (FEFO hoy, otra
mañana). Si se incrusta en el SQL o en el servicio, cambiarla obliga a tocar la consulta y a
reprobar todo.

**Solución:** patrón **Strategy** en `lotes/`.

- `EstrategiaAsignacionLote` (interfaz, `lotes/EstrategiaAsignacionLote.java`): define `ordenar(List<Lote>)`.
- `AsignacionFefo` (`@Component`): implementa **FEFO** ordenando por fecha de vencimiento
  (`Comparator.comparing(Lote::getFechaVencimiento)`).
- El consumidor `CatalogoService` inyecta la interfaz (línea 37) y la usa en `lotesDisponibles()`
  (línea 182): `estrategiaLote.ordenar(candidatos)`.

**Beneficios:** la política de despacho es **intercambiable** sin tocar al consumidor
(`lotesDisponibles`). La consulta SQL solo trae *candidatos* (con stock y no vencidos); el orden
lo decide la Strategy.

> El mismo patrón se aplica en `validacion/`: `ValidadorAtributos` con implementaciones por
> categoría (`ValidadorMedicamento`, `ValidadorPanales`, `ValidadorBiberon`…), seleccionadas por
> `RegistroValidadoresAtributos`.

---

## 8. Observer

**Problema:** tras una venta, si el stock baja del umbral conviene **notificar** sin acoplar
`VentaService` a quién reacciona (log, correo, panel…).

**Solución:** patrón **Observer** en `inventario/`.

- `ObservadorInventario` (interfaz): `onStockBajo(Lote)`.
- `NotificadorStockBajoLog` (`@Component`): implementación que registra el aviso.
- `VentaService` recibe **todos** los observadores por inyección:
  `private final List<ObservadorInventario> observadores;` (línea 40, con comentario explícito).
- Al descontar stock (líneas 93-94): `if (lote.getStockActual() <= umbral) observadores.forEach(o -> o.onStockBajo(lote));`.

**Beneficios:** se pueden añadir nuevos reactores (correo, alerta en el Dashboard) creando otra
`@Component`, sin modificar `VentaService`.

---

## 9. Adapter

**Problema:** `Laboratorio` y `Proveedor` son entidades distintas pero el frontend consume una
salida **común** (`EntidadContactoRes`).

**Solución:** patrón **Adapter** en `adapter/EntidadContactoAdapter.java`, que **convierte** cada
entidad a la forma común (por ejemplo, `pais` queda null para Proveedor, que no lo tiene).

**Evidencia:** `CatalogoService` y `LaboratorioService`/`ProveedorService` producen
`EntidadContactoRes` a través del adaptador, de modo que el frontend usa una sola "forma" de datos
para ambos catálogos.

**Beneficios:** contrato único para el cliente; un cambio en una entidad no se propaga al frontend.

---

## 10. Prototype

**Problema:** dar de alta variantes que comparten casi todos los datos (p. ej. pañales x30 → x50)
obliga a reescribir todo.

**Solución:** patrón **Prototype** — `domain/Prototipo<T>` con el método `clonar()`, implementado
por `Producto.clonar()` (`domain/Producto.java:66`).

- La copia **no** conserva el `id` (es otra fila) y hace **copia profunda** de `atributos` (JSON
  libre), duplicando mapas y listas anidadas (`copiarMapa`/`copiaProfunda`).
- Los **laboratorios** se copian por referencia (son del catálogo, no se duplican).
- Expuesto por `POST /productos/{id}/duplicar`.

**Beneficios:** alta rápida de variantes con datos consistentes.

---

## 11. Singleton

**Problema:** hay parámetros de negocio **únicos** para toda la app (zona horaria, umbral de stock
bajo, categorías válidas) que no deben instanciarse por doquier.

**Solución:** **Singleton** en `config/ConfiguracionNegocio.java` con el idiom
*initialization-on-demand holder*:

- Constructor **privado** (nadie más crea instancias, línea 27).
- La instancia vive en una clase interna `Holder` (líneas 37-39) que la JVM carga y construye
  **en el primer uso** de `instancia()`, de forma **thread-safe** sin `synchronized`.
- Estado **inmutable** (`final`, `Set.of`), así que compartirlo entre hilos es seguro.
- `es final` y no `Serializable`/`Cloneable` → no se puede duplicar por herencia ni clonación.

Además, los `@Service`/`@Component` de Spring (`VentaService`, `GeneradorComprobanteFactory`…)
son también **singletons gestionados por el contenedor**.

**Beneficios:** una sola fuente de verdad para la configuración; sin "números mágicos" dispersos.

---

## 12. Inyección de Dependencias (DI)

**Problema:** si las clases crean sus dependencias (`new VentaRepository()`), quedan acopladas y no
se pueden sustituir en pruebas.

**Solución:** **DI por constructor** gestionada por Spring, simplificada con Lombok
`@RequiredArgsConstructor`.

**Evidencia en el código:**

```java
@Service
@RequiredArgsConstructor
public class VentaService {
    private final VentaRepository ventas;                    // Spring inyecta el repositorio
    private final GeneradorComprobanteFactory comprobantes;  // y la fábrica
    private final List<ObservadorInventario> observadores;   // y TODOS los observadores
    ...
}
```

- Dependencias `final` → obligatorias e inmutables (no hay setters).
- `List<ObservadorInventario>` → Spring inyecta **todas** las implementaciones (clave para Observer).
- `@Value("${app.jwt.secret}")` en `JwtService` inyecta configuración externa.

**Beneficios:** bajo acoplamiento, alta *testeabilidad* (en pruebas se pasan *mocks*), y el
contenedor controla el ciclo de vida.

---

## 13. Decorator

**Problema:** se quiere **auditar** (log) la generación de comprobantes sin mezclar el logging con
la lógica de numeración.

**Solución:** patrón **Decorator** — `comprobante/GeneradorComprobanteConLogging.java`.

- Implementa la misma interfaz `GeneradorComprobante` y **envuelve** a otro generador
  (`delegado`), añadiendo el log **antes de devolver** el número (líneas 17-22).
- Como el resto del sistema solo conoce la interfaz `GeneradorComprobante`, puede decorar
  cualquier implementación (la de serie actual o, en el futuro, una que consulte un sistema
  externo de facturación) sin que `VentaService` cambie.

**Beneficios:** responsabilidades separadas y comportamiento añadible "en capas".

---

## 14. Tabla resumen

| Patrón | Tipo | Archivo(s) clave | Problema que resuelve |
|---|---|---|---|
| Arquitectura en capas (MVC) | Arquitectónico | `web/`, `service/`, `repo/`, `domain/` | Separación de responsabilidades |
| Repository / DAO | Ingeniería | `repo/LoteRepository`, `VentaRepository`… | Encapsular el acceso a datos |
| DTO | Ingeniería | `dto/Dtos.java` | Desacoplar API de entidades |
| Factory + Abstract Factory | Creacional | `comprobante/Fabrica*`, `GeneradorComprobanteFactory` | Familias coherentes de comprobantes |
| Builder | Creacional | `builder/VentaBuilder`, `CompraBuilder`… | Construir agregados complejos |
| Strategy | Comportamiento | `lotes/EstrategiaAsignacionLote`, `AsignacionFefo`, `validacion/` | Políticas intercambiables (FEFO) |
| Observer | Comportamiento | `inventario/ObservadorInventario` | Notificar stock bajo sin acoplar |
| Adapter | Estructural | `adapter/EntidadContactoAdapter` | Salida común Lab/Proveedor |
| Prototype | Creacional | `domain/Prototipo`, `Producto.clonar()` | Duplicar productos como plantilla |
| Singleton | Creacional | `config/ConfiguracionNegocio` | Configuración única e inmutable |
| Inyección de Dependencias | Arquitectónico | `@Service`, `@RequiredArgsConstructor` | Bajo acoplamiento y testeabilidad |
| Decorator | Estructural | `comprobante/GeneradorComprobanteConLogging` | Añadir logging sin tocar la lógica |

> El detalle **línea a línea** de los módulos más críticos (FEFO, anulación con reversión de stock,
> emisión de comprobantes y seguridad JWT) está en [`codificacion-modulos-clave.md`](./codificacion-modulos-clave.md).
