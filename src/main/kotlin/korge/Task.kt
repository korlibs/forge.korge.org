package korge

import korlibs.datastructure.*
import kotlinx.coroutines.*
import kotlin.coroutines.*

class TasksHolder : Extra by Extra() {
    private val tasksToContext = LinkedHashMap<Task, TaskContext>()
    fun getTotalTasks(): Int = synchronized(this) { tasksToContext.size }
    fun getTaskContext(task: Task): TaskContext = synchronized(this) { tasksToContext.getOrPut(task) { TaskContext(task, this) } }
    fun getAllTasks(): List<TaskContext> = synchronized(this) { tasksToContext.map { it.value } }
    fun getActiveTasks(): List<TaskContext> = synchronized(this) { tasksToContext.map { it.value }.filter { it.running } }
}

class TaskContext(val task: Task, val holder: TasksHolder) {
    val completed = CompletableDeferred<Unit>()
    val dependencies by lazy { task.dependencies.map { holder.getTaskContext(it) } }

    var running = false
    var executing = false
    var ratio: Double = 0.0

    override fun toString(): String = if (running) "${task.name}(${(ratio * 100).toInt()}%)" else "${task.name}(waiting...)"

    fun report(desc: String) {
    }

    fun report(range: LongRange) {
        ratio = range.first.toDouble() / range.last.toDouble()
    }

    suspend fun executeOnce() {
        if (!executing) {
            executing = true
            try {
                completed.completeWith(kotlin.runCatching {
                    ensureDependencies()
                    try {
                        running = true
                        withContext(Dispatchers.IO) {
                            task.execute(this@TaskContext)
                        }
                    } finally {
                        running = false
                    }
                })
            } finally {
                executing = false
            }
        }
        completed.await()
    }

    private suspend fun ensureDependencies() {
        CoroutineScope(coroutineContext).run { dependencies.map { async { it.executeOnce() } } }.awaitAll()
    }
}

abstract class Task(val name: String, vararg val baseDependencies: Task) {
    val dependencies: MutableList<Task> = baseDependencies.toMutableList()
    fun <T : Task?> dependsOn(task: T): T {
        if (task != null) dependencies += task
        return task
    }
    fun <T : Collection<out Task>> dependsOn(tasks: T): T {
        dependencies += tasks
        return tasks
    }
    abstract suspend fun execute(context: TaskContext)
    override fun toString(): String = "${this::class.simpleName}"
    fun graphVisitor(visited: MutableSet<Task> = mutableSetOf(), visit: (Task) -> Unit) {
        if (this in visited) return
        visited += this
        for (dep in dependencies) dep.graphVisitor(visited, visit)
        visit(this)
    }
}

fun <T : Task> T.dependsOn(task: Collection<Task>): T {
    dependencies += task
    return this
}

class TaskWithHolder(val task: Task, val holder: TasksHolder = TasksHolder()) {
    companion object {
        operator fun invoke(task: Task, block: (TasksHolder) -> Unit): TaskWithHolder {
            return TaskWithHolder(task).also { block(it.holder) }
        }
    }
}

object TaskExecuter {
    //private fun computeTree(task: Task, out: MutableList<Task> = arrayListOf<Task>(), visited: LinkedHashSet<Task> = LinkedHashSet()): List<Task> {
    //    if (task !in visited) {
    //        visited += task
    //        for (dep in task.dependencies) computeTree(dep, out, visited)
    //        out += task
    //    }
    //    return out
    //}

    suspend fun execute(task: Task, tasks: TasksHolder = TasksHolder(), report: (tasks: List<TaskContext>) -> Unit = {
        print("${System.currentTimeMillis()}[${it.size}]: $it\r")
    }) {
        println("Executing $task...")
        task.graphVisitor {
            println(" - ${it::class.simpleName} DEPENDS ${it.dependencies.map { it::class.simpleName }} ----> $it")
        }

        val job = CoroutineScope(coroutineContext + SupervisorJob()).async {
            tasks.getTaskContext(task).executeOnce()
        }
        while (!job.isCompleted) {
            val totalTasks = tasks.getTotalTasks()
            val activeTasks = tasks.getActiveTasks()
            //val allTasks = tasks.getAllTasks()
            report(activeTasks)
            if (totalTasks > 0 && activeTasks.isEmpty()) break
            delay(100L)
        }
        try {
            job.await()
        } finally {
            report(emptyList())
        }
    }
}