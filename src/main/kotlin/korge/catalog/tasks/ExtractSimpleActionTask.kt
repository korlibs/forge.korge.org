package korge.catalog.tasks

import korge.*
import korge.catalog.*
import java.io.*

class ExtractSimpleActionTask(val action: CatalogModel.SimpleAction, name: String = "Extracting ${action.name}") : Task(name) {
    override suspend fun execute(context: TaskContext) {
        context.report("Extracting ${action.localFile}")

        for (download in action.ldownloads) {
            val localFile = download.realLocalFile(action)

            if (!download.matches()) continue

            val extract = action.extract
            val filter = action.filter
            val copy = action.copy

            val processOutputName: (String) -> String? = when (filter) {
                "extra_libs" -> {
                    val jnaOs = when (OS.CURRENT) {
                        OS.WINDOWS -> "win32"
                        OS.OSX -> "darwin"
                        OS.LINUX -> "linux"
                    }
                    val jnaArch = when (ARCH.CURRENT) {
                        ARCH.X64 -> "x86-64"
                        ARCH.ARM -> "aarch64"
                    }
                    {
                        if (it.contains("jna")) {
                            when {
                                it.contains(jnaOs) && it.contains(jnaArch) -> it.replace("$jnaOs-$jnaArch", jnaArch)
                                else -> null
                            }
                        } else {
                            it
                        }
                    }
                }
                "dist" -> {
                    val osStr: String = when (OS.CURRENT) {
                        OS.WINDOWS -> "win"
                        OS.OSX -> "mac"
                        OS.LINUX -> "unix"
                    }

                    val archStr: String = when (ARCH.CURRENT) {
                        ARCH.X64 -> "x64"
                        ARCH.ARM -> "aarch64"
                    }
                    {
                        if (it.startsWith("dist.all") || it.startsWith("dist.$osStr.$archStr")) {
                            it.substringAfter('/')
                        } else {
                            null
                        }
                    }
                }
                "remove_first" -> { { it.replace('\\', '/').substringAfter('/') } }
                null -> { { it } }
                else -> TODO("Unknown how to handle filter=$filter")
            }

            if (extract != null) {
                val targetFolder = File(context.tools.expand(extract))
                println("EXTRACT: localFile=$localFile, targetFolder=$targetFolder")
                TarTools(processOutputName = processOutputName).extract(localFile, targetFolder, context::report)
            }

            if (copy != null) {
                localFile.copyTo(File(context.tools.expand(copy)), overwrite = true)
            }
        }
    }

    override fun toString(): String = "ExtractSimpleActionTask(extract=${action.extract}, copy=${action.copy})"
}