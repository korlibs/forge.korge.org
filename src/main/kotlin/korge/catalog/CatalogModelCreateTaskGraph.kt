package korge.catalog

import korge.*
import korge.tasks.*
import korge.util.*
import korlibs.datastructure.*
import java.io.*
import java.net.*

val CatalogModel.Installer.task: InstallerTask by extraPropertyThis { InstallerTask(this) }

class InstallerTask(val installer: CatalogModel.Installer) : Task(installer.name) {
    val model get() = installer.model
    val actions = dependsOn(installer.actionGroups.map { model.actionGroups[it]!!.task })

    override suspend fun execute(context: TaskContext) {
    }
}

val CatalogModel.ActionGroup.task: ActionGroupTask by extraPropertyThis { ActionGroupTask(this) }
class ActionGroupTask(val group: CatalogModel.ActionGroup) : Task(group.name) {
    val actions = dependsOn(group.actions.map { it.task })

    init {
        for (n in 1 until actions.size) {
            actions[n].extractTask.dependsOn(actions[n - 1].extractTask)
        }
    }

    override suspend fun execute(context: TaskContext) {
    }
}

val CatalogModel.SimpleAction.task: SimpleActionTask by extraPropertyThis { SimpleActionTask(this) }
class SimpleActionTask(val action: CatalogModel.SimpleAction) : Task(action.name) {
    val downloadTask = dependsOn(DownloadSimpleActionTask(action))
    val extractTask = dependsOn(ExtractSimpleActionTask(action))
    val createShortcutTask = CreateShortcutActionTask(action)

    override suspend fun execute(context: TaskContext) {
    }
}


class DownloadSimpleActionTask(val action: CatalogModel.SimpleAction, name: String = "Download ${action.name}") : Task(name) {
    override suspend fun execute(context: TaskContext) {
        for (download in action.downloads.downloads.values) {
            if (download.matches()) {
                context.report(download.url)
                downloadFile(
                    download.url,
                    File(action.localFile ?: File(URL(download.url).path).name),
                    progress = context::report
                )
            }
        }
    }
}

class ExtractSimpleActionTask(val action: CatalogModel.SimpleAction, name: String = "Extracting ${action.name}") : Task(name) {
    override suspend fun execute(context: TaskContext) {
        context.report("Extracting ${action.localFile}")

        action.filter
        action.copy
    }
}

val TaskContext.installer get() = holder.installer ?: error("Can't find 'CatalogModel.Installer' in the TaskContext")
var TasksHolder.installer: CatalogModel.Installer? by extraProperty { null }

val CatalogModel.Installer.tools by extraPropertyThis { BaseKorgeForgeInstallTools(this.version) }

class CreateShortcutActionTask(val action: CatalogModel.SimpleAction) : Task(action.name) {
    override suspend fun execute(context: TaskContext) {
        val tools = context.installer.tools
        val versionFolder = tools.VersionFolder

        val exe = when (OS.CURRENT) {
            OS.OSX -> File(versionFolder, "MacOS/korge")
            else -> File(versionFolder, "bin/korge64.exe")
        }
        val ico = File(versionFolder, "bin/korge.ico")
        val desc = "KorGE Forge ${tools.version}"

        context.report("Creating shortcuts")
        when (OS.CURRENT) {
            OS.LINUX -> {
                tools.KORGE_FORGE_DESKTOP.writeText("""
                    [Desktop Entry]
                    Name=KorGE Forge ${tools.version}
                    Exec="${tools.VersionFolder}/bin/korge.sh" %u
                    Version=1.0
                    Type=Application
                    Categories=Development;IDE;
                    Terminal=false
                    Icon=${tools.VersionFolder}/bin/korge.svg
                    Comment=Where Kotlin Games Are Created
                    StartupWMClass=korge-forge
                    StartupNotify=true
                """.trimIndent())
            }
            OS.WINDOWS -> {
                createWindowsLnk(exe, tools.START_MENU_LNK, ico, desc)
                createWindowsLnk(exe, tools.DESKTOP_LNK, ico, desc)
            }
            OS.OSX -> {
            }
            else -> Unit
        }
    }
}

val ForgeInstallation.tools by extraPropertyThis { BaseKorgeForgeInstallTools(this.version) }

val ForgeInstallation.uninstallTask by extraPropertyThis { UninstallActionTask(this) }

val ForgeInstallation.openTask by extraPropertyThis { OpenExeActionTask(this) }
val ForgeInstallation.openFolderTask by extraPropertyThis { OpenFolderActionTask(this) }

class UninstallActionTask(val installation: ForgeInstallation) : Task("Uninstall ${installation.version}") {
    val delete = dependsOn(DeleteKorgeForgeFolder(installation))

    override suspend fun execute(context: TaskContext) {
        installation.tools.KORGE_FORGE_DESKTOP.delete()
        installation.tools.START_MENU_LNK.delete()
        installation.tools.DESKTOP_LNK.delete()
    }
}

class DeleteKorgeForgeFolder(val installation: ForgeInstallation) : Task("Deleting KorGE Forge Folder") {
    override suspend fun execute(context: TaskContext) {
        check(installation.folder.absolutePath.contains("KorGE"))
        val files = installation.folder.walkBottomUp().toList()
        for ((it, item) in files.withIndex()) {
            item.delete()
            context.report(it.toLong() until files.size.toLong())
        }
    }
}

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