# Botica – Frontend React (Vite)

    npm install
    cp .env.example .env     # ajusta VITE_API_URL
    npm run dev              # http://localhost:5173

En Spring Boot habilita CORS para http://localhost:5173.

## Contrato de la API esperado (JSON, JWT en Authorization: Bearer)

| Método | Ruta | Notas |
|---|---|---|
| POST | /auth/login | body {correo,password} → {token, usuario:{id,nombre,rol}} |
| GET/POST | /usuarios, /laboratorios, /proveedores, /productos | PUT/DELETE en /{id} (DELETE = baja lógica donde hay `activo`) |
| GET | /usuarios/clientes | clientes activos (accesible a EMPLEADO) |
| GET | /productos | cada uno con `precioVenta` (único), `unidadVenta` (etiqueta), `atributos` (JSON por categoría) y `laboratorios:[{id,nombre}]` |
| POST/PUT | /productos | reciben `laboratorioIds:[...]` y `atributos:{...}` |
| GET | /lotes | incluye productoNombre, laboratorioNombre; `stockActual` está en la **unidad de venta** del producto. PUT /lotes/{id} solo numeroLote, fechaVencimiento y (opcional) laboratorioId |
| GET | /lotes/disponibles?productoId= | stock > 0 y no vencidos, orden FEFO |
| GET | /ventas, /ventas/{id} | detalles:[{productoNombre,numeroLote,cantidad,precioVenta}] — cantidad en unidades |
| POST | /ventas | {clienteId\|null,metodoPago,tipoComprobante,recetaPresentada,recetaNumero,recetaMedico,items:[{loteId,cantidad}]} → el backend toma el empleado del token, usa el precio único del producto y calcula el total |
| GET | /compras, /compras/{id} | detalles:[{productoNombre,laboratorioNombre,numeroLote,cantidad,unidadesPorCaja,precioCompra}] — cantidad = cajas, precio = por caja |
| POST | /compras | {proveedorId,tipoComprobante,numeroComprobante,items:[{productoId,laboratorioId,numeroLote,fechaVencimiento,cantidad,unidadesPorCaja,precioCompra}]} → crea/actualiza lotes; el stock sumado = cantidad × unidadesPorCaja |

## Compra por cajas, venta por unidad

Cada producto tiene **un solo precio de venta** y todo se vende **por unidad**. En cada compra se
registra cuántas cajas llegaron y cuántas unidades trae cada caja **en esa compra** (puede variar):

- Compras 5 cajas de paracetamol de 100 tabletas → el lote sube `5 × 100 = 500` tabletas.
- Vender 3 tabletas descuenta 3 del lote elegido (FEFO).
- El costo se registra por caja (`precioCompra`) y el total de la compra es `cajas × precio por caja`.

Dos tamaños de paquete distintos (pañales x30 y x50) se registran como dos productos separados.

## Patrones aplicados (para no repetir código)
- **Strategy / config-driven UI** (`pages/Productos.jsx`): qué campos pide cada categoría vive en
  una tabla (`ATRIBUTOS_POR_CATEGORIA`), no en un bloque de JSX por categoría. Un único componente
  `CampoAtributo` sabe dibujar cualquier campo (texto, número, select, select con grupos) a partir
  de su definición. Agregar una categoría nueva es agregar una entrada en la tabla, igual que los
  `fields` de `components/CrudPage.jsx` para los demás CRUD.

## Historial de ventas: resumen en ventana y comprobante imprimible
- **Clic en una venta** → abre un resumen en una ventana emergente (`components/ResumenVenta.jsx`):
  cabecera, líneas, total y botón Imprimir. Se cierra con Esc, clic fuera o el botón Cerrar.
- **Botón "Imprimir" en cada fila** → trae `GET /ventas/{id}` (la lista no incluye las líneas) e imprime
  el comprobante en formato ticket de 72 mm (`impresion.js`). Usa un iframe oculto, así que no lo bloquea
  el bloqueador de ventanas emergentes. Los datos del encabezado (nombre, RUC, dirección, teléfono) se
  editan en la constante `NEGOCIO` al inicio de ese archivo; los vacíos no se imprimen.
- `pages/HistorialVentas.jsx` reutiliza la tabla de `Historial` con dos props opcionales, `onSelect` y
  `rowActions`. **Compras no cambió**: sin esas props, `Historial` sigue mostrando el desplegable.
- No hay cambios en la API: usa los mismos `GET /ventas` y `GET /ventas/{id}`.

## Eliminar productos, laboratorios y proveedores
- Cada fila de **Productos, Laboratorios y Proveedores** tiene **Desactivar** (baja lógica, como siempre) y
  **Eliminar** (definitivo: `DELETE /{recurso}/{id}/definitivo`). Usuarios solo se desactivan.
- Si el registro ya tiene historial, el servidor responde 409 con el motivo (por ejemplo, cuántos lotes y
  cuánto stock quedan) y se muestra arriba de la tabla; el registro no se toca y se puede desactivar.
- En los CRUD genéricos se activa con `hardDelete: true` en su configuración (`pages/cruds.jsx`).

