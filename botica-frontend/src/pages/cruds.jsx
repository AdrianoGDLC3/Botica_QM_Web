import { daysTo, fmtDate } from '../api';

const ADMIN = ['ADMINISTRADOR'];

export const usuarios = {
  title: 'Usuarios', endpoint: '/usuarios', editRoles: ADMIN, deleteLabel: 'Desactivar',
  fields: [
    { name: 'nombre', label: 'Nombre', required: true },
    { name: 'dni', label: 'DNI' },
    { name: 'telefono', label: 'Teléfono' },
    { name: 'correo', label: 'Correo', type: 'email', required: true },
    { name: 'password', label: 'Contraseña (vacío = no cambiar)', type: 'password', required: true },
    { name: 'rol', label: 'Rol', required: true, options: ['CLIENTE', 'EMPLEADO', 'ADMINISTRADOR'] },
    { name: 'activo', label: 'Activo', type: 'checkbox' },
  ],
  columns: [
    { key: 'nombre', label: 'Nombre' }, { key: 'dni', label: 'DNI' }, { key: 'correo', label: 'Correo' },
    { key: 'rol', label: 'Rol' }, { key: 'activo', label: 'Activo' },
  ],
};

export const laboratorios = {
  title: 'Laboratorios', endpoint: '/laboratorios', editRoles: ADMIN, deleteLabel: 'Desactivar', hardDelete: true,
  fields: [
    { name: 'nombre', label: 'Nombre', required: true },
    { name: 'ruc', label: 'RUC' },
    { name: 'contacto', label: 'Persona de contacto' },
    { name: 'telefono', label: 'Teléfono' },
    { name: 'correo', label: 'Correo de contacto', type: 'email' },
    { name: 'direccion', label: 'Dirección' },
    { name: 'pais', label: 'País' },
    { name: 'activo', label: 'Activo', type: 'checkbox' },
  ],
  columns: [
    { key: 'nombre', label: 'Nombre' }, { key: 'ruc', label: 'RUC' }, { key: 'contacto', label: 'Contacto' },
    { key: 'telefono', label: 'Teléfono' }, { key: 'correo', label: 'Correo' }, { key: 'activo', label: 'Activo' },
  ],
};

export const proveedores = {
  title: 'Proveedores', endpoint: '/proveedores', editRoles: ADMIN, deleteLabel: 'Desactivar', hardDelete: true,
  fields: [
    { name: 'nombre', label: 'Nombre', required: true },
    { name: 'ruc', label: 'RUC' },
    { name: 'contacto', label: 'Contacto' },
    { name: 'telefono', label: 'Teléfono' },
    { name: 'correo', label: 'Correo', type: 'email' },
    { name: 'direccion', label: 'Dirección' },
    { name: 'activo', label: 'Activo', type: 'checkbox' },
  ],
  columns: [
    { key: 'nombre', label: 'Nombre' }, { key: 'ruc', label: 'RUC' }, { key: 'contacto', label: 'Contacto' },
    { key: 'telefono', label: 'Teléfono' }, { key: 'activo', label: 'Activo' },
  ],
};

export const lotes = {
  title: 'Lotes', endpoint: '/lotes', editRoles: ADMIN, noCreate: true, noDelete: true,
  fields: [
    { name: 'numeroLote', label: 'Número de lote', required: true },
    { name: 'fechaVencimiento', label: 'Fecha de vencimiento', type: 'date', required: true },
    { name: 'laboratorioId', label: 'Laboratorio fabricante', optionsFrom: '/laboratorios', optionsFilter: (l) => l.activo,
      fromRow: (r) => r.laboratorioId ?? '' },
  ],
  columns: [
    { key: 'productoNombre', label: 'Producto' }, { key: 'laboratorioNombre', label: 'Laboratorio' },
    { key: 'numeroLote', label: 'Lote' },
    { label: 'Vence', render: (r) => {
        const d = daysTo(r.fechaVencimiento);
        return <span className={d < 0 ? 'bad' : d <= 60 ? 'warn' : ''}>{fmtDate(r.fechaVencimiento)}</span>;
      } },
    { key: 'stockActual', label: 'Stock' },
  ],
};
