package korge

import korge.app.*
import korge.composable.*
import korge.tasks.*
import kotlinx.coroutines.*
import java.awt.*
import javax.imageio.*
import kotlin.system.*

fun main(args: Array<String>) {
    val vargs = ArrayDeque(args.toList())
    var tasks = arrayListOf<Task>()
    while (vargs.isNotEmpty()) {
        val item = vargs.removeFirst()
        when (item) {
            "--gui" -> tasks.clear()
            "--install" -> tasks.add(InstallKorgeForge)
            "--uninstall" -> tasks.add(UninstallKorgeForge)
            "--open" -> tasks.add(OpenTask)
            "--openfolder" -> tasks.add(OpenInstallFolderTask)
            "help", "-?", "-h", "--help", "/?", "/help" -> {
                println("KorGE Forge Installer $KORGE_FORGE_VERSION")
                println(" --install - Installs KorGE Forge silently")
                println(" --uninstall - Uninstalls KorGE Forge silently")
                println(" --open - Opens KorGE Forge when installed")
                println(" --openfolder - Opens KorGE Forge installation folder")
                println(" --gui - Show the GUI (no parameters also displays the GUI)")
                println(" --help - Show this help")
                exitProcess(0)
            }
            else -> error("Unknown command '$item'")
        }
    }

    when {
        tasks.isNotEmpty() -> {
            runBlocking {
                for (task in tasks) {
                    TaskExecuter.execute(task) {
                        print("$it       \r")
                    }
                    println()
                }
            }
        }
        else -> {
            ComposeJFrame("Install KorGE Forge $KORGE_FORGE_VERSION", Dimension(640, 400), configureFrame = { frame ->
                val image = runCatching { ImageIO.read(InstallKorgeForge::class.java.getResource("/install.png")) }.getOrNull()
                try {
                    if (image != null) setTaskbarIcon(image)
                } catch (e: Throwable) {
                    e.printStackTrace()
                }
                frame.iconImage = image
                frame.isResizable = false
            }) {
                InstallerApp()
            }
        }
    }
}

private fun setTaskbarIcon(image: Image) {
    if (OS.CURRENT == OS.OSX) {
        val taskBar = ClassLoader.getSystemClassLoader().loadClass("java.awt.Taskbar")
        val getTaskBar = taskBar.methods.firstOrNull { it.name == "getTaskbar" }
        val taskBarInstance = getTaskBar?.invoke(null)
        val setIconImage = taskBar.methods.firstOrNull { it.name == "setIconImage" }
        //println("taskBarInstance=$taskBarInstance")
        //Taskbar.getTaskbar().iconImage = image
        setIconImage?.invoke(taskBarInstance, image)
        //println("method=$setIconImage")
    }
}