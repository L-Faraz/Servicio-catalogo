# Servicio-catalogo

Servicio de catalogo (categorias, profesionales y horarios) con sincronizacion contra el servicio central de la catedra (REST + Redis + Kafka).

## Requisitos

- Java 25
- PostgreSQL
- Redis (proporcionado por la catedra)
- Kafka (proporcionado por la catedra)

## Base de datos

El esquema se administra con Flyway. La migracion inicial esta en `src/main/resources/db/migration/V1__init_catalog.sql`.

Crear la base local una vez:

```bash
createdb catalogo_db
```

Hibernate no crea ni modifica el esquema (`ddl-auto: none`).

## Configuracion

Los valores de conexion e integracion se pasan por variables de entorno. No se versionan en el repositorio.

| Variable | Descripcion | Default |
| --- | --- | --- |
| `DB_URL` | URL JDBC de PostgreSQL | `jdbc:postgresql://localhost:5432/catalogo_db` |
| `DB_USERNAME` | Usuario de la base | `postgres` |
| `DB_PASSWORD` | Password de la base | `postgres` |
| `DB_PORT` | Puerto publicado por `compose.yaml` para PostgreSQL | `5432` |
| `CATEDRA_API_URL` | URL base de la API de la catedra | sin default |
| `CATEDRA_JWT_TOKEN` | JWT tecnico de la cuenta del proyecto | sin default |
| `CATEDRA_GROUP_ID` | `groupId` de la integracion tecnica; se usa para derivar topic, consumer group y namespace de escritura | sin default |
| `CATEDRA_PROVISIONING_STATUS` | Estado de provisioning informado por la catedra | sin default |
| `CATEDRA_REDIS_HOST` | Host de Redis | sin default |
| `CATEDRA_REDIS_PORT` | Puerto de Redis | `6379` |
| `CATEDRA_REDIS_USERNAME` | Usuario de Redis | sin default |
| `CATEDRA_REDIS_PASSWORD` | Password de Redis | sin default |
| `CATEDRA_REDIS_READ_NAMESPACE` | Namespace de lectura en Redis | `catedra:sync:*` |
| `CATEDRA_REDIS_WRITE_NAMESPACE` | Namespace de escritura en Redis | `alumnos:{groupId}:*` |
| `CATEDRA_KAFKA_BOOTSTRAP_SERVERS` | Bootstrap servers de Kafka | sin default |
| `CATEDRA_KAFKA_CONSUMER_GROUP_ID` | Consumer group de Kafka | `alumnos-{groupId}` |
| `CATEDRA_KAFKA_CATALOG_TOPIC` | Topico de eventos de catalogo | `catedra.catalog.{groupId}` |
| `CATEDRA_KAFKA_DLT_TOPIC` | Topico de eventos fallidos | `{catalogTopic}.dlt` |
| `CATEDRA_KAFKA_SECURITY_PROTOCOL` | `PLAINTEXT`, `SSL`, `SASL_PLAINTEXT` o `SASL_SSL` | `PLAINTEXT` |
| `CATEDRA_KAFKA_SASL_MECHANISM` | Mecanismo SASL, por ejemplo `PLAIN` o `SCRAM-SHA-512` | sin default |
| `CATEDRA_KAFKA_SASL_JAAS_CONFIG` | Configuracion JAAS del cliente Kafka | sin default |
| `CATEDRA_KAFKA_SSL_TRUSTSTORE_LOCATION` | Ruta al truststore Kafka | sin default |
| `CATEDRA_KAFKA_SSL_TRUSTSTORE_PASSWORD` | Password del truststore Kafka | sin default |
| `CATEDRA_KAFKA_SSL_TRUSTSTORE_TYPE` | Tipo de truststore | `JKS` |
| `CATEDRA_KAFKA_SSL_KEYSTORE_LOCATION` | Ruta al keystore Kafka para mTLS | sin default |
| `CATEDRA_KAFKA_SSL_KEYSTORE_PASSWORD` | Password del keystore Kafka | sin default |
| `CATEDRA_KAFKA_SSL_KEYSTORE_TYPE` | Tipo de keystore | `JKS` |
| `CATEDRA_KAFKA_SSL_KEY_PASSWORD` | Password de la clave privada | sin default |
| `CATEDRA_KAFKA_SSL_ENDPOINT_IDENTIFICATION_ALGORITHM` | Algoritmo de verificacion de hostname; vacio la desactiva | sin default |
| `CATEDRA_KAFKA_RETRY_INITIAL_MS` | Backoff inicial entre reintentos Kafka | `1000` |
| `CATEDRA_KAFKA_MAX_RETRIES` | Cantidad maxima de reintentos Kafka | `3` |
| `CATEDRA_KAFKA_ENABLED` | Habilita el listener de Kafka | `true` |
| `CATEDRA_RECONCILIATION_ENABLED` | Habilita la reconciliacion periodica | `true` |
| `CATEDRA_RECONCILIATION_INTERVAL_MS` | Intervalo de reconciliacion | `60000` |
| `CATEDRA_RECONCILIATION_INITIAL_DELAY_MS` | Delay inicial de reconciliacion | `30000` |
| `CATEDRA_PROCESSED_EVENTS_RETENTION_DAYS` | Dias de retencion de eventos Kafka deduplicados | `7` |
| `CATEDRA_PROCESSED_EVENTS_CLEANUP_ENABLED` | Habilita la limpieza periodica de eventos procesados | `true` |
| `CATEDRA_PROCESSED_EVENTS_CLEANUP_INTERVAL_MS` | Intervalo de limpieza de eventos procesados | `3600000` |
| `CATEDRA_PROCESSED_EVENTS_CLEANUP_INITIAL_DELAY_MS` | Delay inicial de limpieza de eventos procesados | `60000` |
| `APP_JWT_SECRET` | Secreto HS256 para validar los JWT emitidos por `servicio-turnos` | sin default |
| `APP_JWT_ISSUER` | Emisor esperado del JWT | `servicio-turnos` |
| `APP_CORS_ALLOWED_ORIGINS` | Origenes permitidos por CORS, separados por coma | sin default |

