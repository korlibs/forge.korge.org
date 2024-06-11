package korge

import io.airlift.compress.zstd.*
import org.apache.commons.compress.archivers.tar.*
import java.io.*
import java.nio.file.*
import java.util.zip.*

open class TarTools(
    val removeFirstDir: Boolean = false,
    val processOutputName: (String) -> String? = {
        if (removeFirstDir) it.replace('\\', '/').substringAfter('/') else it
    },
) {
    companion object : TarTools()

    val CHUNK_SIZE = 1 * 1024 * 1024

    fun extractTarZstd(inputFile: File, outputDir: File, report: (LongRange) -> Unit = {}) {
        inputFile.inputStream().use {
            extractTarZstd(it.reporter(inputFile.length(), CHUNK_SIZE, report), outputDir)
        }
    }

    fun extractTarZstd(inputStream: InputStream, outputDir: File) {
        extractTar(inputStream.buffered().uncompressZstd(), outputDir)
    }

    fun extractTarGz(inputFile: File, outputDir: File, report: (LongRange) -> Unit = {}) {
        inputFile.inputStream().use {
            extractTarGz(it.reporter(inputFile.length(), CHUNK_SIZE, report), outputDir)
        }
    }

    fun extractTarGz(inputStream: InputStream, outputDir: File) {
        extractTar(inputStream.buffered().uncompressGzip(), outputDir)
    }

    fun extractTar(inputFile: File, outputDir: File) {
        inputFile.inputStream().use {
            extractTar(it.buffered(), outputDir)
        }
    }

    fun extractTar(tarInputStream: InputStream, outputDir: File) {
        if (!outputDir.isDirectory) {
            val tmpDir = File(outputDir.parentFile, "${outputDir.name}.tmp")
            for ((tarInput, entry) in tarInputStream.asTarSequence()) {
                val fullName = processOutputName(entry.name) ?: continue
                val outputFile = File(tmpDir, fullName)
                when {
                    entry.isDirectory -> outputFile.mkdirs()
                    else -> {
                        outputFile.parentFile?.mkdirs()
                        Files.copy(tarInput, outputFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
                    }
                }
            }
            tmpDir.renameTo(outputDir)
        }
    }

    private fun InputStream.uncompressZstd(): InputStream {
        return ZstdInputStream(this)
    }

    private fun InputStream.uncompressGzip(): InputStream {
        return GZIPInputStream(this)
    }

    private fun InputStream.asTarSequence(): Sequence<Pair<TarArchiveInputStream, TarArchiveEntry>> = sequence {
        TarArchiveInputStream(this@asTarSequence).use { tarInput ->
            var entry: TarArchiveEntry? = tarInput.nextEntry
            while (entry != null) {
                yield(tarInput to entry)
                entry = tarInput.nextEntry
            }
        }
    }
}

fun InputStream.reporter(length: Long, chunkSize: Int = 16 * 1024, reporter: (LongRange) -> Unit): ReporterInputStream {
    return ReporterInputStream(this, length, chunkSize, reporter)
}

class ReporterInputStream(val base: InputStream, val length: Long, val chunkSize: Int = 16 * 1024, val reporter: (LongRange) -> Unit) : InputStream() {
    private var lastReport = Int.MIN_VALUE.toLong()
    var position = 0L

    private fun doReport() {
        val distance = position - lastReport
        if (distance >= chunkSize || position >= length) {
            lastReport = position
            reporter(position..length)
        }
    }

    private fun incrementPosition(len: Int) {
        if (len <= 0) return
        position += len
        doReport()
    }

    init {
        doReport()
    }

    override fun read(b: ByteArray, off: Int, len: Int): Int {
        return base.read(b, off, len).also {
            incrementPosition(it)
        }
    }

    override fun read(): Int {
        return base.read().also {
            if (it >= 0) incrementPosition(1)
        }
    }
}
