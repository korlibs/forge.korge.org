package korge.catalog.tasks

import korge.*
import korge.catalog.*
import korlibs.datastructure.*

val CatalogModel.ActionGroup.task: ActionGroupTask by extraPropertyThis { ActionGroupTask(this) }
class ActionGroupTask(val group: CatalogModel.ActionGroup) : Task(group.name) {
    val actions = dependsOn(group.actions.map { it.task })

    init {
        for (n in 1 until actions.size) {
            actions[n].extractTask.dependsOn(actions[n - 1].extractTask)
            actions[n].createShortcutTask?.dependsOn(actions[n - 1])
        }
    }

    override suspend fun execute(context: TaskContext) {
    }

    override fun toString(): String = "ActionGroupTask('${group.name}')"
}