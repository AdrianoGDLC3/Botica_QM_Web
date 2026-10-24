import { useEffect, useId, useMemo, useRef, useState } from 'react';
import { api, money } from '../api';
import Historial, { comprasCols } from './Historial';

const vacio = { productoId: '', productoTexto: '', laboratorioId: '', numeroLote: '', fechaVencimiento: '', cantidad: 1, unidadesPorCaja: '', precioCompra: '' };


const MAX_SUGERENCIAS = 8;
const MIN_LARGO_CONTIENE = 2; 

const etiquetaProducto = (p) => `${p.nombre} ${p.atributos?.concentracion || p.atributos?.talla || ''}`.trim();


const normalizar = (texto) => String(texto ?? '')
  .normalize('NFD').replace(/[\u0300-\u036f]/g, '')
  .toLowerCase()
  .replace(/(\d),(\d)/g, '$1.$2');
const palabrasDe = (texto) => normalizar(texto).match(/[\p{L}\p{N}]+(?:\.\p{N}+)?/gu) ?? [];

function crearIndiceProductos(productos) {
  return productos.map((producto) => ({
    producto,
    campos: [
      { palabras: palabrasDe(producto.nombre), peso: 5 },
      { palabras: palabrasDe(Object.values(producto.atributos || {}).join(' ')), peso: 3 }, // concentración, talla…
      { palabras: palabrasDe(producto.laboratorios.map((l) => l.nombre).join(' ')), peso: 2 },
      { palabras: palabrasDe(producto.categoria), peso: 1 },
    ],
  }));
}

function puntajeTermino(termino, palabra) {
  if (palabra === termino) return 3;
  if (palabra.startsWith(termino)) return 2;
  if (termino.length >= MIN_LARGO_CONTIENE && palabra.includes(termino)) return 1;
  return 0;
}

function buscarSugerencias(indice, texto) {
  const terminos = [...new Set(palabrasDe(texto))];
  if (!terminos.length) {
    return { lista: indice.slice(0, MAX_SUGERENCIAS).map((e) => e.producto), total: indice.length };
  }
  const coinciden = [];
  indice.forEach((entrada, posicion) => {
    let total = 0;
    for (const termino of terminos) {
      let mejor = 0;
      for (const campo of entrada.campos) {
        for (const palabra of campo.palabras) mejor = Math.max(mejor, puntajeTermino(termino, palabra) * campo.peso);
      }
      if (mejor === 0) return; 
      total += mejor;
    }
    coinciden.push({ producto: entrada.producto, total, posicion });
  });
  coinciden.sort((a, b) => b.total - a.total || a.posicion - b.posicion);
  return { lista: coinciden.slice(0, MAX_SUGERENCIAS).map((c) => c.producto), total: coinciden.length };
}

const estilosBuscador = {
  contenedor: { position: 'relative' },
  lista: {
    position: 'absolute', zIndex: 20, top: 'calc(100% + 2px)', left: 0, minWidth: '100%', width: 'max-content', maxWidth: 'min(380px, calc(100vw - 48px))',
    maxHeight: 300, overflowY: 'auto', margin: 0, padding: 4, listStyle: 'none', background: 'var(--card)',
    border: '1px solid #c9d2dc', borderRadius: 8, boxShadow: '0 8px 24px rgba(0,0,0,.12)',
  },
  opcion: (resaltada) => ({ padding: '6px 8px', borderRadius: 6, cursor: 'pointer', background: resaltada ? '#e6f3f1' : 'transparent' }),
  detalle: { display: 'block', fontSize: 12, color: 'var(--muted)' },
  aviso: { padding: '6px 8px', fontSize: 13, color: 'var(--muted)' },
};


