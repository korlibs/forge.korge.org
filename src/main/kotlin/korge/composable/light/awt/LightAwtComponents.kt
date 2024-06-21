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
            LightContainer::class -> AwtLightContainer().also { it.component = JPanel(); it.component.layout = null }
            LightButton::class -> AwtLightButton().also { it.component = JButton(); it.registerEvent() }
            else -> TODO("Unsupported $clazz")
        }
        return comp.also { it.components = this } as T
    }
}

fun Rectangle.toLight(): LightRect = LightRect(x, y, width, height)
fun LightRect.toAwt(): Rectangle = Rectangle(x, y, width, height)

open class AwtLightComponent<TComponent : Component>() : LightComponent {
    override lateinit var components: AwtLightComponents
    lateinit var component: TComponent

    override var enabled: Boolean
        get() = component.isEnabled
        set(value) { component.isEnabled = value }

    override var bounds: LightRect
        get() = component.bounds.toLight()
        set(value) { component.bounds = value.toAwt() }

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

class MyLayout : LayoutManager {
    override fun addLayoutComponent(name: String?, comp: Component?) {
    }

    override fun removeLayoutComponent(comp: Component?) {
    }

    override fun preferredLayoutSize(parent: Container): Dimension {
        return parent.size
    }

    override fun minimumLayoutSize(parent: Container): Dimension {
        //TODO("Not yet implemented")
        return parent.minimumSize
    }

    override fun layoutContainer(parent: Container) {
        TODO("Not yet implemented")
    }
}

class AwtLightContainer() : AwtLightComponent<Container>(), LightContainer {
    val comps = arrayListOf<LightComponent>()
    val container get() = this.component

    override var relayout: (LightContainer) -> Unit = {}

    override val componentCount: Int get() = comps.size

    override fun add(component: LightComponent, index: Int) {
        //println("ADD: $component, index=$index")
        container.add(component.comp, index)
        //comps.add(index, component)
        if (index < 0) comps.add(component) else comps.add(index, component)
        //doRelayout()
    }

    override fun getComponent(index: Int): LightComponent = comps[index]

    override fun remove(index: Int) {
        //println("REMOVE: index=$index")
        container.remove(index)
        comps.removeAt(index)
        //doRelayout()
    }
    override fun removeAll() {
        //println("REMOVE_ALL")
        container.removeAll()
        comps.clear()
        //doRelayout()
    }

    override fun doRelayout() {
        super.doRelayout()
    }

    override fun endChanges() {
        super.endChanges()
        component.repaint()
    }

    //fun doRelayout() {
    //    //this.bounds = this.container.parent.bounds.toLight()
    //    this.bounds = LightRect(0, 0, 1000, 400)
    //    //println("doRelayout: bounds=$bounds")
    //    relayout(this)
    //    //component.repaint()
    //}
}
