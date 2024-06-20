package korge.catalog.tasks

import korge.*
import korge.catalog.*
import korlibs.datastructure.*

val CatalogModel.Installer.task: InstallerTask by extraPropertyThis { InstallerTask(this) }

class InstallerTask(val installer: CatalogModel.Installer) : Task(installer.name) {
    val model get() = installer.model
    val actions = dependsOn(installer.actionGroups.map { model.actionGroups[it]!!.task })

    override suspend fun execute(context: TaskContext) {
    }
}
