package korge.composable.light

import androidx.compose.runtime.*

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
fun LContainer(content: @Composable () -> Unit) {
    val current = LocalLightComponents.current
    ComposeLightComponent({ current.create<LightContainer>() },
        { },
        content
    )
}

interface LightContainer : LightComponent {
    val componentCount: Int
    fun add(component: LightComponent, index: Int)
    fun getComponent(index: Int): LightComponent
    fun remove(index: Int)
    fun removeAll() { repeat(componentCount) { remove(componentCount - 1) } }
}
