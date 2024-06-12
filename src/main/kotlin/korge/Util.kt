@file:OptIn(ExperimentalStdlibApi::class)

package korge

import kotlinx.coroutines.*
import java.io.*
import java.net.*
import java.security.MessageDigest

enum class OS {
    WINDOWS, OSX, LINUX, OTHER;
    companion object {
        val CURRENT: OS by lazy {
            val os = System.getProperty("os.name").lowercase()

            when {
                os.contains("win") -> WINDOWS
                os.contains("mac") -> OSX
                os.contains("nix") || os.contains("nux") || os.contains("aix") -> LINUX
                else -> OTHER
            }
        }
    }
}

enum class ARCH {
    X64, ARM, UNKNOWN;
    companion object {
        val CURRENT: ARCH by lazy {
            val arch = System.getProperty("os.arch").lowercase()
            when {
                arch.contains("arm") || arch.contains("aarch") -> ARM
                arch.contains("x86_64") || arch.contains("amd64") -> X64
                else -> UNKNOWN
            }
        }
    }
}

suspend fun downloadFile(
    url: String,
    outFile: File,
    sha256: String? = null,
    progress: (remaining: LongRange) -> Unit = { range -> Unit }
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
                check(digest.toHexString() != sha256) { "Digest for url=$url expected=${sha256}, given=${digest.toHexString()} : tmp=$tmpFile"}
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
