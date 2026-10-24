
import { useEffect, useMemo, useState } from 'react';
import { api, money } from '../api';
import { useAuth } from '../auth';

const CATEGORIAS = [
  { value: 'MEDICAMENTO', label: 'Medicamento' },
  { value: 'SUPLEMENTO', label: 'Suplemento / vitamina' },
  { value: 'PANALES', label: 'Pañales' },
  { value: 'BIBERON', label: 'Biberón' },
  { value: 'HIGIENE', label: 'Higiene' },
  { value: 'CUIDADO_PERSONAL', label: 'Cuidado personal' },
  { value: 'CURACION', label: 'Curación' },
  { value: 'DISPOSITIVO_MEDICO', label: 'Dispositivo médico' },
  { value: 'ACCESORIO', label: 'Accesorio' },
  { value: 'OTRO', label: 'Otro' },
];
const etiquetaCategoria = (valor) => CATEGORIAS.find((c) => c.value === valor)?.label || valor;
const ES_MEDICO = (cat) => cat === 'MEDICAMENTO' || cat === 'SUPLEMENTO';

const FORMAS_FARMACEUTICAS = {
  'Sólidos': ['Tableta', 'Tableta recubierta', 'Tableta masticable', 'Tableta efervescente', 'Tableta sublingual',
    'Cápsula', 'Cápsula dura', 'Cápsula blanda', 'Gragea', 'Polvo', 'Granulado'],
  'Líquidos': ['Jarabe', 'Suspensión oral', 'Solución oral', 'Gotas', 'Emulsión oral', 'Elixir'],
  'Inyectables': ['Solución inyectable', 'Suspensión inyectable', 'Polvo para solución inyectable'],
  'Tópicos': ['Crema', 'Pomada', 'Ungüento', 'Gel', 'Loción', 'Spray', 'Aerosol'],
  'Otros': ['Óvulo', 'Supositorio', 'Solución oftálmica', 'Gotas óticas', 'Solución nasal'],
};
const TALLAS_PANAL = ['RN', 'RN+', 'S', 'M', 'L', 'XL', 'XXL'];
const ETAPAS_PANAL = ['Recién nacido', 'Bebé', 'Gateador', 'Caminador', 'Niño', 'Adulto'];
const TIPOS_HIGIENE = ['Jabón', 'Alcohol', 'Gel antibacterial', 'Shampoo', 'Desodorante', 'Pasta dental', 'Enjuague bucal', 'Toallas húmedas'];
const TIPOS_CURACION = ['Algodón', 'Gasa', 'Venda', 'Esparadrapo', 'Curitas', 'Guantes'];

const UNIDADES_VENTA = ['UNIDAD', 'TABLETA', 'CAPSULA', 'AMPOLLA', 'VIAL', 'SOBRE', 'FRASCO', 'PAQUETE', 'ML', 'L', 'GRAMO', 'KG'];


const ATRIBUTOS_POR_CATEGORIA = {
  MEDICAMENTO: { titulo: 'Información del medicamento', campos: [
    { key: 'concentracion', label: 'Concentración', placeholder: 'Ej. 500 mg' },
    { key: 'formaFarmaceutica', label: 'Forma farmacéutica', tipo: 'select-grupos', grupos: FORMAS_FARMACEUTICAS },
    { key: 'principioActivo', label: 'Principio activo', placeholder: 'Ej. Paracetamol', completo: true },
  ]},
  PANALES: { titulo: 'Información de pañales', campos: [
    { key: 'talla', label: 'Talla', tipo: 'select', opciones: TALLAS_PANAL },
    { key: 'etapa', label: 'Etapa', tipo: 'select', opciones: ETAPAS_PANAL },
  ]},
  BIBERON: { titulo: 'Información del biberón', campos: [
    { key: 'capacidad', label: 'Capacidad', tipo: 'number', placeholder: 'Ej. 250', combinarCon: 'unidadCapacidad' },
    { key: 'unidadCapacidad', label: 'Unidad', tipo: 'select', opciones: ['mL', 'oz'], valorPorDefecto: 'mL', ocultoEnResumen: true },
    { key: 'material', label: 'Material', tipo: 'select', opciones: ['Plástico', 'Vidrio', 'Silicona'] },
  ]},
  HIGIENE: { titulo: 'Información de higiene', campos: [
    { key: 'tipo', label: 'Tipo de producto', tipo: 'select', opciones: TIPOS_HIGIENE },
    { key: 'contenido', label: 'Contenido', placeholder: 'Ej. 250 mL' },
  ]},
  CURACION: { titulo: 'Información de curación', campos: [
    { key: 'tipo', label: 'Tipo', tipo: 'select', opciones: TIPOS_CURACION },
    { key: 'medida', label: 'Medida / contenido', placeholder: 'Ej. 100 g' },
  ]},
};
ATRIBUTOS_POR_CATEGORIA.SUPLEMENTO = ATRIBUTOS_POR_CATEGORIA.MEDICAMENTO; // mismos campos que medicamento

