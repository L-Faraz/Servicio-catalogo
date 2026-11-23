# Pruebas del servicio de catálogo

## 1. Cómo ejecutar

```bash
./gradlew test
./gradlew build
```

`build` incluye `test`, compilación y empaquetado.

Los tests usan el perfil `test`:

- H2 en memoria con modo PostgreSQL;
- Flyway deshabilitado;
- Kafka deshabilitado;
- reconciliación deshabilitada;
- configuración ficticia de catedra;
- JWT de prueba con secreto local de tests.

Dos tests adicionales usan el perfil `testcontainers` o Kafka embebido:

- `PostgresFlywayIntegrationTest` levanta PostgreSQL con Testcontainers y valida las migraciones reales con `ddl-auto: validate`.
- `KafkaDltIntegrationTest` levanta un broker Kafka embebido y valida la publicación al DLT.

Se necesita Docker disponible para esos dos tests. No se necesita Redis real.

## 2. Stack de pruebas

- JUnit 5.
- Mockito.
- AssertJ.
- Spring Boot Test.
- MockMvc.
- H2.
- Testcontainers con PostgreSQL.
- Kafka embebido.

## 3. Cobertura actual

| Archivo | Qué verifica |
| --- | --- |
| `ServicioCatalogoApplicationTests` | El contexto Spring arranca con el perfil `test`. |
| `CatedraApiClientTest` | URL del snapshot, bearer token, parseo del contrato y error si falta configuración. |
| `SyncServiceImplTest` | Snapshot, validación estricta de snapshot/cambios, copia local incompleta, versión vieja, cambio faltante, incremental normal, versión local adelantada, Redis sin versión, fallo de Redis y versión ya actualizada. |
| `CatalogEventListenerTest` | Evento válido, versión no alcanzada, deduplicación concurrente, provisioning bloqueado, duplicado, tipo inválido, JSON inválido y fallo de sincronización sin ack. |
| `CatalogSearchIntegrationTest` | Filtros combinados, estado habilitado y disponibilidad por día y hora. |
| `CatalogHttpContractTest` | JSON de categorías, paginación de profesionales, detalle/horarios, `ProblemDetail`, roles y `/actuator/metrics`. |
| `SyncStatusServiceTest` | Persistencia de estado, truncamiento del error y recuperación de un `SYNCING` interrumpido. |
| `RedisSyncRepositoryTest` | Claves derivadas de `CATEDRA_REDIS_READ_NAMESPACE` y lectura de `current-version`. |
| `SecurityIntegrationTest` | Sin token, token inválido, token vencido, lectura con rol `USER`, prohibición de sincronización para `USER`, acceso de `SERVICE` y CORS. |
| `CatedraPropertiesTest` | Derivación de topic, consumer group y namespace desde `groupId`; prioridad de valores explícitos; estado de provisioning. |
| `KafkaConsumerConfigTest` | Consumer group derivado, consumer group explícito, propagación SSL/SASL a consumer y producer, y derivación del DLT. |
| `SyncBootstrapTest` | Snapshot inicial con provisioning listo, copia incompleta, omisión con provisioning bloqueado, independencia de Redis y comportamiento sin `provisioningStatus`. |
| `CatalogReconciliationJobTest` | La reconciliación invoca la sincronización incremental cuando está habilitada. |
| `CatedraRedisHealthIndicatorTest` | Health `UP`/`DOWN` con `EXISTS`, sin depender de `PING`. |
| `ProcessedCatalogEventsCleanupJobTest` | Limpieza por retención y omisión cuando la retención no es positiva. |
| `KafkaDltIntegrationTest` | Un registro fallido se publica en el topic DLT con su clave y valor originales. |
| `PostgresFlywayIntegrationTest` | Flyway aplica `V1`-`V3` sobre PostgreSQL real, `ddl-auto: validate` y `email` queda eliminado. |

## 4. Escenarios importantes cubiertos

- Inicialización desde snapshot.
- Actualización incremental normal.
- Recuperación por snapshot ante discontinuidad.
- Recuperación por snapshot si Redis falla o no informa versión.
- Recuperación por snapshot si la versión local es mayor que la remota.
- Deduplicación por `eventId`, incluso con guardas concurrentes.
- No confirmación de offset si la sincronización no alcanza `newVersion`.
- Recuperación por snapshot si existe versión local pero faltan datos.
- Rechazo de snapshots o cambios incompletos antes de borrar la copia local.
- Búsqueda local con filtros por categoría, nombre, estado y disponibilidad.
- Rechazo de accesos no autenticados y no autorizados.
 - Configuración derivada del contrato de catedra.
 - Namespace Redis de lectura aplicado a todas las claves.
 - Estado de sincronización persistente y recuperación ante reinicio.
 - Contrato HTTP de lectura y autorización de métricas.
 - Publicación real al DLT con Kafka embebido y esquema Flyway validado con PostgreSQL real.

## 5. Limitaciones

 - No hay test con Redis real; la ACL de la catedra ya se validó manualmente con `EXISTS`.
 - No hay tests de concurrencia multirréplica sobre el lock de sincronización; el despliegue soportado es de una sola réplica.
 - No hay tests de rollback transaccional con fallos parciales.
 - No hay tests de contrato que validen el JSON completo del snapshot y de `CatalogUpdated` ante campos adicionales.
 - No hay tests de todos los códigos de error del `GlobalExceptionHandler`.
 - No hay un evento real de la catedra disparado de extremo a extremo; sí se validaron suscripción real y DLT con Kafka embebido.

Estas limitaciones no bloquean la entrega y están documentadas.

## 6. Convenciones

- Los tests de servicio son unitarios con mocks cuando se quiere controlar Redis, la API o transacciones.
- Los tests de seguridad y búsqueda usan contexto Spring y MockMvc.
- Los datos de prueba usan valores ficticios de la catedra.
- No se usan secretos reales.
