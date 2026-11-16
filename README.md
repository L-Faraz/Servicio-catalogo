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
| `CATEDRA_API_URL` | URL base de la API de la catedra | sin default |
| `CATEDRA_JWT_TOKEN` | JWT tecnico de la cuenta del proyecto | sin default |
| `CATEDRA_GROUP_ID` | `groupId` de la integracion tecnica | sin default |
| `CATEDRA_PROVISIONING_STATUS` | Estado de provisioning informado por la catedra | sin default |
| `CATEDRA_REDIS_HOST` | Host de Redis | sin default |
| `CATEDRA_REDIS_PORT` | Puerto de Redis | `6379` |
| `CATEDRA_REDIS_USERNAME` | Usuario de Redis | sin default |
| `CATEDRA_REDIS_PASSWORD` | Password de Redis | sin default |
| `CATEDRA_REDIS_READ_NAMESPACE` | Namespace de lectura en Redis | `catedra:sync:*` |
| `CATEDRA_REDIS_WRITE_NAMESPACE` | Namespace de escritura en Redis | sin default |
| `CATEDRA_KAFKA_BOOTSTRAP_SERVERS` | Bootstrap servers de Kafka | sin default |
| `CATEDRA_KAFKA_CONSUMER_GROUP_ID` | Consumer group de Kafka | `servicio-catalogo-group` |
| `CATEDRA_KAFKA_CATALOG_TOPIC` | Topico de eventos de catalogo | `catedra.catalog.events` |
| `CATEDRA_KAFKA_ENABLED` | Habilita el listener de Kafka | `true` |
| `CATEDRA_RECONCILIATION_ENABLED` | Habilita la reconciliacion periodica | `true` |
| `CATEDRA_RECONCILIATION_INTERVAL_MS` | Intervalo de reconciliacion | `60000` |
| `CATEDRA_RECONCILIATION_INITIAL_DELAY_MS` | Delay inicial de reconciliacion | `30000` |

Para desarrollo local se pueden definir en la run config del IDE o en un archivo `application-local.yml` (ignorado por git).

## Sincronizacion

- **Snapshot:** reemplaza el catalogo completo dentro de una transaccion.
- **Incremental:** aplica cada version de cambio en su propia transaccion y avanza la version local.
- **Discontinuidad:** si la version local quedo fuera de la ventana de Redis (`localVersion < oldestVersion`), se fuerza un snapshot.
- **Kafka:** consume `CatalogUpdated`, deduplica por `eventId` en `processed_catalog_events` y confirma el offset solo si el procesamiento fue exitoso.
- **Reconciliacion:** job periodico que ejecuta sincronizacion incremental para recuperar eventos perdidos.

## Endpoints

| Metodo | Ruta | Descripcion |
| --- | --- | --- |
| `GET` | `/api/categories?enabled=` | Lista categorias, opcionalmente filtradas por estado |
| `GET` | `/api/professionals` | Busca profesionales con paginacion |
| `GET` | `/api/professionals/{id}` | Detalle de un profesional |
| `GET` | `/api/professionals/{id}/schedules` | Horarios de un profesional |
| `GET` | `/api/catalog-version` | Version actual del catalogo |
| `POST` | `/api/synchronization/snapshot` | Fuerza una sincronizacion por snapshot |
| `POST` | `/api/synchronization/incremental` | Fuerza una sincronizacion incremental |
| `GET` | `/api/synchronization/status` | Estado actual de la sincronizacion |

Parametros de busqueda de profesionales: `categoryId`, `name`, `enabled` (default `true`), `dayOfWeek`, `time`, `page`, `size`, `sort`.

## Pruebas

```bash
./gradlew test
```

Los tests usan H2 en memoria, Kafka y Flyway desactivados, y no requieren servicios externos.

## Ejecutar

```bash
./gradlew bootRun
```

El servicio escucha en `http://localhost:8081`.
