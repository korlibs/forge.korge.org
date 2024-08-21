package korge.util

import java.nio.*

// classpath.kt
class PluginClasspath(val version: Int, val isJarOnly: Int, val mainPluginDescriptorContent: String?, val entries: List<Plugin>) {
    class Plugin(var name: String, var pluginXml: String, var jars: List<String>) {
        override fun toString(): String = "Plugin(name=$name, pluginXml=${pluginXml.length}, jars=${jars.size})"
    }

    fun encode(): ByteArray {
        val bytes = ByteArray(8 * 1024 * 1024)
        val buffer = ByteBuffer.wrap(bytes)
        buffer.put(version.toByte())
        buffer.put(isJarOnly.toByte())
        if (version == 2) {
            val mainPluginDescriptorContentBytes = (mainPluginDescriptorContent ?: "").encodeToByteArray()
            buffer.putInt(mainPluginDescriptorContentBytes.size)
            buffer.put(mainPluginDescriptorContentBytes)
        }
        buffer.putShort(entries.size.toShort())
        for (entry in entries) {
            //println("- ENTRY: ${entry.name}, pos=${buffer.position()} : XML=${entry.pluginXml.length}")
            buffer.putShort(entry.jars.size.toShort())
            buffer.putStringWithLen(entry.name, long = false)
            buffer.putStringWithLen(entry.pluginXml, long = true)
            for (jar in entry.jars) {
                buffer.putStringWithLen(jar, long = false)
            }
        }
        //println(buffer.position())
        return bytes.copyOf(buffer.position())
    }

    companion object {
        fun parse(bytes: ByteArray): PluginClasspath {
            val buffer = ByteBuffer.wrap(bytes)
            val version = buffer.get().toInt()
            val isJarOnly = buffer.get().toInt()
            check(version == 1 || version == 2)
            check(isJarOnly == 1)
            val mainPluginDescriptorContent = if (version == 2) {
                val mainPluginDescriptorContentSize = buffer.getInt()
                ByteArray(mainPluginDescriptorContentSize).also { buffer.get(it) }.decodeToString()
            } else {
                null
            }
            val count = buffer.getShort().toInt()
            //println(count)
            val plugins = (0 until count).map {
                val jarCount = buffer.getShort()
                //println("--------------")
                //println("JAR_COUNT=$jarCount")
                val name = buffer.getStringWithLen(long = false)
                val pluginXml = buffer.getStringWithLen(long = true)
                val jars = (0 until jarCount).map { buffer.getStringWithLen(long = false) }
                Plugin(name, pluginXml, jars)
            }
            return PluginClasspath(version, isJarOnly, mainPluginDescriptorContent, plugins)
        }

        fun ByteBuffer.getStringWithLen(long: Boolean): String {
            val strLen = if (long) getInt() else getShort().toInt()
            //if (long) println("GET_STRING: $strLen")
            val out = ByteArray(strLen)
            get(out)
            return out.decodeToString()
        }

        fun ByteBuffer.putStringWithLen(str: String, long: Boolean) {
            val bytes = str.encodeToByteArray()
            if (long) putInt(bytes.size) else putShort(bytes.size.toShort())
            put(bytes)
        }
    }
}