// Resumen legible de "atributos" para la columna de la tabla, la vista previa y la búsqueda;
// sale solo de recorrer la tabla de arriba, sin si-la-categoría-es-tal repetido.
function resumenAtributos(categoria, atributos = {}) {
  const definicion = ATRIBUTOS_POR_CATEGORIA[categoria];
  if (!definicion) return '';
  return definicion.campos
    .filter((c) => !c.ocultoEnResumen)
    .map((c) => (atributos[c.key]
      ? (c.combinarCon ? `${atributos[c.key]} ${atributos[c.combinarCon] || ''}`.trim() : atributos[c.key])
      : null))
    .filter(Boolean)
    .join(' · ');
}

// ============================================================================
// 3. MODELO DEL FORMULARIO
// ============================================================================

const formularioVacio = () => ({
  nombre: '', descripcion: '', categoria: 'MEDICAMENTO', requiereReceta: false, atributos: {},
  precioVenta: '', unidadVenta: 'UNIDAD',
  laboratorioIds: [], activo: true,
});

// Fila de la API -> valores del formulario (para editar)
const formularioDesdeProducto = (p) => ({
  nombre: p.nombre, descripcion: p.descripcion || '', categoria: p.categoria,
  requiereReceta: !!p.requiereReceta, atributos: p.atributos || {},
  precioVenta: p.precioVenta, unidadVenta: p.unidadVenta,
  laboratorioIds: p.laboratorios.map((l) => l.id), activo: p.activo,
});

// Valores del formulario -> cuerpo de la petición POST/PUT
const cuerpoDesdeFormulario = (d) => ({
  nombre: d.nombre, descripcion: d.descripcion, categoria: d.categoria, requiereReceta: d.requiereReceta,
  atributos: d.atributos, precioVenta: Number(d.precioVenta),
  unidadVenta: d.unidadVenta.toUpperCase(), laboratorioIds: d.laboratorioIds, activo: d.activo,
});

// Texto de la "Vista previa" del formulario: nombre — detalle — precio / unidad
function textoVistaPrevia(d) {
  const precio = d.precioVenta !== '' && d.precioVenta != null
    ? `${money(d.precioVenta)} / ${d.unidadVenta.toLowerCase()}` : '';
  return [d.nombre || 'Producto', resumenAtributos(d.categoria, d.atributos), precio].filter(Boolean).join(' — ');
}

