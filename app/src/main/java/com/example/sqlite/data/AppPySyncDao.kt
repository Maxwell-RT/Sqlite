package com.example.sqlite.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

/**
 * DTO que representa un Traslado con sus Paradas intermedias para intercambio de datos JSON con app.py.
 */
data class TrasladoConParadas(
    val traslado: Traslado,
    val paradas: List<Parada>
)

/**
 * Métrica con el recuento actual de la cola de sincronización hacia el backend Python.
 */
data class ResumenSync(
    val pendientes: Int,
    val sincronizados: Int,
    val conError: Int
)

/**
 * DAO especializado para sincronización, exportación e importación de datos con una API o backend en Python (app.py).
 *
 * Facilita:
 * 1. La obtención de registros pendientes (traslados, paradas, bitácora) listos para serializar a JSON.
 * 2. La actualización atómica del estado y costo una vez que app.py procesa la información.
 * 3. La actualización de tablas de catálogo (tarifas, centros, tramos) desde app.py.
 */
@Dao
interface AppPySyncDao {

    // =========================================================================
    // 1. CONSULTAS DE EXPORTACIÓN / SYNC HACIA APP.PY (GET PENDING PAYLOADS)
    // =========================================================================

    /** Obtiene todos los traslados pendientes de sincronización hacia app.py */
    @Query("SELECT * FROM traslados WHERE estado != 'EN_CURSO' AND syncEstado != 'SINCRONIZADO' ORDER BY inicioTs ASC")
    suspend fun obtenerTrasladosPendientes(): List<Traslado>

    /** Obtiene las paradas registradas para un traslado específico */
    @Query("SELECT * FROM paradas WHERE trasladoId = :trasladoId ORDER BY orden ASC")
    suspend fun obtenerParadasDeTraslado(trasladoId: Long): List<Parada>

    /**
     * Consulta relacional que combina un traslado con sus paradas para armar el JSON completo hacia app.py.
     */
    @Transaction
    suspend fun obtenerTrasladoConParadas(trasladoId: Long): TrasladoConParadas? {
        val traslado = obtenerTrasladoPorId(trasladoId) ?: return null
        val paradas = obtenerParadasDeTraslado(trasladoId)
        return TrasladoConParadas(traslado, paradas)
    }

    /** Busca un traslado por su clave primaria */
    @Query("SELECT * FROM traslados WHERE id = :id LIMIT 1")
    suspend fun obtenerTrasladoPorId(id: Long): Traslado?

    /** Busca un traslado por su UUID de idempotencia (clave de sincronización con app.py) */
    @Query("SELECT * FROM traslados WHERE uuid = :uuid LIMIT 1")
    suspend fun obtenerTrasladoPorUuid(uuid: String): Traslado?

    /** Obtiene los últimos registros de bitácora de auditoría para enviar a app.py */
    @Query("SELECT * FROM bitacora ORDER BY id DESC LIMIT :limite")
    suspend fun obtenerBitacoraParaExportar(limite: Int = 100): List<Bitacora>

    // =========================================================================
    // 2. ACTUALIZACIÓN DE ESTADO SEGÚN LA RESPUESTA DE APP.PY (RESPONSE UPDATE)
    // =========================================================================

    /**
     * Marca el traslado como 'SINCRONIZADO' y actualiza los cálculos provenientes del backend Python (app.py).
     */
    @Query("""
        UPDATE traslados 
        SET syncEstado = 'SINCRONIZADO',
            syncError = NULL,
            distanciaOficial = :distanciaOficial,
            desviacionPct = :desviacionPct,
            desviado = :desviado,
            costoTotal = :costoTotal
        WHERE uuid = :uuid
    """)
    suspend fun marcarSincronizadoDesdeAppPy(
        uuid: String,
        distanciaOficial: Double?,
        desviacionPct: Double?,
        desviado: Boolean,
        costoTotal: Double?
    )

    /**
     * Registra un estado de error cuando la app.py rechaza o falla procesando un traslado.
     */
    @Query("""
        UPDATE traslados 
        SET syncEstado = 'ERROR',
            syncError = :mensajeError
        WHERE uuid = :uuid
    """)
    suspend fun registrarErrorDesdeAppPy(
        uuid: String,
        mensajeError: String
    )

    // =========================================================================
    // 3. MONITOR Y ESTADÍSTICAS EN TIEMPO REAL
    // =========================================================================

    /**
     * Flow observable con el resumen numérico de traslados pendientes, sincronizados y con error.
     */
    @Query("""
        SELECT 
            COALESCE(SUM(CASE WHEN syncEstado = 'PENDIENTE' THEN 1 ELSE 0 END), 0) AS pendientes,
            COALESCE(SUM(CASE WHEN syncEstado = 'SINCRONIZADO' THEN 1 ELSE 0 END), 0) AS sincronizados,
            COALESCE(SUM(CASE WHEN syncEstado = 'ERROR' THEN 1 ELSE 0 END), 0) AS conError
        FROM traslados 
        WHERE estado != 'EN_CURSO'
    """)
    fun observarResumenSync(): Flow<ResumenSync>

    // =========================================================================
    // 4. IMPORTACIÓN Y ACTUALIZACIÓN DE CATÁLOGOS DESDE APP.PY (UPSERT CATALOGS)
    // =========================================================================

    /** Inserta o actualiza una tarifa descargada desde la API app.py */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun guardarTarifaDesdeAppPy(tarifa: Tarifa): Long

    /** Inserta o actualiza un centro médico/base enviado por app.py */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun guardarCentroDesdeAppPy(centro: Centro): Long

    /** Inserta o actualiza un tramo vial entre centros enviado por app.py */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun guardarTramoDesdeAppPy(tramo: Tramo): Long
}
