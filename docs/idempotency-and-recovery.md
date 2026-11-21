# Idempotencia y recuperación

## 1. Decisiones generales

- Kafka es una notificación; Redis y REST son la fuente de datos.
- El procesamiento se ordena por versión de catálogo, no por el orden de llegada del evento.
- Cada evento `eventId` se procesa como máximo una vez de forma funcional.
- La versión local avanza solo cuando la transacción correspondiente termina correctamente.
- Si no se puede continuar de forma segura, se reconstruye la copia con snapshot.

## 2. Mecanismos implementados

| Mecanismo | Implementación | Efecto |
| --- | --- | --- |
| Deduplicación Kafka | `processed_catalog_events` con PK `eventId` | Un evento repetido no dispara otra sincronización. |
| Lock de sincronización | `ReentrantLock` en `SyncServiceImpl` | Evita snapshot e incremental simultáneos. |
| Transacciones | `TransactionTemplate` | Snapshot e incrementales son atómicos por unidad de trabajo. |
| Control de versión | `catalog_versions.current_version` | Permite detectar atraso, adelanto y discontinuidad. |
| Validación de cambios | `CatalogDiscontinuityException` | Fuerza snapshot ante datos inconclusos o inconsistentes. |
| Validación de snapshot | `SyncServiceImpl.validateSnapshot` | Evita borrar la copia local si faltan colecciones, ids o campos obligatorios. |
| Ack manual | `MANUAL_IMMEDIATE` | El offset se confirma solo después de persistir y de verificar que la versión local alcanzó `newVersion`. |
| Reconciliación | Job periódico | Recupera notificaciones perdidas. |

## 3. Casos cubiertos

| Caso | Comportamiento |
| --- | --- |
| Mensaje Kafka duplicado | Se consulta `processed_catalog_events`; si existe, se hace ack y no se sincroniza. |
| Notificación perdida | La reconciliación compara versión local vs. Redis. |
| Evento con versión vieja | Se consulta la versión actual de Redis; el evento no retrocede la copia. |
| Redis sin `current-version` | Se ejecuta snapshot REST. |
| Redis temporalmente caído | Se ejecuta snapshot REST. |
| `localVersion > redisVersion` | Se ejecuta snapshot REST. |
| `localVersion < oldestAvailableVersion` | Se ejecuta snapshot REST porque faltan incrementos. |
| Falta `changes:{version}` | Se ejecuta snapshot REST. |
| Falta una entidad referenciada por un cambio | Discontinuidad y snapshot REST. |
| JSON inválido desde Redis | Discontinuidad y snapshot REST. |
| `dayOfWeek` inválido | Discontinuidad y snapshot REST. |
| Falla el guardado del evento procesado | No se confirma el offset; el evento puede repetirse y volver a intentarse. |
| Reinicio de la aplicación | `SyncBootstrap` decide snapshot si falta versión o la copia está incompleta, y solo si el provisioning está listo. |
| Provisioning no listo | Se omiten el snapshot inicial y el procesamiento de eventos Kafka. |
| Evento con `newVersion` no aplicada | No se confirma el offset; el evento se reintenta. |
| Snapshot con colecciones o campos faltantes | Se rechaza antes de borrar la copia local. |
| Llamadas GET de consulta | Son idempotentes y pueden reintentarse. |

## 4. Flujo de un evento duplicado

```mermaid
sequenceDiagram
  participant K as Kafka
  participant L as CatalogEventListener
  participant DB as processed_catalog_events
  participant S as SyncService

  K-->>L: CatalogUpdated(eventId = E)
  L->>DB: existsById(E)
  alt ya existe
    L-->>K: ack sin sincronizar
  else no existe
    L->>S: processIncrementalSync()
    S-->>L: exito
    L->>S: isVersionApplied(newVersion)
    S-->>L: true
    L->>DB: save(E)
    L-->>K: ack
  end
```

## 5. Reintentos

El contenedor Kafka usa `DefaultErrorHandler` con `FixedBackOff` configurable (`CATEDRA_KAFKA_RETRY_INITIAL_MS`, `CATEDRA_KAFKA_MAX_RETRIES`).

- Si la sincronización falla, no se confirma el offset y se reintenta.
- Agotados los reintentos, `DeadLetterPublishingRecoverer` publica el registro en `{catalogTopic}.dlt`, configurable con `CATEDRA_KAFKA_DLT_TOPIC`.
- El DLT conserva la partición original para facilitar el diagnóstico.
- No hay control de reintentos por tipo de error; la política es uniforme para todos los fallos de procesamiento.

## 6. Limitaciones actuales

- `processed_catalog_events` tiene índice y job de limpieza configurable, pero la retención depende de que el job esté habilitado.
- Se detecta una copia local vacía/incompleta cuando existe versión local pero no hay entidades; no se valida integridad referencial más allá de las FKs de PostgreSQL.
- El estado de sincronización y el último error son en memoria; se pierden al reiniciar.
- No hay métricas ni alertas de sincronización.
- Hay DLT y backoff configurable, pero no se distingue entre errores funcionales y técnicos para aplicar políticas distintas.
- La reconciliación, los endpoints manuales, el bootstrap y el listener verifican `provisioningStatus`.
- Las claves Redis de lectura están hardcodeadas; `redisReadNamespace` no se aplica.
- No hay tests con Redis o Kafka reales.

Estas limitaciones deben evaluarse antes de la entrega final, pero no corrompen la copia local porque cualquier duda sobre la continuidad deriva en snapshot.

## 7. Decisiones explícitas

- Se prioriza reconstruir desde snapshot antes que aplicar cambios incompletos.
- Se prefiere reprocesar un evento antes que perder un cambio.
- No se intenta adivinar la versión correcta desde el payload del evento: se consulta Redis.
- No se expone el JWT técnico de la catedra fuera del cliente REST.
- No se usa Redis como persistencia principal; la copia vigente es PostgreSQL.
