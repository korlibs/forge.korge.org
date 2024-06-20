package korge.util

import korge.*
import korlibs.datastructure.*
import java.io.*

class ForgeInstallation(val folder: File) : Extra by Extra() {
    val version: String get() = folder.name.removePrefix("KorGE Forge").removeSuffix(".app").trim()

    companion object {
        val InstallBaseFolder = when (OS.CURRENT) {
            OS.LINUX -> File(System.getProperty("user.home"), ".local/share/KorGEForge")
            OS.OSX -> File(System.getProperty("user.home"), "Applications")
            OS.WINDOWS -> File(System.getProperty("user.home"), "AppData/Local/KorGEForge")
        }

        fun list(): List<ForgeInstallation> {
            return when (OS.CURRENT) {
                OS.OSX -> (InstallBaseFolder.listFiles() ?: emptyArray()).filter { it.isDirectory && it.name.contains("KoRGE Forge") }
                else -> (InstallBaseFolder.listFiles() ?: emptyArray()).filter { it.isDirectory }
            }.map { ForgeInstallation(it) }
        }
    }
}
