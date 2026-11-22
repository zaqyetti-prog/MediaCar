package br.com.mediacar

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Equalizador de 15 bandas, com predefinições e pré-amplificação. */
@Composable
fun EqualizerScreen(c: PlayerController, prefs: Prefs, onClose: () -> Unit) {
    var enabled by remember { mutableStateOf(prefs.eqEnabled) }
    val gains = remember { mutableStateListOf<Float>().apply { addAll(prefs.bands.toList()) } }
    var pre by remember { mutableFloatStateOf(prefs.preamp) }

    fun apply() {
        val arr = gains.toFloatArray()
        prefs.bands = arr
        prefs.eqEnabled = enabled
        prefs.preamp = pre
        c.eq.update(enabled, arr, pre)
    }

    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 14.dp)) {
        // Cabeçalho
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            CircleButton(Glyph.BACK, onClose, 72.dp)
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text("Equalizador", fontSize = 38.sp, color = Color.White, fontWeight = FontWeight.Bold)
                Text(
                    "15 bandas  •  toque duas vezes numa banda para zerar",
                    fontSize = 18.sp,
                    color = Pal.Sub,
                    maxLines = 1
                )
            }
            GlassButton(onClick = {
                enabled = !enabled
                apply()
            }, strong = enabled) {
                Text(if (enabled) "Ligado" else "Desligado", fontSize = 24.sp, color = Color.White, fontWeight = FontWeight.Medium)
            }
            Spacer(Modifier.width(12.dp))
            GlassButton(onClick = {
                for (i in gains.indices) gains[i] = 0f
                pre = 0f
                apply()
            }) {
                Text("Zerar", fontSize = 24.sp, color = Color.White)
            }
        }

        // Predefinições
        Row(
            Modifier.fillMaxWidth().padding(vertical = 12.dp).horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            for ((name, values) in EqPresets.list) {
                val selected = gains.toFloatArray().contentEquals(values)
                GlassButton(onClick = {
                    for (i in values.indices) gains[i] = values[i]
                    apply()
                }, strong = selected) {
                    Text(name, fontSize = 24.sp, color = Color.White, fontWeight = FontWeight.Medium)
                }
            }
        }

        // As 15 bandas
        Row(Modifier.weight(1f).fillMaxWidth()) {
            for (i in 0 until EqBands.COUNT) {
                Column(
                    Modifier.weight(1f).fillMaxHeight(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        fmtDb(gains[i]),
                        fontSize = 16.sp,
                        color = if (gains[i] != 0f) Color.White else Pal.Sub,
                        maxLines = 1,
                        softWrap = false
                    )
                    VSlider(
                        value = gains[i],
                        min = -EqBands.MAX_DB,
                        max = EqBands.MAX_DB,
                        onChange = {
                            gains[i] = it
                            apply()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                            .alpha(if (enabled) 1f else 0.4f)
                    )
                    Text(
                        EqBands.LABELS[i],
                        fontSize = 16.sp,
                        color = Pal.Sub,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
        }

        // Pré-amplificação
        Row(Modifier.fillMaxWidth().padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Pré-amp", fontSize = 22.sp, color = Color.White, fontWeight = FontWeight.Medium)
            Spacer(Modifier.width(16.dp))
            BigSlider(
                fraction = (pre + EqBands.MAX_DB) / (2 * EqBands.MAX_DB),
                onChange = {
                    val raw = it * 2 * EqBands.MAX_DB - EqBands.MAX_DB
                    pre = Math.round(raw * 2f) / 2f
                    apply()
                },
                onFinish = {},
                modifier = Modifier.weight(1f).alpha(if (enabled) 1f else 0.4f)
            )
            Spacer(Modifier.width(16.dp))
            Text(fmtDb(pre) + " dB", fontSize = 22.sp, color = Color.White)
        }
        Text(
            "Se o som estourar ao aumentar graves, baixe o pré-amp.",
            fontSize = 16.sp,
            color = Pal.Sub
        )
    }
}

private fun fmtDb(v: Float): String {
    if (v == 0f) return "0"
    val sign = if (v > 0f) "+" else ""
    return if (v % 1f == 0f) sign + v.toInt().toString() else sign + String.format(java.util.Locale.US, "%.1f", v)
}
