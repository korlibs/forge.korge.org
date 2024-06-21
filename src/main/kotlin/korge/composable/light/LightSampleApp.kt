package korge.composable.light

import androidx.compose.runtime.*
import korge.composable.*

@Composable
fun LightSampleApp() {
    LContainer {
        var n by state(0)
        LLabel("Hello world!")
        LButton("Hello world! $n") {
            n++
        }
        LButton("-") { n-- }
        LButton("+") { n++ }
    }
}
