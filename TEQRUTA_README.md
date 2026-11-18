# TeqRuta – app Android (Room + MVVM)

Adaptación del proyecto MVVM/Room original a los requisitos de TeqRuta. Se conserva la arquitectura
(Room local → Repository → ViewModel → Compose) y se reemplaza el CRUD de productos.

Base local nueva: `teqruta.db` (la antigua `productos.db` no se toca).
Usuarios demo (clave `1234`): tecnico1, tecnico2, jefe, gerencia, admin.

## Cobertura de requisitos

| Requisito | Dónde |
|---|---|
| RF-01, RF-02 perfiles y vistas por rol | `AuthRepository`, `ui/TeqRutaRoot.kt` |
| RF-03 a RF-09 técnico | `ui/screen/TecnicoScreen.kt`, `TrasladoRepository` |
| RF-10 a RF-12 jefe | `ui/screen/JefeScreen.kt` |
| RF-13 a RF-15 gerencia, export CSV | `ui/screen/GerenciaScreen.kt`, `Reporte.aCsv` |
| RF-16 a RF-19 mantenedores y tarifas con vigencia | `ui/screen/AdminScreen.kt`, `AdminRepository` |
| RF-20 a RF-23 cálculo, tarifa vigente, desviación, auditoría | `data/remote/ServidorSimulado.kt` |
| RF-25 a RF-27 offline, estado de sync, duplicados (uuid) | `SyncManager`, `Traslado.uuid` |
| RNF-02/03 offline y persistencia | Room + sesión en SharedPreferences |
| RNF-06 cálculo fuera del dispositivo | solo en `ServidorSimulado` (reemplazable por API) |
| RNF-07 bitácora inmutable | `BitacoraDao` (solo insertar/leer) |
| RNF-10/11 consentimiento y ubicación puntual | `TecnicoScreen`, `LocationHelper` |
| RNF-15 umbral configurable | pestaña Parámetros |

## Pendiente / fuera de alcance de esta app
- RF-24 (precio de combustible desde fuente pública): hoy lo ingresa el administrador.
- API real y HTTPS (RNF-08/09): `ServidorSimulado` hace de servidor; falta cliente HTTP y tokens.
- Cifrado de la BD local (p. ej. SQLCipher) y panel web (supuesto S-05).
