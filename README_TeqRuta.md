# TeqRuta – app Android (Room + MVVM)

Adaptación del proyecto `Sqlite_MVVM_ROOM` a los requisitos funcionales y no funcionales de TeqRuta.
Se conserva la arquitectura: **Room/SQLite local → Repository → ViewModel (StateFlow) → Compose**.
La BD ahora es `teqruta.db` (la antigua `productos.db` queda intacta).

Usuarios demo (clave `1234`): `tecnico1`, `tecnico2`, `jefe`, `gerencia`, `admin`.

## Estructura
- `data/` Entidades, DAOs, `AppDatabase`, repositorios, `SyncManager`, `DemoData`.
- `data/remote/ServidorSimulado.kt` Stand-in del servidor: aquí está TODO el cálculo de costos (RNF-06). Se reemplaza por una API real.
- `ui/` ViewModels (`Auth`, `Tecnico`, `Jefe`, `Gerencia`, `Admin`) y `ui/screen/` pantallas Compose.

## Cobertura de requisitos
| Requisito | Estado | Dónde |
|---|---|---|
| RF-01, RF-02 | Hecho (local) | `AuthRepository`, `TeqRutaRoot` (cada rol ve solo su pantalla; el técnico consulta solo sus traslados) |
| RF-03 a RF-09 | Hecho | `TecnicoScreen`, `TrasladoRepository` |
| RF-10 a RF-12 | Hecho | `JefeScreen` (filtros, validar/observar/rechazar con comentario y bitácora, desviaciones) |
| RF-13, RF-14 | Hecho | `GerenciaScreen` (período, por cliente, contrato, técnico y vehículo; solo traslados validados) |
| RF-15 | Parcial | Exporta CSV (abre en Excel) por el menú compartir. Falta PDF |
| RF-16 a RF-19 | Hecho | `AdminScreen` (tarifas con vigencia, nunca se borran) |
| RF-20 a RF-23 | Simulado | `ServidorSimulado`: costo en el "servidor", tarifa vigente a la fecha del traslado, umbral configurable, cálculo auditado en bitácora |
| RF-24 | Pendiente | Consulta automática de precio de combustible: requiere servidor/API. Hoy el admin edita la tarifa |
| RF-25, RF-26 | Hecho | Guarda offline; sincroniza al volver la señal (callback de red) y cada 5 min; estados pendiente / sincronizado / error |
| RF-27 | Base lista | `uuid` único por traslado (idempotencia). Falta resolución de conflictos con un servidor real |
| RNF-01 | Hecho | minSdk 24 (Android 7+) |
| RNF-02, RNF-03 | Hecho | Todo se guarda en Room desde el primer paso; sesión persistente |
| RNF-04, RNF-05 | Hecho | Flujo corto, botones de 56 dp, mensajes de error claros |
| RNF-06 | Hecho | El técnico no calcula costos |
| RNF-07 | Hecho | Tabla `bitacora` solo con insertar/consultar desde el DAO |
| RNF-08 | Parcial | Sin respaldo en la nube (`allowBackup=false`). Falta cifrar la BD (SQLCipher) y TLS al integrar la API |
| RNF-09 | Parcial | Roles y sesión con expiración (12 h). Token real depende del servidor |
| RNF-10, RNF-11 | Parcial | Consentimiento previo y ubicación solo al iniciar/finalizar. Falta política de retención |
| RNF-12 a RNF-14 | Hecho | Sincronización en segundo plano, reintentos, reportes reproducibles |
| RNF-15 | Hecho | Umbral (y tarifas) editables en la app sin recompilar |
| RNF-16 | Parcial | Capa `remote` separada para conectar API/ERP. La API y el servidor no se incluyen |

Nota: las contraseñas se guardan con SHA-256 sin sal (suficiente para demo; en producción debe autenticar el servidor).
