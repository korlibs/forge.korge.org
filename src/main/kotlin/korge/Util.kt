@file:OptIn(ExperimentalStdlibApi::class)

package korge

import kotlinx.coroutines.*
import java.io.*
import java.net.*
import java.security.MessageDigest

enum class OS {
    WINDOWS, OSX, LINUX;
    companion object {
        fun str(): String = "$CURRENT"
        val CURRENT: OS by lazy {
            val os = System.getProperty("os.name").lowercase()

            when {
                os.contains("win") -> WINDOWS
                os.contains("mac") -> OSX
                os.contains("nix") || os.contains("nux") || os.contains("aix") -> LINUX
                else -> LINUX
            }
        }
        operator fun get(name: String): OS? {
            if (name.startsWith("win", ignoreCase = true)) return OS.WINDOWS
            if (name.startsWith("mac", ignoreCase = true) || name.startsWith("osx", ignoreCase = true)) return OS.OSX
            if (name.startsWith("lin", ignoreCase = true) || name.startsWith("unix", ignoreCase = true)) return OS.LINUX
            return null
        }
    }
}

enum class ARCH {
    X64, ARM;
    companion object {
        fun str(): String = if (CURRENT != CURRENT_EMULATED) "$CURRENT_EMULATED on $CURRENT" else "$CURRENT"

        val CURRENT: ARCH by lazy {
            when {
                (System.getenv("PROCESSOR_ARCHITECTURE") ?: "").contains("arm", ignoreCase = true)
                        || (System.getenv("ProgramFiles(Arm)") ?: "").isNotBlank() -> ARM
                else -> CURRENT_EMULATED
            }
        }
        val CURRENT_EMULATED: ARCH by lazy {
            val arch = System.getProperty("os.arch").lowercase()
            when {
                arch.contains("arm", ignoreCase = true) || arch.contains("aarch", ignoreCase = true) -> ARM
                arch.contains("x86_64", ignoreCase = true) || arch.contains("amd64", ignoreCase = true) -> X64
                else -> X64
            }
        }

        operator fun get(name: String): ARCH? {
            if (name.startsWith("arm", ignoreCase = true) || name.startsWith("aarch", ignoreCase = true)) return return ARCH.ARM
            if (name.contains("x64") || name.startsWith("x86_64", ignoreCase = true) || name.startsWith("amd64", ignoreCase = true)) return ARCH.X64
            return null
        }
    }
}

suspend fun downloadFile(
    url: String,
    outFile: File,
    shasum: String? = null,
    progress: (remaining: LongRange) -> Unit = { range -> }
) {
    val shaSize = if (shasum != null && shasum.length == 128) 512 else 256
    val outFile = outFile.absoluteFile
    withContext(Dispatchers.IO) {
        if (!outFile.exists()) {
            outFile.parentFile.mkdirs()
            val tmpFile = File(outFile.parentFile, "${outFile.name}.tmp")
            val connection = URL(url).openConnection()
            val totalLength = connection.contentLengthLong
            val digest = connection.getInputStream().use { inp ->
                tmpFile.outputStream().use { out ->
                    inp.copyTo(out, digest = MessageDigest.getInstance("SHA-$shaSize")) {
                        progress(it..totalLength)
                    }
                }
            }
            if (shasum != null) {
                val downloadedShasum = digest.toHexString().lowercase()
                val expectedShasum = shasum.lowercase()
                check(downloadedShasum == expectedShasum) { "Digest for\n  url=$url\n  expected=${expectedShasum}\n  given=${downloadedShasum}\n  tmp=$tmpFile"}
            }
            tmpFile.renameTo(outFile)
            println("DOWNLOADED $outFile from $url")
        }
    }
}

public fun InputStream.copyTo(out: OutputStream, bufferSize: Int = DEFAULT_BUFFER_SIZE, digest: MessageDigest = MessageDigest.getInstance("SHA-256"), progress: (Long) -> Unit = { }): ByteArray {
    var bytesCopied: Long = 0
    val buffer = ByteArray(bufferSize)
    var bytes = read(buffer)
    digest.reset()
    progress(0L)
    while (bytes >= 0) {
        out.write(buffer, 0, bytes)
        digest.update(buffer, 0, bytes)
        bytesCopied += bytes
        bytes = read(buffer)
        progress(bytesCopied)
    }
    return digest.digest()
}
