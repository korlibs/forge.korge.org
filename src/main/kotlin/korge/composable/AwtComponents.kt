package korge.composable

import androidx.compose.runtime.*
import java.awt.*
import java.awt.event.*
import java.util.concurrent.atomic.AtomicInteger
import javax.swing.*
import javax.swing.border.*

val DEFAULT_SPACING = 4

@Composable
fun Container(layoutManager: LayoutManager?, content: @Composable () -> Unit) {
    ComposeAwtComponent(
        { Container().also { it.layout = layoutManager } },
        {
            set(layoutManager) { this.layout = layoutManager }
        },
        content
    )
}

@Composable
fun Container(horizontal: Boolean, fill: Boolean, spacing: Int = DEFAULT_SPACING, content: @Composable () -> Unit) {
    Container(ComposeLayout(horizontal = horizontal, fill = fill, spacing = spacing), content)
}

@Composable
fun Box(content: @Composable () -> Unit) {
    Container(NoneLayout(), content)
    //Container(null, content)
}

@Composable
fun BackgroundPanel(image: Image?, margin: Insets = Insets(0, 0, 0, 0), content: @Composable () -> Unit) {
    ComposeAwtComponent(
        { ImagePanel(null).also { it.layout = GridLayout(1, 1) } },
        {
            set(image) { this.img = image }
            set(margin) { this.border = EmptyBorder(margin) }
        },
        content
    )
}

class ImagePanel(var img: Image?) : JPanel() {
    init {
        isDoubleBuffered = true
    }

    override fun paint(g: Graphics) {
        // Draws the img to the BackgroundPanel.
        g.drawImage(img, 0, 0, null)
        super.paintChildren(g)
    }
}

@Composable
fun VStack(spacing: Int = DEFAULT_SPACING, content: @Composable () -> Unit) {
    Container(horizontal = false, fill = false, spacing = spacing, content)
}

@Composable
fun HStack(spacing: Int = DEFAULT_SPACING, content: @Composable () -> Unit) {
    Container(horizontal = true, fill = false, spacing = spacing, content)
}

@Composable
fun VFill(spacing: Int = DEFAULT_SPACING, content: @Composable () -> Unit) {
    Container(horizontal = false, fill = true, spacing = spacing, content)
}

@Composable
fun HFill(spacing: Int = DEFAULT_SPACING, content: @Composable () -> Unit) {
    Container(horizontal = true, fill = true, spacing = spacing, content)
}

@Composable
fun Label(text: String, color: Color? = null) {
    ComposeAwtComponent({ JLabel("").also { it.isOpaque = false } }, {
        set(text) { this.text = it }
        set(color) { this.foreground = color }
    })
}

class MyJButton : JButton() {
    companion object {
        var lastButtonId = AtomicInteger(0)
    }
    val buttonId = lastButtonId.incrementAndGet()
}

@Composable
fun Button(text: String, enabled: Boolean = true, onClick: () -> Unit = { }) {
    ComposeAwtComponent({ MyJButton().also { it.isOpaque = false } }, {
        set(text) { this.text = it }
        set(enabled) { this.isEnabled = enabled }
        set(onClick) {
            //println("Update onClick listener: id=${this.buttonId}, $onClick")
            this.getListeners(ActionListener::class.java).forEach { this.removeActionListener(it) }
            this.addActionListener {
                //println("onClick!: id=${this.buttonId}, $onClick")
                onClick()
            }
        }
    })
}

@Composable
fun Image(image: Image?) {
    ComposeAwtComponent({ JLabel("") }, {
        set(image) { this.icon = ImageIcon(image) }
    })
}

@Composable
fun ProgressBar(progress: Double) {
    ComposeAwtComponent({ JProgressBar(0, 1000) }, {
        set(progress) { this.value = (it.coerceIn(0.0, 1.0) * 1000).toInt() }
    })
}

abstract class BaseLayout : LayoutManager {
    override fun addLayoutComponent(name: String?, comp: Component?) {
        //TODO("Not yet implemented")
    }

    override fun removeLayoutComponent(comp: Component?) {
        //TODO("Not yet implemented")
    }
    override fun preferredLayoutSize(parent: Container): Dimension {
        return doLayout(parent, execute = false)
        //return parent.preferredSize
    }
    override fun minimumLayoutSize(parent: Container?): Dimension = Dimension(1, 1)

    override fun layoutContainer(parent: Container) {
        doLayout(parent, execute = true)
    }

    inline fun Container.forEachVisible(block: (Component) -> Unit) {
        //for (c in parent.components) {
        for (n in 0 until componentCount) {
            val c = getComponent(n)
            if (c == this) continue
            if (!c.isVisible) continue
            block(c)
        }
    }

    abstract fun doLayout(parent: Container, execute: Boolean): Dimension
}

class ComposeLayout(val horizontal: Boolean, val fill: Boolean, val spacing: Int) : BaseLayout() {
    //val vertical get() = !horizontal

    override fun doLayout(parent: Container, execute: Boolean): Dimension {
        //println("LAYOUT: horizontal=$horizontal")
        var x = 0
        var y = 0
        var maxX = 0
        var maxY = 0
        parent.forEachVisible { c ->
            val size = c.preferredSize
            val rect = Rectangle(Point(x, y), size)
            //println(" - add[$n]=$rect")
            if (execute) c.bounds = rect
            when {
                horizontal -> {
                    x += size.width
                    maxX = x
                    maxY = maxOf(maxY, size.height)
                    x += spacing
                }
                else -> {
                    y += size.height
                    maxY = y
                    maxX = maxOf(maxX, size.width)
                    y += spacing
                }
            }
        }
        return Dimension(maxX, maxY)
    }
}

class NoneLayout : BaseLayout() {
    override fun doLayout(parent: Container, execute: Boolean): Dimension {
        var maxX = 0
        var maxY = 0

        parent.forEachVisible { c ->
            val size = c.preferredSize
            if (execute) c.bounds = Rectangle(Point(0, 0), size)
            maxX = maxOf(maxX, size.width)
            maxY = maxOf(maxY, size.height)
        }

        return Dimension(maxX, maxY).also {
            //println("NoneLayout: $it")
        }
    }
}