Si `CATEDRA_KAFKA_CATALOG_TOPIC`, `CATEDRA_KAFKA_CONSUMER_GROUP_ID` o `CATEDRA_REDIS_WRITE_NAMESPACE` no se informan, se derivan de `CATEDRA_GROUP_ID` con el formato del contrato de catedra. Un valor explicito siempre tiene prioridad. El listener de Kafka solo se registra cuando hay `groupId` o un topic explicito configurado.

Para desarrollo local se pueden definir en la run config del IDE o en un archivo `.env` en la raiz (ignorado por git). Nunca versionar secretos.

## Seguridad

El servicio actua como resource server: no emite JWT de usuarios, solo valida los tokens emitidos por `servicio-turnos`.

- `APP_JWT_SECRET` es obligatorio y debe tener al menos 32 bytes para HS256.
- El claim `roles` define las autoridades. Cada valor se prefija con `ROLE_`.
- Los endpoints `/api/**` requieren un JWT valido.
- Los endpoints `/api/synchronization/**` requieren rol `SERVICE` o `ADMIN`.
- El resto de las APIs acepta roles `USER`, `SERVICE` o `ADMIN`.
- `CATEDRA_JWT_TOKEN` sigue siendo exclusivamente el token tecnico para la API de la catedra.
- `APP_CORS_ALLOWED_ORIGINS` acepta una lista separada por comas, por ejemplo `http://localhost:3000,http://localhost:8080`.

Ejemplo de payload de JWT emitido por `servicio-turnos`:

```json
{
  "sub": "user-42",
  "iss": "servicio-turnos",
  "roles": ["USER"],
  "iat": 1780000000,
  "exp": 1780003600
}
```

## Sincronizacion

