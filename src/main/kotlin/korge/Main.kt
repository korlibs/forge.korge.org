package korge

import korge.app.*
import korge.catalog.*
import korge.composable.*
import korge.tasks.*
import korge.util.*
import kotlinx.coroutines.*
import java.awt.*
import javax.imageio.*
import javax.xml.catalog.Catalog
import kotlin.system.*

fun main(args: Array<String>) {
    val vargs = ArrayDeque(args.toList())
    var tasks = arrayListOf<TaskWithHolder>()
    while (vargs.isNotEmpty()) {
        val item = vargs.removeFirst()
        when (item) {
            "--gui" -> tasks.clear()
            "--install" -> {
                val installer = CatalogModel.DEFAULT.installers.first()
                tasks.add(TaskWithHolder(installer.task) { it.installer = installer })
            }
            "--uninstall" -> {
                for (installation in ForgeInstallation.list()) {
                    tasks.add(TaskWithHolder(installation.uninstallTask) { it.installation = installation })
                }
            }
            "--open" -> {
                val installation = ForgeInstallation.list().first()
                tasks.add(TaskWithHolder(installation.openTask) { it.installation = installation })
            }
            "--openfolder" -> {
                val installation = ForgeInstallation.list().first()
                tasks.add(TaskWithHolder(installation.openFolderTask) { it.installation = installation })
            }
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
                    TaskExecuter.execute(task.task, task.holder) {
                        print("$it       \r")
                    }
                    println()
                }
            }
        }
        else -> {
            ComposeJFrame("Install KorGE Forge", Dimension(640, 400), configureFrame = { frame ->
                val image = runCatching { ImageIO.read(TaskInfo::class.java.getResource("/install.png")) }.getOrNull()
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