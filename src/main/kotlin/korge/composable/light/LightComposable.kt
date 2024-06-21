package korge.composable.light

import androidx.compose.runtime.*
import kotlin.reflect.*

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

data class LightSize(val width: Int, val height: Int)
data class LightRect(val x: Int, val y: Int, val width: Int, val height: Int) {
    val size = LightSize(width, height)
}

interface LightComponent {
    val parent: LightContainer?
    val components: LightComponents
    var enabled: Boolean
    var bounds: LightRect
    val size: LightSize get() = bounds.size
    var preferredSize: LightSize
}
