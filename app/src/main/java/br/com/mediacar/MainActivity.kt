package br.com.mediacar

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.mutableStateOf
import androidx.core.content.ContextCompat

class MainActivity : ComponentActivity() {

    private lateinit var prefs: Prefs
    private lateinit var controller: PlayerController
    private val hasStorage = mutableStateOf(false)

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            hasStorage.value = granted || storageGranted()
        }

    private fun storageGranted(): Boolean =
        ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) ==
            PackageManager.PERMISSION_GRANTED

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) // tela sempre ligada
        prefs = Prefs(this)
        controller = PlayerController(applicationContext, prefs)

        hasStorage.value = storageGranted()
        if (!hasStorage.value) permissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)

        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                App(prefs, controller, hasStorage.value) {
                    permissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        controller.foreground = true
        // Voltou para a frente: se estava tocando quando saiu, continua sozinho.
        if (controller.pendingResume) {
            controller.pendingResume = false
            if (prefs.autoResume) controller.player.play()
        }
    }

    override fun onStop() {
        // Grava onde parou (com "estava tocando") e pausa ao ir para outro app.
        controller.pendingResume = controller.loaded && controller.player.playWhenReady
        controller.save()
        controller.foreground = false
        controller.player.pause()
        super.onStop()
    }

    override fun onDestroy() {
        controller.release()
        super.onDestroy()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) hideSystemBars()
    }

    @Suppress("DEPRECATION")
    private fun hideSystemBars() {
        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            )
    }
}
