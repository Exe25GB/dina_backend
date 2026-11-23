# DINA Backend

API REST de **DINA**: reserva de espacios de la sede Viña del Mar por bloques de 1 hora.
El usuario pide uno o varios bloques seguidos de un espacio, un administrador aprueba o rechaza,
y el usuario recibe una notificación.

Spring Boot 4 · Java 17 · JPA · H2 (por defecto) o MySQL.

## Ejecutar

```bash
./mvnw spring-boot:run
```

Queda en `http://localhost:8080`. Comprobar: `GET /api/salud`.

Por defecto usa **H2** en el archivo `./data/dina` (no hay que instalar nada; los datos se mantienen al reiniciar).
Para usar **MySQL**:

```bash
SPRING_PROFILES_ACTIVE=mysql DB_URL="jdbc:mysql://HOST:3306/dina_db?createDatabaseIfNotExist=true" DB_USER=root DB_PASSWORD=*** java -jar target/dina_backend-0.0.1-SNAPSHOT.jar
```

En **EC2** (guía del curso): `mvn clean package -DskipTests` y luego
`nohup java -jar target/dina_backend-0.0.1-SNAPSHOT.jar > app.log 2>&1 &`.

## Datos de prueba

Se crean solos al iniciar, solo si faltan (si ya existen, solo se corrige el nombre).

| Correo | Nombre | Rol | Tipo de cuenta | Contraseña |
|---|---|---|---|---|
| exe.guzman@duocuc.cl | Exequiel Guzman | ADMIN | Estudiante | `duoc1234` |
| vict.godoy@duocuc.cl | Victor Godoy | USUARIO | Estudiante | `duoc1234` |
| nan.bernal@profesor.duoc.cl | Nancy Beatriz Bernal | USUARIO | Docente | `duoc1234` |

El tipo de cuenta (Estudiante / Docente) lo deduce la app del dominio del correo.

La contraseña se cambia con la variable `DINA_SEED_PASSWORD` antes del primer inicio. **Cámbiala antes de publicar el backend.**

**Bloques:** 14 bloques de 1 hora, de 08:00 a 22:00. Lunes a viernes todos; sábado hasta las 14:00; domingo cerrado.

**Espacios (79)**, según `MAPA.pdf`:

| Edificio | Espacios |
|---|---|
| Norte | S11–S13, Lab. Industrial, Lab. Diseño, S21–S23, Set TV, Estudio Sonido 1, S31–S39, S41–S49 |
| Central | S53–S54, Estudio Sonido 2, Sala Post-producción, Multicancha 1, Anfiteatro |
| Sur | Sala de Actuación, Multicancha 2, S61–S65, S71–S76, S81–S88, Gimnasio |
| CTI | LC1–LC12, S302, Espacio Cowork + CIIT, S601–S609 |

Capacidad, equipamiento, escuela y pisos del Edificio Central son **supuestos** para pruebas (el mapa no los indica).
Se editan en `config/DataInitializer.java`.

## Reglas de negocio

- Solo cuentas `@duocuc.cl` y `@profesor.duoc.cl` (`dina.dominios-permitidos`).
- Una solicitud junta de 1 a 4 bloques **seguidos** (`dina.solicitudes.max-bloques`).
- Hasta 30 días de anticipación (`dina.solicitudes.max-dias-anticipacion`); no en el pasado ni en bloques que ya comenzaron.
- Asistentes ≤ capacidad del espacio.
- No se puede pedir un bloque ya **aprobado** para ese espacio; un usuario no puede tener dos solicitudes activas en el mismo horario.
- Varias solicitudes **pendientes** pueden competir por el mismo bloque. Al **aprobar** una, las que chocan se **rechazan solas** y se notifica a sus usuarios.
- El usuario puede cancelar su solicitud pendiente o aprobada mientras no haya comenzado.

## Autenticación

`POST /api/auth/login` devuelve un `token`. Todas las demás rutas (salvo `/api/salud`) exigen:

```
Authorization: Bearer <token>
```

El token dura 7 días (`dina.sesion.dias`). Las rutas `/api/admin/**` exigen rol `ADMIN`.

## Endpoints

| Método | Ruta | Descripción |
|---|---|---|
| POST | `/api/auth/login` | `{correo, password}` → `{token, expiraEn, usuario}` |
| GET | `/api/auth/me` | Usuario del token |
| POST | `/api/auth/logout` | Invalida el token |
| GET | `/api/edificios` | Lista de edificios |
| GET | `/api/bloques` | Los 14 bloques |
| GET | `/api/espacios` | Filtros opcionales: `edificio`, `tipo`, `capacidadMin`, `proyector`, `computadores`, `climatizacion` |
| GET | `/api/espacios/{id}` | Detalle |
| GET | `/api/espacios/{id}/disponibilidad?fecha=AAAA-MM-DD` | Estado de cada bloque: `DISPONIBLE`, `OCUPADO`, `NO_DISPONIBLE`, más pendientes y si es tuyo |
| POST | `/api/solicitudes` | `{espacioId, fecha, bloqueIds[], actividad, asistentes}` → queda `PENDIENTE` |
| GET | `/api/solicitudes/mias` | Mis solicitudes |
| PATCH | `/api/solicitudes/{id}/cancelar` | Cancelar la propia |
| GET | `/api/admin/solicitudes?estado=PENDIENTE` | (ADMIN) Listar; sin `estado` = todas |
| PATCH | `/api/admin/solicitudes/{id}/aprobar` | (ADMIN) Cuerpo opcional `{comentario}` |
| PATCH | `/api/admin/solicitudes/{id}/rechazar` | (ADMIN) `{motivo}` obligatorio |
| GET | `/api/notificaciones` | Mis notificaciones |
| GET | `/api/notificaciones/no-leidas` | `{noLeidas}` |
| PATCH | `/api/notificaciones/{id}/leida` | Marcar una como leída |
| PATCH | `/api/notificaciones/leer-todas` | Marcar todas |

Errores, siempre con la misma forma:

```json
{ "estado": 409, "error": "CONFLICT", "mensaje": "El espacio ya está reservado en ese horario", "fecha": "..." }
```

## Notificaciones

Se guardan en la tabla `notificaciones` cuando:
- llega una solicitud nueva → a todos los ADMIN;
- una solicitud se aprueba o se rechaza → al usuario que la pidió.

La app las lee con `GET /api/notificaciones`. Las notificaciones **push** (Firebase Cloud Messaging) aún no están:
el punto para agregarlas es `NotificacionService.notificar(...)`.

## Estructura

```
cl.duoc.dina_backend
├── config/       DinaProperties, DataInitializer (datos de prueba), SeguridadConfig (BCrypt)
├── controller/   Auth, Espacio, Solicitud, AdminSolicitud, Notificacion, Salud
├── dto/          Dtos (records de entrada y salida)
├── exception/    ApiException, GlobalExceptionHandler
├── model/        Usuario, Espacio, BloqueHorario, Solicitud, Notificacion, SesionToken + enums
├── repository/   Spring Data JPA
├── security/     AuthInterceptor (token), WebConfig, @UsuarioActual
└── service/      AuthService, EspacioService, HorarioService, SolicitudService, NotificacionService
```

## Pruebas

```bash
./mvnw test
```

`SolicitudFlujoTest` cubre login, reglas de bloques, aprobación con rechazo automático de cruces, notificaciones y cancelación.
