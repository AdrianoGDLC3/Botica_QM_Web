import { useState } from 'react';
import { api } from '../api';
import { imprimirVenta } from '../impresion';
import ResumenVenta from '../components/ResumenVenta';
import Historial, { ventasCols } from './Historial';

export default function HistorialVentas() {
  const [sel, setSel] = useState(null);     
  const [imprimiendo, setImprimiendo] = useState(null);
  const [error, setError] = useState(null);

  const abrir = async (venta) => {
    setSel({ venta, detalle: null, error: null });
    try {
      const detalle = await api(`/ventas/${venta.id}`);
      setSel((s) => (s && s.venta.id === venta.id ? { ...s, detalle } : s));
    } catch (e) {
      setSel((s) => (s && s.venta.id === venta.id ? { ...s, error: e.message } : s));
    }
  };

  const imprimirFila = async (venta) => {
    setError(null);
    setImprimiendo(venta.id);
    try { imprimirVenta(await api(`/ventas/${venta.id}`)); }
    catch (e) { setError(e.message); }
    finally { setImprimiendo(null); }
  };

  return (
    <>
      {error && <p className="error">{error}</p>}
      <Historial
        title="Historial de ventas" endpoint="/ventas" columns={ventasCols}
        onSelect={abrir}
        rowActions={(r) => (
          <button className="ghost" disabled={imprimiendo === r.id} onClick={() => imprimirFila(r)}>
            🖨 Imprimir
          </button>
        )}
      />
      {sel && (
        <ResumenVenta venta={sel.venta} detalle={sel.detalle} error={sel.error}
          onClose={() => setSel(null)} onPrint={imprimirVenta} />
      )}
    </>
  );
}
