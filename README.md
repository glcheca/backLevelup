# LevelUp — Backend

API REST para la app de productividad académica LevelUp.

## Stack

- Spring Boot 4.2 · Java 21 (Gradle toolchain con auto-descarga vía foojay)
- Spring Data JPA + PostgreSQL (17+) + Flyway
- Spring Security con JWT de acceso + refresh token en cookie `HttpOnly`
- Gradle Wrapper (no requiere Maven)

## Requisitos

- JDK 21 o superior (Gradle descarga el toolchain si falta)
- PostgreSQL 17 (local o vía Docker)
- Docker Desktop solo si prefieres levantar la base con `docker compose`

## Puesta en marcha

1. Copia las variables de entorno:

   ```powershell
   Copy-Item .env.example .env
   ```

2. Levanta PostgreSQL (elige una opción):

   ```powershell
   # Opción A: Docker
   docker compose up -d

   # Opción B: PostgreSQL local
   # Crea la base de datos y el usuario indicados en .env

   # Opción C: H2 en memoria (solo para desarrollo, sin PostgreSQL)
   # No aplica migraciones Flyway; el esquema lo crea Hibernate.
   .\gradlew.bat bootRun --args="--spring.profiles.active=h2"
   ```

3. Arranca la API (si no usaste la opción C):

   ```powershell
   .\gradlew.bat bootRun
   ```

4. Comprueba el estado:

   ```powershell
   curl http://localhost:8080/actuator/health
   ```

La API queda en `http://localhost:8080` y todas las rutas cuelgan de `/api/v1`.

## Endpoints disponibles

Etapa 0 — autenticación y perfil:

| Método | Ruta | Descripción |
|---|---|---|
| POST | `/api/v1/auth/register` | Crea la cuenta e inicia sesión |
| POST | `/api/v1/auth/login` | Inicia sesión |
| POST | `/api/v1/auth/refresh` | Rota el refresh token y devuelve un access token |
| POST | `/api/v1/auth/logout` | Revoca el refresh token |
| GET | `/api/v1/me` | Perfil del usuario autenticado |
| PATCH | `/api/v1/me` | Actualiza nombre, zona horaria o tema |
| GET | `/actuator/health` | Estado del servicio |

Etapa 1 — núcleo académico:

| Método | Ruta | Descripción |
|---|---|---|
| GET | `/api/v1/courses?archived=&page=&size=` | Lista paginada de cursos |
| POST | `/api/v1/courses` | Crea un curso |
| GET | `/api/v1/courses/{id}` | Detalle con horario |
| PATCH | `/api/v1/courses/{id}` | Actualización parcial |
| PATCH | `/api/v1/courses/{id}/archive` | Archiva o desarchiva (`{archived}`) |
| DELETE | `/api/v1/courses/{id}` | Borra el curso (eventos y tareas quedan sin curso) |
| GET | `/api/v1/courses/{id}/schedule` | Bloques de horario del curso |
| POST | `/api/v1/courses/{id}/schedule` | Crea un bloque (devuelve `warning` si solapa) |
| GET | `/api/v1/schedule` | Horario completo del usuario |
| PATCH | `/api/v1/schedule/{blockId}` | Edita un bloque |
| DELETE | `/api/v1/schedule/{blockId}` | Borra un bloque |
| GET | `/api/v1/events?from=&to=&courseId=&type=` | Eventos por rango (solapamiento) |
| POST/PATCH/DELETE | `/api/v1/events[/{id}]` | CRUD de eventos (`all_day` se normaliza a la TZ del usuario) |
| GET | `/api/v1/tasks?status=&courseId=&dueBefore=` | Tareas filtradas |
| POST/PATCH/DELETE | `/api/v1/tasks[/{id}]` | CRUD de tareas (PATCH recibe el objeto completo) |
| PATCH | `/api/v1/tasks/{id}/complete` | Marca como hecha y fija `completed_at` |

Etapa 2 — apuntes y calificaciones:

| Método | Ruta | Descripción |
|---|---|---|
| GET | `/api/v1/notes?courseId=&tag=&q=&pinned=&page=&size=` | Apuntes filtrados (búsqueda sin tildes) |
| POST | `/api/v1/notes` | Crea un apunte |
| GET | `/api/v1/notes/{id}` | Detalle con el markdown completo |
| PATCH | `/api/v1/notes/{id}` | Actualiza (recibe el objeto completo) |
| PATCH | `/api/v1/notes/{id}/pin` | Fija o desfija (`{pinned}`) |
| DELETE | `/api/v1/notes/{id}` | Borra el apunte |
| GET | `/api/v1/notes/tags` | Etiquetas distintas del usuario |
| GET | `/api/v1/courses/{id}/grades` | Evaluaciones del curso |
| POST | `/api/v1/courses/{id}/grades` | Crea una evaluación |
| PATCH/DELETE | `/api/v1/grades[/{id}]` | Edita o borra una evaluación |
| GET | `/api/v1/courses/{id}/grades/summary?target=14` | Promedio, peso pendiente y "qué necesito" |
| GET | `/api/v1/grades/summary?target=14` | Resumen de todos los cursos activos |

Etapa 3 — recordatorios, notificaciones y búsqueda:

| Método | Ruta | Descripción |
|---|---|---|
| GET | `/api/v1/reminders?status=` | Recordatorios propios (pending, sent, failed, cancelled) |
| POST | `/api/v1/reminders` | Crea un recordatorio propio o vinculado a evento/tarea |
| DELETE | `/api/v1/reminders/{id}` | Borra un recordatorio |
| GET | `/api/v1/notifications?unreadOnly=&page=&size=` | Notificaciones in-app paginadas |
| GET | `/api/v1/notifications/unread-count` | `{ unread }` para el badge |
| PATCH | `/api/v1/notifications/{id}/read` | Marca una como leída |
| PATCH | `/api/v1/notifications/read-all` | Marca todas como leídas |
| GET | `/api/v1/search?q=` | Búsqueda global (cursos, eventos, tareas, apuntes; máx. 5 por tipo) |
| POST | `/api/v1/me/password` | Cambia la contraseña validando la actual |
| DELETE | `/api/v1/me` | Borra la cuenta y todo su contenido |

## Variables de entorno

| Variable | Por defecto | Descripción |
|---|---|---|
| `DB_URL` | `jdbc:postgresql://localhost:5432/levelup` | Conexión JDBC |
| `DB_USER` / `DB_PASSWORD` | `levelup` | Credenciales |
| `JWT_SECRET` | secreto de desarrollo | Mínimo 32 caracteres |
| `JWT_ACCESS_TTL` | `PT15M` | Vida del access token |
| `JWT_REFRESH_TTL` | `P30D` | Vida del refresh token |
| `CORS_ORIGINS` | `http://localhost:5173` | Orígenes permitidos |
| `COOKIE_SECURE` | `false` | `true` en producción con HTTPS |
| `REMINDER_POLL_INTERVAL_MS` | `30000` | Frecuencia del job de recordatorios |
| `SEED_ENABLED` | `false` | Siembra los datos demo (el perfil `dev` lo activa) |
| `RATE_LIMIT_ENABLED` | `true` | Límite por IP en login, registro y cambio de contraseña |
| `RATE_LIMIT_WINDOW_SECONDS` | `60` | Ventana deslizante del límite |
| `RATE_LIMIT_MAX_ATTEMPTS` | `10` | Intentos por ventana en login/registro |
| `RATE_LIMIT_MAX_PASSWORD_ATTEMPTS` | `5` | Intentos por ventana en cambio de contraseña |

## Perfiles

| Perfil | Uso | Particularidades |
|---|---|---|
| `default` | Desarrollo con PostgreSQL | Flyway, secretos de desarrollo |
| `dev` | Demos y pruebas manuales | Activa `app.seed.enabled` y siembra los datos demo |
| `h2` | Pruebas sin PostgreSQL | Esquema creado por Hibernate, datos efímeros |
| `test` | Tests automáticos | H2, rate limit desactivado, secreto de prueba |
| `prod` | Despliegue | Sin valores por defecto para `JWT_SECRET` ni `CORS_ORIGINS`; cookies `Secure` |