// ============================================================================
// 4. BÚSQUEDA  (algoritmos de búsqueda)
// ============================================================================
// Antes se buscaba con JSON.stringify(fila).includes(texto), que fallaba de varias formas:
// coincidía con los NOMBRES de las propiedades (buscar "nom" devolvía todo por "nombre"), con ids
// y con "true"/"false"; distinguía mayúsculas y tildes ("panales" no encontraba "Pañales"); y no
// encontraba lo que se ve en pantalla ("vitamina" no hallaba la categoría "Suplemento / vitamina",
// porque en los datos es SUPLEMENTO).
//
// Ahora se combinan tres algoritmos de búsqueda clásicos, cada uno donde le corresponde:
//
//   ALGORITMO               PARA QUÉ SE USA AQUÍ                               COSTO
//   ----------------------  -------------------------------------------------  ----------------
//   Tabla hash (Map)        Índice invertido: palabra -> productos donde       O(1) por palabra
//                           aparece. Dada una palabra, se llega directo.
//   Búsqueda binaria        Sobre el vocabulario ORDENADO, halla todas las     O(log V + k)
//                           palabras que EMPIEZAN con lo escrito ("para" ->
//                           "paracetamol"). Exige lista ordenada: por eso
//                           el vocabulario se ordena al armar el índice.
//   Búsqueda lineal         Recorre el vocabulario palabra por palabra para    O(V)
//                           las que CONTIENEN el texto en medio ("racetamol"
//                           en "paracetamol"). Ahí no sirve ordenar, porque
//                           el texto puede estar en cualquier posición.
//
//   V = palabras distintas del catálogo; k = las que empiezan con lo escrito.
//   Recorrer el vocabulario (V) es mucho más barato que recorrer producto por producto,
//   campo por campo y palabra por palabra.
//
// Flujo de una búsqueda:
//   1. NORMALIZAR  minúsculas y sin tildes ("Pañales" -> "panales"), igual para los datos y lo escrito.
//   2. TÉRMINOS    Lo escrito se parte en palabras: "para 500" son 2 términos.
//   3. HALLAR      Por término: binaria (empieza con) + lineal (contiene) sobre el vocabulario, y el
//                  hash entrega los productos de cada palabra hallada.
//   4. PUNTUAR     Palabra exacta (3) > empieza con (2) > contiene (1), por el peso del campo donde
//                  está (nombre 5 … descripción 1). Por producto se toma la mejor coincidencia.
//   5. COMBINAR    Un producto sale solo si CADA término lo encontró (AND): se intersectan los
//                  resultados de los términos y se suman los puntajes.
//   6. ORDENAR     Mayor puntaje primero; en empate, el orden original. Sin texto, la lista tal cual.

const COINCIDENCIA = { EXACTA: 3, PREFIJO: 2, CONTIENE: 1 };
const MIN_LARGO_CONTIENE = 2; // con 1 letra solo cuenta "empieza con"; "contiene" daría casi todo

// minúsculas, sin tildes ("Pañales" -> "panales") y coma decimal como punto ("7,5" -> "7.5")
const normalizar = (texto) => String(texto ?? '')
  .normalize('NFD').replace(/[\u0300-\u036f]/g, '')
  .toLowerCase()
  .replace(/(\d),(\d)/g, '$1.$2');

// palabras = letras/números, con decimales como una sola palabra ("s/ 7.50" -> ["s", "7.50"])
const palabrasDe = (texto) => normalizar(texto).match(/[\p{L}\p{N}]+(?:\.\p{N}+)?/gu) ?? [];

// Qué se busca de cada producto: el texto que se MUESTRA, con el peso que tiene en el puntaje
function camposDeBusqueda(producto) {
  return [
    { texto: producto.nombre, peso: 5 },
    { texto: etiquetaCategoria(producto.categoria), peso: 3 },
    { texto: resumenAtributos(producto.categoria, producto.atributos), peso: 3 }, // concentración, principio activo, talla…
    { texto: producto.laboratorios.map((l) => l.nombre).join(' '), peso: 2 },
    { texto: `${money(producto.precioVenta)} ${producto.unidadVenta}`, peso: 1 },
    { texto: producto.descripcion, peso: 1 },
    // "activo" no debe encontrar a "inactivo": este campo solo admite "empieza con"
    { texto: producto.activo ? 'activo' : 'inactivo', peso: 1, soloPrefijo: true },
  ];
}

