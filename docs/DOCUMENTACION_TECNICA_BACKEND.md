# Manual Técnico - INKASANA Backend

Este documento describe la arquitectura, los módulos y los flujos técnicos implementados en el backend de **INKASANA**. La aplicación provee la API para organizaciones de salud, profesionales, pacientes, agenda, marketplace, portal de paciente y facturación.

---

## 1. Arquitectura y diseño

### Patrón arquitectónico

El backend utiliza una arquitectura **monolítica modular** sobre Spring Boot. Cada dominio se organiza en un módulo con controladores REST, DTOs, servicios, entidades, repositorios y mappers cuando corresponde. Esta estructura permite evolucionar cada capacidad sin mezclar la lógica de negocio de marketplace, tenant, facturación y autenticación.

La aplicación usa **multitenancy por esquema de PostgreSQL**:

- El esquema `public` conserva información global: organizaciones, especialidades globales, perfiles públicos de marketplace, archivos públicos, portal de paciente y facturación.
- Cada organización activa posee su propio esquema de tenant, donde se almacenan usuarios, pacientes, doctores, agenda, citas, branding y perfiles privados de marketplace.

Una solicitud privada sigue el flujo general:

```text
Cliente web -> CORS -> Spring Security -> JWT -> TenantFilter
-> Controller REST -> Service -> Repository JPA -> PostgreSQL
```

### Tecnologías principales

| Componente | Implementación | Uso |
| :--- | :--- | :--- |
| Framework | Spring Boot 3.5.14 | API HTTP, configuración y ciclo de vida. |
| Lenguaje | Kotlin 1.9.25 sobre Java 21 | Implementación del backend. |
| Persistencia | Spring Data JPA + Hibernate + PostgreSQL | Acceso a datos y ORM. |
| Migraciones | Flyway | Versionado de los esquemas `public` y tenant. |
| Seguridad | Spring Security + OAuth2 Resource Server | Autenticación stateless mediante JWT. |
| Identidad | Auth0 | Inicio de sesión e identidad federada. |
| Pagos | Mercado Pago SDK Java | Suscripciones, checkout y webhooks. |
| Observabilidad | Spring Boot Actuator | Endpoints de salud e información. |

---

## 2. Módulos principales

### Core: organizaciones, especialidades y provisioning

El módulo `modules/core` gestiona información transversal de la plataforma.

- `organization`: crea organizaciones, lista organizaciones y actualiza sus estados. Una organización contiene el `slug`, el tipo, el estado y el nombre de esquema asociado.
- `specialty`: administra el catálogo global de especialidades. Permite consultar especialidades activas, crear, editar, activar y desactivar registros.
- `tenant`: realiza el aprovisionamiento de una organización y aplica las migraciones del esquema tenant mediante `TenantProvisioningService` y `TenantMigrationService`.

### Tenant: operación privada de la clínica u organización

El módulo `modules/tenant` contiene la operación de cada organización.

- `user`: usuarios internos, roles y estados de cuenta.
- `patient`: registro, consulta, actualización y estado de pacientes.
- `doctor`: datos profesionales, especialidades, verificación, aprobación, activación y fotografía de perfil.
- `agenda`: disponibilidad semanal, excepciones de disponibilidad y generación de slots.
- `appointment`: creación, listado, resumen, actualización de estado y cancelación de citas.
- `branding`: información visual y configuración de marca de la organización.
- `marketplace`: perfil privado que se sincroniza hacia la experiencia pública de marketplace.

### API pública

El módulo `modules/publicapi` concentra recursos que no requieren una sesión de tenant tradicional.

- `auth`: login de tenant y puente de autenticación con Auth0.
- `doctorregistration`: registro público de médicos independientes.
- `marketplace`: búsqueda pública de médicos y clínicas, detalle por `slug`, slots públicos e inicio de checkout de cita.
- `patientportal`: perfil, avatar y citas de un paciente autenticado.
- `media`: entrega de recursos multimedia públicos.
- `health`: estado simple de la aplicación.

### Billing e integración externa

El módulo `modules/billing` consulta la suscripción, el historial de pagos, genera sesiones de checkout y procesa cancelaciones. `MercadoPagoWebhookController` recibe los eventos de Mercado Pago en `/api/billing/webhook/mercadopago`.

El módulo `modules/integration/auth0` contiene la integración de gestión con Auth0 cuando se requieren operaciones administrativas sobre usuarios o identidad.

---

## 3. Multitenancy, datos y migraciones

`TenantFilter` se ejecuta después de la autenticación Bearer. Si el JWT tiene `scope = TENANT`, `TenantResolverService` obtiene el esquema desde los claims `schema_name`, `schema`, `organization_slug`, `org_slug` o `slug`. Antes de usarlo, verifica que la organización exista y esté activa.

Si el token no representa un tenant, se utiliza el esquema por defecto `public`. `TenantContext` se limpia al terminar cada solicitud para impedir que el contexto de una organización se filtre hacia otra solicitud.

Las migraciones están separadas por alcance:

| Ubicación | Alcance |
| :--- | :--- |
| `src/main/resources/db/migration/public` | Tablas y cambios del esquema global `public`. |
| `src/main/resources/db/migration/tenant` | Tablas y cambios que se aplican a cada organización. |

Flyway valida y aplica las migraciones públicas durante el arranque. Las migraciones tenant se aplican durante el aprovisionamiento. Una migración ya aplicada en un entorno compartido no debe modificarse; los cambios posteriores se incorporan como una nueva versión.

---

## 4. Seguridad y control de acceso

La aplicación utiliza dos cadenas de seguridad stateless:

1. `/api/auth/**` utiliza tokens JWT emitidos por Auth0.
2. El resto de rutas privadas utiliza JWT internos firmados con HMAC-SHA256 y `APP_JWT_SECRET`.

