package korge.util

import korge.*
import java.io.*

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
