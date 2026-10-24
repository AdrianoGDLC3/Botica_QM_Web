import { useEffect } from 'react';
import { fmtDateTime, money } from '../api';

export default function ResumenVenta({ venta, detalle, error, onClose, onPrint }) {
  useEffect(() => {
    const esc = (e) => e.key === 'Escape' && onClose();
    window.addEventListener('keydown', esc);
    return () => window.removeEventListener('keydown', esc);
  }, [onClose]);

  const lineas = detalle?.detalles;

  return (
    <div className="overlay" onClick={onClose}>
      <div className="modal venta-resumen" role="dialog" aria-modal="true" onClick={(e) => e.stopPropagation()}>
        <h3>Venta #{venta.id} — {[venta.tipoComprobante, venta.numeroComprobante].filter(Boolean).join(' ')}</h3>

        <dl>
          <dt>Fecha</dt><dd>{fmtDateTime(venta.fechaVenta)}</dd>
          <dt>Cliente</dt><dd>{venta.clienteNombre || 'Mostrador'}</dd>
          <dt>Empleado</dt><dd>{venta.empleadoNombre}</dd>
          <dt>Pago</dt><dd>{venta.metodoPago || '—'}</dd>
          {venta.recetaPresentada && (
            <><dt>Receta</dt><dd>{[venta.recetaNumero || 'Presentada', venta.recetaMedico].filter(Boolean).join(' · ')}</dd></>
          )}
        </dl>

        {error && <p className="error">{error}</p>}
        {!lineas && !error && <p className="hint">Cargando detalle…</p>}
        {lineas && (
          <div className="table-wrap">
          <table className="stack-sm">
            <thead><tr><th>Producto</th><th>Lote</th><th>Cant.</th><th>Precio</th><th>Subtotal</th></tr></thead>
            <tbody>
              {lineas.map((d, i) => (
                <tr key={i}>
                  <td data-label="Producto">{d.productoNombre}{d.requiereReceta && <small className="warn"> · con receta</small>}</td>
                  <td data-label="Lote">{d.numeroLote}</td><td data-label="Cant.">{d.cantidad}</td>
                  <td data-label="Precio">{money(d.precioVenta)}</td><td data-label="Subtotal">{money(d.cantidad * d.precioVenta)}</td>
                </tr>
              ))}
            </tbody>
          </table>
          </div>
        )}

        <h2 className="total">Total: {money(venta.total)}</h2>
        <div className="actions">
          <button className="ghost" onClick={onClose}>Cerrar</button>
          <button className="primary" disabled={!lineas} onClick={() => onPrint(detalle)}>🖨 Imprimir</button>
        </div>
      </div>
    </div>
  );
}
