package korge.catalog.tasks

import korge.*
import korge.catalog.*
import korlibs.datastructure.*

val CatalogModel.Installer.task: InstallerTask by extraPropertyThis { InstallerTask(this) }

class InstallerTask(val installer: CatalogModel.Installer) : Task(installer.name) {
    val model get() = installer.model
    val groups = dependsOn(installer.actionGroups.map { model.actionGroups[it]!!.task })

    init {
        for (g in 0 until groups.size) {
            val prevGroup = groups.getOrNull(g - 1)
            val group = groups[g]
            println("GROUP: ${group.name}")
            for (n in 0 until group.actions.size) {
                val prevAction = group.actions.getOrNull(n - 1) ?: prevGroup?.actions?.lastOrNull()
                val currAction = group.actions[n]

                currAction.dependsOn(prevAction)
                currAction.extractTask.dependsOn(prevAction)
                currAction.createShortcutTask?.dependsOn(prevAction)
                //println(" - currAction.createShortcutTask=${prevAction}")
            }
        }
    }

    override suspend fun execute(context: TaskContext) {
    }
}
