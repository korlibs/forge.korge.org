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

    @Test
    fun testV2() {
        val bytes = PluginClasspathRebuilderTest::class.java.getResource("/plugin-classpath.2024-02.txt")!!.readBytes()
        val parsed = PluginClasspath.parse(bytes)
        //println(parsed.mainPluginDescriptorContent)
        //for (entry in parsed.entries) {
        //    println(entry)
        //    if (entry.pluginXml.contains("lombok"))  {
        //        println(entry.pluginXml)
        //    }
        //}
    }
}