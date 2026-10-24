import { fmtDateTime, money } from './api';

// Datos que salen en el encabezado del comprobante impreso. Completa los que quieras mostrar;
// los vacíos no se imprimen.
const NEGOCIO = { nombre: 'Botica', ruc: '', direccion: '', telefono: '' };

// Todo texto que viene de la base se escapa: un nombre con "<" o "&" no debe romper el HTML.
const esc = (s) => String(s ?? '').replace(/[&<>"']/g, (c) => (
  { '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));

function htmlComprobante(v) {
  const filas = (v.detalles || []).map((d) => `
    <tr>
      <td>${esc(d.productoNombre)}<small>Lote ${esc(d.numeroLote)}${d.requiereReceta ? ' · con receta' : ''}</small></td>
      <td class="n">${esc(d.cantidad)}</td>
      <td class="n">${money(d.precioVenta)}</td>
      <td class="n">${money(d.cantidad * d.precioVenta)}</td>
    </tr>`).join('');

  const receta = v.recetaPresentada
    ? `<p>Receta: ${esc([v.recetaNumero || 'presentada', v.recetaMedico].filter(Boolean).join(' · '))}</p>` : '';
  const linea = (txt) => (txt ? `<div>${esc(txt)}</div>` : '');

  return `<!doctype html><html lang="es"><head><meta charset="utf-8"><title>Venta #${esc(v.id)}</title>
<style>
  @page { margin: 4mm; }
  * { box-sizing: border-box; }
  body { font: 12px/1.35 'Courier New', monospace; width: 72mm; margin: 0 auto; color: #000; }
  .c { text-align: center; } h1 { font-size: 15px; margin: 0 0 2px; } h2 { font-size: 13px; margin: 6px 0 0; }
  p { margin: 2px 0; } hr { border: 0; border-top: 1px dashed #000; margin: 6px 0; }
  table { width: 100%; border-collapse: collapse; } th { text-align: left; border-bottom: 1px dashed #000; }
  th.n, td.n { text-align: right; white-space: nowrap; padding-left: 8px; } td { padding: 2px 0; vertical-align: top; }
  small { display: block; font-size: 10px; }
  .total { display: flex; justify-content: space-between; font-size: 14px; font-weight: bold; }
</style></head><body>
  <div class="c">
    <h1>${esc(NEGOCIO.nombre)}</h1>
    ${linea(NEGOCIO.ruc && 'RUC ' + NEGOCIO.ruc)}${linea(NEGOCIO.direccion)}${linea(NEGOCIO.telefono && 'Tel. ' + NEGOCIO.telefono)}
    <h2>${esc(v.tipoComprobante || 'COMPROBANTE')}</h2>
    <div>${esc(v.numeroComprobante || 'Venta #' + v.id)}</div>
  </div>
  <hr>
  <p>Fecha: ${esc(fmtDateTime(v.fechaVenta))}</p>
  <p>Cliente: ${esc(v.clienteNombre || 'Mostrador')}</p>
  <p>Atendió: ${esc(v.empleadoNombre)}</p>
  <p>Pago: ${esc(v.metodoPago || '—')}</p>
  ${receta}
  <hr>
  <table>
    <thead><tr><th>Producto</th><th class="n">Cant</th><th class="n">P.U.</th><th class="n">Importe</th></tr></thead>
    <tbody>${filas}</tbody>
  </table>
  <hr>
  <div class="total"><span>TOTAL</span><span>${money(v.total)}</span></div>
  <hr>
  <p class="c">¡Gracias por su compra!</p>
</body></html>`;
}

// Imprime con un iframe oculto en vez de window.open: no lo bloquea el bloqueador de ventanas
// emergentes (aunque antes se haya hecho un fetch) y no toca la pantalla actual.
export function imprimirVenta(venta) {
  document.getElementById('iframe-impresion')?.remove();
  const iframe = document.createElement('iframe');
  iframe.id = 'iframe-impresion';
  iframe.setAttribute('aria-hidden', 'true');
  iframe.style.cssText = 'position:fixed;right:0;bottom:0;width:0;height:0;border:0;';
  document.body.appendChild(iframe);
  const doc = iframe.contentWindow.document;
  doc.open();
  doc.write(htmlComprobante(venta));
  doc.close();
  setTimeout(() => { iframe.contentWindow.focus(); iframe.contentWindow.print(); }, 150);
}

export { htmlComprobante }; // expuesto para poder probarlo
