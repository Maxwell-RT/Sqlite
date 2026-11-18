#!/usr/bin/env python3
"""
Backend Servidor Flask (app.py) para TeqRuta.

Procesa las solicitudes de sincronización enviadas desde el AppPySyncDao
de la aplicación Android TeqRuta. Realiza el cálculo oficial de distancias
y costos de traslado (RNF-06) fuera del dispositivo móvil.
"""

from flask import Flask, request, jsonify
import logging

app = Flask(__name__)
logging.basicConfig(level=logging.INFO)

# --- Base de datos simulada de tarifas y tramos en el servidor Python ---

TARIFAS_SERVIDOR = {
    "COMBUSTIBLE": 1250.0,    # $ / litro
    "KM_PARTICULAR": 350.0    # $ / km
}

# Distancias oficiales entre centros (origenId, destinoId) -> km
TRAMOS_SERVIDOR = {
    (1, 2): 15.5,
    (2, 1): 15.5,
    (2, 3): 22.0,
    (3, 2): 22.0,
    (1, 3): 35.0,
    (3, 1): 35.0
}

UMBRAL_DESVIACION_PCT = 10.0


@app.route("/api/health", methods=["GET"])
def health_check():
    """Endpoint de estado para verificar conectividad desde Android."""
    return jsonify({
        "estado": "OK",
        "servicio": "TeqRuta Python Backend (app.py)",
        "version": "1.0.0"
    }), 200


@app.route("/api/sync/traslados", methods=["POST"])
def sincronizar_traslado():
    """
    Endpoint principal consumido por la app Android a través de AppPySyncDao.

    Espera un JSON estructurado como:
    {
        "traslado": {
            "uuid": "...",
            "tecnicoId": 1,
            "vehiculoId": 1,
            "origenId": 1,
            "destinoId": 2,
            "kmInicio": 1000,
            "kmFin": 1020,
            "peajes": 1500,
            "inicioTs": 1700000000000
        },
        "paradas": [
            { "centroId": 3, "kmLlegada": 1010, "peajes": 500 }
        ]
    }
    """
    payload = request.get_json(silent=True)
    if not payload:
        return jsonify({"error": "Payload JSON no válido o vacío"}), 400

    traslado = payload.get("traslado", {})
    paradas = payload.get("paradas", [])

    uuid_val = traslado.get("uuid")
    if not uuid_val:
        return jsonify({"error": "El campo 'uuid' del traslado es obligatorio"}), 400

    km_inicio = traslado.get("kmInicio", 0)
    km_fin = traslado.get("kmFin")

    if km_fin is None:
        return jsonify({
            "uuid": uuid_val,
            "exito": False,
            "error": "El traslado no tiene un kilometraje final registrado"
        }), 400

    declarada = float(km_fin - km_inicio)
    origen_id = traslado.get("origenId")
    destino_id = traslado.get("destinoId")

    # 1. Distancia oficial calculada por tramos
    cadena = [origen_id, destino_id] + [p.get("centroId") for p in paradas]
    distancia_oficial = 0.0
    completa = True

    for a, b in zip(cadena[:-1], cadena[1:]):
        if a == b:
            continue
        dist = TRAMOS_SERVIDOR.get((a, b))
        if dist is None:
            completa = False
            break
        distancia_oficial += dist

    dist_oficial_res = distancia_oficial if completa else None

    # 2. Porcentaje de desviación
    desviacion_pct = None
    desviado = False
    if dist_oficial_res and dist_oficial_res > 0:
        desviacion_pct = abs(declarada - dist_oficial_res) / dist_oficial_res * 100.0
        desviado = desviacion_pct > UMBRAL_DESVIACION_PCT

    # 3. Cálculo de costos
    distancia_costo = dist_oficial_res if dist_oficial_res is not None else declarada
    tipo_vehiculo = traslado.get("tipoVehiculo", "EMPRESA")
    consumo_kml = float(traslado.get("consumoKmPorLitro", 10.0))
    peajes = int(traslado.get("peajes", 0))

    if tipo_vehiculo == "EMPRESA":
        precio_litro = TARIFAS_SERVIDOR.get("COMBUSTIBLE", 1250.0)
        costo_base = (distancia_costo / consumo_kml) * precio_litro
    else:
        precio_km = TARIFAS_SERVIDOR.get("KM_PARTICULAR", 350.0)
        costo_base = distancia_costo * precio_km

    costo_total = costo_base + peajes

    app.logger.info(f"Traslado {uuid_val} procesado. Costo: ${costo_total:.2f}, Desviado: {desviado}")

    return jsonify({
        "uuid": uuid_val,
        "exito": True,
        "distanciaOficial": dist_oficial_res,
        "desviacionPct": round(desviacion_pct, 2) if desviacion_pct else None,
        "desviado": desviado,
        "costoTotal": round(costo_total, 2)
    }), 200


if __name__ == "__main__":
    print("=" * 60)
    print(" Servidor Python Backend para TeqRuta (app.py)")
    print(" Escuchando en http://0.0.0.0:5000")
    print("=" * 60)
    app.run(host="0.0.0.0", port=5000, debug=True)