function BuscadorProducto({ indice, texto, seleccionado, onTexto, onElegir }) {
  const idLista = useId();
  const entradaRef = useRef(null);
  const listaRef = useRef(null);
  const [abierto, setAbierto] = useState(false);
  const [resaltado, setResaltado] = useState(0);

  const mostrar = abierto && !seleccionado; 
  const { lista, total } = useMemo(() => buscarSugerencias(indice, texto), [indice, texto]);
  const activa = Math.min(resaltado, Math.max(lista.length - 1, 0));

   useEffect(() => {
    entradaRef.current?.setCustomValidity(texto && !seleccionado ? 'Elige un producto de la lista' : '');
  }, [texto, seleccionado]);

  useEffect(() => {
    if (mostrar) listaRef.current?.children[activa]?.scrollIntoView?.({ block: 'nearest' });
  }, [activa, mostrar]);

  const elegir = (producto) => { onElegir(producto); setAbierto(false); };

  const alTeclear = (e) => {
    if (e.key === 'ArrowDown') {
      e.preventDefault();
      if (!abierto) setAbierto(true); else setResaltado(Math.min(activa + 1, lista.length - 1));
    } else if (e.key === 'ArrowUp') {
      e.preventDefault();
      setResaltado(Math.max(activa - 1, 0));
    } else if (e.key === 'Enter' && mostrar) {
      e.preventDefault();
      if (lista[activa]) elegir(lista[activa]);
    } else if (e.key === 'Escape' && mostrar) {
      e.preventDefault();
      setAbierto(false);
    }
  };

  return (
    <div style={estilosBuscador.contenedor}>
      <input
        ref={entradaRef} required type="text" autoComplete="off" placeholder="Escribe para buscar…"
        role="combobox" aria-expanded={mostrar} aria-controls={idLista} aria-autocomplete="list"
        value={texto}
        onChange={(e) => { onTexto(e.target.value); setAbierto(true); setResaltado(0); }}
        onFocus={() => setAbierto(true)}
        onBlur={() => setAbierto(false)}
        onKeyDown={alTeclear}
      />
      {mostrar && (
        <ul id={idLista} role="listbox" ref={listaRef} style={estilosBuscador.lista}>
          {lista.map((p, i) => (
            <li key={p.id} role="option" aria-selected={i === activa} style={estilosBuscador.opcion(i === activa)}
              onMouseEnter={() => setResaltado(i)}
              onMouseDown={(e) => { e.preventDefault(); elegir(p); }}> {/* mousedown: elige antes de que el campo pierda el foco */}
              {etiquetaProducto(p)}
              <small style={estilosBuscador.detalle}>
                {p.unidadVenta.toLowerCase()} · {p.laboratorios.map((l) => l.nombre).join(', ') || 'sin laboratorio asociado'}
              </small>
            </li>
          ))}
          {!lista.length && <li role="presentation" style={estilosBuscador.aviso}>Sin coincidencias para "{texto.trim()}"</li>}
          {total > lista.length && (
            <li role="presentation" style={estilosBuscador.aviso}>Hay {total - lista.length} más: sigue escribiendo para afinar</li>
          )}
        </ul>
      )}
    </div>
  );
}


