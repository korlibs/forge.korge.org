package korge.composable

import androidx.compose.runtime.*

@Composable
fun <T> state(initial: T) = remember { mutableStateOf(initial) }
