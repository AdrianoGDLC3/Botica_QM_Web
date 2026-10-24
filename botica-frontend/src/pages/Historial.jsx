import { Fragment, useEffect, useState } from 'react';
import { api, fmtDateTime, money } from '../api';

const comprobante = (r) => `${r.tipoComprobante || ''} ${r.numeroComprobante || ''}`.trim();

export const ventasCols = [
  { key: 'id', label: '#' },
  { label: 'Fecha', render: (r) => fmtDateTime(r.fechaVenta) },
  { label: 'Cliente', render: (r) => r.clienteNombre || 'Mostrador' },
  { key: 'empleadoNombre', label: 'Empleado' },
  { label: 'Comprobante', render: comprobante },
  { key: 'metodoPago', label: 'Pago' },
  { label: 'Receta', render: (r) => (r.recetaPresentada ? [r.recetaNumero || 'Sí', r.recetaMedico].filter(Boolean).join(' · ') : '—') },
  { label: 'Total', render: (r) => money(r.total) },
];

export const comprasCols = [
  { key: 'id', label: '#' },
  { label: 'Fecha', render: (r) => fmtDateTime(r.fechaCompra) },
  { key: 'proveedorNombre', label: 'Proveedor' },
  { key: 'usuarioNombre', label: 'Registró' },
  { label: 'Comprobante', render: comprobante },
  { label: 'Total', render: (r) => money(r.total) },
];

export default function Historial({ title, endpoint, columns, reloadKey, onSelect, rowActions }) {
  const [rows, setRows] = useState([]);
  const [open, setOpen] = useState(null);
  const [detail, setDetail] = useState([]);

  useEffect(() => { api(endpoint).then(setRows).catch(() => {}); }, [endpoint, reloadKey]);

  const toggle = async (id) => {
    if (open === id) return setOpen(null);
    const d = await api(`${endpoint}/${id}`).catch(() => null);
    setDetail(d?.detalles || []);
    setOpen(id);
  };

  // Las compras traen laboratorioNombre por línea (de qué fabricante vino ese lote); las ventas no.
  const conLab = detail.some((d) => d.laboratorioNombre);
  // Solo las compras traen unidadesPorCaja: ahí la cantidad son cajas y el precio es por caja
  const conCaja = detail.some((d) => d.unidadesPorCaja);
  const nCols = columns.length + (rowActions ? 1 : 0);
  const lblCant = conCaja ? 'Cajas' : 'Cant.';
  const lblPrecio = conCaja ? 'Precio por caja' : 'Precio';

  return (
    <div className="card">
      <h3>{title}</h3>
      <div className="table-wrap">
      <table className="stack">
        <thead><tr>{columns.map((c) => <th key={c.label}>{c.label}</th>)}{rowActions && <th></th>}</tr></thead>
        <tbody>
          {rows.map((r) => (
            <Fragment key={r.id}>
              <tr className="clickable" onClick={() => (onSelect ? onSelect(r) : toggle(r.id))}>
                {columns.map((c) => <td key={c.label} data-label={c.label}>{c.render ? c.render(r) : r[c.key]}</td>)}
                {rowActions && <td className="row-actions" onClick={(e) => e.stopPropagation()}>{rowActions(r)}</td>}
              </tr>
              {!onSelect && open === r.id && (
                <tr className="fila-detalle">
                  <td colSpan={nCols}>
                    <div className="table-wrap">
                    <table className="inner stack">
                      <thead>
                        <tr>
                          <th>Producto</th>{conLab && <th>Laboratorio</th>}<th>Lote</th>
                          <th>{conCaja ? 'Cajas' : 'Cant.'}</th>{conCaja && <th>Uds. por caja</th>}<th>{conCaja ? 'Precio por caja' : 'Precio'}</th><th>Subtotal</th>
                        </tr>
                      </thead>
                      <tbody>
                        {detail.map((d, i) => {
                          const p = d.precioVenta ?? d.precioCompra;
                          return (
                            <tr key={i}>
                              <td data-label="Producto">{d.productoNombre}{d.requiereReceta && <small className="warn"> · con receta</small>}</td>
                              {conLab && <td data-label="Laboratorio">{d.laboratorioNombre}</td>}
                              <td data-label="Lote">{d.numeroLote}</td><td data-label={lblCant}>{d.cantidad}</td>{conCaja && <td data-label="Uds. por caja">{d.unidadesPorCaja}</td>}
                              <td data-label={lblPrecio}>{money(p)}</td><td data-label="Subtotal">{money(d.cantidad * p)}</td>
                            </tr>
                          );
                        })}
                      </tbody>
                    </table>
                    </div>
                  </td>
                </tr>
              )}
            </Fragment>
          ))}
          {!rows.length && <tr><td colSpan={nCols}>Sin registros</td></tr>}
        </tbody>
      </table>
      </div>
    </div>
  );
}
