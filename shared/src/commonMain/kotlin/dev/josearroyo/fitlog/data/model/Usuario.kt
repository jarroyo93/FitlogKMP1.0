package dev.josearroyo.fitlog.data.model

import kotlinx.serialization.Serializable


@Serializable
enum class Sexo(val etiqueta: String) {
    MASCULINO("Masculino"),
    FEMENINO("Femenino"),
    OTRO("Otro")
}
@Serializable
enum class EstadoSuscripcion { ACTIVO, SUSPENDIDO, VENCIDO, HUERFANO }

@Serializable
enum class RolUsuario { SUPERADMIN, ENTRENADOR, ATLETA }

@Serializable
enum class TipoPlanSuscripcion(val dias: Int, val etiqueta: String) {
    SEMANAL(7, "Semanal (7 días)"),
    QUINCENAL(15, "Quincenal (15 días)"),
    MENSUAL(30, "Mensual (30 días)"),
    TRIMESTRAL(90, "Trimestral (90 días)"),
    PERSONALIZADO(0, "Personalizado")
}

@Serializable
enum class EstadoPeriodo { ACTIVO, DIFERIDO, COMPLETADO, CANCELADO, CONGELADO }

@Serializable
data class PeriodoFacturable(
    val id: String = "",
    val entrenadorId: String = "",
    val atletaId: String = "",
    val tipoPlan: String = "",
    val fechaInicio: Long = 0L,
    val fechaFin: Long? = null,
    val fechaCreacion: Long = 0L,
    val estado: EstadoPeriodo = EstadoPeriodo.DIFERIDO,
    val diasRestantesAlCongelar: Long = 0
)

@Serializable
data class Usuario(
    val id: String = "",
    val rol: RolUsuario = RolUsuario.ATLETA,
    val entrenadorId: String? = null,
    val estadoSuscripcion: EstadoSuscripcion = EstadoSuscripcion.HUERFANO,
    val codigoVinculacion: String? = null,
    val expiracionCodigo: Long? = null,
    val authId: String = "",
    val fechaVencimientoAnualidad: Long? = null,
    val nombres: String = "",
    val apellidos: String = "",
    val tipoDocumento: String = "",
    val numeroDocumento: String = "",

    // 🚀 APLICAMOS EL SERIALIZADOR AQUÍ:
    @Serializable(with = TimestampLongSerializer::class)
    val fechaNacimiento: Long = 0L,

    val tipoSangre: String = "",
    val sexo: Sexo = Sexo.MASCULINO,
    val nacionalidad: String = "",
    val telefono: String = "",
    val planActivo: String = "Ninguno",
    val fechaInicioSuscripcion: Long? = null,
    val vencimientoSuscripcion: Long? = null,
    val correo: String = "",

    // 🚀 Y TAMBIÉN AQUÍ (Ya que en tu app nativa fechaCreacion era de tipo Date/Timestamp):
    @Serializable(with = TimestampLongSerializer::class)
    val fechaCreacion: Long = 0L,

    val saldoMilisegundosRestantes: Long? = null,
    val motivoPausa: String? = null,
    val requiereCambioContrasena: Boolean = false,
    val fotoPerfilUrl: String? = null,
    val especialidad: String? = null,
    val biografia: String? = null,
    val certificaciones: List<String> = listOf()
)