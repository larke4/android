fun main() {
    val simulationTime = 10
    val humansCount = 3

    val humans = Array(humansCount) { i ->
        Human("Человек ${i + 1}", 20 + i, 1 + Math.random() * 2)
    }

    for (t in 1..simulationTime) {
        println("Шаг $t")
        for (h in humans) {
            h.move()
            println(h)
        }
        println("----------")
        Thread.sleep(1000)
    }
}