import { useCallback, useEffect, useMemo, useState } from 'react';
import { api, daysTo, fmtDate, fmtDateTime, money } from '../api';
import './Dashboard.css';

const num = (v) => Number(v ?? 0);
const safeArray = (v) => Array.isArray(v) ? v : [];
const isoToday = () => {
  const d = new Date();
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
};
const isoMonthsAgo = (months) => {
  const d = new Date();
  d.setDate(1);
  d.setMonth(d.getMonth() - months);
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
};
const dateKey = (v) => v ? String(v).slice(0, 10) : '';
const monthStart = (key) => `${key.slice(0, 7)}-01`;
const monthLabel = (key) => {
  const [year, month] = key.split('-').map(Number);
  return new Date(year, month - 1, 1).toLocaleDateString('es-PE', { month: 'short' }).replace('.', '');
};
const monthsBetween = (from, to) => {
  if (!from || !to || from > to) return [];
  const [fy, fm] = from.slice(0, 7).split('-').map(Number);
  const [ty, tm] = to.slice(0, 7).split('-').map(Number);
  const count = Math.min(24, Math.max(1, (ty - fy) * 12 + tm - fm + 1));
  return Array.from({ length: count }, (_, i) => {
    const d = new Date(fy, fm - 1 + i, 1);
    return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-01`;
  });
};
const inRange = (value, from, to) => {
  const key = dateKey(value);
  return !!key && key >= from && key <= to;
};

function Icon({ name }) {
  const paths = {
    cart: <><path d="M3 3h2l2.2 11.2a2 2 0 0 0 2 1.6h8.9a2 2 0 0 0 1.9-1.4L22 8H7"/><circle cx="10" cy="20" r="1.3"/><circle cx="18" cy="20" r="1.3"/></>,
    receipt: <><path d="M6 3h12v18l-3-2-3 2-3-2-3 2z"/><path d="M9 8h6M9 12h6"/></>,
    box: <><path d="m12 3 9 5-9 5-9-5z"/><path d="M3 8v9l9 5 9-5V8M12 13v9"/></>,
    users: <><circle cx="9" cy="8" r="3"/><path d="M3 20v-2a6 6 0 0 1 12 0v2M16 5a3 3 0 0 1 0 6M18 14a5 5 0 0 1 3 4v2"/></>,
    alert: <><path d="m12 3 10 18H2z"/><path d="M12 9v5M12 17h.01"/></>,
    refresh: <><path d="M20 7v5h-5M4 17v-5h5"/><path d="M5.5 9A7 7 0 0 1 18 6l2 6M4 12l2 6a7 7 0 0 0 12.5-3"/></>,
    calendar: <><rect x="3" y="5" width="18" height="16" rx="2"/><path d="M16 3v4M8 3v4M3 10h18"/></>,
  };
  return <svg viewBox="0 0 24 24" aria-hidden="true" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round">{paths[name] || paths.box}</svg>;
}

function Metric({ icon, title, value, hint, tone }) {
  return <article className="dash-metric"><div className={`dash-metric-icon ${tone}`}><Icon name={icon}/></div><div className="dash-metric-copy"><span>{title}</span><strong>{value}</strong><small>{hint}</small></div></article>;
}

function SalesChart({ sales, from, to }) {
  const points = useMemo(() => {
    const months = monthsBetween(from, to);
    return months.map((month) => ({
      key: month.slice(0, 7), label: monthLabel(month),
      value: sales.filter((s) => dateKey(s.fechaVenta)?.startsWith(month.slice(0, 7))).reduce((sum, s) => sum + num(s.total), 0),
    }));
  }, [sales, from, to]);
  const max = Math.max(1, ...points.map((p) => p.value));
  const width = 520, left = 44, right = 12, top = 28, bottom = 38;
  const plotWidth = width - left - right;
  const plotHeight = 122;
  const x = (i) => points.length <= 1 ? left + plotWidth / 2 : left + i * plotWidth / (points.length - 1);
  const y = (v) => top + plotHeight - (v / max) * plotHeight;
  const coords = points.map((p, i) => `${x(i)},${y(p.value)}`).join(' ');
  const area = points.length ? `${x(0)},${top + plotHeight} ${coords} ${x(points.length - 1)},${top + plotHeight}` : '';
  const total = points.reduce((sum, p) => sum + p.value, 0);
  return <div className="dash-chart-wrap"><svg className="dash-line-chart" viewBox="0 0 520 190" role="img" aria-label="Ventas por mes dentro del período seleccionado">
    {[0, 1, 2, 3].map((i) => { const value = max * (3 - i) / 3; const yy = top + i * plotHeight / 3; return <g key={i}><line x1={left} y1={yy} x2={width - right} y2={yy} className="dash-gridline"/><text x={left - 8} y={yy + 4} textAnchor="end" className="dash-axis-label">{Math.round(value).toLocaleString('es-PE')}</text></g>; })}
    {points.length > 0 && <><polygon points={area} className="dash-chart-area"/><polyline points={coords} className="dash-chart-line"/>{points.map((p, i) => <g key={p.key}><circle cx={x(i)} cy={y(p.value)} r="4" className="dash-chart-dot"/><text x={x(i)} y="174" textAnchor="middle" className="dash-axis-label">{p.label}</text></g>)}</>}
  </svg><div className="dash-chart-foot"><span><i/> Ventas registradas en el período</span><strong>{money(total)}</strong></div></div>;
}

export default function Dashboard() {
  const [lotes, setLotes] = useState([]);
  const [productos, setProductos] = useState([]);
  const [ventas, setVentas] = useState([]);
  const [compras, setCompras] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [updatedAt, setUpdatedAt] = useState(new Date());
  const [desde, setDesde] = useState(isoMonthsAgo(7));
  const [hasta, setHasta] = useState(isoToday());
  const [rangoAplicado, setRangoAplicado] = useState({ desde: isoMonthsAgo(7), hasta: isoToday() });
  const [rangoError, setRangoError] = useState('');

  const cargar = useCallback(async () => {
    setLoading(true);
    setError('');
    const requests = [api('/lotes'), api('/productos'), api('/ventas'), api('/compras')];
    const results = await Promise.allSettled(requests);
    const setters = [setLotes, setProductos, setVentas, setCompras];
    const labels = ['lotes', 'productos', 'ventas', 'compras'];
    const failed = [];
    results.forEach((r, i) => {
      if (r.status === 'fulfilled') setters[i](safeArray(r.value));
      else failed.push(labels[i]);
    });
    if (failed.length === labels.length) setError('No se pudo cargar ningún dato. Comprueba que el backend esté iniciado y que la sesión siga activa.');
    else if (failed.length) setError(`No se pudieron cargar: ${failed.join(', ')}. El resto de indicadores sí está disponible.`);
    setUpdatedAt(new Date());
    setLoading(false);
  }, []);
  useEffect(() => { cargar(); }, [cargar]);

  const aplicarRango = () => {
    if (!desde || !hasta) { setRangoError('Selecciona una fecha inicial y una fecha final.'); return; }
    if (desde > hasta) { setRangoError('La fecha inicial no puede ser posterior a la fecha final.'); return; }
    setRangoError('');
    setRangoAplicado({ desde, hasta });
  };
  const hoy = isoToday();
  const ventasPeriodo = useMemo(() => ventas.filter((v) => inRange(v.fechaVenta, rangoAplicado.desde, rangoAplicado.hasta)), [ventas, rangoAplicado]);
  const comprasPeriodo = useMemo(() => compras.filter((c) => inRange(c.fechaCompra, rangoAplicado.desde, rangoAplicado.hasta)), [compras, rangoAplicado]);
  const ventasHoy = ventas.filter((v) => dateKey(v.fechaVenta) === hoy);
  const totalPeriodo = ventasPeriodo.reduce((s, v) => s + num(v.total), 0);
  const unidadesPeriodo = ventasPeriodo.reduce((s, v) => s + safeArray(v.detalles).reduce((a, d) => a + num(d.cantidad), 0), 0);
  const clientesPeriodo = new Set(ventasPeriodo.map((v) => v.clienteNombre).filter(Boolean)).size;
  const productosActivos = productos.filter((p) => p.activo === true).length;
  const conStock = lotes.filter((l) => num(l.stockActual) > 0);
  const vencidos = conStock.filter((l) => daysTo(l.fechaVencimiento) < 0);
  const porVencer = conStock.filter((l) => { const d = daysTo(l.fechaVencimiento); return d >= 0 && d <= 60; });
  const stockCritico = conStock.filter((l) => num(l.stockActual) <= 10).sort((a, b) => num(a.stockActual) - num(b.stockActual)).slice(0, 5);
  const topProductos = useMemo(() => {
    const map = new Map();
    ventasPeriodo.forEach((v) => safeArray(v.detalles).forEach((d) => {
      const key = d.productoNombre || 'Producto';
      const old = map.get(key) || { nombre: key, cantidad: 0, total: 0 };
      old.cantidad += num(d.cantidad);
      old.total += num(d.cantidad) * num(d.precioVenta);
      map.set(key, old);
    }));
    return [...map.values()].sort((a, b) => b.cantidad - a.cantidad).slice(0, 5);
  }, [ventasPeriodo]);
  const ultimasVentas = [...ventasPeriodo].sort((a, b) => String(b.fechaVenta || '').localeCompare(String(a.fechaVenta || ''))).slice(0, 5);
  const ultimasCompras = [...comprasPeriodo].sort((a, b) => String(b.fechaCompra || '').localeCompare(String(a.fechaCompra || ''))).slice(0, 4);
  const moneda = (n) => money(n);
  const rangoTexto = `${fmtDate(rangoAplicado.desde)} al ${fmtDate(rangoAplicado.hasta)}`;

  return <section className="dashboard-page">
    <header className="dash-heading"><div><div className="dash-eyebrow">BOTICA Q&M · PANEL ADMINISTRATIVO</div><h1>Dashboard de Gestión</h1><p>Resumen operativo y comercial de tu botica.</p></div><div className="dash-heading-actions"><button className="dash-refresh" onClick={cargar} disabled={loading}><Icon name="refresh"/>{loading ? 'Actualizando…' : 'Actualizar datos'}</button></div></header>
    <section className="dash-date-filter" aria-label="Filtro de fechas del dashboard">
      <div className="dash-date-filter-title"><Icon name="calendar"/><div><strong>Filtrar por período</strong><small>Aplica el rango a ventas, compras, gráfico y productos más vendidos.</small></div></div>
      <div className="dash-date-fields"><label>Desde<input type="date" value={desde} max={hasta || undefined} onChange={(e) => setDesde(e.target.value)} /></label><label>Hasta<input type="date" value={hasta} min={desde || undefined} max={hoy} onChange={(e) => setHasta(e.target.value)} /></label><button className="dash-refresh" onClick={aplicarRango}>Aplicar fechas</button><button className="dash-reset" onClick={() => { const f = isoMonthsAgo(7), t = isoToday(); setDesde(f); setHasta(t); setRangoAplicado({ desde: f, hasta: t }); setRangoError(''); }}>Últimos 8 meses</button></div>
      {rangoError && <p className="dash-range-error" role="alert">{rangoError}</p>}
      <p className="dash-range-applied">Período aplicado: <strong>{rangoTexto}</strong></p>
    </section>
    {error && <div className="dash-error" role="alert">{error}</div>}
    <div className="dash-metrics">
      <Metric icon="cart" title="Ventas del período" value={moneda(totalPeriodo)} hint={`${ventasPeriodo.length} comprobantes · ${unidadesPeriodo} unidades`} tone="green"/>
      <Metric icon="receipt" title="Ventas de hoy" value={moneda(ventasHoy.reduce((s, v) => s + num(v.total), 0))} hint={`${ventasHoy.length} comprobantes registrados hoy`} tone="blue"/>
      <Metric icon="box" title="Productos activos" value={String(productosActivos)} hint={`${conStock.length} lotes con stock`} tone="orange"/>
      <Metric icon="users" title="Clientes atendidos" value={String(clientesPeriodo)} hint="Clientes distintos del período" tone="purple"/>
    </div>

    <div className="dash-grid dash-grid-main">
      <section className="dash-panel dash-sales-panel"><div className="dash-panel-heading"><div><h2>Ventas por mes</h2><p>{rangoTexto} · soles (S/)</p></div><span className="dash-tag">Datos del sistema</span></div><SalesChart sales={ventasPeriodo} from={rangoAplicado.desde} to={rangoAplicado.hasta}/></section>
      <section className="dash-panel"><div className="dash-panel-heading"><div><h2>Estado del inventario</h2><p>Seguimiento de lotes y vencimientos (independiente del período)</p></div></div><div className="dash-inventory-summary"><div className="dash-ring"><div><strong>{conStock.length}</strong><span>lotes con stock</span></div></div><div className="dash-legend"><div><i className="green-dot"/><span>Productos activos</span><b>{productosActivos}</b></div><div><i className="orange-dot"/><span>Por vencer (60 días)</span><b>{porVencer.length}</b></div><div><i className="red-dot"/><span>Vencidos con stock</span><b>{vencidos.length}</b></div></div></div><div className="dash-alert-note"><Icon name="alert"/><span>{vencidos.length ? `${vencidos.length} lote(s) vencidos requieren atención inmediata.` : porVencer.length ? `${porVencer.length} lote(s) vencerán dentro de 60 días.` : 'No hay lotes vencidos o próximos a vencer.'}</span></div></section>
    </div>

    <div className="dash-grid dash-grid-lower">
      <section className="dash-panel"><div className="dash-panel-heading"><div><h2>Productos más vendidos</h2><p>Según ventas del período seleccionado</p></div></div><div className="dash-table-wrap"><table className="dash-table"><thead><tr><th>Producto</th><th>Unidades</th><th>Total estimado</th></tr></thead><tbody>{topProductos.map((p, i) => <tr key={p.nombre}><td><span className="dash-rank">{i + 1}</span>{p.nombre}</td><td>{p.cantidad}</td><td>{moneda(p.total)}</td></tr>)}{!topProductos.length && <tr><td colSpan="3" className="dash-empty">No hay ventas en el período seleccionado.</td></tr>}</tbody></table></div></section>
      <section className="dash-panel"><div className="dash-panel-heading"><div><h2>Stock crítico</h2><p>Lotes con 10 unidades o menos</p></div><span className={`dash-counter ${stockCritico.length ? 'is-warning' : ''}`}>{stockCritico.length}</span></div><div className="dash-table-wrap"><table className="dash-table"><thead><tr><th>Producto / lote</th><th>Stock</th><th>Vencimiento</th></tr></thead><tbody>{stockCritico.map((l) => <tr key={l.id}><td><strong>{l.productoNombre || 'Producto'}</strong><small className="dash-subline">Lote {l.numeroLote || '—'}</small></td><td><span className="dash-stock-low">{num(l.stockActual)}</span></td><td>{fmtDate(l.fechaVencimiento)}</td></tr>)}{!stockCritico.length && <tr><td colSpan="3" className="dash-empty">No se detecta stock crítico.</td></tr>}</tbody></table></div></section>
    </div>

    <div className="dash-grid dash-grid-bottom">
      <section className="dash-panel"><div className="dash-panel-heading"><div><h2>Últimas ventas del período</h2><p>Movimientos dentro del rango seleccionado</p></div><a href="/historial-ventas">Ver historial →</a></div><div className="dash-table-wrap"><table className="dash-table"><thead><tr><th>Fecha</th><th>Cliente</th><th>Total</th></tr></thead><tbody>{ultimasVentas.map((v) => <tr key={v.id}><td>{fmtDateTime(v.fechaVenta)}</td><td>{v.clienteNombre || 'Cliente general'}</td><td><strong>{moneda(v.total)}</strong></td></tr>)}{!ultimasVentas.length && <tr><td colSpan="3" className="dash-empty">No hay ventas en el período seleccionado.</td></tr>}</tbody></table></div></section>
      <section className="dash-panel"><div className="dash-panel-heading"><div><h2>Últimas compras del período</h2><p>Recepción de mercadería dentro del rango</p></div><a href="/compras">Ver compras →</a></div><div className="dash-table-wrap"><table className="dash-table"><thead><tr><th>Fecha</th><th>Proveedor</th><th>Total</th></tr></thead><tbody>{ultimasCompras.map((c) => <tr key={c.id}><td>{fmtDateTime(c.fechaCompra)}</td><td>{c.proveedorNombre || 'Proveedor'}</td><td><strong>{moneda(c.total)}</strong></td></tr>)}{!ultimasCompras.length && <tr><td colSpan="3" className="dash-empty">No hay compras en el período seleccionado.</td></tr>}</tbody></table></div></section>
    </div>
    <footer className="dash-footer"><span>{loading ? 'Cargando información…' : `Información actualizada ${updatedAt.toLocaleTimeString('es-PE', { hour: '2-digit', minute: '2-digit' })}`}</span><span>El rango filtra datos comerciales; los indicadores de inventario muestran el estado actual.</span></footer>
  </section>;
}
