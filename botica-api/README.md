# Botica â€“ API REST (Spring Boot 3, Java 17, PostgreSQL)

## Requisitos
- JDK 17+ y Maven (o abrir el proyecto en IntelliJ / VS Code / STS)
- Base ya migrada, en este orden: script de tablas â†’ migracion_laboratorio_lote.sql â†’
  migracion_producto_precio_unico.sql â†’ migracion_producto_atributos.sql â†’
  migracion_compra_unidades_por_caja.sql â†’
  migracion_venta_receta.sql â†’
  migracion_serie_comprobante.sql
  (`ddl-auto: none`, el esquema lo maneja SQL a mano)

> Si en algÃºn momento corriste `migracion_producto_presentaciones.sql` (una versiÃ³n anterior,
> con precio por presentaciÃ³n), no pasa nada: `migracion_producto_precio_unico.sql` detecta eso
> y lo deshace automÃ¡ticamente. Ejecuta solo los scripts listados arriba, en orden.

## ConfiguraciÃ³n (variables de entorno o `application.yml`)
| Variable | Por defecto |
|---|---|
| DB_URL | jdbc:postgresql://<HOST>:<PUERTO>/<BASE_DE_DATOS> |
| DB_USER / DB_PASSWORD | postgres / postgres |
| JWT_SECRET | cambiar (mÃ­nimo 32 caracteres) |
| CORS_ORIGINS | http://localhost:5173 (varios separados por coma) |

Supabase: `DB_URL=jdbc:postgresql://<HOST>:<PUERTO>/<BASE_DE_DATOS>`, con el usuario y clave de la conexiÃ³n de la base.

## Ejecutar
    mvn spring-boot:run

Prueba rÃ¡pida (usuarios del script de datos, clave `password`):

    curl -X POST localhost:8080/api/auth/login -H "Content-Type: application/json" \
         -d '{"correo":"admin@botica.com","password":"password"}'

(si no configuraste `server.servlet.context-path: /api`, quita el `/api` del ejemplo)

## Permisos por rol
| Recurso | ADMINISTRADOR | EMPLEADO |
|---|---|---|
| /auth/login | pÃºblico | pÃºblico |
| /usuarios (CRUD) | sÃ­ | no |
| /usuarios/clientes (GET) | sÃ­ | sÃ­ |
| productos, laboratorios, proveedores, lotes | leer y escribir | solo leer |
| /ventas, /compras | sÃ­ | sÃ­ |
| POST /productos/{id}/duplicar | sÃ­ | no |
| DELETE /productos/{id}/definitivo, /laboratorios/{id}/definitivo, /proveedores/{id}/definitivo | sÃ­ | no |

## Modelo: producto, atributos por categorÃ­a y unidad de compra/venta
- **producto**: cualquier cosa que la botica compre y venda (medicamento, paÃ±al, biberÃ³n, jabÃ³n,
  alcohol, mascarilla, suplemento, insumo de curaciÃ³n...). Tiene `categoria` (MEDICAMENTO,
  SUPLEMENTO, PANALES, BIBERON, HIGIENE, CUIDADO_PERSONAL, CURACION, DISPOSITIVO_MEDICO,
  ACCESORIO, OTRO) y `requiereReceta`.
- **atributos** (JSONB): lo especÃ­fico de cada categorÃ­a, en JSON libre â€” el backend no valida su
  forma, solo la persiste y la devuelve tal cual. El frontend decide quÃ© claves mostrar segÃºn la
  categorÃ­a (`concentracion`/`formaFarmaceutica`/`principioActivo` para medicamentos, `talla`/`etapa`
  para paÃ±ales, `capacidad`/`unidadCapacidad`/`material` para biberones, etc.). Agregar una categorÃ­a
  nueva con sus propios campos no requiere ninguna migraciÃ³n de base de datos.
- **Un solo precio**: `precioVenta` es Ãºnico por producto, expresado por `unidadVenta`. Dos tamaÃ±os
  de paquete distintos del mismo artÃ­culo (paÃ±ales x30 y x50) son dos productos separados, cada uno
  con su propio precio â€” no son la misma ficha con distinta presentaciÃ³n.
- **"Marca" y "Fabricante" no son campos de producto**: ya estÃ¡n resueltos por `laboratorios`
  (fabricante) y `proveedor` (a quiÃ©n se le compra), que el producto ya referencia.
