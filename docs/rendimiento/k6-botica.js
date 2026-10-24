// Prueba de rendimiento con k6 (Semana 13) — Sistema de Gestión Integral para Botica Q&M.
//
// Simula la concurrencia de usuarios de la botica sobre los tres flujos críticos:
//   1) Login (POST /api/auth/login)
//   2) Consulta de ventas (GET /api/ventas)
//   3) Registro de una venta (POST /api/ventas)
//
// Uso:
//   k6 run docs/rendimiento/k6-botica.js
//   k6 run -e BASE_URL=http://localhost:8080 -e USUARIO=admin@botica.pe -e PASSWORD=admin123 docs/rendimiento/k6-botica.js
//
// Requiere que la API esté levantada y con datos de catálogo (producto + lote con stock)
// para que el registro de venta pueda completarse.

import http from 'k6/http';
import { check, group, sleep } from 'k6';
import { Trend, Rate } from 'k6/metrics';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const USUARIO  = __ENV.USUARIO  || 'admin@botica.pe';
const PASSWORD = __ENV.PASSWORD || 'admin123';
const LOTE_ID  = Number(__ENV.LOTE_ID || 1); // lote con stock para la venta

// Métricas de negocio específicas del proyecto
const duracionLogin    = new Trend('duracion_login', true);
const duracionConsulta = new Trend('duracion_consulta_ventas', true);
const duracionRegistro = new Trend('duracion_registro_venta', true);
const tasaErroresNegocio = new Rate('errores_negocio');

// Escenario escalonado: sube a 30 usuarios virtuales, sostiene la carga y baja.
export const options = {
  scenarios: {
    concurrencia_botica: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: '20s', target: 10 }, // calentamiento
        { duration: '40s', target: 30 }, // carga objetivo
        { duration: '20s', target: 30 }, // sostenido
        { duration: '10s', target: 0 },  // enfriamiento
      ],
      gracefulRampDown: '10s',
    },
  },
  thresholds: {
    // Criterios de aceptación (SLA) del proyecto
    http_req_failed: ['rate<0.02'],           // < 2% de peticiones fallidas
    http_req_duration: ['p(95)<800'],         // p95 por debajo de 800 ms
    'duracion_login': ['p(95)<900'],
    'duracion_consulta_ventas': ['p(95)<600'],
    'duracion_registro_venta': ['p(95)<1000'],
    errores_negocio: ['rate<0.05'],
  },
};

function loginYTomarToken() {
  const res = http.post(
    `${BASE_URL}/api/auth/login`,
    JSON.stringify({ correo: USUARIO, password: PASSWORD }),
    { headers: { 'Content-Type': 'application/json' }, tags: { flujo: 'login' } },
  );
  duracionLogin.add(res.timings.duration);
  const ok = check(res, {
    'login 200': (r) => r.status === 200,
    'login devuelve token': (r) => !!r.json('token'),
  });
  if (!ok) {
    tasaErroresNegocio.add(1);
    return null;
  }
  return res.json('token');
}

export default function () {
  // 1) LOGIN
  let token = null;
  group('login', () => {
    token = loginYTomarToken();
  });
  if (!token) {
    sleep(1);
    return;
  }
  const authHeaders = {
    headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${token}` },
  };

  sleep(Math.random() * 2 + 1); // tiempo de "pensar" del usuario

  // 2) CONSULTA de ventas (con búsqueda)
  group('consulta_ventas', () => {
    const res = http.get(`${BASE_URL}/api/ventas?q=`, {
      ...authHeaders,
      tags: { flujo: 'consulta' },
    });
    duracionConsulta.add(res.timings.duration);
    check(res, { 'consulta 200': (r) => r.status === 200 });
  });

  sleep(Math.random() * 2 + 1);

  // 3) REGISTRO de venta
  group('registro_venta', () => {
    const body = JSON.stringify({
      tipoComprobante: 'BOLETA',
      metodoPago: 'EFECTIVO',
      recetaPresentada: false,
      items: [{ loteId: LOTE_ID, cantidad: 1 }],
    });
    const res = http.post(`${BASE_URL}/api/ventas`, body, {
      ...authHeaders,
      tags: { flujo: 'registro' },
    });
    duracionRegistro.add(res.timings.duration);
    // 201 = registrada; 409 = sin stock (se acepta como respuesta válida del servidor bajo carga)
    check(res, { 'registro 201 o 409': (r) => r.status === 201 || r.status === 409 });
  });

  sleep(Math.random() * 3 + 1);
}
