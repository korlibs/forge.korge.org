package korge.catalog.tasks

import korge.*
import korge.catalog.*

class DownloadSimpleActionTask(val action: CatalogModel.SimpleAction, name: String = "Download ${action.downloads.name}") : Task(name) {
    override suspend fun execute(context: TaskContext) {
        for (download in action.downloads.downloads.values) {
            if (download.matches()) {
                context.report(download.url)
                downloadFile(
                    download.url,
                    download.realLocalFile(action),
                    progress = context::report,
                    sha256 = download.sha256
                )
            }
        }
    }

    override fun toString(): String = "DownloadSimpleActionTask(${action.downloads})"
}