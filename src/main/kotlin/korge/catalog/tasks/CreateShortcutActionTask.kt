package korge.catalog.tasks

import korge.*
import korge.catalog.*
import korge.util.*

class CreateShortcutActionTask(val action: CatalogModel.SimpleAction) : Task(action.name) {
    override suspend fun execute(context: TaskContext) {
        val tools = context.tools

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