- **Todo se vende por unidad**: `unidadVenta` es solo la etiqueta (TABLETA, UNIDAD, ML...) y el stock
  del lote vive en esa unidad. CuÃ¡ntas unidades trae una caja **no es dato del producto**: se ingresa
  en cada compra, porque puede variar entre compras.
- **lote**: nÃºmero, vencimiento, laboratorio fabricante; `stockActual` (NUMERIC) siempre estÃ¡
  expresado en la `unidadVenta` del producto.

## Patrones aplicados

- **Template Method** (`ServicioCatalogoContacto`): Laboratorio y Proveedor comparten campos
  (`EntidadContacto`, `@MappedSuperclass`) y el mismo flujo de listar/crear/actualizar/desactivar.
  Cada uno solo aporta su repositorio y su `aplicar()` (`LaboratorioService`, `ProveedorService`).
- **Adapter** (`adapter.EntidadContactoAdapter`): convierte `Laboratorio`/`Proveedor` (entidades JPA)
  a `EntidadContactoRes` antes de devolverlas por la API, para no exponer la entidad de persistencia
  tal cual. El JSON no cambiÃ³ (mismos campos), solo se dejÃ³ de filtrar la entidad directamente.
- **Strategy** â€” dos casos:
  - `validacion.ValidadorAtributos`: cada categorÃ­a de producto valida sus propios `atributos`
    (talla, forma farmacÃ©utica, material...) contra los mismos catÃ¡logos que ya usan los `<select>`
    del frontend. Antes se guardaba cualquier JSON sin revisar su forma.
  - `lotes.EstrategiaAsignacionLote` (`AsignacionFefo`): decide quÃ© lote ofrecer primero
    (`/lotes/disponibles`). Hoy es FEFO; cambiar la polÃ­tica es agregar OTRA implementaciÃ³n,
    sin tocar `CatalogoService` ni la consulta SQL.
- **Singleton** â€” dos niveles:
  - `config.ConfiguracionNegocio`: singleton explÃ­cito (constructor privado + clase interna *holder*,
    thread-safe sin `synchronized`, estado inmutable). Concentra lo que es igual para toda la app:
    zona horaria (America/Lima), umbral de stock bajo (10) y categorÃ­as de producto vÃ¡lidas. Antes eran
    constantes sueltas en `Tiempo`, `VentaService` y `CatalogoService`; los valores no cambiaron.
  - Todo `@Service`/`@Component` de Spring ya es una Ãºnica instancia gestionada por el contenedor (por
    ejemplo, `JwtService`), y `validacion.RegistroValidadoresAtributos` arma una sola vez al arrancar el
    mapa categorÃ­a â†’ validador (Strategy). Para beans se usa el scope de Spring, no un singleton a mano.
- **Abstract Factory** (`comprobante.FabricaComprobante`): una fÃ¡brica por tipo de comprobante
  (`FabricaBoleta` â†’ B001, `FabricaFactura` â†’ F001) que crea la familia coherente de productos:
  `DescriptorComprobante` (tipo y serie) + `GeneradorComprobante` (numeraciÃ³n, ya envuelto en su Decorator
  de logging). `GeneradorComprobanteFactory.paraTipo(tipo)` es el cliente: Spring le inyecta todas las
  fÃ¡bricas y elige la del tipo pedido (mismo error 400 si el tipo no existe). Agregar un tipo nuevo es
  agregar OTRO `@Component` que implemente la interfaz. AdemÃ¡s, cada serie tiene su propio correlativo en la
  base (`serie_comprobante`, upsert atÃ³mico seguro ante ventas concurrentes), en vez de usar el ID interno de
  la venta, que dejaba huecos.
- **Builder** (`builder.*`): `VentaBuilder` + `VentaDetalleBuilder`, `CompraBuilder` +
  `CompraDetalleBuilder` y `LoteBuilder` arman las entidades de las transacciones paso a paso. Normalizan los
  opcionales (vacÃ­o â†’ null), exigen los campos obligatorios en `build()` y enlazan cada detalle con su
  cabecera, de modo que la relaciÃ³n bidireccional nunca queda a medias. Cada `build()` devuelve una
  instancia nueva. Los servicios (`VentaService`, `CompraService`) conservan exactamente las mismas
  reglas, validaciones y orden de operaciones.
