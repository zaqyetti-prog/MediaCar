package br.com.mediacar

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Cores no estilo iOS (escuro, com azul de destaque). */
object Pal {
    val Accent = Color(0xFF0A84FF)
    val Green = Color(0xFF30D158)
    val Orange = Color(0xFFFF9F0A)
    val Pink = Color(0xFFFF375F)
    val Purple = Color(0xFFBF5AF2)
    val Sub = Color(0xFFAEB4C2)
}

/** Fundo escuro com brilhos de cor, parecido com o papel de parede do iOS. */
@Composable
fun AppBackground(content: @Composable BoxScope.() -> Unit) {
    Box(
        Modifier
            .fillMaxSize()
            .background(Color(0xFF05070D))
            .drawBehind {
                drawRect(
                    Brush.radialGradient(
                        listOf(Color(0x660A84FF), Color.Transparent),
                        center = Offset(size.width * 0.12f, size.height * 0.08f),
                        radius = size.width * 0.6f
                    )
                )
                drawRect(
                    Brush.radialGradient(
                        listOf(Color(0x55BF5AF2), Color.Transparent),
                        center = Offset(size.width * 0.92f, size.height * 0.96f),
                        radius = size.width * 0.55f
                    )
                )
            },
        content = content
    )
}

/** Efeito de "vidro" translúcido com borda clara. */
fun Modifier.glass(radius: Dp = 28.dp, alpha: Float = 0.10f): Modifier {
    val shape = RoundedCornerShape(radius)
    return this
        .clip(shape)
        .background(
            Brush.verticalGradient(listOf(Color.White.copy(alpha = alpha + 0.06f), Color.White.copy(alpha = alpha))),
            shape
        )
        .border(
            1.dp,
            Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.45f), Color.White.copy(alpha = 0.08f))),
            shape
        )
}

