# GuÃ­a del Ejecutable â€” Sistema de GestiÃ³n Integral Botica Q&M

**Curso:** Desarrollo Web Integrado Â· **SecciÃ³n:** 38447
**Profesor:** Ramires Pacheco, Luis Enrrique

Instrucciones para instalar, configurar y ejecutar el **backend** (API REST) y el **frontend**
(interfaz web) del sistema, asÃ­ como para correr las pruebas.

---

## Ãndice

1. [Requisitos previos](#1-requisitos-previos)
2. [Estructura del proyecto](#2-estructura-del-proyecto)
3. [Base de datos](#3-base-de-datos)
4. [Ejecutar el backend](#4-ejecutar-el-backend)
5. [Ejecutar el frontend](#5-ejecutar-el-frontend)
6. [Uso del sistema](#6-uso-del-sistema)
7. [Ejecutar las pruebas](#7-ejecutar-las-pruebas)
8. [Compilar para producciÃ³n](#8-compilar-para-producciÃ³n)
9. [SoluciÃ³n de problemas](#9-soluciÃ³n-de-problemas)

---

## 1. Requisitos previos

| Herramienta | VersiÃ³n mÃ­nima | Notas |
|---|---|---|
| **JDK** | 17 | `java -version` |
| **Maven** | 3.9 | O usar el wrapper `mvnw` si estÃ¡ presente |
| **Node.js** | 18 LTS | Incluye `npm` |
| **PostgreSQL** | 13+ | Local o en la nube (p. ej. Supabase) |
| **Git** | â€” | Para clonar el repositorio (opcional) |

> Opcional para pruebas de rendimiento: **k6** (`winget install grafana.k6`).

VerificaciÃ³n rÃ¡pida:

```bash
java -version
mvn -version
node -v
npm -v
```

---

## 2. Estructura del proyecto

```
Botica Q&M/
â”œâ”€â”€ backend/botica-api (10)/botica-api/   â†’ API REST (Spring Boot)
â”‚   â”œâ”€â”€ pom.xml
â”‚   â”œâ”€â”€ sql/                               â†’ scripts de migraciÃ³n
â”‚   â””â”€â”€ src/main/java/com/botica/â€¦
â”œâ”€â”€ frontend/botica-frontend (9)/botica-frontend/  â†’ SPA (React + Vite)
â”‚   â”œâ”€â”€ package.json
â”‚   â””â”€â”€ src/â€¦
â””â”€â”€ docs/                                  â†’ manuales y guÃ­as
```

> Las rutas contienen espacios y parÃ©ntesis. En PowerShell, **encierra siempre las rutas entre
> comillas** y usa el operador de llamada `&` para ejecutables con ruta entre comillas.

---

## 3. Base de datos

1. Crea una base de datos llamada `botica` en PostgreSQL.
2. Crea el **esquema** con los scripts de la carpeta `sql/` (el proyecto usa `ddl-auto: none`, es
   decir, el esquema se administra por SQL, no lo genera Hibernate).
3. Ejecuta las **migraciones** que correspondan segÃºn tu versiÃ³n, por ejemplo:

```bash
psql -h <host> -U postgres -d botica -f "backend/botica-api (10)/botica-api/sql/migracion_venta_compra_anulacion.sql"
```

> Si usas Supabase, copia la *connection string* y aÃ±ade `?sslmode=require`.

---

## 4. Ejecutar el backend

### 4.1 Configurar variables de entorno

**Windows (PowerShell):**
```powershell
$env:DB_URL      = "jdbc:postgresql://<HOST>:<PUERTO>/<BASE_DE_DATOS>"
$env:DB_USER     = "postgres"
$env:DB_PASSWORD = "tu_password"
$env:JWT_SECRET  = "una-clave-larga-de-al-menos-32-caracteres-123"
$env:CORS_ORIGINS = "http://localhost:5173"
```

**Linux/macOS (bash):**
```bash
export DB_URL="jdbc:postgresql://<HOST>:<PUERTO>/<BASE_DE_DATOS>"
export DB_USER="postgres"
export DB_PASSWORD="tu_password"
export JWT_SECRET="una-clave-larga-de-al-menos-32-caracteres-123"
export CORS_ORIGINS="http://localhost:5173"
```

> Si no defines variables, la aplicaciÃ³n usa valores por defecto (`localhost:5432/botica`,
> usuario/clave `postgres`). **Cambia `JWT_SECRET` en producciÃ³n.**

### 4.2 Arrancar

```bash
# Desde la carpeta del backend
mvn spring-boot:run
```

La API queda disponible en **http://localhost:8080/api**.

### 4.3 Construir y ejecutar el JAR

```bash
mvn clean package
java -jar target/botica-api-1.0.0.jar
```

### 4.4 Comprobar que responde

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d "{\"correo\":\"admin@botica.pe\",\"password\":\"admin123\"}"
```

Debe devolver un JSON con `token` y `usuario`.

---

## 5. Ejecutar el frontend

```bash
# Desde la carpeta del frontend
npm install        # solo la primera vez
npm run dev
```

La interfaz queda disponible en **http://localhost:5173**.

### 5.1 Apuntar a otra URL de API (opcional)

Por defecto el frontend consume `http://localhost:8080/api`. Para cambiarla, crea un archivo
`.env` en la carpeta del frontend:

```
VITE_API_URL=http://localhost:8080/api
```

---

## 6. Uso del sistema

1. Abre **http://localhost:5173**.
2. Inicia sesiÃ³n con un usuario existente (por ejemplo `admin@botica.pe` / `admin123`, si tu base
   ya tiene ese usuario). Si la base estÃ¡ vacÃ­a, crÃ©alo con el script/consola de usuarios de tu
   entorno.
3. Navega con el menÃº lateral: Dashboard, Nueva venta, Historial, Compras, Productos, Lotes,
   Laboratorios, Proveedores, Usuarios y Ayuda.
4. Pulsa el botÃ³n flotante **ðŸ’¬** para abrir el asistente de ayuda.

> El detalle funcional de cada pantalla estÃ¡ en [`manual-de-usuario.md`](./manual-de-usuario.md).

---

## 7. Ejecutar las pruebas

### Backend
```bash
# Desde backend/botica-api (10)/botica-api
mvn test
```
- Usa **H2 en memoria** (no toca la base real).
- Al terminar, el reporte de cobertura queda en `target/site/jacoco/index.html`.

### Frontend
```bash
# Desde frontend/botica-frontend (9)/botica-frontend
npm test            # una sola pasada
npm run test:watch  # modo vigilancia
npm run test:coverage
```
El reporte de cobertura se genera en `coverage/`.

### Rendimiento (k6, opcional)
```bash
# Con el backend levantado
k6 run docs/rendimiento/k6-botica.js
```

---

## 8. Compilar para producciÃ³n

### Backend
```bash
mvn clean package     # genera target/botica-api-1.0.0.jar
```

### Frontend
```bash
npm run build         # genera la carpeta dist/
npm run preview       # sirve localmente el build para verificarlo
```

Sirve el contenido de `dist/` con cualquier servidor web (Nginx, IIS, `serve`, etc.) y asegÃºrate
de que `VITE_API_URL` apunte al backend de producciÃ³n.

---

## 9. SoluciÃ³n de problemas

| SÃ­ntoma | Causa probable | SoluciÃ³n |
|---|---|---|
| `Connection refused` al iniciar el backend | PostgreSQL no estÃ¡ corriendo o la URL/credenciales son incorrectas | Verifica `DB_URL`, `DB_USER`, `DB_PASSWORD` y que el servicio estÃ© activo |
| `401` en todas las llamadas | Falta el token o estÃ¡ vencido | Inicia sesiÃ³n de nuevo; el frontend aÃ±ade el token automÃ¡ticamente |
| `403` al crear/editar catÃ¡logos | El usuario no es ADMIN | Usa un usuario con rol ADMINISTRADOR |
| `CORS error` en el navegador | Origen no permitido | Ajusta `CORS_ORIGINS` al origen del frontend |
| `npm run build` falla por rutas con espacios | Bug de shims de npm en Windows | Usa `node node_modules/vite/bin/vite.js build` directamente |
| Errores `IllegalClassFormatException` de JaCoCo en `mvn test` | Ruido al instrumentar clases del JDK muy nuevo | Son avisos; el build y las pruebas no se afectan |
| El login dice "correo o contraseÃ±a incorrectos" | Usuario inexistente o clave distinta | Verifica/crea el usuario en la tabla `usuario` |

---

## Comandos rÃ¡pidos (resumen)

```bash
# Backend
cd "backend/botica-api (10)/botica-api"
mvn spring-boot:run          # desarrollo
mvn test                     # pruebas + cobertura
mvn clean package            # jar de producciÃ³n

# Frontend
cd "frontend/botica-frontend (9)/botica-frontend"
npm install && npm run dev   # desarrollo
npm test                     # pruebas
npm run build                # build de producciÃ³n
```

