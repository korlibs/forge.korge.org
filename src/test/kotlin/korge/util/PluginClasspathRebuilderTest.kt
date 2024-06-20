package korge.util

import kotlin.test.*

class PluginClasspathRebuilderTest {
    @Test
    fun test() {
        val bytes = PluginClasspathRebuilderTest::class.java.getResource("/plugin-classpath.txt")!!.readBytes()
        val parsed = PluginClasspath.parse(bytes)
        val encoded = parsed.encode()
        assertEquals(bytes.size, encoded.size)
        assertContentEquals(bytes, encoded)
    }
}