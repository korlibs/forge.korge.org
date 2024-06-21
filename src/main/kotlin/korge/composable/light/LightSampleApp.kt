package korge.composable.light

import androidx.compose.runtime.*
import korge.composable.*

fun FillXLayout(it: LightContainer, apply: Boolean): LightSize {
    var x = 0
    //println("CHLDREN: ${it.children.size}")
    val children = it.children
    val bounds = it.bounds
    val childWidth = bounds.width / children.size
    for (c in children) {
        val cpsize = c.preferredSize
        //println(" - $c")
        //c.bounds = LightRect(x, 0, width = childWidth, height = bounds.height)
        if (apply) c.bounds = c.bounds.copy(x = x, y = 0, width = cpsize.width, height = cpsize.height)
        //c.bounds = it.bounds.copy(x)
        x += cpsize.width
    }
    return LightSize(x, 32)
}

@Composable
fun LightSampleApp() {
    LContainer(::FillXLayout) {
        var n by state(0)
        LLabel(if (n < 0) "NEGATIVE WORLD" else "Hello world!")
        LButton("-") { n-- }
        LButton("+") { n++ }
        if (n >= 0) {
            LButton("N: $n") {
                n++
            }
        }
    }
}
