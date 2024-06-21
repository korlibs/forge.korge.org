package korge.composable.light

import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.*
import kotlinx.coroutines.*
import kotlin.reflect.*
import kotlin.system.*

val LocalLightComponents = compositionLocalOf<LightComponents> { LightComponents.Dummy }

fun LightContainer.setComposeContent(
    components: LightComponents = this.components,
    content: @Composable () -> Unit
): Composition {
    return LightComponentApplier(this).composition.apply {
        setContent {
            CompositionLocalProvider(LocalLightComponents provides components) {
                content()
            }
        }
    }
}

@Suppress("NONREADONLY_CALL_IN_READONLY_COMPOSABLE")
@Composable
inline fun <T : LightComponent> ComposeLightComponent(
    noinline factory: () -> T,
    update: @DisallowComposableCalls Updater<T>.() -> Unit,
    content: @Composable () -> Unit
) {
    ComposeNode<T, LightComponentApplier>(factory, update, content)
}

@Composable
inline fun <T : LightComponent> ComposeLightComponent(
    noinline factory: () -> T,
    update: @DisallowComposableCalls Updater<T>.() -> Unit
) {
    ComposeNode<T, LightComponentApplier>(factory, update)
}

//@Suppress("NONREADONLY_CALL_IN_READONLY_COMPOSABLE")
//@Composable
//inline fun <reified T : LightComponent> ComposeLightComponent(
//    update: @DisallowComposableCalls Updater<T>.() -> Unit,
//    content: @Composable () -> Unit
//) {
//    ComposeNode<T, LightComponentApplier>(@Composable { lightComponent<T>() }, update, content)
//}
//
//@Composable inline fun <reified T : LightComponent> ComposeLightComponent(
//    update: @DisallowComposableCalls Updater<T>.() -> Unit
//) {
//    ComposeNode<T, LightComponentApplier>({ lightComponent<T>() }, update)
//}

//@Composable
//inline fun <reified T : LightComponent> lightComponent(): T = LocalLightComponentProvider.current.create(T::class)

interface LightComponents {
    object Dummy : LightComponents
    fun <T : LightComponent> create(clazz: KClass<T>): T = TODO()
}

inline fun <reified T : LightComponent> LightComponents.create(): T = create(T::class)

interface LightComponent {
    val components: LightComponents
    var enabled: Boolean
    fun setBounds(x: Int, y: Int, width: Int, height: Int) {
    }
}

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

    override fun insertTopDown(index: Int, instance: LightComponent) {
        val cont = currentContainer ?: return
        //instance.name = "${id++}"
        //println("insertTopDown[$index]: $instance")
        cont.add(instance, cont.componentCount - 1 - index)
    }

    override fun insertBottomUp(index: Int, instance: LightComponent) {
        val cont = currentContainer ?: return
        //instance.name = "${id++}"
        //println("insertBottomUp[$index]: $instance")
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
