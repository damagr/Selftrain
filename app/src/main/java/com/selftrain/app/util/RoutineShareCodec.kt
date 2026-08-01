package com.selftrain.app.util

import com.google.gson.Gson
import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets
import java.util.Base64
import java.util.zip.Deflater
import java.util.zip.Inflater

// Payload compartido por QR. Sin ids ni gifUrl: el receptor matchea por nombre
// y recalcula el gif con findMatchingGifUrl(name), como hace seedIfEmpty().
// Ponytail: JVM-puro (sin android.*) para poder testear en unit tests.
data class SharedRoutine(
    val v: Int = 1,
    val name: String,
    val method: String = "bilbo",
    val notes: String = "",
    val days: List<SharedDay>
)

data class SharedDay(
    val name: String,
    val exercises: List<SharedExercise>
)

data class SharedExercise(
    val name: String,
    val muscleGroup: String,
    val category: String,
    val isBilboEligible: Boolean,
    val equipment: String = ""
)

object RoutineShareCodec {
    private const val MAGIC = "ST1"            // json plano
    private const val MAGIC_DEFLATE = "STZ1"   // deflate + base64 (payloads grandes)
    private const val COMPRESS_THRESHOLD = 1024 // bytes crudos; QR sigue siendo pequeño y escaneable

    private val gson = Gson()

    fun encode(routine: SharedRoutine): String {
        val json = gson.toJson(routine)
        return if (json.toByteArray(StandardCharsets.UTF_8).size > COMPRESS_THRESHOLD) {
            MAGIC_DEFLATE + Base64.getEncoder().encodeToString(deflate(json))
        } else {
            MAGIC + json
        }
    }

    fun decode(payload: String): SharedRoutine? = try {
        when {
            payload.startsWith(MAGIC_DEFLATE) -> {
                val raw = Base64.getDecoder().decode(payload.removePrefix(MAGIC_DEFLATE))
                gson.fromJson(inflate(raw), SharedRoutine::class.java)
            }
            payload.startsWith(MAGIC) ->
                gson.fromJson(payload.removePrefix(MAGIC), SharedRoutine::class.java)
            else -> null
        }
    } catch (_: Exception) {
        null
    }

    private fun deflate(s: String): ByteArray {
        val deflater = Deflater(Deflater.BEST_COMPRESSION)
        val out = ByteArrayOutputStream()
        try {
            deflater.setInput(s.toByteArray(StandardCharsets.UTF_8))
            deflater.finish()
            val buf = ByteArray(4096)
            while (!deflater.finished()) {
                val n = deflater.deflate(buf)
                out.write(buf, 0, n)
            }
        } finally {
            deflater.end()
        }
        return out.toByteArray()
    }

    private fun inflate(bytes: ByteArray): String {
        val inflater = Inflater()
        val out = ByteArrayOutputStream()
        try {
            inflater.setInput(bytes)
            val buf = ByteArray(4096)
            while (!inflater.finished()) {
                val n = inflater.inflate(buf)
                out.write(buf, 0, n)
            }
        } finally {
            inflater.end()
        }
        return out.toString(StandardCharsets.UTF_8.name())
    }
}
