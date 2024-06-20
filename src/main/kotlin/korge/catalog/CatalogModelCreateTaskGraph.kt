package korge.catalog

import korge.*
import korge.catalog.tasks.*
import korge.tasks.*
import korge.util.*
import korlibs.datastructure.*

//val TaskContext.installer: CatalogModel.Installer get() = holder.installer ?: error("Can't find 'CatalogModel.Installer' in the TaskContext")
var TasksHolder.installer: CatalogModel.Installer? by extraProperty { null }
var TasksHolder.installation: ForgeInstallation? by extraProperty { null }
val TasksHolder.installerVersion: String get() = installer?.version ?: installation?.version ?: error("Not installer, not installation are defined")

val TasksHolder.tools: BaseKorgeForgeInstallTools by extraPropertyThis { BaseKorgeForgeInstallTools(this.installerVersion) }
val TaskContext.tools: BaseKorgeForgeInstallTools get() = holder.tools

//val CatalogModel.Installer.tools by extraPropertyThis { BaseKorgeForgeInstallTools(this.version) }

val ForgeInstallation.tools by extraPropertyThis { BaseKorgeForgeInstallTools(this.version) }

val ForgeInstallation.uninstallTask by extraPropertyThis { UninstallActionTask(this) }

val ForgeInstallation.openTask by extraPropertyThis { OpenExeActionTask(this) }
val ForgeInstallation.openFolderTask by extraPropertyThis { OpenFolderActionTask(this) }

