package korge.catalog.tasks

import korge.OS
import korge.Task
import korge.TaskContext
import korge.catalog.CatalogModel
import korge.catalog.installer
import korge.catalog.tools
import korge.util.PluginClasspath
import korge.util.createWindowsLnk
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import javax.swing.JOptionPane
import kotlin.io.path.getOwner

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
        if (tools.classpath.exists()) {
            PatchKorgeClassPath.patch(tools.classpath)
        }

        if (OS.CURRENT == OS.LINUX) {
            if (tools.linuxChromeSandbox.toPath().getOwner()?.name != "root") {
                val command = arrayOf(
                    "sh", "-c",
                    "/usr/bin/chown root:root '${tools.linuxChromeSandbox}'; /usr/bin/chmod 04755 '${tools.linuxChromeSandbox}'"
                )

                when {
                    File("/usr/bin/pkexec").exists() -> {
                        ProcessBuilder("/usr/bin/pkexec", "--disable-internal-agent", *command).start().waitFor()
                    }
                    File("/usr/bin/kdesudo").exists() -> {
                        ProcessBuilder("/usr/bin/kdesudo", "--comment", "System needs administrative privileges. Please enter your password.", *command).start().waitFor()
                    }
                    File("/usr/bin/gksudo").exists() -> {
                        ProcessBuilder("/usr/bin/gksudo", "--preserve-env", "--sudo-mode", "--description", "System", *command).start().waitFor()
                    }
                    else -> {
                        val message = "Couldn't find a mechanism to set permissions. Please execute: ${command.joinToString(" ")}"
                        println(message)
                        JOptionPane.showConfirmDialog(null, message, "Information", JOptionPane.OK_OPTION, JOptionPane.WARNING_MESSAGE)
                    }
                }
            }
        }

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