package dev.josearroyo.fitlog.data.model

import kotlinx.serialization.Serializable

@Serializable
enum class NivelExperiencia { PRINCIPIANTE, MEDIO, AVANZADO }

@Serializable
enum class MetodoComposicionCorporal { ANTROPOMETRIA, BIOIMPEDANCIA, AMBOS }

@Serializable
data class ValoracionFisica(
    val id: String = "",

    // 🚀 CRÍTICO: Sin este serializador personalizado, Firestore KMP no puede decodificar el Timestamp nativo
    @Serializable(with = TimestampLongSerializer::class)
    val fechaRegistro: Long = 0L,

    val pesoKg: Double = 0.0,
    val alturaCm: Double = 0.0,
    val objetivoInicial: String = "",
    val ultimoPeriodoConsistenciaMeses: Int? = null,
    val periodoInactividadActualMeses: Int? = null,
    val nivelExperiencia: NivelExperiencia = NivelExperiencia.PRINCIPIANTE,
    val mostrarComposicionAvanzada: Boolean = false,
    val metodoComposicion: MetodoComposicionCorporal = MetodoComposicionCorporal.ANTROPOMETRIA,

    // Antropometría
    val abdomen1: Double? = null,
    val abdomen2: Double? = null,
    val brazoFlexionado: Double? = null,
    val brazoRelajado: Double? = null,
    val gluteo: Double? = null,
    val musloProminente: Double? = null,
    val piernaMedial: Double? = null,
    val pantorrilla: Double? = null,
    val observacionesLadoIzquierdo: String = "",

    // Bioimpedancia
    val porcentajeGrasaCorporal: Double? = null,
    val masaMuscularKg: Double? = null,
    val grasaVisceral: Int? = null,
    val aguaCorporalPorcentaje: Double? = null,
    val edadMetabolica: Int? = null
)