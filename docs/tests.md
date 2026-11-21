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

No se necesitan PostgreSQL, Redis ni Kafka para correr la suite actual.

## 2. Stack de pruebas

- JUnit 5.
- Mockito.
- AssertJ.
- Spring Boot Test.
- MockMvc.
- H2.

## 3. Cobertura actual

| Archivo | Qué verifica |
| --- | --- |
| `ServicioCatalogoApplicationTests` | El contexto Spring arranca con el perfil `test`. |
| `CatedraApiClientTest` | URL del snapshot, bearer token, parseo del contrato y error si falta configuración. |
| `SyncServiceImplTest` | Snapshot, validación estricta de snapshot/cambios, copia local incompleta, versión vieja, cambio faltante, incremental normal, versión local adelantada, Redis sin versión, fallo de Redis y versión ya actualizada. |
| `CatalogEventListenerTest` | Evento válido, versión no alcanzada, deduplicación concurrente, provisioning bloqueado, duplicado, tipo inválido, JSON inválido y fallo de sincronización sin ack. |
| `CatalogSearchIntegrationTest` | Filtros combinados, estado habilitado y disponibilidad por día y hora. |
| `SecurityIntegrationTest` | Sin token, token inválido, token vencido, lectura con rol `USER`, prohibición de sincronización para `USER`, acceso de `SERVICE` y CORS. |
| `CatedraPropertiesTest` | Derivación de topic, consumer group y namespace desde `groupId`; prioridad de valores explícitos; estado de provisioning. |
| `KafkaConsumerConfigTest` | Consumer group derivado, consumer group explícito, propagación SSL/SASL a consumer y producer, y derivación del DLT. |
| `SyncBootstrapTest` | Snapshot inicial con provisioning listo, copia incompleta, omisión con provisioning bloqueado, independencia de Redis y comportamiento sin `provisioningStatus`. |
| `CatalogReconciliationJobTest` | La reconciliación invoca la sincronización incremental cuando está habilitada. |
| `CatedraRedisHealthIndicatorTest` | Health `UP`/`DOWN` con `EXISTS`, sin depender de `PING`. |
| `ProcessedCatalogEventsCleanupJobTest` | Limpieza por retención y omisión cuando la retención no es positiva. |

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

## 5. Limitaciones

- No hay tests con PostgreSQL, Redis ni Kafka reales.
- No se usan Testcontainers.
- No hay tests de concurrencia sobre el lock de sincronización.
- No hay tests de rollback transaccional con fallos parciales.
- No hay tests de contrato que validen el JSON completo del snapshot y de `CatalogUpdated` ante campos adicionales.
- No hay tests de todos los códigos de error del `GlobalExceptionHandler`.
- Hay pruebas unitarias de limpieza de `processed_catalog_events`; faltaría un test de integración con PostgreSQL.
- Hay pruebas del indicador de health de Redis; no hay un test HTTP dedicado de `/actuator/health`.

Estas limitaciones están alineadas con el estado actual del repositorio y deben revisarse antes de la entrega final.

## 6. Convenciones

- Los tests de servicio son unitarios con mocks cuando se quiere controlar Redis, la API o transacciones.
- Los tests de seguridad y búsqueda usan contexto Spring y MockMvc.
- Los datos de prueba usan valores ficticios de la catedra.
- No se usan secretos reales.
