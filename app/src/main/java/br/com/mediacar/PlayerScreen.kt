package br.com.mediacar

import android.view.SurfaceView
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.delay

@Composable
fun PlayerScreen(
    c: PlayerController,
    fill: Boolean,
    waitingName: String?,
    onCancelWait: () -> Unit,
    onBack: () -> Unit,
    onEq: () -> Unit,
    onToggleFill: () -> Unit
) {
    val context = LocalContext.current
    val surface = remember { SurfaceView(context) }
    DisposableEffect(surface) {
        c.player.setVideoSurfaceView(surface)
        onDispose { c.player.clearVideoSurfaceView(surface) }
    }

    var showUi by remember { mutableStateOf(true) }
    var poke by remember { mutableIntStateOf(0) }
    var drag by remember { mutableStateOf<Float?>(null) }

    // Esconde os controles depois de alguns segundos tocando
    LaunchedEffect(showUi, poke, c.playing) {
        if (showUi && c.playing) {
            delay(5000)
            showUi = false
        }
    }
    val uiVisible = showUi || !c.playing

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = {
                        showUi = !showUi
                        poke++
                    },
                    onDoubleTap = { o ->
                        // toque duplo: esquerda volta 10 s, direita avança 10 s
                        if (o.x < size.width / 2) c.seekBy(-10_000) else c.seekBy(10_000)
                        showUi = true
                        poke++
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        AndroidView(
            factory = { surface },
            modifier = if (fill) Modifier.fillMaxSize() else Modifier.aspectRatio(c.aspect.coerceIn(0.4f, 3.5f))
        )

        if (uiVisible) {
            // Barra de cima
            Row(
                Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .background(Brush.verticalGradient(listOf(Color(0xCC000000), Color.Transparent)))
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircleButton(Glyph.BACK, onBack, 72.dp)
                Spacer(Modifier.width(16.dp))
                Text(
                    c.title,
                    fontSize = 28.sp,
                    color = Color.White,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                GlassButton(onClick = {
                    onToggleFill()
                    poke++
                }) {
                    Text(if (fill) "Esticado" else "Ajustado", fontSize = 22.sp, color = Color.White)
                }
                Spacer(Modifier.width(12.dp))
                GlassButton(onClick = onEq) {
                    GlyphIcon(Glyph.EQ, 30.dp)
                    Spacer(Modifier.width(8.dp))
                    Text("EQ", fontSize = 22.sp, color = Color.White, fontWeight = FontWeight.Medium)
                }
            }

            // Controles de baixo
            Column(
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xDD000000))))
                    .padding(horizontal = 28.dp, vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    val shownPos = drag?.let { (it * c.durationMs).toLong() } ?: c.positionMs
                    val frac = drag ?: if (c.durationMs > 0) (c.positionMs.toFloat() / c.durationMs).coerceIn(0f, 1f) else 0f
                    Text(FileUtil.fmtTime(shownPos), fontSize = 22.sp, color = Color.White)
                    Spacer(Modifier.width(14.dp))
                    BigSlider(
                        fraction = frac,
                        onChange = {
                            drag = it
                            poke++
                        },
                        onFinish = {
                            val f = drag
                            if (f != null && c.durationMs > 0) c.player.seekTo((f * c.durationMs).toLong())
                            drag = null
                        },
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(14.dp))
                    Text(FileUtil.fmtTime(c.durationMs), fontSize = 22.sp, color = Color.White)
                }
                Spacer(Modifier.padding(top = 6.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(22.dp)
                ) {
                    CircleButton(Glyph.REW, { c.seekBy(-10_000); poke++ }, 72.dp)
                    CircleButton(Glyph.PREV, { c.previous(); poke++ }, 72.dp)
                    CircleButton(
                        if (c.playing) Glyph.PAUSE else Glyph.PLAY,
                        { c.togglePlay(); poke++ },
                        96.dp,
                        accent = true
                    )
                    CircleButton(Glyph.NEXT, { c.next(); poke++ }, 72.dp)
                    CircleButton(Glyph.FWD, { c.seekBy(10_000); poke++ }, 72.dp)
                }
            }
        }

        // Aguardando o pendrive ao retomar automaticamente
        if (waitingName != null) {
            MessageCard(
                title = "Retomando o último vídeo",
                text = "$waitingName\nAguardando o pendrive ficar pronto…",
                button = "Cancelar",
                onButton = onCancelWait,
                modifier = Modifier.align(Alignment.Center)
            )
        } else if (!c.loaded) {
            MessageCard(
                title = "Nenhum vídeo carregado",
                text = "Volte e escolha um vídeo na lista de pastas.",
                button = "Voltar às pastas",
                onButton = onBack,
                modifier = Modifier.align(Alignment.Center)
            )
        } else if (c.errorMsg != null) {
            MessageCard(
                title = "Não foi possível reproduzir",
                text = c.errorMsg ?: "",
                button = if (c.player.hasNextMediaItem()) "Próximo vídeo" else "Voltar às pastas",
                onButton = { if (c.player.hasNextMediaItem()) c.next() else onBack() },
                modifier = Modifier.align(Alignment.Center)
            )
        }
    }
}

@Composable
fun MessageCard(
    title: String,
    text: String,
    button: String,
    onButton: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier
            .padding(32.dp)
            .glass(radius = 32.dp, alpha = 0.22f)
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(title, fontSize = 32.sp, color = Color.White, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        Spacer(Modifier.padding(top = 12.dp))
        Text(text, fontSize = 24.sp, color = Pal.Sub, textAlign = TextAlign.Center)
        Spacer(Modifier.padding(top = 20.dp))
        GlassButton(onClick = onButton, strong = true) {
            Text(button, fontSize = 26.sp, color = Color.White, fontWeight = FontWeight.Medium)
        }
    }
}
