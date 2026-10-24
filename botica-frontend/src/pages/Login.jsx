import { useState } from 'react';
import { Navigate, useNavigate } from 'react-router-dom';
import { useAuth } from '../auth';

export default function Login() {
  const { user, login } = useAuth();
  const nav = useNavigate();
  const [f, setF] = useState({ correo: '', password: '' });
  const [error, setError] = useState('');
  if (user) return <Navigate to="/" replace />;

  const submit = async (e) => {
    e.preventDefault();
    try { await login(f.correo, f.password); nav('/'); }
    catch (err) { setError(err.message); }
  };

  return (
    <div className="login">
      <form className="card" onSubmit={submit}>
        <h2>💊 Botica — Ingresar</h2>
        <label>Correo<input type="email" required value={f.correo} onChange={(e) => setF({ ...f, correo: e.target.value })} /></label>
        <label>Contraseña<input type="password" required value={f.password} onChange={(e) => setF({ ...f, password: e.target.value })} /></label>
        {error && <p className="error">{error}</p>}
        <button className="primary">Ingresar</button>
      </form>
    </div>
  );
}
