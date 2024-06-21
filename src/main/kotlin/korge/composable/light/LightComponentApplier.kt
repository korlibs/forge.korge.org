package korge.composable.light

import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.*
import kotlinx.coroutines.*
import kotlin.system.*

class LightComponentApplier(val container: LightContainer) : AbstractApplier<LightComponent>(container), AutoCloseable {
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

    //var id = 0

    val currentContainer get() = current as? LightContainer

    override fun up() {
        if (current is LightContainer) {
            current.bounds = LightRect(0, 0, 1000, 400)
            (current as LightContainer).doRelayout()
        }
        //println("up: $current")
        super.up()
        //println(" ---> $current")
    }
    override fun down(node: LightComponent) {
        //println("down: $current :: $node")
        super.down(node)
    }

    override fun onBeginChanges() {
        //println("onBeginChanges: $current")
        currentContainer?.beginChanges()
    }

    override fun onEndChanges() {
        //println("onEndChanges: $current")
        //current.components
        currentContainer?.endChanges()
    }

    override fun insertTopDown(index: Int, instance: LightComponent) {
        //val cont = currentContainer ?: return
        //instance.name = "${id++}"
        //println("insertTopDown[$index]: $instance")
        //cont.add(instance, cont.componentCount - 1 - index)
    }

    override fun insertBottomUp(index: Int, instance: LightComponent) {
        val cont = currentContainer ?: return
        //instance.name = "${id++}"
        //println("insertBottomUp[$index]: $current -> $instance")
        cont.add(instance, index)
    }

    override fun remove(index: Int, count: Int) {
        val cont = currentContainer ?: return
        //println("remove[$index]..$count")
        repeat(count) { cont.remove(index) }
    }

    override fun move(from: Int, to: Int, count: Int) {
        val cont = currentContainer ?: return
        //println("move: $from -> $to [$count]")
        val components = (0 until count).map { cont.getComponent(from + it) }

        for (n in 0 until count) {
            cont.add(components[n], to + n)
        }
    }

    override fun onClear() {
        val cont = currentContainer ?: return
        //println("onClear")
        cont.removeAll()
    }
}
