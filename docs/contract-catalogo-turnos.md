# Contrato de integracion: Servicio-catalogo -> servicio-turnos

Version: 1.0
Estado: vigente
Servicio proveedor: `Servicio-catalogo`
Servicio consumidor: `servicio-turnos`

## 1. Objetivo

Definir como `servicio-turnos` consulta la informacion vigente de categorias, profesionales y horarios semanales para construir la disponibilidad y validar reservas.

El catalogo es una copia local sincronizada desde la catedra. `servicio-turnos` no debe replicar el catalogo ni consultar directamente a la catedra para obtener profesionales u horarios.

## 2. Decisiones de arquitectura

- `servicio-turnos` consulta directamente a `Servicio-catalogo` por HTTP REST.
- Para disponibilidad, KMP consulta a `servicio-turnos`; no consulta el catalogo para calcular slots.
- La busqueda de profesionales desde KMP queda fuera de este contrato. Puede resolverse contra los endpoints de lectura del catalogo con JWT `USER` o a traves de `servicio-turnos`, siempre sobre la copia local del catalogo.
- `servicio-turnos` es el emisor de los JWT de los usuarios finales.
- `servicio-turnos` tambien emite el JWT de servicio usado en llamadas internas al catalogo.
- `Servicio-catalogo` solo valida JWT; no emite tokens.
- No se crea un `InternalCatalogController`: se reutilizan los endpoints `/api/**` ya existentes.
- El JWT tecnico de la catedra (`CATEDRA_JWT_TOKEN`) nunca se envia al catalogo ni a KMP.

## 3. Autenticacion

Todas las llamadas de `servicio-turnos` al catalogo deben incluir:

```http
Authorization: Bearer <jwt>
```

El JWT debe cumplir:

- Firma HS256 con el secreto configurado en `APP_JWT_SECRET`.
- Emisor igual a `APP_JWT_ISSUER` (por defecto `servicio-turnos`).
- Token vigente (`exp` mayor al instante actual).
- Claim `roles` con el valor `SERVICE`.

Ejemplo de payload del token de servicio:

```json
{
  "sub": "servicio-turnos",
  "iss": "servicio-turnos",
  "roles": ["SERVICE"],
  "iat": 1780000000,
  "exp": 1780003600
}
```

Los endpoints de lectura aceptan `USER`, `SERVICE` o `ADMIN`. Para integracion entre servicios se debe usar `SERVICE`.

`servicio-turnos` y `Servicio-catalogo` deben compartir el mismo `APP_JWT_SECRET`. El secreto debe tener al menos 32 bytes.

El token de servicio no propaga la identidad del usuario final. El catalogo solo devuelve datos de catalogo, no datos de usuarios ni reservas. La asociacion usuario-reserva y la trazabilidad se mantienen en `servicio-turnos`, que conserva el `sub` del JWT de usuario. Por ese motivo las llamadas al catalogo no necesitan `externalPatientId` ni un identificador de usuario.

## 4. URL base

- Desarrollo local: `http://localhost:8081`
- Desde `servicio-turnos` con Compose independiente: `http://host.docker.internal:8081`
- Docker Compose en una red compartida: `http://servicio-catalogo:8081`

## 5. Endpoints del contrato

### 5.1. `GET /api/categories`

Devuelve las categorias disponibles en la copia local del catalogo.

Parametros:

| Parametro | Tipo | Obligatorio | Descripcion |
| --- | --- | --- | --- |
| `enabled` | boolean | No | Si se omite devuelve todas las categorias. `true` o `false` filtra por estado. |

Respuesta `200`:

```json
[
  {
    "id": 3,
    "name": "Cardiologia",
    "description": "Consultas cardiologicas",
    "enabled": true,
    "createdAt": "2026-09-01T10:00:00Z",
    "updatedAt": "2026-09-02T11:30:00Z"
  }
]
```

Sin filtro, las categorias se devuelven ordenadas por `name`.

Uso esperado en turnos: filtros de busqueda y datos descriptivos de la categoria.

### 5.2. `GET /api/professionals`

