package korge.tasks

import korge.*
import kotlinx.coroutines.*
import java.io.*
import kotlin.io.path.*
import kotlin.time.Duration.Companion.seconds

val KORGE_FORGE_VERSION = "2024.1"

object OpenTask : Task("Opening KorGE Forge") {
    override suspend fun execute(context: TaskContext) {
        when (OS.CURRENT) {
            OS.OSX -> ProcessBuilder("open", KorgeForgeInstallTools.MacAPP.absolutePath).start().waitFor()
            OS.LINUX -> ProcessBuilder("gtk-launch", KorgeForgeInstallTools.KORGE_FORGE_DESKTOP.name).start().waitFor()
            OS.WINDOWS -> ProcessBuilder("cmd", "/c", "start", InstallKorgeForge.exe.absolutePath).start().waitFor()
        }
    }
}

object OpenInstallFolderTask : Task("Opening Install Folder for KorGE Forge") {
    override suspend fun execute(context: TaskContext) {
        when (OS.CURRENT) {
            OS.OSX -> ProcessBuilder("open", "-R", KorgeForgeInstallTools.MacAPP.absolutePath).start().waitFor()
            OS.LINUX -> ProcessBuilder("xdg-open", File(KorgeForgeInstallTools.VersionFolder, "bin").absolutePath).start().waitFor()
            OS.WINDOWS -> ProcessBuilder("explorer.exe", InstallKorgeForge.exe.parentFile.absolutePath).start().waitFor()
        }
    }
}

object TestTask1 : Task("Test1") {
    override suspend fun execute(context: TaskContext) {
        println("Test1a")
        delay(0.25.seconds)
        error("Error1")
        println("Test1b")
    }
}

object TestTask2 : Task("Test2", TestTask1) {
    override suspend fun execute(context: TaskContext) {
        println("Test2")
    }
}

object UninstallKorgeForge : Task("Uninstalling KorGE Forge", DeleteKorgeForgeFolder) {
    override suspend fun execute(context: TaskContext) {
        when (OS.CURRENT) {
            OS.LINUX -> {
                KorgeForgeInstallTools.KORGE_FORGE_DESKTOP.delete()
            }
            else -> {
                KorgeForgeInstallTools.START_MENU_LNK.delete()
                KorgeForgeInstallTools.DESKTOP_LNK.delete()
            }
        }
    }
}

object DeleteKorgeForgeFolder : Task("Deleting KorGE Forge Folder") {
    override suspend fun execute(context: TaskContext) {
        val files = ExtractForge.outDirectory.walkBottomUp().toList()
        for ((it, item) in files.withIndex()) {
            item.delete()
            context.report(it.toLong() until files.size.toLong())
        }
    }
}

object DeleteDownloadArtifacts : Task("Deleting KorGE Forge Downloaded Artifacts") {
    val files by lazy {
        listOf(
            DownloadJBR.localFile,
            DownloadForge.localFile,
            DownloadForgeExtraLibs.localFile,
            DownloadForgeProductInfo.localFile,
        )
    }

    val enabled get() = files.any { it.exists() }

    override suspend fun execute(context: TaskContext) {
        for (file in files) {
            file.delete()
        }
    }
}

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

object DownloadJBR : Task("Download JBR", DownloadForge) {
    fun url(os: OS = OS.CURRENT, arch: ARCH = ARCH.CURRENT): String =
        "https://cache-redirector.jetbrains.com/intellij-jbr/${filename(os, arch)}"

    fun filename(os: OS = OS.CURRENT, arch: ARCH = ARCH.CURRENT): String {
        val _os = when (os) {
            OS.WINDOWS -> "windows"
            OS.OSX -> "osx"
            OS.LINUX -> "linux"
            else -> TODO()
        }
        val _arch = when (arch) {
            ARCH.X64 -> "x64"
            ARCH.ARM -> "aarch64"
        }

        return "jbr_jcef-21.0.3-${_os}-${_arch}-b480.1.tar.gz"
    }

    val fileName by lazy { filename() }
    val url by lazy { url() }
    val localFile by lazy { KorgeForgeInstallTools.getInstallerLocalFile(fileName) }

    // https://github.com/JetBrains/JetBrainsRuntime/releases/tag/jbr-release-21.0.3b480.1
    override suspend fun execute(context: TaskContext) {
        context.report(url)
        downloadFile(
            url,
            localFile,
            progress = context::report
        )
    }
}

object DownloadForge : Task("Download KorGE Forge") {
    val BASE_URL = "https://github.com/korlibs/forge.korge.org/releases/download/2024.1.1-alpha"
    val baseFile = "korgeforge-241.15989.20240606.tar.zst"
    val url = "$BASE_URL/$baseFile"
    val sha256 = "CE48EE906CD06FA98B036B675F18F131B91D778171BA26AB560B0ABE2B69475E"

    val localFile by lazy { KorgeForgeInstallTools.getInstallerLocalFile(baseFile) }

