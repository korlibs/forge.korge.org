package korge.catalog.tasks

import korge.*
import korge.catalog.*
import korge.util.*

class UninstallActionTask(val installation: ForgeInstallation) : Task("Uninstall ${installation.version}") {
    val delete = dependsOn(DeleteKorgeForgeFolder(installation))

    override suspend fun execute(context: TaskContext) {
        installation.tools.KORGE_FORGE_DESKTOP.delete()
        installation.tools.START_MENU_LNK.delete()
        installation.tools.DESKTOP_LNK.delete()
    }
}