// ---- Tabla hash: índice invertido ------------------------------------------
// postings: palabra -> [{ pos, peso, soloPrefijo }]  (pos = lugar del producto en la lista)
// vocabulario: todas las palabras distintas, ORDENADAS (requisito de la búsqueda binaria)
// Se arma una sola vez cuando cambia la lista, no en cada tecla.
function crearIndice(productos) {
  const postings = new Map();
  productos.forEach((producto, pos) => {
    for (const campo of camposDeBusqueda(producto)) {
      for (const palabra of palabrasDe(campo.texto)) {
        const entrada = { pos, peso: campo.peso, soloPrefijo: !!campo.soloPrefijo };
        const lista = postings.get(palabra);
        if (lista) lista.push(entrada); else postings.set(palabra, [entrada]);
      }
    }
  });
  const vocabulario = [...postings.keys()].sort(); // orden por código de carácter, el mismo de "<" y ">="
  return { productos, postings, vocabulario };
}

// ---- Búsqueda binaria ---------------------------------------------------------
// Primer lugar i de la lista ORDENADA donde lista[i] >= objetivo. Cada vuelta descarta la mitad.
function limiteInferior(lista, objetivo) {
  let bajo = 0;
  let alto = lista.length;
  while (bajo < alto) {
    const medio = (bajo + alto) >>> 1;
    if (lista[medio] < objetivo) bajo = medio + 1;
    else alto = medio;
  }
  return bajo;
}

// Las palabras que empiezan con el prefijo están todas juntas, justo desde ese límite
// (si la palabra exacta existe, es la primera del grupo).
function palabrasConPrefijo(vocabulario, prefijo) {
  const encontradas = [];
  for (let i = limiteInferior(vocabulario, prefijo); i < vocabulario.length && vocabulario[i].startsWith(prefijo); i++) {
    encontradas.push(vocabulario[i]);
  }
  return encontradas;
}

// ---- Búsqueda lineal ----------------------------------------------------------
// Palabras que tienen el texto en el MEDIO o al final (las que empiezan con él ya las dio la binaria)
function palabrasQueContienen(vocabulario, texto) {
  const encontradas = [];
  for (const palabra of vocabulario) {
    if (palabra.includes(texto) && !palabra.startsWith(texto)) encontradas.push(palabra);
  }
  return encontradas;
}

// ---- Un término -> mejor puntaje de cada producto: Map(pos -> puntaje) -----------
function puntajesDelTermino(indice, termino) {
  const mejores = new Map();
  const registrar = (palabra, tipo) => {
    for (const { pos, peso, soloPrefijo } of indice.postings.get(palabra)) { // hash: acceso directo
      if (tipo === COINCIDENCIA.CONTIENE && soloPrefijo) continue;
      const puntaje = tipo * peso;
      if (puntaje > (mejores.get(pos) ?? 0)) mejores.set(pos, puntaje);
    }
  };
  for (const palabra of palabrasConPrefijo(indice.vocabulario, termino)) {            // binaria
    registrar(palabra, palabra === termino ? COINCIDENCIA.EXACTA : COINCIDENCIA.PREFIJO);
  }
  if (termino.length >= MIN_LARGO_CONTIENE) {
    for (const palabra of palabrasQueContienen(indice.vocabulario, termino)) {        // lineal
      registrar(palabra, COINCIDENCIA.CONTIENE);
    }
  }
  return mejores;
}

// ---- Todos los términos (AND) y orden por relevancia ---------------------------
function buscarProductos(indice, consulta) {
  const terminos = [...new Set(palabrasDe(consulta))];
  if (!terminos.length) return indice.productos;

  let totales = null; // Map(pos -> puntaje acumulado de los términos vistos)
  for (const termino of terminos) {
    const delTermino = puntajesDelTermino(indice, termino);
    if (totales === null) {
      totales = delTermino;
    } else {
      const interseccion = new Map(); // se queda solo con lo que cumplen TODOS los términos
      for (const [pos, puntaje] of delTermino) {
        if (totales.has(pos)) interseccion.set(pos, totales.get(pos) + puntaje);
      }
      totales = interseccion;
    }
    if (!totales.size) return [];
  }
  return [...totales]
    .sort(([posA, puntajeA], [posB, puntajeB]) => puntajeB - puntajeA || posA - posB)
    .map(([pos]) => indice.productos[pos]);
}

