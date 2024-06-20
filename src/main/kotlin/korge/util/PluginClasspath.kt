package korge.util

import java.nio.*

class PluginClasspath(val version: Int, val entries: List<Plugin>) {
    class Plugin(var name: String, var pluginXml: String, var jars: List<String>)

    fun encode(): ByteArray {
        val bytes = ByteArray(8 * 1024 * 1024)
        val buffer = ByteBuffer.wrap(bytes)
        buffer.putShort(version.toShort())
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
        println(buffer.position())
        return bytes.copyOf(buffer.position())
    }

    companion object {
        fun parse(bytes: ByteArray): PluginClasspath {
            val buffer = ByteBuffer.wrap(bytes)
            val version = buffer.getShort().toInt()
            val count = buffer.getShort().toInt()
            val plugins = (0 until count).map {
                val jarCount = buffer.getShort()
                //println("--------------")
                //println("JAR_COUNT=$jarCount")
                val name = buffer.getStringWithLen(long = false)
                val pluginXml = buffer.getStringWithLen(long = true)
                val jars = (0 until jarCount).map { buffer.getStringWithLen(long = false) }
                Plugin(name, pluginXml, jars)
            }
            return PluginClasspath(version, plugins)
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
