import { useEffect, useState } from 'react';
import { api } from '../api';
import { useAuth } from '../auth';

export default function CrudPage({ cfg }) {
  const { user } = useAuth();
  const canEdit = cfg.editRoles.includes(user.rol);
  const [rows, setRows] = useState([]);
  const [opts, setOpts] = useState({});
  const [modal, setModal] = useState(null); 
  const [q, setQ] = useState('');
  const [error, setError] = useState('');

  const load = () => api(cfg.endpoint).then(setRows).catch((e) => setError(e.message));

  useEffect(() => {
    setRows([]); setModal(null); setQ(''); setError('');
    load();
    cfg.fields.filter((f) => f.optionsFrom).forEach((f) =>
      api(f.optionsFrom).then((d) => setOpts((o) => ({ ...o, [f.name]: d }))).catch(() => {}));
  }, [cfg]);

  const blank = () => Object.fromEntries(cfg.fields.map((f) =>
    [f.name, f.multiple ? [] : f.type === 'checkbox' ? true : '']));
  const fromRow = (row) => Object.fromEntries(cfg.fields.map((f) =>
    [f.name, f.fromRow ? f.fromRow(row) : f.type === 'password' ? '' : row[f.name] ?? '']));

  const set = (f, e) => {
    const t = e.target;
    const v = f.multiple ? Array.from(t.selectedOptions, (o) => Number(o.value))
      : f.type === 'checkbox' ? t.checked
      : f.type === 'number' && t.value !== '' ? Number(t.value)
      : t.value;
    setModal((m) => ({ ...m, data: { ...m.data, [f.name]: v } }));
  };

  const save = async (e) => {
    e.preventDefault(); setError('');
    const body = { ...modal.data };
    cfg.fields.forEach((f) => { if (modal.id && f.type === 'password' && !body[f.name]) delete body[f.name]; });
    try {
      await api(modal.id ? `${cfg.endpoint}/${modal.id}` : cfg.endpoint, { method: modal.id ? 'PUT' : 'POST', body });
      setModal(null); load();
    } catch (err) { setError(err.message); }
  };

  const remove = async (row) => {
    if (!confirm(`¿${cfg.deleteLabel || 'Eliminar'} este registro?`)) return;
    try { await api(`${cfg.endpoint}/${row.id}`, { method: 'DELETE' }); load(); }
    catch (err) { setError(err.message); }
  };

   const eliminar = async (row) => {
    if (!confirm(`¿Eliminar definitivamente "${row.nombre}"? No se puede deshacer.\n\nSi ya tiene historial, el sistema no lo permitirá y podrás desactivarlo.`)) return;
    setError('');
    try { await api(`${cfg.endpoint}/${row.id}/definitivo`, { method: 'DELETE' }); load(); }
    catch (err) { setError(err.message); }
  };

  const input = (f) => {
    const v = modal.data[f.name];
    if (f.type === 'checkbox') return <input type="checkbox" checked={!!v} onChange={(e) => set(f, e)} />;
    if (f.type === 'textarea') return <textarea value={v} onChange={(e) => set(f, e)} />;
    if (f.options || f.optionsFrom) {
      let list = f.options || opts[f.name] || [];
      if (f.optionsFilter) list = list.filter(f.optionsFilter);
      return (
        <select multiple={f.multiple} value={v} required={f.required} onChange={(e) => set(f, e)}>
          {!f.multiple && <option value="">— Seleccione —</option>}
          {list.map((o) => typeof o === 'string'
            ? <option key={o} value={o}>{o}</option>
            : <option key={o.id} value={o.id}>{o[f.optionLabel || 'nombre']}</option>)}
        </select>
      );
    }
    return (
      <input type={f.type || 'text'} value={v} step={f.type === 'number' ? '0.01' : undefined}
        required={f.required && !(modal.id && f.type === 'password')} onChange={(e) => set(f, e)} />
    );
  };

  const cell = (c, r) => c.render ? c.render(r) : typeof r[c.key] === 'boolean' ? (r[c.key] ? 'Sí' : 'No') : r[c.key];
  const shown = rows.filter((r) => JSON.stringify(r).toLowerCase().includes(q.toLowerCase()));

  return (
    <>
      <div className="head">
        <h1>{cfg.title}</h1>
        <input placeholder="Buscar…" value={q} onChange={(e) => setQ(e.target.value)} />
        {canEdit && !cfg.noCreate && <button className="primary" onClick={() => setModal({ id: null, data: blank() })}>+ Nuevo</button>}
      </div>
      {error && !modal && <p className="error">{error}</p>}
      <div className="card">
        <div className="table-wrap">
        <table className="stack">
          <thead>
            <tr>{cfg.columns.map((c) => <th key={c.label}>{c.label}</th>)}{canEdit && <th>Acciones</th>}</tr>
          </thead>
          <tbody>
            {shown.map((r) => (
              <tr key={r.id}>
                {cfg.columns.map((c) => <td key={c.label} data-label={c.label}>{cell(c, r)}</td>)}
                {canEdit && (
                  <td className="row-actions">
                    <button className="ghost" onClick={() => setModal({ id: r.id, data: fromRow(r) })}>Editar</button>
                    {!cfg.noDelete && <button className="danger" onClick={() => remove(r)}>{cfg.deleteLabel || 'Eliminar'}</button>}
                    {cfg.hardDelete && <button className="danger" onClick={() => eliminar(r)}>Eliminar</button>}
                  </td>
                )}
              </tr>
            ))}
            {!shown.length && <tr><td colSpan="20">Sin registros</td></tr>}
          </tbody>
        </table>
        </div>
      </div>

      {modal && (
        <div className="overlay" onClick={() => setModal(null)}>
          <form className="modal" onClick={(e) => e.stopPropagation()} onSubmit={save}>
            <h3>{modal.id ? 'Editar' : 'Nuevo'} — {cfg.title}</h3>
            <div className="fields">
              {cfg.fields.map((f) => (
                <label key={f.name} className={f.type === 'checkbox' ? 'chk' : ''}>
                  {f.label}{f.required && ' *'}{input(f)}
                </label>
              ))}
            </div>
            {error && <p className="error">{error}</p>}
            <div className="actions">
              <button type="button" className="ghost" onClick={() => setModal(null)}>Cancelar</button>
              <button className="primary">Guardar</button>
            </div>
          </form>
        </div>
      )}
    </>
  );
}
