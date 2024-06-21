package korge.composable.light.awt

import korge.composable.light.*
import java.awt.*
import javax.swing.*
import kotlin.reflect.*

object AwtLightComponentsExperiment {
    @JvmStatic
    fun main(args: Array<String>) {
        val frame = JFrame()
        AwtLightComponents.create<LightContainer>().also {
            frame.add((it as AwtLightComponent<*>).component)
        }.setComposeContent {
            LightSampleApp()
        }

        frame.minimumSize = Dimension(100, 100)
        frame.setLocationRelativeTo(null)
        frame.pack()
        frame.defaultCloseOperation = JFrame.EXIT_ON_CLOSE
        frame.isVisible = true
    }
}

object AwtLightComponents : LightComponents {
    override fun <T : LightComponent> create(clazz: KClass<T>): T {
        val comp: AwtLightComponent<*> = when (clazz) {
            LightLabel::class -> AwtLightLabel().also { it.component = JLabel() }
            LightContainer::class -> AwtLightContainer().also { it.component = JPanel() }
            LightButton::class -> AwtLightButton().also { it.component = JButton(); it.registerEvent() }
            else -> TODO("Unsupported $clazz")
        }
        return comp.also { it.components = this } as T
    }
}

open class AwtLightComponent<TComponent : Component>() : LightComponent {
    override lateinit var components: AwtLightComponents
    lateinit var component: TComponent

    override var enabled: Boolean
        get() = component.isEnabled
        set(value) { component.isEnabled = value }

    val LightComponent.comp get() = (this as AwtLightComponent<*>).component
}

open class AwtLightButton() : AwtLightComponent<JButton>(), LightButton {
    override var text: String
        get() = component.text
        set(value) { component.text = value }
    override var onClick: () -> Unit = {}

    fun registerEvent() {
        component.addActionListener {
            onClick()
        }
    }
}

open class AwtLightLabel() : AwtLightComponent<JLabel>(), LightLabel {
    override var text: String
        get() = component.text
        set(value) { component.text = value }

}

class AwtLightContainer() : AwtLightComponent<Container>(), LightContainer {
    val _components = arrayListOf<LightComponent>()
    val container get() = this.component

    override val componentCount: Int get() = _components.size

    override fun add(component: LightComponent, index: Int) {
        container.add(component.comp, index)
        if (index < 0) _components.add(component) else _components.add(index, component)
    }

    override fun getComponent(index: Int): LightComponent = _components[index]

    override fun remove(index: Int) {
        container.remove(index)
        _components.removeAt(index)
    }
    override fun removeAll() {
        container.removeAll()
        _components.clear()
    }
}
