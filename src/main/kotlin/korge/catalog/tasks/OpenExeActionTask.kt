package korge.catalog.tasks

import korge.*
import korge.catalog.*
import korge.util.*

class OpenExeActionTask(val installation: ForgeInstallation) : Task("Opening KorGE Forge") {
    override suspend fun execute(context: TaskContext) {
        val tools = installation.tools
        when (OS.CURRENT) {
            OS.OSX -> ProcessBuilder("open", tools.MacAPP.absolutePath).start().waitFor()
            OS.LINUX -> ProcessBuilder("gtk-launch", tools.KORGE_FORGE_DESKTOP.name).start().waitFor()
            OS.WINDOWS -> ProcessBuilder("cmd", "/c", "start", tools.exe.absolutePath).start().waitFor()
        }
    }
}