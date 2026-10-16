# Servicio-catalogo

Servicio de catalogo (categorias, profesionales y horarios) con sincronizacion contra el servicio central de la catedra (REST + Redis + Kafka).

## Requisitos

- Java 21
- PostgreSQL

## Base de datos

Crear la base local una vez:

```bash
createdb catalogo_db
```

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

Para desarrollo local se pueden definir en la run config del IDE o en un archivo `application-local.yml` (ignorado por git).

## Ejecutar

```bash
./gradlew bootRun
```

El servicio escucha en `http://localhost:8081`.