### Datos demo

`DemoDataSeeder` se ejecuta al arrancar con el perfil `dev` (o `SEED_ENABLED=true`) y es
idempotente: si el usuario ya existe no vuelve a sembrar nada.

| Email | Contraseña |
|---|---|
| `demo@levelup.test` | `Demo1234!` |

Crea 5 cursos con horario, 20 eventos, 15 tareas, 10 apuntes y 17 evaluaciones.

## Seguridad

- Contraseñas con BCrypt; JWT firmado con HS384.
- Rate limit en memoria por IP para `POST /auth/login`, `POST /auth/register` y
  `POST /me/password` (responde `429` con `Retry-After`).
- Cabeceras: `X-Frame-Options: DENY`, `X-Content-Type-Options: nosniff`,
  `Referrer-Policy: no-referrer`, `Cache-Control: no-store` en respuestas autenticadas y HSTS
  cuando la petición llega por HTTPS.
- Errores con `ProblemDetail` sin filtrar detalles internos.

## Despliegue

- `Dockerfile`: build multi-stage (`bootJar`) sobre `eclipse-temurin:21-jre` con usuario sin
  privilegios.
- `docker-compose.prod.yml`: PostgreSQL + backend + frontend + Caddy.
- `deploy/Caddyfile`: HTTPS automático para `DOMAIN`.
- `application-prod.yml`: perfil de producción; el arranque falla si faltan secretos.

```powershell
Copy-Item .env.prod.example .env.prod
docker compose -f docker-compose.prod.yml up -d --build
```

> El compose no se pudo ejecutar en la máquina de desarrollo (Docker no instalado); queda como
> artefacto listo para probar en el servidor.


## Migraciones

Están en `src/main/resources/db/migration` y las aplica Flyway al arrancar.
Nunca uses `ddl-auto=update`: el esquema se versiona con SQL.

## Tests

```powershell
.\gradlew.bat test
```

Los tests usan el perfil `test` con H2 en memoria, así que **no necesitan
PostgreSQL**. Son 66 tests que cubren autenticación (registro, duplicado, 401,
perfil, login fallido, rotación de refresh y logout), cursos y horario, eventos,
tareas, apuntes (búsqueda con tildes, etiquetas, aislamiento por usuario),
calificaciones (promedio ponderado, pesos incompletos, proyección y validación),
Etapa 3 (recordatorios, job con reloj fijo, notificaciones, búsqueda global,
cambio de contraseña y borrado de cuenta) y Etapa 4 (rate limit y seed demo).

## Calidad de código

```powershell
.\gradlew.bat spotlessApply        # formatea (google-java-format) + quita imports sin usar
.\gradlew.bat spotlessCheck        # verifica el formato
.\gradlew.bat checkstyleMain checkstyleTest   # reglas en config/checkstyle/checkstyle.xml
```

`gradlew build` ejecuta `spotlessCheck` y `checkstyle` junto con los tests, así que
el CI falla si el formato o el estilo no se cumplen.

## Estructura

```
com.levelup.levelup
├─ config/     SecurityConfig, JwtProperties, RateLimitFilter
├─ common/     ApiException, GlobalExceptionHandler, PageResponse, TextNormalizer
├─ auth/       JWT, refresh tokens, /auth
├─ user/       entidad User, /me
├─ course/     curso + horario (class_block)
├─ event/      eventos
├─ task/       tareas y subtareas
├─ note/       apuntes y etiquetas (búsqueda normalizada)
├─ grade/      evaluaciones y promedio ponderado
├─ reminder/   recordatorios, job de despacho y hook de eventos/tareas
├─ notification/ notificaciones in-app
├─ search/     búsqueda global (cursos, eventos, tareas, apuntes)
└─ seed/       datos demo para el perfil dev
```
