package korge.catalog.tasks

import korge.*
import korge.catalog.*
import korge.util.*
import java.io.*

class OpenFolderActionTask(val installation: ForgeInstallation) : Task("Opening KorGE Forge Folder") {
    override suspend fun execute(context: TaskContext) {
        val tools = installation.tools
        when (OS.CURRENT) {
            OS.OSX -> ProcessBuilder("open", "-R", tools.MacAPP.absolutePath).start().waitFor()
            OS.LINUX -> ProcessBuilder("xdg-open", File(tools.VersionFolder, "bin").absolutePath).start().waitFor()
            OS.WINDOWS -> ProcessBuilder("explorer.exe", tools.exe.parentFile.absolutePath).start().waitFor()
        }
    }
}