// ============================================================================
// 5. DATOS Y ACCIONES
// ============================================================================
// Todo lo que habla con la API vive aquí; los componentes solo reciben datos y funciones.

function useProductos() {
  const [productos, setProductos] = useState([]);
  const [laboratorios, setLaboratorios] = useState([]);
  const [error, setError] = useState('');

  const cargar = () => api('/productos').then(setProductos).catch((e) => setError(e.message));

  useEffect(() => {
    cargar();
    api('/laboratorios').then(setLaboratorios).catch(() => {});
  }, []);

  // id null = crear. Devuelve true si se guardó, para que la pantalla cierre el modal.
  const guardar = async (id, datos) => {
    setError('');
    try {
      await api(id ? `/productos/${id}` : '/productos', { method: id ? 'PUT' : 'POST', body: cuerpoDesdeFormulario(datos) });
      cargar();
      return true;
    } catch (err) { setError(err.message); return false; }
  };

  const desactivar = async (producto) => {
    if (!confirm(`¿Desactivar "${producto.nombre}"?`)) return;
    try { await api(`/productos/${producto.id}`, { method: 'DELETE' }); cargar(); }
    catch (err) { setError(err.message); }
  };

  // Eliminación definitiva en dos tiempos: primero Desactivar, luego Eliminar. Solo pasa si el
  // producto no tiene lotes; si los tiene, el servidor responde 409 explicando cuánto stock queda y
  // que el producto se conserva desactivado (los lotes respaldan compras y ventas ya registradas).
  const eliminar = async (producto) => {
    if (!confirm(`¿Eliminar definitivamente "${producto.nombre}"? No se puede deshacer.\n\nSi ya tiene lotes o historial, el sistema no lo permitirá y quedará desactivado.`)) return;
    setError('');
    try { await api(`/productos/${producto.id}/definitivo`, { method: 'DELETE' }); cargar(); }
    catch (err) { setError(err.message); }
  };

  return { productos, laboratorios, error, guardar, desactivar, eliminar };
}

// ============================================================================
// 6. COMPONENTES
// ============================================================================

// Un solo input/select que sabe dibujarse a partir de la definición de un campo de "atributos"
function CampoAtributo({ campo, valor, onChange }) {
  if (campo.tipo === 'select') {
    return (
      <select value={valor ?? campo.valorPorDefecto ?? ''} onChange={(e) => onChange(e.target.value)}>
        {!campo.valorPorDefecto && <option value="">Seleccionar...</option>}
        {campo.opciones.map((o) => <option key={o} value={o}>{o}</option>)}
      </select>
    );
  }
  if (campo.tipo === 'select-grupos') {
    return (
      <select value={valor || ''} onChange={(e) => onChange(e.target.value)}>
        <option value="">Seleccionar...</option>
        {Object.entries(campo.grupos).map(([grupo, opciones]) => (
          <optgroup key={grupo} label={grupo}>{opciones.map((o) => <option key={o} value={o}>{o}</option>)}</optgroup>
        ))}
      </select>
    );
  }
  return (
    <input type={campo.tipo === 'number' ? 'number' : 'text'} min={campo.tipo === 'number' ? '0' : undefined}
      value={valor || ''} placeholder={campo.placeholder} onChange={(e) => onChange(e.target.value)} />
  );
}

