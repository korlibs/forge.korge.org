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

        frame.minimumSize = Dimension(200, 200)
        frame.setLocationRelativeTo(null)
        frame.pack()
        frame.defaultCloseOperation = JFrame.EXIT_ON_CLOSE
        frame.isVisible = true
    }
}

object AwtLightComponents : LightComponents {
    override fun <T : LightComponent> create(clazz: KClass<T>): T {
        val comp: AwtLightComponent<*> = when (clazz) {
            LightLabel::class -> AwtLightLabel()
            LightContainer::class -> AwtLightContainer()
            LightButton::class -> AwtLightButton()
            else -> TODO("Unsupported $clazz")
        }
        //comp.component.size = comp.component.preferredSize
        return comp.also { it.components = this } as T
    }
}

fun Rectangle.toLight(): LightRect = LightRect(x, y, width, height)
fun LightRect.toAwt(): Rectangle = Rectangle(x, y, width, height)

fun Dimension.toLight(): LightSize = LightSize(width, height)
fun LightSize.toAwt(): Dimension = Dimension(width, height)

abstract class AwtLightComponent<TComponent : Component>() : LightComponent {
    override var parent: LightContainer? = null
    override lateinit var components: AwtLightComponents
    abstract val component: TComponent

    override var enabled: Boolean
        get() = component.isEnabled
        set(value) { component.isEnabled = value }

    override var bounds: LightRect
        get() = component.bounds.toLight()
        set(value) { component.bounds = value.toAwt() }
    override var preferredSize: LightSize
        get() = component.preferredSize.toLight()
        set(value) { component.preferredSize = value.toAwt() }

    val LightComponent.comp get() = (this as AwtLightComponent<*>).component
}

open class AwtLightButton(override val component: JButton = JButton()) : AwtLightComponent<JButton>(), LightButton {
    override var text: String
        get() = component.text
        set(value) { component.text = value }
    override var onClick: () -> Unit = {}

    init {
        component.addActionListener {
            onClick()
        }
    }
}

open class AwtLightLabel(override val component: JLabel = JLabel()) : AwtLightComponent<JLabel>(), LightLabel {
    override var text: String
        get() = component.text
        set(value) { component.text = value }
}

class MyLayout(val container: AwtLightContainer) : LayoutManager {
    override fun addLayoutComponent(name: String?, comp: Component?) {
    }

    override fun removeLayoutComponent(comp: Component?) {
    }

    override fun preferredLayoutSize(parent: Container): Dimension {
        return container.doRelayout(apply = false).toAwt()
        //return parent.size
    }


    override fun minimumLayoutSize(parent: Container): Dimension {
        //TODO("Not yet implemented")
        return parent.minimumSize
    }

    override fun layoutContainer(parent: Container) {
        println("layoutContainer: $parent")
        //this.container.bounds = LightRect(0, 0, parent.width, parent.height)
        //println(this.container.bounds)
        this.container.doRelayout(true)
    }
}

typealias LightLayout = (parent: LightContainer, apply: Boolean) -> LightSize

val DummyLightLayout: LightLayout = { parent, apply -> parent.size }

class AwtLightContainer(override val component: JPanel = JPanel()) : AwtLightComponent<Container>(), LightContainer {
    val comps = arrayListOf<LightComponent>()
    val container get() = this.component

    init {
        component.layout = MyLayout(this)
    }

    override var relayout: LightLayout = DummyLightLayout

    override val componentCount: Int get() = comps.size

    override fun add(component: LightComponent, index: Int) {
        //println("ADD: $component, index=$index")
        (component as AwtLightComponent<*>).parent = this
        container.add(component.comp, index)
        //comps.add(index, component)
        if (index < 0) comps.add(component) else comps.add(index, component)
        //doRelayout()
    }

    override fun getComponent(index: Int): LightComponent = comps[index]

    override fun remove(index: Int) {
        //println("REMOVE: index=$index")
        (component as AwtLightComponent<*>).parent = null
        container.remove(index)
        comps.removeAt(index)
        //doRelayout()
    }
    override fun removeAll() {
        //println("REMOVE_ALL")
        comps.forEach { (it as AwtLightComponent<*>).parent = null }

        container.removeAll()
        comps.clear()
        //doRelayout()
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
