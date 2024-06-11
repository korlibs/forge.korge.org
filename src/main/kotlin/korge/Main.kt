package korge

import korge.app.*
import korge.composable.*
import korge.tasks.*
import java.awt.*
import java.io.*
import kotlin.io.path.*

fun main() {
    ComposeJFrame("Install KorGE Forge $KORGE_FORGE_VERSION", Dimension(640, 400)) {
        InstallerApp()
    }
}
