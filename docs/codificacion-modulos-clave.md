# Explicación Línea a Línea de los Módulos Clave — Botica Q&M

**Curso:** Desarrollo Web Integrado · **Sección:** 38447
**Profesor:** Ramires Pacheco, Luis Enrrique
**Proyecto:** Sistema de Gestión Integral para Botica Q&M
**Documento:** Análisis de codificación de los módulos críticos (Semana 15)

---

## Índice

1. [Módulo 1 — Cálculo FEFO de lotes](#módulo-1--cálculo-fefo-de-lotes)
2. [Módulo 2 — Anulación lógica con reversión de stock](#módulo-2--anulación-lógica-con-reversión-de-stock)
3. [Módulo 3 — Emisión de comprobantes y resumen de venta](#módulo-3--emisión-de-comprobantes-y-resumen-de-venta)
4. [Módulo 4 — Seguridad con JWT](#módulo-4--seguridad-con-jwt)

> Convención: `archivo:línea` apunta al código real del repositorio. Las tablas numeran bloques
> de líneas y explican su propósito.

---

## Módulo 1 — Cálculo FEFO de lotes

**Objetivo:** al vender, ofrecer primero el lote que **vence antes** (FEFO: *First-Expired,
First-Out*), para minimizar mermas por caducidad —política estándar en farmacia—.

**Archivos:** `lotes/EstrategiaAsignacionLote.java`, `lotes/AsignacionFefo.java`,
`repo/LoteRepository.java`, `service/CatalogoService.java`.

### 1.1 La interfaz (Strategy)

`lotes/EstrategiaAsignacionLote.java`

```java
// 10: interfaz — define el "contrato" de ordenamiento
public interface EstrategiaAsignacionLote {
// 11: recibe los lotes candidatos y devuelve el orden de despacho
    List<Lote> ordenar(List<Lote> lotes);
// 12: }
}
```

| Línea | Explicación |
|---|---|
| 7-9 | El comentario documenta la intención: aislar "qué lote despachar primero" para poder cambiar la política sin tocar a quien la usa. |
| 10 | La interfaz es el punto de extensión (patrón **Strategy**). |
| 11 | El único método recibe una lista y devuelve otra ordenada; no conoce SQL ni el resto del sistema. |

### 1.2 La implementación concreta (FEFO)

`lotes/AsignacionFefo.java`

```java
// 11: @Component → Spring la registra y la inyecta donde se pida la interfaz
@Component
// 12: implementa el contrato
public class AsignacionFefo implements EstrategiaAsignacionLote {
    @Override
// 14: recibe la lista de lotes
    public List<Lote> ordenar(List<Lote> lotes) {
// 15: ordena por fecha de vencimiento ascendente (el más próximo, primero)
        return lotes.stream().sorted(Comparator.comparing(Lote::getFechaVencimiento)).toList();
    }
}
```

| Línea | Explicación |
|---|---|
| 11 | `@Component`: la clase es un bean gestionado por Spring (Singleton). |
| 12 | Implementa `EstrategiaAsignacionLote` (firma compatible con el consumidor). |
| 14 | Recibe los candidatos (lotes con stock y no vencidos). |
| 15 | **El núcleo del FEFO:** `Comparator.comparing(Lote::getFechaVencimiento)` ordena de fecha menor a mayor → el lote más próximo a vencer queda primero. `toList()` produce una lista inmutable. |

### 1.3 La consulta que alimenta el FEFO

`repo/LoteRepository.java`

```java
// 20-21: comentario que aclara el reparto de responsabilidades
// FEFO: con stock, no vencidos. El orden final lo decide EstrategiaAsignacionLote (Strategy),
// no esta query — así se puede cambiar la política sin tocar SQL.
// 22: consulta JPQL
    @Query("select l from Lote l join fetch l.producto join fetch l.laboratorio " +
// 23: filtros: del producto, con stock > 0 y vencimiento futuro
           "where l.producto.id = :productoId and l.stockActual > 0 and l.fechaVencimiento > :hoy")
// 24: método del repositorio
    List<Lote> disponibles(@Param("productoId") Long productoId, @Param("hoy") LocalDate hoy);
```

| Línea | Explicación |
|---|---|
| 22 | `select l from Lote l` con `join fetch l.producto join fetch l.laboratorio`: trae en **una sola consulta** el lote con su producto y laboratorio, evitando el problema **N+1**. |
| 23 | **Filtros de candidatos:** `stockActual > 0` (solo lotes con stock) y `fechaVencimiento > :hoy` (excluye vencidos). El **orden** no se fija aquí, a propósito. |
| 24 | Devuelve los lotes que cumplen las condiciones; la lista llega *desordenada* y será la Strategy quien la ordene. |

### 1.4 El consumidor (uso de la Strategy)

`service/CatalogoService.java`

```java
// 37: se inyecta la INTERFAZ, no la implementación → desacoplamiento
    private final EstrategiaAsignacionLote estrategiaLote; // Strategy (FEFO)
...
// 180: endpoint lógico "lotes disponibles para vender"
    public List<LoteRes> lotesDisponibles(Long productoId) {
// 181: (obtiene candidatos del repositorio)
// 182: aplica la estrategia y convierte a DTO de salida
        return estrategiaLote.ordenar(candidatos).stream().map(this::res).toList();
    }
```

| Línea | Explicación |
|---|---|
| 37 | Se inyecta la **interfaz** `EstrategiaAsignacionLote`; hoy llega `AsignacionFefo`. Cambiar la política = otra implementación, sin tocar esta clase. |
| 180-182 | `lotesDisponibles` obtiene los candidatos del repositorio, los pasa por `estrategiaLote.ordenar(...)` y los mapea a `LoteRes` para el frontend (que preselecciona el primero en el POS). |

**Flujo completo:**

```
POS pide /lotes/disponibles?productoId=N
        │
        ▼
LoteRepository.disponibles(N, hoy)  → lotes con stock, no vencidos (sin orden)
        │
        ▼
AsignacionFefo.ordenar(...)         → ordena por vencimiento (FEFO)
        │
        ▼
Frontend: el primer lote = el que vence antes (preseleccionado)
```

---

## Módulo 2 — Anulación lógica con reversión de stock

**Objetivo:** anular una venta/compra **sin borrarla** (historial contable) y **devolver/descontar
el stock** de forma atómica y segura ante concurrencia.

**Archivos:** `service/VentaService.java` (anular), `service/CompraService.java` (anular),
`repo/LoteRepository.java` (`bloquear`).

### 2.1 Anulación de venta (devuelve stock)

`service/VentaService.java`

```java
// 182-184: comentario: no borra, marca y devuelve stock; bloquea en orden de id para no deadlock;
//          motivo obligatorio.
// 185: @Transactional → todo el método es UNA transacción atómica
    @Transactional
// 186: firma del método
    public VentaRes anular(Long id, AnularReq req) {
// 187-188: busca la venta o lanza 404
        Venta v = ventas.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Venta no encontrada"));
// 189-190: si ya estaba anulada → 409 (no se anula dos veces)
        if (Boolean.TRUE.equals(v.getAnulado()))
            throw new ApiException(HttpStatus.CONFLICT, "La venta ya está anulada");

// 192: acumulador ordenado por id de lote (evita deadlocks al bloquear filas)
        Map<Long, Integer> revertir = new TreeMap<>();
// 193: agrupa las líneas por lote y SUMA cantidades repetidas del mismo lote
        v.getDetalles().forEach(d -> revertir.merge(d.getLote().getId(), d.getCantidad(), Integer::sum));
// 194: recorre en orden de id de lote
        for (Map.Entry<Long, Integer> e : revertir.entrySet()) {
// 195-197: bloquea la fila del lote (SELECT … FOR UPDATE) o falla
            Lote lote = lotes.bloquear(e.getKey())
                    .orElseThrow(() -> new ApiException(HttpStatus.CONFLICT,
                            "No se puede anular: el lote " + e.getKey() + " ya no existe"));
// 198: devuelve al stock las unidades que la venta había descontado
            lote.setStockActual(lote.getStockActual().add(BigDecimal.valueOf(e.getValue())));
// 199: persiste el lote
            lotes.save(lote);
        }
// 201: marca la venta como anulada (baja lógica)
        v.setAnulado(true);
// 202: guarda el motivo (obligatorio, ya validado con @NotBlank en AnularReq)
        v.setMotivoAnulacion(req.motivo().trim());
// 203: sella la fecha de anulación
        v.setFechaAnulacion(Tiempo.ahora());
// 204: persiste la venta
        ventas.save(v);
// 205: devuelve el DTO actualizado
        return res(v, false);
    }
```

| Línea | Explicación |
|---|---|
| 185 | `@Transactional` garantiza **atomicidad**: si algo falla, se revierte todo (ni stock cambiado ni marca puesta). |
| 187-188 | Carga la venta; si no existe → `404`. |
| 189-190 | **Idempotencia controlada:** anular dos veces devuelve `409`. |
| 192 | `TreeMap<Long,Integer>` mantiene los lotes **ordenados por id**. Esto evita *deadlocks*: dos anulaciones concurrentes bloquearán las filas en el mismo orden. |
| 193 | `merge(..., Integer::sum)` **agrupa** por lote: si la venta tiene dos líneas del mismo lote, se revierte la suma. |
| 195-197 | `lotes.bloquear(...)` ejecuta `SELECT … FOR UPDATE` (ver 2.3): nadie más modifica esa fila mientras dura la transacción. |
| 198 | **Reversión:** `stock + cantidades`. |
| 201-203 | **Baja lógica:** no se hace `delete`; se marca `anulado=true`, se guarda el motivo y la fecha. |
| 205 | Retorna el DTO con el estado nuevo. |

### 2.2 Anulación de compra (descuenta stock, con validación)

`service/CompraService.java`

```java
// 166: transacción atómica
    @Transactional
// 167: firma
    public CompraRes anular(Long id, AnularReq req) {
// 168-169: busca o 404
        Compra c = compras.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Compra no encontrada"));
// 170-171: ya anulada → 409
        if (Boolean.TRUE.equals(c.getAnulado()))
            throw new ApiException(HttpStatus.CONFLICT, "La compra ya está anulada");

// 173: acumulador ordenado por id de lote
        Map<Long, BigDecimal> revertir = new TreeMap<>();
// 174-175: agrupa por lote: unidades a descontar = cantidad(cajas) × unidadesPorCaja
        c.getDetalles().forEach(d -> revertir.merge(d.getLote().getId(),
                BigDecimal.valueOf((long) d.getCantidad() * d.getUnidadesPorCaja()), BigDecimal::add));
// 176: recorre en orden
        for (Map.Entry<Long, BigDecimal> e : revertir.entrySet()) {
// 177-179: bloquea el lote
            Lote lote = lotes.bloquear(e.getKey())
                    .orElseThrow(() -> new ApiException(HttpStatus.CONFLICT,
                            "No se puede anular: el lote " + e.getKey() + " ya no existe"));
// 180-185: VALIDACIÓN: si no hay stock suficiente para revertir → 409 con explicación
            if (lote.getStockActual().compareTo(e.getValue()) < 0)
                throw new ApiException(HttpStatus.CONFLICT,
                        "No se puede anular: el lote " + lote.getNumeroLote() + " de "
                                + lote.getProducto().getNombre() + " solo tiene " + lote.getStockActual()
                                + " y hay que revertir " + e.getValue() + " unidades. "
                                + "Hay ventas posteriores que consumieron ese stock.");
// 186: descuenta del stock lo que la compra había ingresado
            lote.setStockActual(lote.getStockActual().subtract(e.getValue()));
// 187: persiste el lote
            lotes.save(lote);
        }
// 189-191: baja lógica de la compra
        c.setAnulado(true);
        c.setMotivoAnulacion(req.motivo().trim());
        c.setFechaAnulacion(Tiempo.ahora());
// 192: persiste
        compras.save(c);
// 193: devuelve DTO
        return res(c, false);
    }
```

| Línea | Explicación |
|---|---|
| 166 | Transacción atómica. |
| 173-175 | Agrupa por lote y calcula las **unidades** a revertir: `cajas × unidadesPorCaja` (coherente con cómo ingresó el stock). |
| 180-185 | **Regla de negocio crítica:** si el stock actual es menor al que se quiere revertir significa que **ya se vendió** parte de esa mercadería → `409` con mensaje explicativo; la compra queda intacta. |
| 186 | Descuenta el stock ingresado. |
| 189-191 | Baja lógica con motivo y fecha. |

> **Diferencia clave:** en la **venta** se *suma* stock (se devuelve lo vendido); en la **compra**
> se *resta* stock (se retira lo ingresado) y **puede fallar** si ya se consumió.

### 2.3 El bloqueo de fila que hace todo seguro

`repo/LoteRepository.java`

```java
// 36-39
    // Bloqueo de fila (SELECT ... FOR UPDATE) para que ventas/compras simultáneas no descuadren el stock
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select l from Lote l where l.id = :id")
    Optional<Lote> bloquear(@Param("id") Long id);
```

| Línea | Explicación |
|---|---|
| 37 | `@Lock(PESSIMISTIC_WRITE)` traduce a `SELECT … FOR UPDATE` en PostgreSQL. |
| 38-39 | Obtiene el lote con **bloqueo exclusivo**: dos transacciones no pueden modificar el mismo stock a la vez → se evita el *lost update*. |

---

## Módulo 3 — Emisión de comprobantes y resumen de venta

**Objetivo:** numerar comprobantes de forma **consecutiva por serie** (sin huecos, seguro ante
concurrencia) y **imprimir** el comprobante con los datos de la venta.

**Archivos:** `comprobante/GeneradorComprobanteSerie.java`,
`repo/SerieComprobanteRepository.java`, `service/VentaService.java`, frontend `impresion.js`.

### 3.1 Correlativo atómico por serie (backend)

`repo/SerieComprobanteRepository.java`

```java
// 13: consulta NATIVA (SQL de PostgreSQL), un solo statement
    @Query(value = "INSERT INTO serie_comprobante (serie, correlativo) VALUES (:serie, 1) " +
// 14: si la serie ya existe, incrementa el correlativo
            "ON CONFLICT (serie) DO UPDATE SET correlativo = serie_comprobante.correlativo + 1 " +
// 15: devuelve el nuevo correlativo
            "RETURNING correlativo", nativeQuery = true)
// 16: firma
    Long incrementarYObtener(@Param("serie") String serie);
```

| Línea | Explicación |
|---|---|
| 13 | `INSERT … VALUES (:serie, 1)`: si la serie **no existe**, la crea empezando en 1. |
| 14 | `ON CONFLICT (serie) DO UPDATE … + 1`: si **existe**, incrementa. Es un *upsert* atómico. |
| 15 | `RETURNING correlativo` devuelve el valor final en la misma sentencia → **sin condiciones de carrera** entre ventas simultáneas o varias instancias de la app. |

`comprobante/GeneradorComprobanteSerie.java`

```java
// 18-22:
    @Override
    public String siguienteNumero() {
// 20: pide a la BD el siguiente correlativo de la serie (atómico)
        Long correlativo = repo.incrementarYObtener(serie);
// 21: formatea "B001-000123" (serie + guion + 6 dígitos con ceros a la izquierda)
        return serie + "-" + String.format("%06d", correlativo);
    }
```

| Línea | Explicación |
|---|---|
| 20 | Obtiene el correlativo real de la base (no el id interno de la venta). |
| 21 | `String.format("%06d", n)` rellena con ceros: 123 → `000123`. El número final es `B001-000123`. |

### 3.2 Uso en el registro de venta (backend)

`service/VentaService.java`

```java
// 63-69: Builder arma la cabecera; nótese que aún NO se fija el número
        VentaBuilder venta = VentaBuilder.nueva()
                .cliente(cliente).empleado(empleado).fechaVenta(Tiempo.ahora())
                .tipoComprobante(tipo).metodoPago(req.metodoPago())
                .receta(req.recetaPresentada(), req.recetaNumero(), req.recetaMedico());
...
// 114-121: se decide el número
        Venta v = venta
// 117: si el front NO mandó número...
                .numeroComprobante(blank(req.numeroComprobante())
// 118: ...lo genera la Abstract Factory de la serie correspondiente
                        ? comprobantes.paraTipo(tipo).siguienteNumero()
// 119: ...si el front SÍ mandó uno (p. ej. migración), se respeta
                        : req.numeroComprobante().trim())
// 120: total redondeado a 2 decimales (HALF_UP)
                .total(total.setScale(2, RoundingMode.HALF_UP))
// 121: crea la entidad Venta
                .build();
// 122: persiste la venta (con sus detalles por cascade)
        ventas.save(v);
// 123: devuelve el DTO con el número ya asignado
        return res(v, true);
```

| Línea | Explicación |
|---|---|
| 117 | `blank(...)` comprueba si el cliente envió número de comprobante. |
| 118 | Si no, `comprobantes.paraTipo(tipo).siguienteNumero()` obtiene el generador de la serie (`BOLETA`→`B001`, `FACTURA`→`F001`) y pide el siguiente número —gracias a la **Abstract Factory**. |
| 119 | Si el usuario lo envió (por ejemplo, al registrar un comprobante físico), se respeta. |
| 120 | El **servidor** calcula el total (nunca confía en el cliente) y lo redondea. |
| 121-122 | `build()` crea la entidad y `save()` la persiste; los detalles se guardan en cascada. |
| 123 | Retorna el DTO (incluye `numeroComprobante`) que el frontend imprimirá. |

### 3.3 Resumen/comprobante imprimible (frontend)

`src/impresion.js`

```js
// 8-9: escapa caracteres peligrosos para que un nombre con "<" o "&" no rompa el HTML
const esc = (s) => String(s ?? '').replace(/[&<>"']/g, (c) => (
  { '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));

// 11: construye el HTML del comprobante a partir del objeto venta
function htmlComprobante(v) {
// 12-18: una fila <tr> por cada detalle (producto, lote, cantidad, P.U., importe)
  const filas = (v.detalles || []).map((d) => `
    <tr>
      <td>${esc(d.productoNombre)}<small>Lote ${esc(d.numeroLote)}${d.requiereReceta ? ' · con receta' : ''}</small></td>
      <td class="n">${esc(d.cantidad)}</td>
      <td class="n">${money(d.precioVenta)}</td>
      <td class="n">${money(d.cantidad * d.precioVenta)}</td>
    </tr>`).join('');
// 20-21: bloque de receta (solo si se presentó)
  const receta = v.recetaPresentada ? `...` : '';
// 24-57: documento HTML completo con estilos de ticket (72 mm)
  ...
// 54: total formateado con el helper money()
  <div class="total"><span>TOTAL</span><span>${money(v.total)}</span></div>
```

```js
// 62: exporta la función de impresión
export function imprimirVenta(venta) {
// 63: elimina un iframe previo si existe
  document.getElementById('iframe-impresion')?.remove();
// 64-68: crea un iframe OCULTO (0x0) fuera de pantalla
  const iframe = document.createElement('iframe');
  iframe.id = 'iframe-impresion';
  iframe.setAttribute('aria-hidden', 'true');
  iframe.style.cssText = 'position:fixed;right:0;bottom:0;width:0;height:0;border:0;';
  document.body.appendChild(iframe);
// 69-72: escribe el HTML del comprobante en el iframe
  const doc = iframe.contentWindow.document;
  doc.open(); doc.write(htmlComprobante(venta)); doc.close();
// 73: da foco e imprime tras 150 ms (deja que el documento se pinte)
  setTimeout(() => { iframe.contentWindow.focus(); iframe.contentWindow.print(); }, 150);
}
```

| Línea | Explicación |
|---|---|
| 8-9 | `esc()` **sanitiza** todos los datos provenientes de la BD (previene inyección de HTML). |
| 12-18 | Genera las filas de la tabla de detalle, mostrando `· con receta` cuando aplica. |
| 24-57 | Plantilla HTML del **ticket** de 72 mm con estilos de impresora térmica. |
| 62-73 | `imprimirVenta` usa un **iframe oculto** en vez de `window.open`: no lo bloquea el bloqueador de *pop-ups* (aunque antes se haya hecho un `fetch`) y no navega fuera de la pantalla actual. |
| 73 | El pequeño `setTimeout` asegura que el contenido esté renderizado antes de `print()`. |

---

## Módulo 4 — Seguridad con JWT

**Objetivo:** autenticación **sin estado** (stateless) mediante **JSON Web Token** y autorización
por **rol**, con respuestas `401`/`403` correctas.

**Archivos:** `security/JwtService.java`, `security/JwtFilter.java`, `config/SecurityConfig.java`,
`service/AuthService.java`.

### 4.1 Generación y lectura del token

`security/JwtService.java`

```java
// 19-23: el constructor inyecta la clave y la expiración desde application.yml
    public JwtService(@Value("${app.jwt.secret}") String secret,
                      @Value("${app.jwt.expiration-minutes}") long minutes) {
// 21: construye la clave HMAC-SHA a partir del secreto (mínimo 32 bytes)
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
// 22: convierte minutos a milisegundos
        this.ttlMs = minutes * 60_000;
    }

// 25: genera un token para un usuario
    public String generar(Usuario u) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
// 28: "sub" = id del usuario
                .subject(String.valueOf(u.getId()))
// 29: claim personalizado con el rol
                .claim("rol", u.getRol().name())
// 30: fecha de emisión
                .issuedAt(new Date(now))
// 31: fecha de expiración (emisión + ttl)
                .expiration(new Date(now + ttlMs))
// 32: firma con la clave secreta
                .signWith(key)
// 33: serializa a String
                .compact();
    }

// 36-38: valida la firma y expiración, y devuelve las claims
    public Claims leer(String token) {
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
    }
```

| Línea | Explicación |
|---|---|
| 21 | La clave se deriva del secreto; si es muy corta, `hmacShaKeyFor` lanza error (fail-fast). |
| 28 | El `subject` del token es el **id** del usuario (no el correo), usado luego como principal. |
| 29 | Se incluye el **rol** como claim para autorizar por rol sin consultar la BD en cada petición. |
| 30-31 | `issuedAt`/`expiration` definen la vigencia del token. |
| 32-33 | `signWith(key).compact()` firma y produce el string `header.payload.signature`. |
| 37 | `verifyWith(key)` valida la **firma**; `parseSignedClaims` además comprueba la **expiración** y lanza `JwtException` si algo falla. |

### 4.2 El filtro que autentica cada petición

`security/JwtFilter.java`

```java
// 18: hereda OncePerRequestFilter (se ejecuta una vez por petición)
public class JwtFilter extends OncePerRequestFilter {
    private final JwtService jwt;
    public JwtFilter(JwtService jwt) { this.jwt = jwt; }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
// 26: lee la cabecera Authorization
        String h = req.getHeader("Authorization");
// 27: solo actúa si empieza con "Bearer "
        if (h != null && h.startsWith("Bearer ")) {
            try {
// 29: valida el token y obtiene las claims (quita los 7 caracteres de "Bearer ")
                Claims c = jwt.leer(h.substring(7));
// 30-32: crea la autenticación: principal = id, authorities = ROLE_<rol>
                var auth = new UsernamePasswordAuthenticationToken(
                        Long.valueOf(c.getSubject()), null,
                        List.of(new SimpleGrantedAuthority("ROLE_" + c.get("rol", String.class))));
// 33: la registra en el contexto de seguridad
                SecurityContextHolder.getContext().setAuthentication(auth);
// 34-36: si el token es inválido/vencido → NO autentica (seguirá 401)
            } catch (JwtException | IllegalArgumentException e) { /* sin autenticar */ }
        }
// 38: continúa la cadena de filtros
        chain.doFilter(req, res);
    }
}
```

| Línea | Explicación |
|---|---|
| 18 | `OncePerRequestFilter` asegura una sola ejecución por petición. |
| 26-27 | Lee `Authorization: Bearer <token>`; si no está o no es Bearer, no hace nada (queda anónimo). |
| 29 | Delega en `JwtService.leer` (firma + expiración). Si falla, cae al `catch`. |
| 30-32 | Construye el `Authentication`: **principal = id del usuario** (por eso los controllers usan `@AuthenticationPrincipal Long empleadoId`) y **authority = `ROLE_EMPLEADO`/`ROLE_ADMINISTRADOR`**. |
| 33 | Publica la autenticación en el `SecurityContextHolder`. |
| 34-36 | Token inválido/vencido → no autentica → la petición sigue y termina en **401** si la ruta está protegida. |
| 38 | La petición continúa hacia los filtros y el controller. |

### 4.3 Reglas de autorización

`config/SecurityConfig.java`

```java
// 40-59: cadena de seguridad
        http
// 41: sin CSRF (API stateless con token)
            .csrf(c -> c.disable())
// 42: aplica CORS
            .cors(Customizer.withDefaults())
// 43: sesiones STATELESS (no usa HttpSession)
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
// 44: sin autenticación → 401 (no redirige a un login HTML)
            .exceptionHandling(e -> e.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
            .authorizeHttpRequests(a -> a
// 46: permitir el despachador de errores
                .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
// 47: preflight CORS
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
// 48: login público
                .requestMatchers("/auth/**").permitAll()
// 50: clientes: ADMIN y EMPLEADO
                .requestMatchers(HttpMethod.GET, "/usuarios/clientes").hasAnyRole(admin, emp)
// 51: resto de usuarios: solo ADMIN
                .requestMatchers("/usuarios/**").hasRole(admin)
// 53: LECTURA de catálogos: ADMIN y EMPLEADO
                .requestMatchers(HttpMethod.GET, "/productos/**", ...).hasAnyRole(admin, emp)
// 55: ESCRITURA de catálogos: solo ADMIN
                .requestMatchers("/productos/**", ...).hasRole(admin)
// 57-58: dashboard, faq, ventas, compras: ADMIN y EMPLEADO
                .requestMatchers("/faq/**").hasAnyRole(admin, emp)
                .requestMatchers("/dashboard/**").hasAnyRole(admin, emp)
                .requestMatchers("/ventas/**", "/compras/**").hasAnyRole(admin, emp)
// 59: cualquier otra ruta: denegada
                .anyRequest().denyAll())
// 60: registra el JwtFilter antes del filtro de usuario/contraseña
            .addFilterBefore(new JwtFilter(jwt), UsernamePasswordAuthenticationFilter.class);
```

| Línea | Explicación |
|---|---|
| 41 | API sin estado con token → CSRF no aplica y se deshabilita. |
| 43 | `STATELESS`: no se crean sesiones en el servidor. |
| 44 | `HttpStatusEntryPoint(UNAUTHORIZED)`: si falta/expira el token, responde **401** en JSON (no redirige a un login). |
| 50-51 | `/usuarios/clientes` es la única ruta de usuarios abierta a EMPLEADO (la usa el POS). |
| 53-55 | **Separación lectura/escritura:** leer catálogos es para ambos; crearlos/editarlos, solo ADMIN. |
| 57-58 | Rutas operativas y de ayuda para ambos roles. |
| 59 | `denyAll()` como red de seguridad: lo no listado queda cerrado. |
| 60 | El filtro JWT se ejecuta **antes** del filtro estándar, para que la autenticación ya esté resuelta. |

### 4.4 Verificación de credenciales

`service/AuthService.java` (patrón general)

```java
// 1) Busca el usuario por correo
// 2) Comprueba que esté ACTIVO
// 3) Verifica la contraseña con el PasswordEncoder (BCrypt), nunca texto plano
// 4) Si todo es correcto, JwtService.generar(usuario) -> devuelve { token, usuario }
//    Si no, lanza ApiException(401) "correo o contraseña incorrectos"
```

**Flujo de seguridad completo:**

```
1. POST /auth/login  {correo, password}
        │  (AuthService valida credenciales con BCrypt)
        ▼
   { token }  ← JwtService.generar() (firma HMAC, incluye id + rol)
        │
2. El frontend guarda el token y lo envía en:  Authorization: Bearer <token>
        │
3. JwtFilter:  valida firma/expiración → registra Authentication (ROLE_<rol>)
        │
4. SecurityConfig: ¿la ruta permite ese rol?
        ├─ sí  → llega al Controller (principal = id del usuario)
        ├─ sin token  → 401 Unauthorized
        └─ rol insuf. → 403 Forbidden
```

**Evidencia en pruebas:** `src/test/.../seguridad/SeguridadWebTest.java` verifica `401` sin token,
`403` para EMPLEADO en rutas de ADMIN y `200` con rol autorizado.

---

## Resumen

| Módulo | Idea central | Garantía técnica |
|---|---|---|
| **FEFO** | Despachar primero el lote que vence antes | Strategy + consulta de candidatos; orden desacoplado del SQL |
| **Anulación** | Baja lógica + reversión de stock | `@Transactional` + `SELECT … FOR UPDATE` + agrupación ordenada (anti-deadlock) |
| **Comprobantes** | Numeración consecutiva por serie + impresión | Upsert atómico (`ON CONFLICT … RETURNING`) + Abstract Factory + iframe oculto |
| **Seguridad JWT** | Autenticación stateless por token y autorización por rol | Firma HMAC + filtro `OncePerRequest` + reglas declarativas (401/403) |

> Los patrones de diseño implicados (Strategy, Builder, Abstract Factory, etc.) se explican en
> [`patrones-de-diseno.md`](./patrones-de-diseno.md).
