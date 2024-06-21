package korge.composable.light

import androidx.compose.runtime.*
import korge.composable.*

@Composable
fun LightSampleApp() {
    LContainer({
        var x = 0
        //println("CHLDREN: ${it.children.size}")
        for (c in it.children) {
            //println(" - $c")
            c.bounds = LightRect(x, 0, 100, 40)
            //c.bounds = it.bounds.copy(x)
            x += 100
        }
    }) {
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
