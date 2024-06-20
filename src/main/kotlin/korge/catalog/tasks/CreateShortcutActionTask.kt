package korge.catalog.tasks

import korge.*
import korge.catalog.*
import korge.util.*
import java.io.*
import java.util.jar.JarFile
import java.util.zip.*

object PatchKorgeClassPath {
    fun patch(classPathFile: File) {
        val classPaths = PluginClasspath.parse(classPathFile.readBytes())
        for (entry in classPaths.entries) {
            if (entry.name == "KorgePlugin") {
                for (jar in entry.jars) {
                    ZipFile(File(classPathFile.parentFile, "${entry.name}/$jar")).use {
                        val pluginXml: ZipEntry? = it.getEntry("META-INF/plugin.xml")
                        if (pluginXml != null) {
                            val pluginXmlBytes = it.getInputStream(pluginXml).use { it.readBytes() }
                            entry.pluginXml = pluginXmlBytes.decodeToString()
                        }
                    }
                }
            }
        }
        classPathFile.writeBytes(classPaths.encode())
    }
}

class CreateShortcutActionTask(val action: CatalogModel.SimpleAction) : Task(action.name) {
    override suspend fun execute(context: TaskContext) {
        val tools = context.tools

        context.report("Patching classpaths")

        println("PATCHING CLASSPATHS:")
        PatchKorgeClassPath.patch(tools.classpath)

        context.report("Installing")

        tools.installedVersionFile.writeText(context.holder.installer?.name ?: tools.version)

        context.report("Creating shortcuts")
        when (OS.CURRENT) {
            OS.LINUX -> {
                tools.KORGE_FORGE_DESKTOP.writeText("""
                    [Desktop Entry]
                    Name=KorGE Forge ${tools.version}
                    Exec="${tools.exe}" %u
                    Version=1.0
                    Type=Application
                    Categories=Development;IDE;
                    Terminal=false
                    Icon=${tools.svgIco}
                    Comment=Where Kotlin Games Are Created
                    StartupWMClass=korge-forge
                    StartupNotify=true
                """.trimIndent())
            }
            OS.WINDOWS -> {
                createWindowsLnk(tools.exe, tools.START_MENU_LNK, tools.ico, tools.desc)
                createWindowsLnk(tools.exe, tools.DESKTOP_LNK, tools.ico, tools.desc)
            }
            OS.OSX -> {
            }
            else -> Unit
        }
    }
}