Busca profesionales en la copia local con paginacion.

Parametros:

| Parametro | Tipo | Obligatorio | Descripcion |
| --- | --- | --- | --- |
| `categoryId` | integer int64 | No | Filtra por categoria. Debe ser positivo. |
| `name` | string | No | Busca coincidencia parcial, sin distinguir mayusculas, en `firstName` o `lastName`. |
| `enabled` | boolean | No | Si se omite usa `true`. `false` devuelve profesionales deshabilitados. |
| `dayOfWeek` | enum | No | Dia ISO: `MONDAY`, `TUESDAY`, `WEDNESDAY`, `THURSDAY`, `FRIDAY`, `SATURDAY`, `SUNDAY`. |
| `time` | hora | No | Formato `HH:mm` o `HH:mm:ss`. |
| `page` | integer | No | Pagina solicitada, base 0. Default `0`. |
| `size` | integer | No | Tamano de pagina. Default `20`. |
| `sort` | string | No | Formato `propiedad,asc` o `propiedad,desc`. Default `lastName,asc`. |

Cuando se envia `dayOfWeek` o `time`, solo se consideran horarios con `enabled = true`. Si se envian ambos, el horario debe cumplir el dia y el rango:

```text
startTime <= time < endTime
```

Respuesta `200`:

```json
{
  "content": [
    {
      "id": 15,
      "firstName": "Ana",
      "lastName": "Gomez",
      "categoryId": 3,
      "categoryName": "Cardiologia",
      "enabled": true,
      "createdAt": "2026-09-01T10:00:00Z",
      "updatedAt": "2026-09-02T11:30:00Z"
    }
  ],
  "totalElements": 1,
  "totalPages": 1,
  "last": true,
  "first": true,
  "size": 20,
  "number": 0,
  "numberOfElements": 1,
  "empty": false
}
```

La respuesta es un `Page` de Spring Data. Ademas de `content`, incluye metadatos estandar de paginacion (`totalElements`, `totalPages`, `number`, `size`, `first`, `last`, `numberOfElements`, `empty`). Los consumidores deben usar `content` y los metadatos, sin depender del detalle interno del objeto `pageable`.

Uso esperado en turnos: busqueda de profesionales y validacion de que el profesional siga vigente antes de iniciar una reserva.

### 5.3. `GET /api/professionals/{id}`

Devuelve el detalle de un profesional.

Respuesta `200`:

```json
{
  "id": 15,
  "firstName": "Ana",
  "lastName": "Gomez",
  "categoryId": 3,
  "categoryName": "Cardiologia",
  "enabled": true,
  "createdAt": "2026-09-01T10:00:00Z",
  "updatedAt": "2026-09-02T11:30:00Z",
  "schedules": [
    {
      "id": 41,
      "professionalId": 15,
      "dayOfWeek": "MONDAY",
      "startTime": "09:00:00",
      "endTime": "17:00:00",
      "slotDurationMinutes": 30,
      "enabled": true,
      "createdAt": "2026-09-01T10:00:00Z",
      "updatedAt": "2026-09-02T11:30:00Z"
    }
  ]
}
```

El detalle incluye todos los horarios del profesional, incluso los deshabilitados. Para construir disponibilidad se debe usar `GET /api/professionals/{id}/schedules`, que devuelve solo horarios habilitados.

Si el profesional no existe: `404` con codigo `RESOURCE_NOT_FOUND`.

### 5.4. `GET /api/professionals/{id}/schedules`

Devuelve los horarios semanales habilitados del profesional. Es el endpoint principal para construir disponibilidad.

Respuesta `200`:

```json
[
  {
    "id": 41,
    "professionalId": 15,
    "dayOfWeek": "MONDAY",
    "startTime": "09:00:00",
    "endTime": "17:00:00",
    "slotDurationMinutes": 30,
    "enabled": true,
    "createdAt": "2026-09-01T10:00:00Z",
    "updatedAt": "2026-09-02T11:30:00Z"
  }
]
```

Reglas de interpretacion:

