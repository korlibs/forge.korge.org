package korge.tasks

import korge.*
import korge.catalog.*
import korge.util.*
import korlibs.datastructure.*
import java.io.*
import kotlin.io.path.*

@Deprecated("")
val KORGE_FORGE_VERSION = "2024.1.unknown"

/*
object InstallKorgeForge : Task("Installing KorGE Forge", ExtractForge, ExtractJBR, ExtractExtraLibs, ExtractProductInfo) {
    val exe = when (OS.CURRENT) {
        OS.OSX -> File(ExtractForge.outDirectory, "MacOS/korge")
        else -> File(ExtractForge.outDirectory, "bin/korge64.exe")
    }
    val ico = File(ExtractForge.outDirectory, "bin/korge.ico")
    val desc = "KorGE Forge $KORGE_FORGE_VERSION"

    override suspend fun execute(context: TaskContext) {
        when (OS.CURRENT) {
            OS.LINUX -> {
                KorgeForgeInstallTools.KORGE_FORGE_DESKTOP.writeText("""
                    [Desktop Entry]
                    Name=KorGE Forge ${KORGE_FORGE_VERSION}
                    Exec="${KorgeForgeInstallTools.VersionFolder}/bin/korge.sh" %u
                    Version=1.0
                    Type=Application
                    Categories=Development;IDE;
                    Terminal=false
                    Icon=${KorgeForgeInstallTools.VersionFolder}/bin/korge.svg
                    Comment=Where Kotlin Games Are Created
                    StartupWMClass=korge-forge
                    StartupNotify=true
                """.trimIndent())
            }
            OS.WINDOWS -> {
                createWindowsLnk(exe, KorgeForgeInstallTools.START_MENU_LNK, ico, desc)
                createWindowsLnk(exe, KorgeForgeInstallTools.DESKTOP_LNK, ico, desc)
            }
            OS.OSX -> {
            }
            else -> Unit
        }
    }
}

object ExtractForge : Task("Extracting KorGE Forge", DownloadForge) {
    val outDirectory by lazy {
        KorgeForgeInstallTools.VersionFolder
    }

    val osStr: String = when (OS.CURRENT) {
        OS.WINDOWS -> "win"
        OS.OSX -> "mac"
        OS.LINUX -> "unix"
    }

    val archStr: String = when (ARCH.CURRENT) {
        ARCH.X64 -> "x64"
        ARCH.ARM -> "aarch64"
    }

    override suspend fun execute(context: TaskContext) {
        context.report("${outDirectory.absoluteFile}")

        TarTools(processOutputName = {
            if (it.startsWith("dist.all") || it.startsWith("dist.$osStr.$archStr")) {
                it.substringAfter('/')
            } else {
                null
            }
        }).extractTarZstd(DownloadForge.localFile, outDirectory, context::report)
    }
}

object ExtractExtraLibs : Task("Extracting Extra Libs", DownloadForgeExtraLibs, ExtractForge) {
    val outDirectory = File(ExtractForge.outDirectory, "lib-native")

    val jnaOs = when (OS.CURRENT) {
        OS.WINDOWS -> "win32"
        OS.OSX -> "darwin"
        OS.LINUX -> "linux"
    }
    val jnaArch = when (ARCH.CURRENT) {
        ARCH.X64 -> "x86-64"
        ARCH.ARM -> "aarch64"
    }

    override suspend fun execute(context: TaskContext) {
        context.report("${outDirectory.absoluteFile}")
        TarTools(processOutputName = {
            if (it.contains("jna")) {
                when {
                    it.contains(jnaOs) && it.contains(jnaArch) -> it.replace("$jnaOs-$jnaArch", jnaArch)
                    else -> null
                }
            } else {
                it
            }
        }).extractTarZstd(DownloadForgeExtraLibs.localFile, outDirectory, context::report)
        outDirectory.copyRecursively(File(outDirectory.parentFile, "lib"), overwrite = true)
    }
}

object ExtractProductInfo : Task("Extracting Product Info", DownloadForgeProductInfo, ExtractForge) {
    val outDirectory = File(ExtractForge.outDirectory, "Resources")

    override suspend fun execute(context: TaskContext) {
        if (OS.CURRENT == OS.OSX) {
            DownloadForgeProductInfo.localFile.copyTo(File(outDirectory, "product-info.json"), overwrite = true)
        }
    }
}

object ExtractJBR : Task("Extracting JBR", DownloadJBR, ExtractExtraLibs, ExtractForge, ExtractProductInfo) {
    val outDirectory = File(ExtractForge.outDirectory, "jbr")

    override suspend fun execute(context: TaskContext) {
        context.report("${outDirectory.absoluteFile}")
        TarTools(removeFirstDir = true).extractTarGz(DownloadJBR.localFile, outDirectory, context::report)
    }
}
*/

val CatalogModel.Installer.tools by extraPropertyThis { BaseKorgeForgeInstallTools(version) }

open class BaseKorgeForgeInstallTools(val version: String) {
    val home = System.getProperty("user.home")
    val Folder = when (OS.CURRENT) {
        OS.OSX -> File(ForgeInstallation.InstallBaseFolder, "KorGE Forge ${version}.app/Contents")
        else -> ForgeInstallation.InstallBaseFolder
    }
    val MacAPP = Folder.parentFile
    val VersionFolder = when (OS.CURRENT) {
        OS.OSX -> Folder
        else -> File(Folder, version)
    }
    val START_MENU = when (OS.CURRENT) {
        OS.LINUX -> "${home}/.local/share/applications"
        else -> "${System.getenv("APPDATA")}\\Microsoft\\Windows\\Start Menu"
    }
    val START_MENU_LNK = File(START_MENU, "KorGE Forge ${version}.lnk")
    val DESKTOP_LNK = File(getDesktopFolder(), "KorGE Forge ${version}.lnk")
    val KORGE_FORGE_DESKTOP = File(START_MENU, "korge-forge-${version}.desktop")

    val exe = when (OS.CURRENT) {
        OS.OSX -> File(VersionFolder, "MacOS/korge")
        OS.LINUX -> File(VersionFolder, "bin/korge.sh")
        else -> File(VersionFolder, "bin/korge64.exe")
    }
    val ico = File(VersionFolder, "bin/korge.ico")
    val svgIco = File(VersionFolder, "bin/korge.svg")
    val desc = "KorGE Forge $version"
    val pluginsFolder = File(VersionFolder, "plugins")
    val classpath = File(pluginsFolder, "plugin-classpath.txt")

    fun isInstalled(): Boolean = VersionFolder.isDirectory

    fun getInstallerLocalFile(fileName: String): File {
        val tenativeLocalFiles = listOf(
            File(fileName).absoluteFile,
            File(Folder, fileName).absoluteFile
        )
        return tenativeLocalFiles.firstOrNull { it.exists() }
            ?: tenativeLocalFiles.firstOrNull { it.toPath().isWritable() }
            ?: File(fileName).absoluteFile
    }

    fun expand(copy: String): String {
        val replaced = copy.replace(Regex("\\$\\w+")) {
            when (it.value) {
                "\$home" -> home
                "\$out" -> VersionFolder.absolutePath
                else -> error("Unknown variable ${it.value}")
            }
        }
        //error("EXPAND '$copy' -> '$replaced'")
        return replaced
    }
}