- **Prototype** (`domain.Prototipo`, `Producto.clonar()`): copia profunda de un producto (los `atributos`
  JSON anidados se duplican; los laboratorios se comparten porque son del catÃ¡logo), sin id, lista para
  guardarse como producto nuevo. Ãštil porque paÃ±ales x30 y x50 son dos productos separados.
  Ãšnico agregado de API (no reemplaza nada): `POST /productos/{id}/duplicar` (solo ADMINISTRADOR, 201 con el
  producto nuevo, nombre `"<original> (copia)"`).
- **Decorator** (`comprobante.GeneradorComprobanteConLogging`): agrega una traza de auditorÃ­a a
  cualquier `GeneradorComprobante` sin tocar su lÃ³gica de numeraciÃ³n; se puede envolver cualquier
  implementaciÃ³n futura (por ejemplo, una que consulte un sistema externo de facturaciÃ³n).
- **Observer** (`inventario.ObservadorInventario`): `VentaService` avisa a todos los observadores
  registrados cuando el stock de un lote queda bajo tras una venta, sin conocerlos. Hoy solo existe
  `NotificadorStockBajoLog` (deja un log); agregar una alerta por correo es agregar OTRO
  `@Component` que implemente la interfaz, sin tocar `VentaService`.

## Reglas de negocio
- **Compra** (`POST /compras`): cada lÃ­nea indica producto, laboratorio fabricante (debe estar
  asociado al producto), lote, vencimiento, **cajas compradas**, **unidades por caja** (en esa compra)
  y **precio por caja**. El servidor suma al stock `cajas Ã— unidadesPorCaja` (crea el lote si no
  existe, o le suma si ya existe con la misma fecha de vencimiento y laboratorio). El total de la
  compra es `cajas Ã— precio por caja`.
- **Venta** (`POST /ventas`): cada lÃ­nea indica lote y cantidad **en unidades**. El precio es el Ãºnico
  `precioVenta` del producto. El servidor bloquea el lote (`SELECT â€¦ FOR UPDATE`), valida que no estÃ©
  vencido y que el stock alcance, y descuenta. Todo en una transacciÃ³n. Si no se envÃ­a nÃºmero de
  comprobante se genera (`B001-000012`).
- **Receta** (`POST /ventas`): si algÃºn producto de la venta tiene `requiereReceta`, el request debe traer
  `recetaPresentada: true`; si no, responde 409 (`Requiere receta: X, Yâ€¦`) y no se descuenta nada.
  `recetaNumero` y `recetaMedico` son opcionales y se guardan en la venta; cada lÃ­nea guarda ademÃ¡s
  `requiereReceta` como foto del producto al momento de vender. Esto registra la confirmaciÃ³n del
  vendedor, no valida la receta en sÃ­.
- **Lotes**: solo se corrigen nÃºmero, vencimiento y laboratorio; el stock cambia Ãºnicamente por
  compras y ventas. `GET /lotes/disponibles?productoId=` devuelve stock > 0 y no vencidos, orden FEFO.
- **DELETE**: baja lÃ³gica (`activo = false`) en usuarios, proveedores, laboratorios y productos.
- **DELETE â€¦/definitivo** (productos, laboratorios, proveedores): eliminaciÃ³n real, solo si no se pierde
  historial; si no, responde **409** con el motivo y el registro queda intacto (se puede desactivar):
  - *Producto*: solo si no tiene lotes. Los lotes nacen de una compra, asÃ­ que un producto con lotes
    tiene compras (y quizÃ¡ ventas); el mensaje indica cuÃ¡ntos lotes y cuÃ¡nto stock quedan. Sus lotes
    no se tocan: el producto se desactiva y los lotes siguen visibles en Lotes.
  - *Laboratorio*: solo si ningÃºn lote lo tiene como fabricante. Si procede, primero se quita de la lista de
    fabricantes de los productos (`producto_laboratorio`) y luego se borra.
  - *Proveedor*: solo si no tiene compras.
  Las reglas se revisan en el servicio, sin depender de que la base tenga o no `ON DELETE CASCADE`.
- Errores: siempre `{ "message": "..." }` (400 validaciÃ³n, 401 sin sesiÃ³n, 403 sin permiso, 409 conflicto).

