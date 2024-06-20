package korge.catalog.tasks

import korge.*
import korge.catalog.*
import korlibs.datastructure.*

val CatalogModel.SimpleAction.task: SimpleActionTask by extraPropertyThis { SimpleActionTask(this) }

class SimpleActionTask(val action: CatalogModel.SimpleAction) : Task(action.name) {
    val downloadTask = dependsOn(DownloadSimpleActionTask(action))
    val extractTask = dependsOn(ExtractSimpleActionTask(action).also { it.dependsOn(downloadTask) })
    //val extractTask = dependsOn(ExtractSimpleActionTask(action))
    val createShortcutTask = dependsOn(if (action.createShortcut != null) CreateShortcutActionTask(action) else null)

    override suspend fun execute(context: TaskContext) {
    }

    override fun toString(): String = "SimpleActionTask[$action]"
}