@Composable
fun GlassButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    strong: Boolean = false,
    content: @Composable RowScope.() -> Unit
) {
    Row(
        modifier
            .glass(radius = 36.dp, alpha = if (strong) 0.30f else 0.12f)
            .clickable(onClick = onClick)
            .padding(horizontal = 22.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        content = content
    )
}

@Composable
fun CircleButton(
    glyph: Glyph,
    onClick: () -> Unit,
    diameter: Dp = 72.dp,
    accent: Boolean = false,
    modifier: Modifier = Modifier
) {
    Box(
        modifier
            .size(diameter)
            .clip(CircleShape)
            .background(if (accent) Pal.Accent else Color.White.copy(alpha = 0.16f))
            .border(1.dp, Color.White.copy(alpha = 0.3f), CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        GlyphIcon(glyph, diameter * 0.5f)
    }
}

/** Ícones desenhados em código (não dependem de fontes nem de bibliotecas). */
enum class Glyph { PLAY, PAUSE, NEXT, PREV, REW, FWD, FOLDER, VIDEO, BACK, CHEVRON, EQ, DRIVE }

@Composable
fun GlyphIcon(glyph: Glyph, iconSize: Dp, tint: Color = Color.White, modifier: Modifier = Modifier) {
    Canvas(modifier.size(iconSize)) { drawGlyph(glyph, size.minDimension, tint) }
}

private fun DrawScope.drawGlyph(g: Glyph, s: Float, c: Color) {
    fun p(x: Float, y: Float) = Offset(x * s, y * s)
    fun tri(x1: Float, y1: Float, x2: Float, y2: Float, x3: Float, y3: Float) {
        val path = Path().apply {
            moveTo(x1 * s, y1 * s)
            lineTo(x2 * s, y2 * s)
            lineTo(x3 * s, y3 * s)
            close()
        }
        drawPath(path, c)
    }
    val stroke = Stroke(width = s * 0.09f, cap = StrokeCap.Round, join = StrokeJoin.Round)
    when (g) {
        Glyph.PLAY -> tri(0.30f, 0.18f, 0.82f, 0.50f, 0.30f, 0.82f)
        Glyph.PAUSE -> {
            drawRoundRect(c, p(0.24f, 0.2f), Size(0.18f * s, 0.6f * s), CornerRadius(0.04f * s))
            drawRoundRect(c, p(0.58f, 0.2f), Size(0.18f * s, 0.6f * s), CornerRadius(0.04f * s))
        }
        Glyph.NEXT -> {
            tri(0.20f, 0.20f, 0.68f, 0.50f, 0.20f, 0.80f)
            drawRect(c, p(0.72f, 0.2f), Size(0.10f * s, 0.6f * s))
        }
        Glyph.PREV -> {
            tri(0.80f, 0.20f, 0.32f, 0.50f, 0.80f, 0.80f)
            drawRect(c, p(0.18f, 0.2f), Size(0.10f * s, 0.6f * s))
        }
        Glyph.REW -> {
            tri(0.50f, 0.25f, 0.12f, 0.50f, 0.50f, 0.75f)
            tri(0.88f, 0.25f, 0.50f, 0.50f, 0.88f, 0.75f)
        }
        Glyph.FWD -> {
            tri(0.12f, 0.25f, 0.50f, 0.50f, 0.12f, 0.75f)
            tri(0.50f, 0.25f, 0.88f, 0.50f, 0.50f, 0.75f)
        }
        Glyph.FOLDER -> {
            drawRoundRect(c, p(0.10f, 0.20f), Size(0.36f * s, 0.22f * s), CornerRadius(0.05f * s))
            drawRoundRect(c, p(0.10f, 0.30f), Size(0.80f * s, 0.52f * s), CornerRadius(0.07f * s))
        }
        Glyph.VIDEO -> {
            drawRoundRect(c, p(0.10f, 0.20f), Size(0.80f * s, 0.60f * s), CornerRadius(0.10f * s), style = stroke)
            tri(0.42f, 0.35f, 0.68f, 0.50f, 0.42f, 0.65f)
        }
        Glyph.BACK -> {
            val path = Path().apply {
                moveTo(0.62f * s, 0.20f * s)
                lineTo(0.36f * s, 0.50f * s)
                lineTo(0.62f * s, 0.80f * s)
            }
            drawPath(path, c, style = stroke)
        }
        Glyph.CHEVRON -> {
            val path = Path().apply {
                moveTo(0.38f * s, 0.20f * s)
                lineTo(0.64f * s, 0.50f * s)
                lineTo(0.38f * s, 0.80f * s)
            }
            drawPath(path, c, style = stroke)
        }
        Glyph.EQ -> {
            for (x in floatArrayOf(0.22f, 0.50f, 0.78f)) {
                drawLine(c, p(x, 0.18f), p(x, 0.82f), strokeWidth = s * 0.07f, cap = StrokeCap.Round)
            }
            drawCircle(c, s * 0.11f, p(0.22f, 0.65f))
            drawCircle(c, s * 0.11f, p(0.50f, 0.35f))
            drawCircle(c, s * 0.11f, p(0.78f, 0.55f))
        }
        Glyph.DRIVE -> {
            drawRoundRect(c, p(0.20f, 0.38f), Size(0.60f * s, 0.46f * s), CornerRadius(0.06f * s))
            drawRoundRect(c, p(0.34f, 0.14f), Size(0.32f * s, 0.28f * s), CornerRadius(0.03f * s))
        }
    }
}

/** Barra deslizante grande (fácil de tocar no carro). [fraction] vai de 0 a 1. */
@Composable
fun BigSlider(
    fraction: Float,
    onChange: (Float) -> Unit,
    onFinish: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = Pal.Accent
) {
    val change by rememberUpdatedState(onChange)
    val finish by rememberUpdatedState(onFinish)
    Box(
        modifier
            .height(56.dp)
            .pointerInput(Unit) {
                detectTapGestures(onTap = { o ->
                    change((o.x / size.width).coerceIn(0f, 1f))
                    finish()
                })
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { o -> change((o.x / size.width).coerceIn(0f, 1f)) },
                    onDragEnd = { finish() },
                    onDragCancel = { finish() }
                ) { ch, _ ->
                    ch.consume()
                    change((ch.position.x / size.width).coerceIn(0f, 1f))
                }
            }
            .drawBehind {
                val h = 10.dp.toPx()
                val cy = size.height / 2
                drawRoundRect(
                    Color.White.copy(alpha = 0.25f),
                    Offset(0f, cy - h / 2),
                    Size(size.width, h),
                    CornerRadius(h / 2)
                )
                val x = fraction.coerceIn(0f, 1f) * size.width
                drawRoundRect(color, Offset(0f, cy - h / 2), Size(x.coerceAtLeast(h), h), CornerRadius(h / 2))
                val r = 15.dp.toPx()
                drawCircle(Color.White, r, Offset(x.coerceIn(r, size.width - r), cy))
            }
    )
}

/** Barra deslizante vertical do equalizador (de [min] a [max] dB). Toque duplo zera. */
@Composable
fun VSlider(
    value: Float,
    min: Float,
    max: Float,
    onChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val change by rememberUpdatedState(onChange)

    fun toValue(y: Float, h: Int): Float {
        val f = (1f - y / h).coerceIn(0f, 1f)
        val raw = min + f * (max - min)
        val snapped = Math.round(raw * 2f) / 2f // passos de 0,5 dB
        return if (kotlin.math.abs(snapped) < 0.5f) 0f else snapped
    }

    Box(
        modifier
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = { change(0f) },
                    onTap = { o -> change(toValue(o.y, size.height)) }
                )
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { o -> change(toValue(o.y, size.height)) }
                ) { ch, _ ->
                    ch.consume()
                    change(toValue(ch.position.y, size.height))
                }
            }
            .drawBehind {
                val w = 8.dp.toPx()
                val cx = size.width / 2
                drawRoundRect(
                    Color.White.copy(alpha = 0.22f),
                    Offset(cx - w / 2, 0f),
                    Size(w, size.height),
                    CornerRadius(w / 2)
                )
                val zeroY = size.height * (max / (max - min))
                val vy = (1f - (value - min) / (max - min)) * size.height
                val top = minOf(zeroY, vy)
                val bottom = maxOf(zeroY, vy)
                drawRoundRect(
                    if (value >= 0f) Pal.Accent else Pal.Orange,
                    Offset(cx - w / 2, top),
                    Size(w, (bottom - top).coerceAtLeast(1f)),
                    CornerRadius(w / 2)
                )
                drawLine(
                    Color.White.copy(alpha = 0.5f),
                    Offset(cx - 12.dp.toPx(), zeroY),
                    Offset(cx + 12.dp.toPx(), zeroY),
                    strokeWidth = 2.dp.toPx()
                )
                val r = minOf(15.dp.toPx(), size.width / 2)
                drawCircle(Color.White, r, Offset(cx, vy.coerceIn(r, size.height - r)))
            }
    )
}
