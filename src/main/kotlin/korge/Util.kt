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
    }
}

suspend fun downloadFile(
    url: String,
    outFile: File,
    sha256: String? = null,
    progress: (remaining: LongRange) -> Unit = { range -> }
) {
    withContext(Dispatchers.IO) {
        if (!outFile.exists()) {
            outFile.parentFile.mkdirs()
            val tmpFile = File(outFile.parentFile, "${outFile.name}.tmp")
            val connection = URL(url).openConnection()
            val totalLength = connection.contentLengthLong
            val digest = connection.getInputStream().use { inp ->
                tmpFile.outputStream().use { out ->
                    inp.copyTo(out) {
                        progress(it..totalLength)
                    }
                }
            }
            if (sha256 != null) {
                val downloadedSha256 = digest.toHexString().lowercase()
                val expectedSha256 = sha256.lowercase()
                check(downloadedSha256 == expectedSha256) { "Digest for\n  url=$url\n  expected=${expectedSha256}\n  given=${downloadedSha256}\n  tmp=$tmpFile"}
            }
            tmpFile.renameTo(outFile)
        }
    }
}

public fun InputStream.copyTo(out: OutputStream, bufferSize: Int = DEFAULT_BUFFER_SIZE, progress: (Long) -> Unit = { }): ByteArray {
    var bytesCopied: Long = 0
    val buffer = ByteArray(bufferSize)
    var bytes = read(buffer)
    val digest = MessageDigest.getInstance("SHA-256")
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