    override suspend fun execute(context: TaskContext) {
        context.report(url)
        downloadFile(
            url,
            localFile,
            sha256 = sha256,
            progress = context::report
        )
    }
}

object DownloadForgeExtraLibs : Task("Download KorGE Forge Extra Libs") {
    val baseFile = "korgeforge-extra-libs-241.15989.20240606.tar.zst"
    val url = "${DownloadForge.BASE_URL}/$baseFile"
    val sha256 = "118e10e049457e28120ff6dd409f1212e9b6684962b49ab067973ddc347de127"

    val localFile by lazy { KorgeForgeInstallTools.getInstallerLocalFile(baseFile) }

    override suspend fun execute(context: TaskContext) {
        context.report(url)
        downloadFile(
            url,
            localFile,
            sha256 = sha256,
            progress = context::report
        )
    }
}

object DownloadForgeProductInfo : Task("Download KorGE Forge Product Info") {
    val baseFile = "korgeforge-mac-product-info.241.15989.20240606.json"
    val url = "${DownloadForge.BASE_URL}/$baseFile"
    val sha256 = "0bcd093ea0df95f49ead6c20cd8bfc28d17940590c2e4bf64ecb97e274b637c1"

    val localFile by lazy { KorgeForgeInstallTools.getInstallerLocalFile(baseFile) }

    override suspend fun execute(context: TaskContext) {
        context.report(url)
        downloadFile(
            url,
            localFile,
            sha256 = sha256,
            progress = context::report
        )
    }
}

object KorgeForgeInstallTools {
    val Folder = when (OS.CURRENT) {
        OS.LINUX -> File(System.getProperty("user.home"), ".local/share/KorGEForge")
        OS.OSX -> File(System.getProperty("user.home"), "Applications/KorGE Forge ${KORGE_FORGE_VERSION}.app/Contents")
        else -> File(System.getProperty("user.home"), "AppData/Local/KorGEForge")
    }
    val MacAPP = Folder.parentFile
    val VersionFolder = when (OS.CURRENT) {
        OS.OSX -> Folder
        else -> File(Folder, KORGE_FORGE_VERSION)
    }
    val START_MENU = when (OS.CURRENT) {
        OS.LINUX -> "${System.getProperty("user.home")}/.local/share/applications"
        else -> "${System.getenv("APPDATA")}\\Microsoft\\Windows\\Start Menu"
    }
    val START_MENU_LNK = File(START_MENU, "KorGE Forge ${KORGE_FORGE_VERSION}.lnk")
    val DESKTOP_LNK = File(getDesktopFolder(), "KorGE Forge ${KORGE_FORGE_VERSION}.lnk")
    val KORGE_FORGE_DESKTOP = File(START_MENU, "korge-forge-${KORGE_FORGE_VERSION}.desktop")

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

fun createWindowsLnk(exe: File, lnk: File, ico: File, description: String) {
    val createShortcutPs1File = File(System.getProperty("java.io.tmpdir"), "create_shortcut.ps1")
    createShortcutPs1File.writeText("""
        param (
            [string]${'$'}targetPath,
            [string]${'$'}shortcutPath,
            [string]${'$'}description,
            [string]${'$'}iconPath
        )

        ${'$'}WScriptShell = New-Object -ComObject WScript.Shell
        ${'$'}shortcut = ${'$'}WScriptShell.CreateShortcut(${'$'}shortcutPath)
        ${'$'}shortcut.TargetPath = ${'$'}targetPath
        ${'$'}shortcut.Description = ${'$'}description
        ${'$'}shortcut.IconLocation = ${'$'}iconPath
        ${'$'}shortcut.Save()
    """.trimIndent())

    ProcessBuilder(
        "powershell.exe",
        "-NoProfile",
        "-ExecutionPolicy", "Bypass",
        "-File", createShortcutPs1File.absolutePath,
        "-targetPath", exe.absolutePath,
        "-shortcutPath", lnk.absolutePath,
        "-description", description,
        "-iconPath", ico.absolutePath
    ).inheritIO().start().waitFor()

    //powershell -NoProfile -ExecutionPolicy Bypass -File create_shortcut.ps1 -targetPath demo.exe -shortcutPath demo.lnk -description "hello" -iconPath = "demo.ico"
}

fun getDesktopFolder(): File = when (OS.CURRENT) {
    OS.WINDOWS -> {
        val tryPath = runCatching {
            ProcessBuilder(
                "powershell.exe",
                "-NoProfile",
                "-ExecutionPolicy", "Bypass",
                "/c", "[Environment]::GetFolderPath('Desktop')",
            ).inheritIO().start().also { it.waitFor() }.inputStream.bufferedReader().readText().takeIf { it.isNotBlank() }
        }.getOrNull()
        tryPath?.let { File(it) }?.takeIf { it.isDirectory } ?: File(System.getenv("USERPROFILE"), "Desktop")
    }
    OS.OSX -> File(System.getProperty("user.home"), "Desktop")
    OS.LINUX -> File(System.getProperty("user.home"), "Desktop")
}