- `dayOfWeek` usa los nombres ISO de `java.time.DayOfWeek`, por ejemplo `MONDAY`.
- `startTime` y `endTime` son horas locales sin zona horaria.
- La ventana es semiabierta: `startTime` incluido y `endTime` excluido.
- Los slots se generan desde `startTime` en incrementos de `slotDurationMinutes` hasta antes de `endTime`.
- Solo se devuelven horarios con `enabled = true`.
- Si el profesional no existe: `404` con codigo `RESOURCE_NOT_FOUND`.

Ejemplo de generacion para un lunes:

```text
Horario: 09:00-17:00, slotDurationMinutes: 30
Slots: 09:00, 09:30, 10:00, ..., 16:30
```

`servicio-turnos` debe combinar estos slots con:

- la fecha seleccionada por el usuario;
- las ocupaciones informadas por la catedra;
- los holds y reservas propias.

### 5.5. `GET /api/catalog-version`

Devuelve la version local aplicada del catalogo.

Respuesta `200`:

```json
{
  "localVersion": 42,
  "lastSyncedAt": "2026-09-02T11:30:00Z"
}
```

Si el catalogo todavia no tiene una version local aplicada: `404`.

Uso esperado en turnos: diagnostico y deteccion de datos desactualizados. No es obligatorio para construir disponibilidad.

## 6. Errores

Los errores funcionales usan `ProblemDetail` (RFC 7807) e incluyen el campo `code`:

```json
{
  "title": "Resource not found",
  "status": 404,
  "detail": "No existe el profesional 999",
  "code": "RESOURCE_NOT_FOUND"
}
```

`type` e `instance` son opcionales y pueden aparecer segun como Spring complete el `ProblemDetail`. El campo estable para decidir es `code`; el texto de `detail` no debe usarse para logica de negocio.

Codigos del catalogo:

| HTTP | Codigo | Descripcion |
| --- | --- | --- |
| `400` | `VALIDATION_ERROR` | Parametros o cuerpo invalidos. |
| `400` | `INVALID_PARAMETER` | Parametro con formato invalido. |
| `400` | `BAD_REQUEST` | Solicitud invalida. |
| `404` | `RESOURCE_NOT_FOUND` | Recurso inexistente. |
| `503` | `INTEGRATION_NOT_CONFIGURED` | Integracion con la catedra sin configurar. |
| `503` | `CATEDRA_UNAVAILABLE` | Error de comunicacion con la catedra. |
| `500` | `INTERNAL_ERROR` | Error interno no esperado. |

Errores de seguridad:

| HTTP | Descripcion |
| --- | --- |
| `401` | Token ausente, invalido o vencido. |
| `403` | Token valido sin rol suficiente. |

## 7. Reglas de integracion

- Antes de crear un hold o confirmar una reserva, `servicio-turnos` debe validar que el profesional siga existiendo y habilitado.
- La validacion de vigencia debe usar datos actuales del catalogo, no datos historicos guardados en una reserva.
- `servicio-turnos` puede guardar datos descriptivos historicos del profesional dentro de una reserva, pero no debe usarlos como catalogo vigente.
- Si el catalogo no esta disponible, `servicio-turnos` no debe iniciar operaciones que requieran validar informacion vigente.
- Las llamadas GET son idempotentes y pueden reintentarse con timeout y backoff.
- El token de servicio debe obtenerse o renovarse sin intervencion del usuario final.
- El JWT de usuario de KMP no debe reemplazar al token de servicio en llamadas internas.
- CORS no aplica a la comunicacion servidor a servidor; solo a clientes web.

## 8. Fuera de alcance

- Crear, editar o eliminar profesionales, categorias u horarios desde `servicio-turnos`.
- Consultar ocupaciones de la catedra a traves del catalogo.
- Crear holds o reservas en el catalogo.
- Exponer datos de usuarios finales.
- Replicar el catalogo dentro de `servicio-turnos`.

## 9. Versionado

Cambios incompatibles en rutas, campos o semantica de horarios requieren una nueva version de este documento y coordinacion con `servicio-turnos`. Agregar campos opcionales no rompe el contrato.
