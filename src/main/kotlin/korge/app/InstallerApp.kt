package korge.app

import androidx.compose.runtime.*
import korge.*
import korge.catalog.*
import korge.catalog.tasks.*
import korge.composable.*
import korge.composable.Label
import korge.tasks.*
import korge.util.*
import java.awt.*
import javax.imageio.*
import javax.swing.*

val installerImage by lazy {
    runCatching {
        ImageIO.read(
            TaskInfo::class.java.getResource("/korge-forge-installer-bg.jpg")
                ?.readBytes()
                ?.inputStream()
        )
            ?.getScaledInstance(640, 400, java.awt.Image.SCALE_SMOOTH)
    }.getOrNull()
}

data class TaskInfo(val task: String, val ratio: Double)

@Composable
fun InstallerApp() {
    //var action by state<Task?>(null)
    var action by state<Task?>(null)
    var selectedIndex by state<Int>(0)
    var activeTasks by state<List<TaskInfo>>(emptyList())
    //val installed = KorgeForgeInstallTools.isInstalled()
    val installers = CatalogModel.DEFAULT.installers
    val installations = ForgeInstallation.list()
    val selectedInstaller = installers[selectedIndex]

    println("InstallerApp: action=$action")

    LaunchedEffect(action) {
        println("LaunchedEffect(action): $action")
        if (action != null) {
            try {
                reasonToAllowFrameClosing = action!!.name
                val holder = TasksHolder()
                holder.installer = installers[selectedIndex]
                TaskExecuter.execute(action!!, holder) {
                    print("$it     \r")
                    activeTasks = it.map { TaskInfo(it.task.name, it.ratio) }
                }
                println()
            } catch (e: Throwable) {
                e.printStackTrace()
                JOptionPane.showMessageDialog(null, "${e.cause ?: e}", "Error", JOptionPane.ERROR_MESSAGE)
            } finally {
                reasonToAllowFrameClosing = null
                println("set action=null")
                action = null
            }
        }
    }

    BackgroundPanel(installerImage, margin = Insets(8, 8, 8, 8)) {
        //Image(installerImage)
        VStack {
            Label(
                "KorGE Forge Installer: Detected os=${OS.str()}, arch=${ARCH.str()}, installed=${installations}, jvm=${System.getProperty("java.version")}",
                color = Color.WHITE
            )
            HStack {
                //Button("Test", enabled = action == null) { action = TestTask2 }
                //Button(if (installed) "Reinstall" else "Install", enabled = action == null) {
                DropDown(installers, selectedIndex = selectedIndex, enabled = action == null) { index, value ->
                    selectedIndex = index
                }
                //Button("Install", enabled = action == null && !selectedInstaller.tools.isInstalled()) {
                val alreadyInstalled = selectedInstaller.tools.isInstalled()
                val installedVersion = selectedInstaller.tools.installedVersion
                Button(
                    when {
                        action is InstallerTask -> "Installing..."
                        alreadyInstalled && installedVersion != selectedInstaller.name -> "Update '$installedVersion' -> '${selectedInstaller.name}'"
                        alreadyInstalled -> "Reinstall"
                        else -> "Install"
                    }, enabled = action == null) {
                    println("Install pressed")
                    //action = InstallKorgeForge
                    action = installers[selectedIndex].task
                }
                //Button("Test1", enabled = action == null) {
                //    action = TestTask1
                //}
            }

            if (installations.isNotEmpty() && action == null) {
                HStack {
                    Label("Existing Installations:", color = Color.WHITE)
                }
                for (installation in installations) {
                    val installed = installation.tools.isInstalled()
                    HStack {
                        Button("Uninstall ${installation.version}", enabled = action == null && installed) {
                            println("Uninstall ${installation.version} pressed")
                            action = installation.uninstallTask
                        }
                        Button("Open", enabled = action == null && installed) {
                            println("Open pressed")
                            action = installation.openTask
                        }
                        Button("Open Folder", enabled = action == null && installed) {
                            println("Open Installation Folder")
                            action = installation.openFolderTask
                        }
                    }
                }
            }
            if (DeleteDownloadCacheTask.isAvailable && action == null) {
                HStack {
                    Label("Download Cache:", color = Color.WHITE)
                }
                Button("Delete Download Cache", enabled = action == null) {
                    println("Delete Cache pressed")
                    action = DeleteDownloadCacheTask
                }
            }
            for (task in activeTasks) {
                HStack {
                    Label(task.task, color = Color.WHITE)
                    ProgressBar(task.ratio)
                }
            }
        }
    }
}
