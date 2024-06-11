import io.airlift.compress.zstd.*
import java.io.*
import kotlin.test.*

class UncompressTest {
    @Test
    fun test() {
        //println(this::class.java.getResourceAsStream("demo.tar.zst").uncompressZstd().readNBytes(1024).decodeToString())
        TarTools.extractTarZstd(this::class.java.getResourceAsStream("demo.tar.zst"), File("out/demo"))
    }

    fun InputStream.uncompressZstd(): InputStream {
        return ZstdInputStream(this)
    }
}