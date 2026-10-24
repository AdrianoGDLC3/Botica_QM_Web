import { useEffect, useState } from 'react';
import { api, fmtDate, money } from '../api';

export default function NuevaVenta() {
  const [productos, setProductos] = useState([]);
  const [clientes, setClientes] = useState([]);
  const [q, setQ] = useState('');
  const [cart, setCart] = useState([]);
  const [dni, setDni] = useState('');
  const [metodoPago, setMetodoPago] = useState('EFECTIVO');
  const [tipoComprobante, setTipoComprobante] = useState('BOLETA');
  const [msg, setMsg] = useState(null);
  const [receta, setReceta] = useState({ presentada: false, numero: '', medico: '' });

  useEffect(() => {
    api('/productos').then((d) => setProductos(d.filter((p) => p.activo))).catch(() => {});
    api('/usuarios/clientes').then(setClientes).catch(() => {});
  }, []);

  const clienteEncontrado = dni.length === 8 ? clientes.find((c) => c.dni === dni) : null;
  const nombreCliente = dni.length < 8 ? '' : (clienteEncontrado ? clienteEncontrado.nombre : 'ANONIMO101');
  const clienteId = clienteEncontrado ? clienteEncontrado.id : null;

  const loteDe = (l) => l.lotes.find((x) => x.id === l.loteId);
  const setQty = (productoId, n) => setCart((c) => c.map((l) =>
    l.producto.id !== productoId ? l : { ...l, cantidad: Math.max(1, Math.min(n, Number(loteDe(l).stockActual))) }));
  const setLote = (productoId, id) => setCart((c) => c.map((l) =>
    l.producto.id !== productoId ? l : { ...l, loteId: Number(id), cantidad: 1 }));

  const add = async (producto) => {
    setMsg(null);
    const found = cart.find((l) => l.producto.id === producto.id);
    if (found) return setQty(producto.id, found.cantidad + 1);
    try {
      const lotes = await api(`/lotes/disponibles?productoId=${producto.id}`);
      if (!lotes.length) return setMsg({ ok: false, text: `${producto.nombre}: sin stock disponible` });
      setCart((c) => [...c, { producto, lotes, loteId: lotes[0].id, cantidad: 1 }]);
    } catch (e) { setMsg({ ok: false, text: e.message }); }
  };

  const conReceta = cart.filter((l) => l.producto.requiereReceta);
  const total = cart.reduce((s, l) => s + l.cantidad * l.producto.precioVenta, 0);

  const confirmar = async () => {
    try {
      const v = await api('/ventas', {
        method: 'POST',
        body: {
          clienteId,
          metodoPago, tipoComprobante,
          recetaPresentada: receta.presentada, recetaNumero: receta.numero, recetaMedico: receta.medico,
          items: cart.map((l) => ({ loteId: l.loteId, cantidad: l.cantidad })),
        },
      });
      setMsg({ ok: true, text: `Venta #${v.id} registrada por ${money(v.total)}` });
      setCart([]); setDni(''); setReceta({ presentada: false, numero: '', medico: '' });
    } catch (e) { setMsg({ ok: false, text: e.message }); }
  };

  const detalle = (p) => p.atributos?.concentracion || p.atributos?.talla || p.atributos?.tipo || '';
  const lista = productos.filter((p) => `${p.nombre} ${detalle(p)}`.toLowerCase().includes(q.toLowerCase())).slice(0, 30);

  return (
    <>
      <h1>Nueva venta</h1>
      {msg && <p className={msg.ok ? 'ok' : 'error'}>{msg.text}</p>}
      <div className="pos">
        <div className="card">
          <input placeholder="Buscar producto…" value={q} onChange={(e) => setQ(e.target.value)} />
          <div className="table-wrap pos-lista">
          <table>
            <thead><tr><th>Producto</th><th>Precio</th><th></th></tr></thead>
            <tbody>
              {lista.map((p) => (
                <tr key={p.id}>
                  <td>{p.nombre} {detalle(p)}</td>
                  <td>{money(p.precioVenta)} / {p.unidadVenta.toLowerCase()}</td>
                  <td><button className="ghost" onClick={() => add(p)}>Agregar</button></td>
                </tr>
              ))}
              {!lista.length && <tr><td colSpan="3">Sin resultados</td></tr>}
            </tbody>
          </table>
          </div>
        </div>

        <div className="card">
          <h3>Carrito</h3>
          <div className="carrito-items">
          {!cart.length && <p>Agrega productos para vender.</p>}
          {cart.map((l) => (
            <div className="line" key={l.producto.id}>
              <b>{l.producto.nombre} {detalle(l.producto)}{l.producto.requiereReceta && <span className="warn"> · Requiere receta</span>}</b>
              <select value={l.loteId} onChange={(e) => setLote(l.producto.id, e.target.value)}>
                {l.lotes.map((x) => (
                  <option key={x.id} value={x.id}>
                    {x.numeroLote} · vence {fmtDate(x.fechaVencimiento)} · stock {x.stockActual} {l.producto.unidadVenta.toLowerCase()}
                  </option>
                ))}
              </select>
              <div>
                <input type="number" min="1" max={Number(loteDe(l).stockActual)} value={l.cantidad}
                  onChange={(e) => setQty(l.producto.id, Number(e.target.value) || 1)} />
                <span> {l.producto.unidadVenta.toLowerCase()} × {money(l.producto.precioVenta)} = <b>{money(l.cantidad * l.producto.precioVenta)}</b></span>
                <button className="danger" onClick={() => setCart((c) => c.filter((x) => x.producto.id !== l.producto.id))}>✕</button>
              </div>
            </div>
          ))}
          </div>
          {conReceta.length > 0 && (
            <div className="receta">
              <b>Receta obligatoria</b>
              <small>Requieren receta: {conReceta.map((l) => l.producto.nombre).join(', ')}</small>
              <label className="chk">El cliente presenta la receta
                <input type="checkbox" checked={receta.presentada}
                  onChange={(e) => setReceta({ ...receta, presentada: e.target.checked })} />
              </label>
              <input placeholder="N.º de receta (opcional)" value={receta.numero}
                onChange={(e) => setReceta({ ...receta, numero: e.target.value })} />
              <input placeholder="Médico / CMP (opcional)" value={receta.medico}
                onChange={(e) => setReceta({ ...receta, medico: e.target.value })} />
            </div>
          )}
          <label>DNI del cliente (opcional)
            <input inputMode="numeric" maxLength={8} placeholder="8 dígitos" value={dni}
              onChange={(e) => setDni(e.target.value.replace(/\D/g, '').slice(0, 8))} />
          </label>
          {nombreCliente && (
            <p className={clienteEncontrado ? 'ok cliente-info' : 'cliente-info'}>
              {clienteEncontrado ? `Cliente: ${nombreCliente}` : `Sin registrar — se vende como ${nombreCliente}`}
            </p>
          )}
          <label>Comprobante
            <select value={tipoComprobante} onChange={(e) => setTipoComprobante(e.target.value)}>
              <option>BOLETA</option><option>FACTURA</option>
            </select>
          </label>
          <label>Método de pago
            <select value={metodoPago} onChange={(e) => setMetodoPago(e.target.value)}>
              <option>EFECTIVO</option><option>YAPE</option><option>PLIN</option><option>TARJETA</option>
            </select>
          </label>
          <h2 className="total">Total: {money(total)}</h2>
          <button className="primary" disabled={!cart.length || (conReceta.length > 0 && !receta.presentada)} onClick={confirmar}>Confirmar venta</button>
        </div>
      </div>
    </>
  );
}
