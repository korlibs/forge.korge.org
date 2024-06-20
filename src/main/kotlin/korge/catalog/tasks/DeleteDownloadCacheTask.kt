package korge.catalog.tasks

import korge.*
import java.io.*

object DeleteDownloadCacheTask : Task("Delete Download Cache") {
    val file = File("korge-forge-installer-download-cache")

    val isAvailable get() = file.isDirectory

    override suspend fun execute(context: TaskContext) {
        file.deleteRecursively()
    }
}