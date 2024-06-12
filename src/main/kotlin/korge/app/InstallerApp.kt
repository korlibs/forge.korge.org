package korge.app

import androidx.compose.runtime.*
import korge.*
import korge.composable.*
import korge.composable.Label
import korge.tasks.*
import java.awt.*
import javax.imageio.*

val installerImage by lazy {
    runCatching {
        ImageIO.read(
            InstallKorgeForge::class.java.getResource("/korge-forge-installer-bg.jpg")
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
    var activeTasks by state<List<TaskInfo>>(emptyList())
    val installed = KorgeForgeInstallTools.isInstalled()

    println("InstallerApp: action=$action")

    LaunchedEffect(action) {
        println("LaunchedEffect(action): $action")
        if (action != null) {
            try {
                reasonToAllowFrameClosing = action!!.name
                TaskExecuter.execute(action!!) {
                    print("$it     \r")
                    activeTasks = it.map { TaskInfo(it.task.name, it.ratio) }
                }
                println()
            } finally {
                reasonToAllowFrameClosing = null
                action = null
            }
        }
    }

    BackgroundPanel(installerImage, margin = Insets(8, 8, 8, 8)) {
        //Image(installerImage)
        VStack {
            Label(
                "KorGE Forge Installer: Detected os=${OS.CURRENT}, arch=${ARCH.CURRENT}, installed=$installed",
                color = Color.WHITE
            )
            HStack {
                Button(if (installed) "Reinstall" else "Install", enabled = action == null) {
                    println("Install pressed")
                    action = InstallKorgeForge
                }
                Button("Uninstall", enabled = action == null && installed) {
                    println("Uninstall pressed")
                    action = UninstallKorgeForge
                }
                Button("Delete Download Cache", enabled = action == null && DeleteDownloadArtifacts.enabled) {
                    println("Delete Cache pressed")
                    action = DeleteDownloadArtifacts
                }
                Button("Open", enabled = action == null && installed) {
                    println("Open pressed")
                    action = OpenTask
                }
                Button("Open Folder", enabled = action == null && installed) {
                    println("Open Installation Folder")
                    action = OpenInstallFolderTask
                }
                //Button("Test1", enabled = action == null) {
                //    action = TestTask1
                //}
                //Button("Test2", enabled = action == null) {
                //    action = TestTask2
                //}
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