export default function Compras() {
  const [provs, setProvs] = useState([]);
  const [productos, setProductos] = useState([]);
  const [labsActivos, setLabsActivos] = useState(new Set());
  const [h, setH] = useState({ proveedorId: '', tipoComprobante: 'FACTURA', numeroComprobante: '' });
  const [items, setItems] = useState([{ ...vacio }]);
  const [msg, setMsg] = useState(null);
  const [reload, setReload] = useState(0);

  useEffect(() => {
    api('/proveedores').then((d) => setProvs(d.filter((p) => p.activo))).catch(() => {});
    api('/productos').then((d) => setProductos(d.filter((p) => p.activo))).catch(() => {});
    api('/laboratorios').then((d) => setLabsActivos(new Set(d.filter((l) => l.activo).map((l) => l.id)))).catch(() => {});
  }, []);

  const indiceProductos = useMemo(() => crearIndiceProductos(productos), [productos]);
  const productoDe = (id) => productos.find((p) => p.id === Number(id));
  const labsDe = (productoId) => { const p = productoDe(productoId); return p ? p.laboratorios.filter((l) => labsActivos.has(l.id)) : []; };

  const setItem = (i, k, v) => setItems((xs) => xs.map((x, j) => (j === i ? { ...x, [k]: v } : x)));
   const setProductoTexto = (i, texto) => setItems((xs) => xs.map((x, j) => (j === i ? { ...x, productoTexto: texto, productoId: '', laboratorioId: '' } : x)));
  const elegirProducto = (i, p) => setItems((xs) => xs.map((x, j) => (j === i ? { ...x, productoId: p.id, productoTexto: etiquetaProducto(p), laboratorioId: '' } : x)));
  const total = items.reduce((s, x) => s + Number(x.cantidad || 0) * Number(x.precioCompra || 0), 0);

  const guardar = async (e) => {
    e.preventDefault();
    if (items.some((x) => !x.productoId)) {
      setMsg({ ok: false, text: 'Elige un producto de la lista en todas las líneas.' });
      return;
    }
    try {
      const r = await api('/compras', {
        method: 'POST',
        body: {
          ...h, proveedorId: Number(h.proveedorId),
          items: items.map((x) => ({
            productoId: Number(x.productoId), laboratorioId: Number(x.laboratorioId),
            numeroLote: x.numeroLote, fechaVencimiento: x.fechaVencimiento,
            cantidad: Number(x.cantidad), unidadesPorCaja: Number(x.unidadesPorCaja), precioCompra: Number(x.precioCompra),
          })),
        },
      });
      setMsg({ ok: true, text: `Compra #${r.id} registrada por ${money(r.total)}` });
      setItems([{ ...vacio }]); setH({ ...h, numeroComprobante: '' }); setReload((n) => n + 1);
    } catch (err) { setMsg({ ok: false, text: err.message }); }
  };

  return (
    <>
      <h1>Compras</h1>
      {msg && <p className={msg.ok ? 'ok' : 'error'}>{msg.text}</p>}
      <form className="card" onSubmit={guardar}>
        <h3>Registrar compra</h3>
        <div className="fields">
          <label>Proveedor *
            <select required value={h.proveedorId} onChange={(e) => setH({ ...h, proveedorId: e.target.value })}>
              <option value="">— Seleccione —</option>
              {provs.map((p) => <option key={p.id} value={p.id}>{p.nombre}</option>)}
            </select>
          </label>
          <label>Tipo de comprobante
            <select value={h.tipoComprobante} onChange={(e) => setH({ ...h, tipoComprobante: e.target.value })}>
              <option>FACTURA</option><option>BOLETA</option><option>GUIA</option>
            </select>
          </label>
          <label>N.º de comprobante
            <input value={h.numeroComprobante} onChange={(e) => setH({ ...h, numeroComprobante: e.target.value })} />
          </label>
        </div>

        <div className="table-wrap sin-scroll">
        <table className="stack-lg">
          <thead>
            <tr>
              <th>Producto</th><th>Laboratorio</th><th>N.º lote</th><th>Vence</th>
              <th>Cajas</th><th>Unidades por caja</th><th>Precio por caja</th><th></th>
            </tr>
          </thead>
          <tbody>
            {items.map((x, i) => {
              const prod = productoDe(x.productoId);
              const opcionesLab = labsDe(x.productoId);
              return (
                <tr key={i}>
                  <td data-label="Producto">
                    <BuscadorProducto indice={indiceProductos} texto={x.productoTexto} seleccionado={x.productoId}
                      onTexto={(texto) => setProductoTexto(i, texto)} onElegir={(p) => elegirProducto(i, p)} />
                  </td>
                  <td data-label="Laboratorio">
                    <select required value={x.laboratorioId} disabled={!x.productoId}
                      onChange={(e) => setItem(i, 'laboratorioId', e.target.value)}>
                      <option value="">{x.productoId ? '—' : 'Elige producto'}</option>
                      {opcionesLab.map((l) => <option key={l.id} value={l.id}>{l.nombre}</option>)}
                    </select>
                    {x.productoId && !opcionesLab.length && (
                      <small className="error">Sin laboratorio fabricante asociado. Configúralo en Productos.</small>
                    )}
                  </td>
                  <td data-label="N.º lote"><input required value={x.numeroLote} onChange={(e) => setItem(i, 'numeroLote', e.target.value)} /></td>
                  <td data-label="Vence"><input required type="date" value={x.fechaVencimiento} onChange={(e) => setItem(i, 'fechaVencimiento', e.target.value)} /></td>
                  <td data-label="Cajas"><input required type="number" min="1" step="1" value={x.cantidad} onChange={(e) => setItem(i, 'cantidad', e.target.value)} /></td>
                  <td data-label="Unidades por caja">
                    <input required type="number" min="1" step="1" value={x.unidadesPorCaja} onChange={(e) => setItem(i, 'unidadesPorCaja', e.target.value)} />
                    {prod && Number(x.cantidad) > 0 && Number(x.unidadesPorCaja) > 0 && (
                      <small>= {(Number(x.cantidad) * Number(x.unidadesPorCaja)).toLocaleString()} {prod.unidadVenta.toLowerCase()} al stock</small>
                    )}
                  </td>
                  <td data-label="Precio por caja"><input required type="number" min="0" step="0.01" value={x.precioCompra} onChange={(e) => setItem(i, 'precioCompra', e.target.value)} /></td>
                  <td>{items.length > 1 && <button type="button" className="danger" onClick={() => setItems(items.filter((_, j) => j !== i))}>✕</button>}</td>
                </tr>
              );
            })}
          </tbody>
        </table>
        </div>
        <div className="actions">
          <button type="button" className="ghost" onClick={() => setItems([...items, { ...vacio }])}>+ Agregar línea</button>
          <b>Total: {money(total)}</b>
          <button className="primary">Registrar compra</button>
        </div>
      </form>

      <Historial title="Historial de compras" endpoint="/compras" columns={comprasCols} reloadKey={reload} />
    </>
  );
}
