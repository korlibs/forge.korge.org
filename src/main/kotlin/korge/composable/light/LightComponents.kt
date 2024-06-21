package korge.composable.light

import androidx.compose.runtime.*
import korge.composable.light.awt.*

@Composable
fun LButton(text: String, enabled: Boolean = true, onClick: () -> Unit = { }) {
    val current = LocalLightComponents.current
    //ComposeLightComponent({ lightComponent<LightButton>() }) {
    ComposeLightComponent({ current.create<LightButton>() }) {
        set(text) { this.text = it }
        set(enabled) { this.enabled = enabled }
        set(onClick) { this.onClick = onClick }
    }
}

interface LightButton : LightComponent {
    var text: String
    var onClick: () -> Unit
}

@Composable
fun LLabel(text: String) {
    val current = LocalLightComponents.current
    ComposeLightComponent({ current.create<LightLabel>() }) {
        set(text) { this.text = it }
        //set(color) { this.foreground = color }
    }
}

interface LightLabel : LightComponent {
    var text: String
}

@Composable
fun LContainer(relayout: LightLayout = DummyLightLayout, content: @Composable () -> Unit) {
    val current = LocalLightComponents.current
    ComposeLightComponent(
        { current.create<LightContainer>() },
        {
            set(relayout) { this.relayout = relayout }
        },
        content
    )
}

interface LightContainer : LightComponent {
    val componentCount: Int
    var relayout: LightLayout
    fun add(component: LightComponent, index: Int)
    fun getComponent(index: Int): LightComponent
    fun remove(index: Int)
    fun removeAll() { repeat(componentCount) { remove(componentCount - 1) } }
    fun beginChanges() {
    }
    fun endChanges() {
    }
}

fun LightContainer.doRelayout(apply: Boolean = true): LightSize {
    return relayout(this, apply)
}

class LightContainerList(val container: LightContainer) : AbstractList<LightComponent>() {
    override val size: Int  get() = container.componentCount
    override operator fun get(index: Int): LightComponent = container.getComponent(index)
}

val LightContainer.children: LightContainerList get() = LightContainerList(this)
