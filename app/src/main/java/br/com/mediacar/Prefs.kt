package br.com.mediacar

import android.content.Context

/** Tudo o que o app lembra: último vídeo, posição, equalizador e opções. */
class Prefs(context: Context) {
    private val sp = context.applicationContext.getSharedPreferences("mediacar", Context.MODE_PRIVATE)

    var lastPath: String?
        get() = sp.getString("lastPath", null)
        set(v) = sp.edit().putString("lastPath", v).apply()

    var lastPos: Long
        get() = sp.getLong("lastPos", 0L)
        set(v) = sp.edit().putLong("lastPos", v).apply()

    /** Estava tocando (e não pausado) quando o app foi fechado ou o carro desligou. */
    var wasPlaying: Boolean
        get() = sp.getBoolean("wasPlaying", false)
        set(v) = sp.edit().putBoolean("wasPlaying", v).apply()

    var autoResume: Boolean
        get() = sp.getBoolean("autoResume", true)
        set(v) = sp.edit().putBoolean("autoResume", v).apply()

    var fillMode: Boolean
        get() = sp.getBoolean("fillMode", false)
        set(v) = sp.edit().putBoolean("fillMode", v).apply()

    /** Tamanho da lista de pastas: 0 = médio, 1 = grande, 2 = enorme. */
    var sizeLevel: Int
        get() = sp.getInt("sizeLevel", 1)
        set(v) = sp.edit().putInt("sizeLevel", v).apply()

    var eqEnabled: Boolean
        get() = sp.getBoolean("eqEnabled", true)
        set(v) = sp.edit().putBoolean("eqEnabled", v).apply()

    var preamp: Float
        get() = sp.getFloat("preamp", 0f)
        set(v) = sp.edit().putFloat("preamp", v).apply()

    var bands: FloatArray
        get() {
            val s = sp.getString("bands", null) ?: return FloatArray(EqBands.COUNT)
            val parts = s.split(",")
            return FloatArray(EqBands.COUNT) { parts.getOrNull(it)?.toFloatOrNull() ?: 0f }
        }
        set(v) = sp.edit().putString("bands", v.joinToString(",")).apply()
}
