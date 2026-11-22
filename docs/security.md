# Seguridad del servicio de catálogo

## 1. Identidades

El sistema distingue dos tipos de identidad:

| Identidad | Dueño | Uso |
| --- | --- | --- |
| Cuenta técnica de la catedra | Alumno, configurada fuera del repositorio | REST, Redis y Kafka de la catedra. Su JWT es `CATEDRA_JWT_TOKEN`. |
| Usuario final | `servicio-turnos` | Login desde KMP; sus JWT se usan para consumir APIs del proyecto. |

El JWT técnico de la catedra nunca se envía a KMP ni a `servicio-turnos`, y no se usa para proteger los endpoints de este servicio.

Para la comunicación entre servicios se usa un JWT de servicio emitido por `servicio-turnos` con rol `SERVICE`. El token de servicio no propaga la identidad del usuario final: este catálogo solo devuelve datos de catálogo y no toma decisiones de propiedad sobre reservas.

## 2. Resource server

`Servicio-catalogo` no emite tokens. Solo valida JWT HS256.

Validaciones implementadas:

- firma HS256 con `APP_JWT_SECRET`;
- secreto de al menos 32 bytes;
- emisor esperado `APP_JWT_ISSUER`, por defecto `servicio-turnos`;
- vigencia (`exp`);
- claim `roles`, convertido a autoridades con prefijo `ROLE_`;
- `sub` como principal.

El decode y validación se configuran en `SecurityConfig` con `NimbusJwtDecoder` y `JwtValidators.createDefaultWithIssuer(...)`.

## 3. Autorización por endpoint

| Ruta | Roles permitidos |
| --- | --- |
| `/error` | público |
| `/actuator/health`, `/actuator/health/**` | público, configurado en la cadena de seguridad |
| `/api/synchronization/**` | `SERVICE`, `ADMIN` |
| `/api/**` restante | `USER`, `SERVICE`, `ADMIN` |
| cualquier otra ruta | autenticado |

Los endpoints de consulta son de solo lectura. Las operaciones de sincronización están restringidas porque pueden reemplazar la copia local.

Spring Boot Actuator está incluido en `build.gradle`. Solo `/actuator/health` y sus subrutas son públicos; el resto de Actuator no se expone en la configuración actual.

## 4. CORS

`CorsConfig` toma los orígenes de `APP_CORS_ALLOWED_ORIGINS`, separados por coma.

- Se usan `allowedOriginPatterns`, no `allowedOrigins` estrictos.
- Métodos permitidos: `GET`, `POST`, `PUT`, `PATCH`, `DELETE`, `OPTIONS`.
- Headers permitidos: `*`.
- Headers expuestos: `X-Total-Count`, `Link`, `Location`.
- `allowCredentials = true`.
- `maxAge = 3600`.

Sin variable configurada, no se registran orígenes permitidos. CORS solo aplica a clientes web; la comunicación servidor a servidor no depende de él.

## 5. Secretos

| Secreto | Variable | Uso |
| --- | --- | --- |
| JWT del proyecto | `APP_JWT_SECRET` | Validar JWT recibidos por este servicio y por `servicio-turnos`. |
| JWT técnico de catedra | `CATEDRA_JWT_TOKEN` | Llamar a la API REST de la catedra. |
| Credenciales Redis | `CATEDRA_REDIS_USERNAME`, `CATEDRA_REDIS_PASSWORD` | Conectarse a Redis de la catedra. |
| Credenciales Kafka | `CATEDRA_KAFKA_SASL_JAAS_CONFIG`, `CATEDRA_KAFKA_SSL_TRUSTSTORE_PASSWORD`, `CATEDRA_KAFKA_SSL_KEYSTORE_PASSWORD`, `CATEDRA_KAFKA_SSL_KEY_PASSWORD` | Conectarse al broker con SASL o mTLS. |
| Credenciales DB | `DB_USERNAME`, `DB_PASSWORD` | Conectarse a PostgreSQL. |

Todos se externalizan por variables de entorno. No hay secretos en el código fuente, tests, README ni documentación.

No se registran tokens en logs. Los mensajes de error de integración pueden incluir mensajes de excepción de Redis o HTTP, pero no el valor del bearer token, de la configuración JAAS ni de los passwords de keystore/truststore.

## 6. Contrato con `servicio-turnos`

El detalle de endpoints, DTO, errores y autenticación está en [`contract-catalogo-turnos.md`](contract-catalogo-turnos.md).

Reglas relevantes:

- `servicio-turnos` usa JWT de rol `SERVICE` para consultar el catálogo.
- La app KMP no consulta la catedra ni usa el token técnico.
- El catálogo no verifica propiedad de reservas porque no gestiona reservas.
- No se reemplaza el JWT de servicio por el JWT del usuario final en llamadas internas.

## 7. Limitaciones

- No se valida el claim `aud`.
- No hay rotor de secretos ni revocación anticipada de JWT en este servicio.
- No hay rate limiting ni protección contra fuerza bruta; `servicio-turnos` es responsable del login.
- No hay mTLS entre servicios; se confía en HS256 compartido y en la red de despliegue.
- No hay auditoría de accesos ni correlación de requests.
- Se expone únicamente `/actuator/health`; no se exponen métricas, beans, env ni endpoints de escritura de Actuator.
