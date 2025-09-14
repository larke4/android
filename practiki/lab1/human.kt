import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.PI
import kotlin.random.Random

class Human(
    var fullName: String,
    var age: Int,
    var speed: Double
) {

    var x: Double = 0.0
        private set
    var y: Double = 0.0
        private set

    fun move() {
        val angle = Random.nextDouble(0.0, 2 * PI)
        x += speed * cos(angle)
        y += speed * sin(angle)
    }

    override fun toString(): String {
        return "$fullName ($age лет) находится в точке (%.2f, %.2f)".format(x, y)
    }
}