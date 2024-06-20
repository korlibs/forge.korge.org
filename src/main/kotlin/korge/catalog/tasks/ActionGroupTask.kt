package korge.catalog.tasks

import korge.*
import korge.catalog.*
import korlibs.datastructure.*

val CatalogModel.ActionGroup.task: ActionGroupTask by extraPropertyThis { ActionGroupTask(this) }
class ActionGroupTask(val group: CatalogModel.ActionGroup) : Task(group.name) {
    val actions = dependsOn(group.actions.map { it.task })

    override suspend fun execute(context: TaskContext) {
    }

    override fun toString(): String = "ActionGroupTask('${group.name}')"
}