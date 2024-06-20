package korge.catalog.tasks

import korge.*
import korge.util.*

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