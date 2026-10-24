# Manual de Usuario — Sistema de Gestión Integral Botica Q&M

**Curso:** Desarrollo Web Integrado · **Sección:** 38447
**Profesor:** Ramires Pacheco, Luis Enrrique
**Proyecto:** Sistema de Gestión Integral para Botica Q&M

Este manual está dirigido al personal de la botica (vendedores y administrador). Explica paso
a paso el uso de cada módulo del sistema.

---

## Índice

1. [Ingreso al sistema](#1-ingreso-al-sistema)
2. [Conociendo la pantalla](#2-conociendo-la-pantalla)
3. [Registrar una venta (POS)](#3-registrar-una-venta-pos)
4. [Historial de ventas: buscar, editar y anular](#4-historial-de-ventas)
5. [Registrar una compra](#5-registrar-una-compra)
6. [Compras: editar y anular](#6-compras-editar-y-anular)
7. [Productos](#7-productos)
8. [Lotes e inventario](#8-lotes-e-inventario)
9. [Laboratorios y proveedores](#9-laboratorios-y-proveedores)
10. [Usuarios (solo administrador)](#10-usuarios-solo-administrador)
11. [Dashboard de Gestión](#11-dashboard-de-gestión)
12. [Asistente de ayuda (chatbot / FAQ)](#12-asistente-de-ayuda-chatbot--faq)
13. [Preguntas frecuentes](#13-preguntas-frecuentes)

---

## 1. Ingreso al sistema

1. Abre el navegador en la dirección del sistema (por ejemplo `http://localhost:5173`).
2. Escribe tu **correo** y **contraseña** y pulsa **Iniciar sesión**.
3. Si los datos son correctos, entras al **Dashboard**. Si fallan, verás un mensaje de error.

> El acceso está protegido por rol. Un **EMPLEADO** puede vender, comprar y consultar catálogos
> y reportes. El **ADMINISTRADOR** además gestiona usuarios y da de alta/edita catálogos.

Para salir, pulsa **Cerrar sesión** al pie del menú lateral.

---

## 2. Conociendo la pantalla

- **Menú lateral (izquierda):** accesos a Inicio (Dashboard), Nueva venta, Historial de ventas,
  Compras, Productos, Lotes, Laboratorios, Proveedores, Usuarios y Ayuda.
- **Barra superior:** muestra tu nombre y rol. En tablet y móvil aparece el botón **☰** para
  abrir/cerrar el menú.
- **Botón flotante 💬 (abajo a la derecha):** abre el asistente de ayuda en cualquier pantalla.

En pantallas pequeñas (celular/tablet) el menú se oculta y se abre con el botón ☰; el contenido
se adapta automáticamente.

---

## 3. Registrar una venta (POS)

1. Entra a **Nueva venta**.
2. En el buscador escribe parte del nombre del producto y pulsa **Agregar** en la fila.
   - Se abre el producto en el **carrito**. El sistema preselecciona el lote con la fecha de
     vencimiento más próxima (política **FEFO**: *First-Expired, First-Out*).
3. En el carrito:
   - Elige el **lote** si necesitas vender otro (el desplegable muestra lote, vencimiento y stock).
   - Ajusta la **cantidad** (no puede superar el stock del lote).
4. Si algún producto **requiere receta**, aparece un aviso rojo. Debes marcar
   *“El cliente presenta la receta”* para poder confirmar. Puedes anotar el número de receta y
   el médico (opcional).
5. (Opcional) Escribe el **DNI del cliente**:
   - Si coincide con un cliente registrado, se asocia su nombre.
   - Si no coincide o lo dejas vacío, la venta se registra como **ANONIMO101**.
6. Elige el **tipo de comprobante** (BOLETA/FACTURA) y el **método de pago**
   (EFECTIVO, YAPE, PLIN, TARJETA).
7. Pulsa **Confirmar venta**. Aparece el resumen de la venta.
8. En el resumen puedes pulsar **Imprimir** para generar el comprobante, o **Cerrar**.

> El **stock** se descuenta automáticamente del lote elegido al confirmar la venta.

---

## 4. Historial de ventas

Entra a **Historial de ventas**.

### Buscar y filtrar
- Escribe en la **barra de búsqueda** (cliente, vendedor o N.º de comprobante). Los resultados
  se actualizan solos tras una pausa breve.
- Usa los filtros **Desde** y **Hasta** para acotar por rango de fechas.

### Editar una venta
1. Pulsa **Editar** en la fila.
2. Puedes cambiar: **cliente, comprobante, método de pago y datos de receta**.
3. Pulsa **Guardar**.

> No se pueden cambiar los importes ni las líneas de una venta ya registrada, porque el stock
> ya fue descontado.

### Anular una venta
1. Pulsa **Anular** en la fila.
2. Escribe el **motivo** (obligatorio) y confirma.
3. La venta queda marcada como **Anulada** (fila resaltada) y el **stock vuelve** a sus lotes.

> La anulación no borra el registro: se conserva para el historial contable.

---

## 5. Registrar una compra

1. Entra a **Compras** y pulsa **+ Nuevo**.
2. Elige el **proveedor**.
3. Agrega cada producto comprado con:
   - **Producto** y **laboratorio** (fabricante del lote).
   - **Número de lote** y **fecha de vencimiento**.
   - **Cantidad** = número de **cajas** compradas.
   - **Unidades por caja** = cuántas unidades trae cada caja *en esta compra*.
   - **Precio de compra** por caja.
4. Pulsa **Guardar**.

> El stock que ingresa = *cantidad × unidades por caja* (en unidades). Si el lote ya existía, se
> **suma** al stock; si no, se **crea**.

---

## 6. Compras: editar y anular

- **Editar:** botón **Editar** en la fila → cambia proveedor, comprobante y N.º (no toca líneas
  ni stock) → **Guardar**.
- **Anular:** botón **Anular** → escribe el **motivo** → confirma. Se **descuenta** el stock que
  había ingresado. Si esas unidades ya se vendieron, el sistema **no permitirá anular** e
  indicará *stock insuficiente para revertir*.

---

## 7. Productos

Entra a **Productos**.

- **Crear:** pulsa **+ Nuevo** y completa **nombre, categoría, precio de venta y unidad de venta**.
  Marca **requiere receta** si corresponde. Puedes añadir **atributos** propios de la categoría
  (por ejemplo concentración o talla) y asociar **laboratorios**.
- **Editar:** botón **Editar** en la fila.
- **Duplicar:** botón **Duplicar** crea una copia del producto como plantilla (útil para
  variantes que cambian solo algún dato).
- **Desactivar:** botón **Eliminar** desactiva el producto (baja lógica). Conserva su historial.
- **Eliminar definitivo:** solo si no tiene historial; si lo tiene, el sistema lo impide y sugiere
  desactivarlo.

---

## 8. Lotes e inventario

Entra a **Lotes** para ver, por producto: **N.º de lote, laboratorio, fecha de vencimiento y
stock actual** (en unidades).

- Puedes **editar** un lote para corregir su laboratorio o vencimiento.
- El **stock** solo cambia por ventas, compras y sus anulaciones.
- Las alertas de vencimiento aparecen en el **Dashboard** (lotes por vencer / vencidos).

---

## 9. Laboratorios y proveedores

- **Laboratorios:** fabricantes de los productos (nombre, país, RUC, contacto…).
- **Proveedores:** a quién se le compra (nombre, RUC, contacto…).

En ambas pantallas puedes **crear**, **editar**, **desactivar** y (si no tienen historial)
**eliminar definitivamente**.

---

## 10. Usuarios (solo administrador)

Entra a **Usuarios** (visible solo para ADMINISTRADOR).

- **Crear:** **+ Nuevo** → nombre, DNI/teléfono (opcional), **correo**, **contraseña** y **rol**
  (ADMINISTRADOR o EMPLEADO). La contraseña se guarda cifrada.
- **Editar:** cambia datos y, si quieres, la contraseña (déjala vacía para conservarla).
- **Desactivar:** impide el ingreso sin borrar el historial.

---

## 11. Dashboard de Gestión

Pantalla inicial con los indicadores del negocio:

- **Ventas de hoy**, **ventas del mes**, **cantidad de ventas** y **ticket promedio**.
- **Compras del mes**, **productos activos** y alertas de **lotes con stock / por vencer / vencidos**.
- **Gráficos:** ventas por día (últimos 30 días), ventas por método de pago y **top 5 productos**.
- **Tabla de alertas** de lotes por vencer o vencidos, para priorizar su venta.

---

## 12. Asistente de ayuda (chatbot / FAQ)

Pulsa el botón flotante **💬** (o entra a **Ayuda** en el menú). El asistente permite:

- Ver **preguntas frecuentes** destacadas.
- Navegar por **temas** (Ventas, Inventario y lotes, Compras, Productos, Usuarios y acceso, Reportes).
- Usar la **barra de búsqueda** para encontrar ayuda por palabra clave (por ejemplo
  `venta`, `stock`, `receta`, `anular`).
- Leer la **respuesta automática** de cada pregunta.

Desde el widget puedes pulsar **Ver todo** para abrir el centro de ayuda a pantalla completa.

---

## 13. Preguntas frecuentes

**¿Qué pasa si un producto no tiene stock?**
No podrás agregarlo al carrito; verás *“sin stock disponible”*. Registra una compra para
ingresar mercadería.

**¿Puedo vender sin registrar al cliente?**
Sí. Sin DNI (o con un DNI no registrado) la venta se emite como **ANONIMO101**.

**¿Se puede deshacer una anulación?**
No. Si te equivocaste, vuelve a registrar la operación.

**¿Por qué no me permite anular una compra?**
Porque parte de esas unidades ya se vendió (no hay stock para revertir). Debes anular primero
las ventas asociadas.

**¿Cómo cambio mi contraseña?**
Un ADMINISTRADOR debe editarla desde la pantalla **Usuarios**.

**Me saca al login de repente.**
La sesión caduca por seguridad; vuelve a iniciar sesión.