Las rutas públicas actuales incluyen `/actuator/health`, `/actuator/info`, `/api/health`, recursos de autenticación pública, registro de doctor, marketplace público, media pública y el webhook de Mercado Pago.

Las rutas restantes requieren un token válido. El convertidor `JwtAuthConverter` transforma los claims del token en authorities utilizadas por Spring Security.

### Manejo de errores

`GlobalExceptionHandler` devuelve errores homogéneos en formato `ApiError` con los campos `status`, `error`, `message` y `path`.

| Situación | Código HTTP | Código de error |
| :--- | :--- | :--- |
| Regla de negocio o validación | 400 | `BUSINESS_ERROR`, `VALIDATION_ERROR` o `CONSTRAINT_VIOLATION` |
| Estado incompatible | 409 | `CONFLICT` |
| Permiso insuficiente | 403 | `FORBIDDEN` |
| Archivo mayor a 5 MB | 400 | `FILE_TOO_LARGE` |
| Error no controlado | 500 | `INTERNAL_SERVER_ERROR` |

---

## 5. Endpoints principales

| Recurso | Método | Descripción |
| :--- | :--- | :--- |
| `/api/platform/organizations` | GET / POST | Listado y registro de organizaciones. |
| `/api/platform/specialties` | GET / POST / PUT / PATCH | Administración del catálogo global de especialidades. |
| `/api/auth/auth0-tenant-login` | POST | Intercambio de sesión Auth0 para tenant. |
| `/api/auth/patient-login` | POST | Inicio de sesión de paciente desde Auth0. |
| `/api/tenant/users` | GET / POST / PATCH | Gestión de usuarios de una organización. |
| `/api/tenant/doctors` | GET / POST / PATCH | Gestión de doctores, estados, aprobación y fotografía. |
| `/api/tenant/patients` | GET / POST / PATCH | Gestión de pacientes. |
| `/api/tenant/appointments` | GET / POST / PATCH | Gestión de citas y cambios de estado. |
| `/api/tenant/doctors/{doctorId}/availability` | GET / POST / PATCH / DELETE | Disponibilidad de un doctor. |
| `/api/tenant/doctors/{doctorId}/slots` | GET | Slots disponibles para reservar. |
| `/api/public/marketplace/doctors` | GET | Búsqueda pública de médicos. |
| `/api/public/marketplace/clinics` | GET | Búsqueda pública de clínicas. |
| `/api/public/marketplace/doctors/{slug}/appointment-checkout` | POST | Inicio de checkout de cita pública. |
| `/api/public/patient-portal/me` | GET / PATCH | Perfil del paciente. |
| `/api/billing/subscription` | GET | Resumen de la suscripción del tenant. |
| `/api/billing/checkout-session` | POST | Creación de checkout de suscripción. |

---

## 6. Configuración y ejecución

La configuración se resuelve mediante variables de entorno. Las principales son:

- Base de datos: `DB_SERVER`, `DB_PORT`, `DB_NAME`, `DB_USERNAME`, `DB_PASSWORD`.
- Auth0: `AUTH0_DOMAIN`, `AUTH0_MANAGEMENT_DOMAIN`, `AUTH0_MANAGEMENT_CLIENT_ID`, `AUTH0_MANAGEMENT_CLIENT_SECRET`, `AUTH0_MANAGEMENT_AUDIENCE`.
- JWT interno: `APP_JWT_SECRET`, `APP_JWT_EXPIRATION_SECONDS`.
- Mercado Pago: `MERCADOPAGO_ACCESS_TOKEN`, `MERCADOPAGO_WEBHOOK_SECRET`, `MERCADOPAGO_WEBHOOK_URL`, `MERCADOPAGO_CURRENCY`.
- Frontend y CORS: `FRONTEND_URL`, `ALLOWED_ORIGINS`.
- Operación: `PORT`, `SHOW_SQL`.

Comandos habituales de desarrollo:

```powershell
.\mvnw.cmd spring-boot:run
.\mvnw.cmd test
```

La configuración de carga de archivos limita el tamaño por archivo y por solicitud a 5 MB.

---

## 7. Despliegue y operación

El backend está desplegado en **Render** y utiliza **Neon PostgreSQL** como base de datos de producción. El frontend desplegado en Vercel debe estar incluido en `FRONTEND_URL` y `ALLOWED_ORIGINS`; las mismas URLs deben estar registradas en Auth0 como callback, logout y web origin.

Las comprobaciones operativas principales son:

1. Consultar `/actuator/health`.
2. Confirmar la ejecución correcta de Flyway.
3. Validar login, una operación de tenant y aislamiento por esquema.
4. Validar el webhook y los flujos de Mercado Pago en el modo autorizado.

---

## 8. Pendientes técnicos del backend

Las siguientes capacidades no están implementadas y deben considerarse parte de la hoja de ruta:

- Auditoría de plataforma mediante `audit_logs` y eventos administrativos críticos.
- Endpoints globales para Super Admin: tenants, clínicas, doctores, pacientes, citas, pagos y estados.
- Roles `ADMIN`, `ORG_ADMIN` y `ASSISTANT` con permisos diferenciados.
- Aprobación, rechazo y bloqueo de clínicas, hospitales, doctores, pacientes y tenants.
- Endpoints de panel de clínica para `ORG_ADMIN`.
- Pruebas de integración por rol y pruebas en staging.
- Registro de notificaciones, confirmación y recordatorio de citas por WhatsApp, y confirmaciones por correo.
- Sugerencia de especialidad a partir de síntomas y resumen del motivo de cita para el médico como asistencia no diagnóstica.
- QA de producción contra Render y Neon con cuentas controladas.
