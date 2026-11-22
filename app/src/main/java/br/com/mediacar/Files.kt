package br.com.mediacar

import android.os.Environment
import java.io.File
import java.util.Locale

/** Procura pastas e vídeos no aparelho e nos pendrives. */
object FileUtil {

    private val VIDEO_EXT = setOf(
        "mp4", "mkv", "mov", "webm", "ts", "m4v", "3gp", "flv",
        "mpg", "mpeg", "avi", "wmv", "m2ts", "mts"
    )

    /** Pastas do sistema que não são armazenamento de verdade. */
    private val BLOCK = setOf(
        "self", "emulated", "shell", "user", "runtime", "asec", "obb", "secure",
        "pass_through", "installer", "androidwritable", "vendor", "product",
        "expand", "appfuse", "data", "media_rw"
    )

    fun isVideo(f: File): Boolean = f.isFile && f.extension.lowercase(Locale.ROOT) in VIDEO_EXT

    /** Memória interna e pendrives/cartões que o Android deixa o app ler. */
    fun roots(): List<File> {
        val seen = HashSet<String>()
        val out = ArrayList<File>()
        fun add(f: File) {
            if (!f.isDirectory) return
            val key = try { f.canonicalPath } catch (e: Exception) { f.absolutePath }
            if (key in seen) return
            if (f.listFiles() == null) return // sem permissão ou desmontado
            seen.add(key)
            out.add(f)
        }
        add(Environment.getExternalStorageDirectory())
        for (base in listOf("/storage", "/mnt", "/mnt/media_rw")) {
            File(base).listFiles()?.sortedBy { it.name }?.forEach {
                if (it.name.lowercase(Locale.ROOT) !in BLOCK) add(it)
            }
        }
        return out
    }

    fun rootLabel(f: File): String {
        val internal = Environment.getExternalStorageDirectory()
        return if (f.absolutePath == internal.absolutePath) "Memória interna"
        else "Pendrive / cartão  (${f.name})"
    }

    /** Subpastas e vídeos de uma pasta, em ordem natural (episódio 2 antes do 10). */
    fun list(dir: File): Pair<List<File>, List<File>> {
        val items = dir.listFiles() ?: return Pair(emptyList(), emptyList())
        val folders = items.filter { it.isDirectory && !it.name.startsWith(".") }.sortedWith(NATURAL)
        val videos = items.filter { isVideo(it) }.sortedWith(NATURAL)
        return Pair(folders, videos)
    }

    fun videosIn(dir: File?): List<File> = if (dir == null) emptyList() else list(dir).second

    private val NATURAL = Comparator<File> { a, b -> naturalCompare(a.name.lowercase(Locale.ROOT), b.name.lowercase(Locale.ROOT)) }

    private fun naturalCompare(a: String, b: String): Int {
        var i = 0
        var j = 0
        while (i < a.length && j < b.length) {
            if (a[i].isDigit() && b[j].isDigit()) {
                var si = i
                while (si < a.length && a[si].isDigit()) si++
                var sj = j
                while (sj < b.length && b[sj].isDigit()) sj++
                val na = a.substring(i, si).trimStart('0')
                val nb = b.substring(j, sj).trimStart('0')
                if (na.length != nb.length) return na.length - nb.length
                val c = na.compareTo(nb)
                if (c != 0) return c
                i = si
                j = sj
            } else {
                if (a[i] != b[j]) return a[i].compareTo(b[j])
                i++
                j++
            }
        }
        return (a.length - i) - (b.length - j)
    }

    fun fmtSize(bytes: Long): String =
        if (bytes >= 1_073_741_824L) String.format(Locale.getDefault(), "%.1f GB", bytes / 1_073_741_824.0)
        else String.format(Locale.getDefault(), "%d MB", bytes / 1_048_576L)

    fun fmtTime(ms: Long): String {
        val s = (ms / 1000).coerceAtLeast(0)
        val h = s / 3600
        val m = (s % 3600) / 60
        val sec = s % 60
        return if (h > 0) String.format(Locale.US, "%d:%02d:%02d", h, m, sec)
        else String.format(Locale.US, "%d:%02d", m, sec)
    }
}
