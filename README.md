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

## Migraciones

Están en `src/main/resources/db/migration` y las aplica Flyway al arrancar.
Nunca uses `ddl-auto=update`: el esquema se versiona con SQL.

## Tests

```powershell
.\gradlew.bat test
```

Los tests usan el perfil `test` con H2 en memoria, así que **no necesitan
PostgreSQL**. Cubren el flujo completo de autenticación (registro, duplicado,
401, perfil, login fallido, rotación de refresh y logout).

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
├─ config/   SecurityConfig, JwtProperties
├─ common/   ApiException, GlobalExceptionHandler
├─ auth/     JWT, refresh tokens, /auth
└─ user/     entidad User, /me
```