// Bloque del formulario con los campos propios de la categoría elegida (nada si no tiene)
function SeccionAtributos({ datos, onCampo, onAtributo }) {
  const definicion = ATRIBUTOS_POR_CATEGORIA[datos.categoria];
  if (!definicion) return null;
  return (
    <>
      <h4>{definicion.titulo}</h4>
      <div className="fields">
        {definicion.campos.map((campo) => (
          <label key={campo.key} className={campo.completo ? 'completo' : ''}>
            {campo.label}
            <CampoAtributo campo={campo} valor={datos.atributos[campo.key]} onChange={(v) => onAtributo(campo.key, v)} />
          </label>
        ))}
        {ES_MEDICO(datos.categoria) && (
          <label className="chk">Requiere receta
            <input type="checkbox" checked={datos.requiereReceta} onChange={(e) => onCampo('requiereReceta', e.target.checked)} />
          </label>
        )}
      </div>
    </>
  );
}

// Ventana de crear/editar. No sabe de la API: recibe valores y avisa los cambios.
function ModalProducto({ titulo, datos, laboratorios, error, onCampo, onAtributo, onGuardar, onCerrar }) {
  return (
    <div className="overlay" onClick={onCerrar}>
      <form className="modal" onClick={(e) => e.stopPropagation()} onSubmit={onGuardar}>
        <h3>{titulo} — Producto</h3>

        <h4>Información general</h4>
        <div className="fields">
          <label className="completo">Categoría *
            <select value={datos.categoria} onChange={(e) => onCampo('categoria', e.target.value)}>
              {CATEGORIAS.map((c) => <option key={c.value} value={c.value}>{c.label}</option>)}
            </select>
          </label>
          <label>Nombre *
            <input required value={datos.nombre} onChange={(e) => onCampo('nombre', e.target.value)} placeholder="Ej. Paracetamol" />
          </label>
          <label>Precio de venta *
            <input required type="number" min="0" step="0.01" value={datos.precioVenta}
              onChange={(e) => onCampo('precioVenta', e.target.value)} placeholder="S/ 0.00" />
          </label>
        </div>

        <SeccionAtributos datos={datos} onCampo={onCampo} onAtributo={onAtributo} />

        <h4>Unidad de venta</h4>
        <div className="fields">
          <label>Se vende por (ej. Tableta, Unidad, mL)
            <input required list="unidadesVenta" value={datos.unidadVenta} onChange={(e) => onCampo('unidadVenta', e.target.value.toUpperCase())} />
          </label>
        </div>
        <datalist id="unidadesVenta">{UNIDADES_VENTA.map((u) => <option key={u} value={u} />)}</datalist>
        <p className="hint">
          Todo se vende por unidad ({datos.unidadVenta.toLowerCase() || '…'}) al precio de arriba. Cuántas cajas y cuántas
          unidades trae cada caja se indica en cada compra, no aquí.
        </p>

        <h4>Fabricante</h4>
        <label>Laboratorios fabricantes (Ctrl+clic para varios)
          <select multiple value={datos.laboratorioIds}
            onChange={(e) => onCampo('laboratorioIds', Array.from(e.target.selectedOptions, (o) => Number(o.value)))}>
            {laboratorios.filter((l) => l.activo).map((l) => <option key={l.id} value={l.id}>{l.nombre}</option>)}
          </select>
        </label>
        <small>¿No aparece el laboratorio que buscas? Créalo primero en Laboratorios.</small>

        <h4>Información adicional</h4>
        <div className="fields">
          <label className="completo">Descripción
            <textarea value={datos.descripcion} onChange={(e) => onCampo('descripcion', e.target.value)} />
          </label>
          <label className="chk">Activo
            <input type="checkbox" checked={datos.activo} onChange={(e) => onCampo('activo', e.target.checked)} />
          </label>
        </div>

        <div className="resumen">
          <strong>Vista previa</strong>
          {textoVistaPrevia(datos)}
        </div>

        {error && <p className="error">{error}</p>}
        <div className="actions">
          <button type="button" className="ghost" onClick={onCerrar}>Cancelar</button>
          <button className="primary">Guardar</button>
        </div>
      </form>
    </div>
  );
}

