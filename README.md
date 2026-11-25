# LevelUp — Backend

API REST para la app de productividad académica LevelUp.

## Stack

- Spring Boot 4.2 · Java 21 (Gradle toolchain con auto-descarga vía foojay)
- Spring Data JPA + PostgreSQL 17 + Flyway
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

## Endpoints disponibles (Etapa 0)

| Método | Ruta | Descripción |
|---|---|---|
| POST | `/api/v1/auth/register` | Crea la cuenta e inicia sesión |
| POST | `/api/v1/auth/login` | Inicia sesión |
| POST | `/api/v1/auth/refresh` | Rota el refresh token y devuelve un access token |
| POST | `/api/v1/auth/logout` | Revoca el refresh token |
| GET | `/api/v1/me` | Perfil del usuario autenticado |
| PATCH | `/api/v1/me` | Actualiza nombre, zona horaria o tema |
| GET | `/actuator/health` | Estado del servicio |

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

## Estructura

```
com.levelup.levelup
├─ config/   SecurityConfig, JwtProperties
├─ common/   ApiException, GlobalExceptionHandler
├─ auth/     JWT, refresh tokens, /auth
└─ user/     entidad User, /me
```
