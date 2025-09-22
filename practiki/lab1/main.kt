fun main() {
    val simulationTime = 10
    val humansCount = 3

    val humans = Array(humansCount) { i ->
        Human("Человек ${i + 1}", 20 + i, 1 + Math.random() * 2)
    }

    val driver = Driver("Иван Петров", 35, 2.5, "Tesla")

    val allActors = humans.toList() + driver

    val threads = allActors.map { actor ->
        Thread {
            for (t in 1..simulationTime) {
                actor.move()
                println("Шаг $t: $actor")
                Thread.sleep(1000)
            }
        }
    }

    threads.forEach { it.start() }
    threads.forEach { it.join() }
}
