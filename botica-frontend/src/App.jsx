import { Routes, Route, Navigate, NavLink, Outlet } from 'react-router-dom';
import { useAuth } from './auth';
import Login from './pages/Login';
import Dashboard from './pages/Dashboard';
import NuevaVenta from './pages/NuevaVenta';
import Compras from './pages/Compras';
import HistorialVentas from './pages/HistorialVentas';
import CrudPage from './components/CrudPage';
import Productos from './pages/Productos';
import { usuarios, laboratorios, proveedores, lotes } from './pages/cruds';

const ADMIN = ['ADMINISTRADOR'];
const STAFF = ['ADMINISTRADOR', 'EMPLEADO'];

const MENU = [
  { path: '/', label: 'Dashboard', roles: STAFF, el: <Dashboard /> },
  { path: '/ventas', label: 'Nueva venta', roles: STAFF, el: <NuevaVenta /> },
  { path: '/historial-ventas', label: 'Historial de ventas', roles: STAFF,
    el: <HistorialVentas /> },
  { path: '/compras', label: 'Compras', roles: STAFF, el: <Compras /> },
  { path: '/productos', label: 'Productos', roles: STAFF, el: <Productos /> },
  { path: '/lotes', label: 'Lotes', roles: STAFF, el: <CrudPage cfg={lotes} /> },
  { path: '/laboratorios', label: 'Laboratorios', roles: STAFF, el: <CrudPage cfg={laboratorios} /> },
  { path: '/proveedores', label: 'Proveedores', roles: STAFF, el: <CrudPage cfg={proveedores} /> },
  { path: '/usuarios', label: 'Usuarios', roles: ADMIN, el: <CrudPage cfg={usuarios} /> },
];

function Guard({ roles, children }) {
  const { user } = useAuth();
  if (!roles.includes(user.rol)) return <p className="card">No tienes acceso a esta pantalla.</p>;
  return children;
}

function Layout() {
  const { user, logout } = useAuth();
  if (!user) return <Navigate to="/login" replace />;
  return (
    <div className="layout">
      <aside>
        <h2>💊 Botica</h2>
        <nav>
          {MENU.filter((m) => m.roles.includes(user.rol)).map((m) => (
            <NavLink key={m.path} to={m.path} end={m.path === '/'}>{m.label}</NavLink>
          ))}
        </nav>
        <div className="user">
          <b>{user.nombre}</b>
          <small>{user.rol}</small>
          <button className="ghost" onClick={logout}>Cerrar sesión</button>
        </div>
      </aside>
      <main><Outlet /></main>
    </div>
  );
}

export default function App() {
  return (
    <Routes>
      <Route path="/login" element={<Login />} />
      <Route element={<Layout />}>
        {MENU.map((m) => (
          <Route key={m.path} path={m.path} element={<Guard roles={m.roles}>{m.el}</Guard>} />
        ))}
      </Route>
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}