- **Snapshot:** reemplaza el catalogo completo dentro de una transaccion.
- **Incremental:** aplica cada version de cambio en su propia transaccion y avanza la version local.
- **Discontinuidad:** si la version local quedo fuera de la ventana de Redis (`localVersion < oldestVersion`), si `localVersion > redisVersion` o si Redis no informa la version actual, se fuerza un snapshot.
- **Kafka:** consume `catedra.catalog.{groupId}` con el consumer group `alumnos-{groupId}`, deduplica por `eventId` en `processed_catalog_events` y confirma el offset solo si el procesamiento fue exitoso y la version local alcanzo `newVersion`.
- **Provisioning:** si `CATEDRA_PROVISIONING_STATUS` esta informado y no es `PROVISIONED`, no se ejecuta el snapshot inicial, no corre la reconciliacion y los endpoints manuales de sincronizacion devuelven `503`.
- **Reconciliacion:** job periodico que ejecuta sincronizacion incremental para recuperar eventos perdidos.
- **Limpieza:** job periodico que elimina eventos deduplicados anteriores a la retencion configurada.

## Endpoints

| Metodo | Ruta | Descripcion | Acceso |
| --- | --- | --- | --- |
| `GET` | `/api/categories?enabled=` | Lista categorias, opcionalmente filtradas por estado | `USER`, `SERVICE`, `ADMIN` |
| `GET` | `/api/professionals` | Busca profesionales con paginacion | `USER`, `SERVICE`, `ADMIN` |
| `GET` | `/api/professionals/{id}` | Detalle de un profesional | `USER`, `SERVICE`, `ADMIN` |
| `GET` | `/api/professionals/{id}/schedules` | Horarios de un profesional | `USER`, `SERVICE`, `ADMIN` |
| `GET` | `/api/catalog-version` | Version actual del catalogo | `USER`, `SERVICE`, `ADMIN` |
| `POST` | `/api/synchronization/snapshot` | Fuerza una sincronizacion por snapshot | `SERVICE`, `ADMIN` |
| `POST` | `/api/synchronization/incremental` | Fuerza una sincronizacion incremental | `SERVICE`, `ADMIN` |
| `GET` | `/api/synchronization/status` | Estado actual de la sincronizacion | `SERVICE`, `ADMIN` |

Parametros de busqueda de profesionales: `categoryId`, `name`, `enabled` (default `true`), `dayOfWeek`, `time`, `page`, `size`, `sort`.

## Contrato con servicio-turnos

El contrato de integracion para construir disponibilidad y validar reservas esta en [`docs/contract-catalogo-turnos.md`](docs/contract-catalogo-turnos.md).

`servicio-turnos` consume los endpoints de lectura del catalogo con un JWT de rol `SERVICE`. La app KMP no consulta el catalogo directamente para disponibilidad.

## Documentacion

- [Arquitectura](docs/architecture.md)
- [Modelo de datos](docs/data-model.md)
- [Estrategia de sincronizacion](docs/sync-strategy.md)
- [Idempotencia y recuperacion](docs/idempotency-and-recovery.md)
- [Seguridad](docs/security.md)
- [Pruebas](docs/tests.md)

## Pruebas

```bash
./gradlew test
```

Los tests usan H2 en memoria, Kafka y Flyway desactivados, y no requieren servicios externos.

## Docker

Copiar `.env.example` a `.env` y completar los valores reales. El archivo `.env` esta ignorado por git.

```bash
cp .env.example .env
docker compose up --build -d
docker compose ps
curl http://localhost:8081/actuator/health
docker compose logs -f servicio-catalogo
```

El compose levanta PostgreSQL y el servicio de catalogo. Redis y Kafka son externos y se configuran con las variables `CATEDRA_*`. Si no se informan, el servicio arranca sin sincronizacion y responde con el catalogo local persistido.

El healthcheck del servicio usa `/actuator/health`. La base de datos tiene un healthcheck con `pg_isready` y volumen persistente `catalogo_db_data`.

## Ejecutar sin Docker

```bash
./gradlew bootRun
```

El servicio escucha en `http://localhost:8081`.