function FilaProducto({ producto: p, puedeEditar, onEditar, onDesactivar, onEliminar }) {
  return (
    <tr>
      <td data-label="Nombre">{p.nombre}</td>
      <td data-label="Categoría">{etiquetaCategoria(p.categoria)}</td>
      <td data-label="Detalle">{resumenAtributos(p.categoria, p.atributos) || '—'}</td>
      <td data-label="Precio">{money(p.precioVenta)} / {p.unidadVenta.toLowerCase()}</td>
      <td data-label="Laboratorios">{p.laboratorios.map((l) => l.nombre).join(', ')}</td>
      <td data-label="Activo">{p.activo ? 'Sí' : 'No'}</td>
      {puedeEditar && (
        <td className="row-actions">
          <button className="ghost" onClick={() => onEditar(p)}>Editar</button>
          <button className="danger" onClick={() => onDesactivar(p)}>Desactivar</button>
          {}
          <button className="danger" disabled={p.activo} onClick={() => onEliminar(p)}
            title={p.activo ? 'Primero desactívalo' : 'Eliminar definitivamente'}>Eliminar</button>
        </td>
      )}
    </tr>
  );
}

function TablaProductos({ productos, consulta, puedeEditar, ...acciones }) {
  const textoVacio = consulta.trim() ? `Sin resultados para "${consulta.trim()}"` : 'Sin registros';
  return (
    <div className="card">
      <div className="table-wrap">
      <table className="stack">
        <thead>
          <tr>
            <th>Nombre</th><th>Categoría</th><th>Detalle</th><th>Precio</th><th>Laboratorios</th><th>Activo</th>
            {puedeEditar && <th>Acciones</th>}
          </tr>
        </thead>
        <tbody>
          {productos.map((p) => <FilaProducto key={p.id} producto={p} puedeEditar={puedeEditar} {...acciones} />)}
          {!productos.length && <tr><td colSpan={puedeEditar ? 7 : 6}>{textoVacio}</td></tr>}
        </tbody>
      </table>
      </div>
    </div>
  );
}


export default function Productos() {
  const { user } = useAuth();
  const puedeEditar = user.rol === 'ADMINISTRADOR';
  const { productos, laboratorios, error, guardar, desactivar, eliminar } = useProductos();

  const [modal, setModal] = useState(null); // { id, datos } mientras el formulario está abierto
  const [consulta, setConsulta] = useState('');

  // El índice se rehace solo cuando cambia la lista; el filtrado, cuando cambia lo escrito
  const indice = useMemo(() => crearIndice(productos), [productos]);
  const visibles = useMemo(() => buscarProductos(indice, consulta), [indice, consulta]);

  const cambiarCampo = (k, v) => setModal((m) => ({ ...m, datos: { ...m.datos, [k]: v } }));
  const cambiarAtributo = (k, v) => setModal((m) => ({ ...m, datos: { ...m.datos, atributos: { ...m.datos.atributos, [k]: v } } }));

  const alGuardar = async (e) => {
    e.preventDefault();
    if (await guardar(modal.id, modal.datos)) setModal(null);
  };

  return (
    <>
      <div className="head">
        <h1>Productos</h1>
        <input placeholder="Buscar…" value={consulta} onChange={(e) => setConsulta(e.target.value)} />
        {puedeEditar && <button className="primary" onClick={() => setModal({ id: null, datos: formularioVacio() })}>+ Nuevo</button>}
      </div>
      {error && !modal && <p className="error">{error}</p>}

      <TablaProductos
        productos={visibles} consulta={consulta} puedeEditar={puedeEditar}
        onEditar={(p) => setModal({ id: p.id, datos: formularioDesdeProducto(p) })}
        onDesactivar={desactivar} onEliminar={eliminar}
      />

      {modal && (
        <ModalProducto
          titulo={modal.id ? 'Editar' : 'Nuevo'} datos={modal.datos} laboratorios={laboratorios} error={error}
          onCampo={cambiarCampo} onAtributo={cambiarAtributo} onGuardar={alGuardar} onCerrar={() => setModal(null)}
        />
      )}
    </>
  );
}
