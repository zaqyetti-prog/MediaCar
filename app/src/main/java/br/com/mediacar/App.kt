package br.com.mediacar

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import kotlinx.coroutines.delay
import java.io.File

enum class Screen { BROWSER, PLAYER, EQ }

/** Controla qual tela aparece e a retomada automática do último vídeo. */
@Composable
fun App(prefs: Prefs, c: PlayerController, hasStorage: Boolean, onRequestPermission: () -> Unit) {
    var screen by remember { mutableStateOf(Screen.BROWSER) }
    var eqReturn by remember { mutableStateOf(Screen.BROWSER) }
    var dir by remember {
        mutableStateOf(prefs.lastPath?.let { File(it).parentFile }?.takeIf { it.isDirectory })
    }
    var rescan by remember { mutableIntStateOf(0) }
    var sizeLevel by remember { mutableIntStateOf(prefs.sizeLevel) }
    var fill by remember { mutableStateOf(prefs.fillMode) }
    var waiting by remember { mutableStateOf<File?>(null) }
    var cancelled by remember { mutableStateOf(false) }

    val roots = remember(rescan, hasStorage) { if (hasStorage) FileUtil.roots() else emptyList() }

    // Atualiza posição na tela e grava o progresso
    LaunchedEffect(Unit) {
        while (true) {
            c.tick()
            delay(500)
        }
    }

    // Ao abrir o app: se estava tocando, volta a tocar sozinho de onde parou.
    // Se o pendrive ainda não montou (carro acabou de ligar), espera até ~2 minutos.
    LaunchedEffect(hasStorage) {
        if (!hasStorage || !prefs.autoResume || !prefs.wasPlaying) return@LaunchedEffect
        val path = prefs.lastPath ?: return@LaunchedEffect
        val f = File(path)
        waiting = f
        screen = Screen.PLAYER
        var tries = 0
        while (!f.exists() && tries < 80 && !cancelled) {
            delay(1500)
            tries++
        }
        if (!cancelled && f.exists()) {
            c.playFile(f, prefs.lastPos, true)
        } else {
            screen = Screen.BROWSER
        }
        waiting = null
    }

    fun up() {
        val d = dir ?: return
        val isRoot = roots.any { it.absolutePath == d.absolutePath }
        dir = if (isRoot) null else d.parentFile
    }

    BackHandler(enabled = screen != Screen.BROWSER || dir != null) {
        when (screen) {
            Screen.EQ -> screen = eqReturn
            Screen.PLAYER -> {
                if (waiting != null) {
                    cancelled = true
                    waiting = null
                }
                c.save()
                c.player.pause()
                screen = Screen.BROWSER
            }
            Screen.BROWSER -> up()
        }
    }

    AppBackground {
        if (!hasStorage) {
            MessageCard(
                title = "Permita o acesso aos vídeos",
                text = "O Media Car precisa ler os arquivos do aparelho e do pendrive para mostrar seus vídeos.",
                button = "Permitir",
                onButton = onRequestPermission,
                modifier = Modifier.align(Alignment.Center)
            )
        } else {
            when (screen) {
                Screen.BROWSER -> BrowserScreen(
                    dir = dir,
                    roots = roots,
                    rescanKey = rescan,
                    lastPath = prefs.lastPath,
                    sizeLevel = sizeLevel,
                    canContinue = c.loaded || prefs.lastPath != null,
                    onSizeLevel = {
                        sizeLevel = it
                        prefs.sizeLevel = it
                    },
                    onContinue = {
                        if (c.loaded) {
                            c.player.play()
                        } else {
                            prefs.lastPath?.let { p ->
                                val f = File(p)
                                if (f.exists()) c.playFile(f, prefs.lastPos, true)
                            }
                        }
                        screen = Screen.PLAYER
                    },
                    onEq = {
                        eqReturn = Screen.BROWSER
                        screen = Screen.EQ
                    },
                    onRescan = { rescan++ },
                    onUp = { up() },
                    onFolder = { dir = it },
                    onPlay = {
                        c.playFile(it, 0L, true)
                        screen = Screen.PLAYER
                    }
                )
                Screen.PLAYER -> PlayerScreen(
                    c = c,
                    fill = fill,
                    waitingName = waiting?.nameWithoutExtension,
                    onCancelWait = {
                        cancelled = true
                        waiting = null
                        screen = Screen.BROWSER
                    },
                    onBack = {
                        c.save()
                        c.player.pause()
                        screen = Screen.BROWSER
                    },
                    onEq = {
                        eqReturn = Screen.PLAYER
                        screen = Screen.EQ
                    },
                    onToggleFill = {
                        fill = !fill
                        prefs.fillMode = fill
                    }
                )
                Screen.EQ -> EqualizerScreen(c, prefs) { screen = eqReturn }
            }
        }
    }
}
