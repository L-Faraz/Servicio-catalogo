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
| `SyncServiceImplTest` | Snapshot, snapshot sin versión, versión vieja, cambio faltante, incremental normal, versión local adelantada, Redis sin versión, fallo de Redis y versión ya actualizada. |
| `CatalogEventListenerTest` | Evento válido, duplicado, tipo inválido, JSON inválido y fallo de sincronización sin ack. |
| `CatalogSearchIntegrationTest` | Filtros combinados, estado habilitado y disponibilidad por día y hora. |
| `SecurityIntegrationTest` | Sin token, token inválido, token vencido, lectura con rol `USER`, prohibición de sincronización para `USER`, acceso de `SERVICE` y CORS. |
| `CatedraPropertiesTest` | Derivación de topic, consumer group y namespace desde `groupId`; prioridad de valores explícitos; estado de provisioning. |
| `KafkaConsumerConfigTest` | Consumer group derivado y consumer group explícito. |
| `SyncBootstrapTest` | Snapshot inicial con provisioning listo, omisión con provisioning bloqueado y comportamiento sin `provisioningStatus`. |

## 4. Escenarios importantes cubiertos

- Inicialización desde snapshot.
- Actualización incremental normal.
- Recuperación por snapshot ante discontinuidad.
- Recuperación por snapshot si Redis falla o no informa versión.
- Recuperación por snapshot si la versión local es mayor que la remota.
- Deduplicación por `eventId`.
- No confirmación de offset ante fallo de sincronización.
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
- No hay pruebas de limpieza de `processed_catalog_events`, porque esa funcionalidad todavía no existe.
- No hay pruebas de Actuator, porque la dependencia todavía no está incluida.

Estas limitaciones están alineadas con el estado actual del repositorio y deben revisarse antes de la entrega final.

## 6. Convenciones

- Los tests de servicio son unitarios con mocks cuando se quiere controlar Redis, la API o transacciones.
- Los tests de seguridad y búsqueda usan contexto Spring y MockMvc.
- Los datos de prueba usan valores ficticios de la catedra.
- No se usan secretos reales.
