package korge.composable

import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.*
import kotlinx.coroutines.*
import java.awt.*
import java.awt.event.*
import javax.swing.*
import kotlin.system.*

var reasonToAllowFrameClosing: String? = null

fun ComposeJFrame(title: String, size: Dimension = Dimension(640, 400), configureFrame: (JFrame) -> Unit = {}, content: @Composable () -> Unit) {
    try {
        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())
    } catch (ex: Throwable) {
        ex.printStackTrace()
    }

    val frame = JFrame(title)

    setComposeContent(frame.contentPane, content)

    frame.contentPane.preferredSize = size

    frame.addWindowListener(object : WindowAdapter() {
        override fun windowClosing(e: WindowEvent?) {
            if (reasonToAllowFrameClosing != null) {
                val dialogResult = JOptionPane.showConfirmDialog(null, reasonToAllowFrameClosing!!, "Are you sure to quit?", JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE)
                if (dialogResult != JOptionPane.YES_OPTION) return
            }
            frame.isVisible = false
            System.exit(0)
        }
    })

    //frame.layout = FlowLayout(FlowLayout.LEFT)
    //frame.contentPane.add(JLabel(ImageIcon(image)))

    configureFrame(frame)

    frame.pack()
    frame.setLocationRelativeTo(null)
    frame.isVisible = true

    frame.defaultCloseOperation = JFrame.DO_NOTHING_ON_CLOSE

}

fun setComposeContent(
    root: Container,
    content: @Composable () -> Unit
): Composition {
    return AwtComponentApplier(root).composition.apply { setContent(content) }
}

class AwtComponentApplier(val container: Container) : AbstractApplier<Component>(container), AutoCloseable {
    val context = Dispatchers.Main + object : MonotonicFrameClock {
        override suspend fun <R> withFrameNanos(onFrame: (Long) -> R): R =
            onFrame(measureNanoTime { delay(16L) })
    }
    init {
        //println("NodeApplier for container=$container")
    }

    private val scheduleScope = CoroutineScope(context + SupervisorJob())
    val snapshotObserver = Snapshot.registerGlobalWriteObserver {
        //println("Snapshot.registerGlobalWriteObserver: ${Thread.currentThread()}")
        scheduleScope.launch {
            //println("Snapshot.registerGlobalWriteObserver.launch: ${Thread.currentThread()}")
            Snapshot.sendApplyNotifications()
        }
    }
    val recomposer = Recomposer(context).also { recomposer ->
        CoroutineScope(context).launch(start = CoroutineStart.UNDISPATCHED) {
            //println("runRecomposeAndApplyChanges")
            recomposer.runRecomposeAndApplyChanges()
        }
    }
    val composition = ControlledComposition(this, recomposer)

    override fun close() {
        snapshotObserver.dispose()
        recomposer.close()
    }

    var id = 0

    val currentContainer get() = current as? Container

    override fun insertTopDown(index: Int, instance: Component) {
        //val cont = currentContainer ?: return
        //instance.name = "${id++}"
        //println("insertTopDown[$index]: $instance")
        //cont.add(instance, cont.componentCount - 1 - index)
        //cont.doUpdate()
    }

    override fun insertBottomUp(index: Int, instance: Component) {
        val cont = currentContainer ?: return
        instance.name = "${id++}"
        //println("insertBottomUp[$index]: $instance")
        cont.add(instance, index)
        cont.doUpdate()
    }

    override fun remove(index: Int, count: Int) {
        val cont = currentContainer ?: return
        //println("remove[$index]..$count")
        repeat(count) { cont.remove(index) }
        cont.doUpdate()
    }

    override fun move(from: Int, to: Int, count: Int) {
        val cont = currentContainer ?: return
        //println("move: $from -> $to [$count]")
        val components = (0 until count).map { cont.getComponent(from + it) }

        for (n in 0 until count) {
            cont.add(components[n], to + n)
        }
        cont.doUpdate()
    }

    override fun onClear() {
        val cont = currentContainer ?: return
        //println("onClear")
        cont.removeAll()
        cont.doUpdate()
    }

    private fun Component.doUpdate() {
        revalidate()
        repaint()
    }
}

@Suppress("NONREADONLY_CALL_IN_READONLY_COMPOSABLE")
@Composable
inline fun <T : Component> ComposeAwtComponent(
    noinline factory: () -> T,
    update: @DisallowComposableCalls Updater<T>.() -> Unit,
    content: @Composable () -> Unit
) {
    ComposeNode<T, AwtComponentApplier>(factory, update, content)
}

@Composable inline fun <T : Component> ComposeAwtComponent(
    noinline factory: () -> T,
    update: @DisallowComposableCalls Updater<T>.() -> Unit
) {
    ComposeNode<T, AwtComponentApplier>(factory, update)
}
