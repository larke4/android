class Driver(
    fullName: String,
    age: Int,
    speed: Double,
    val carModel: String
) : Human(fullName, age, speed) {

    override fun move() {
        x += speed
    }

    override fun toString(): String {
        return "$fullName ($age лет, водитель $carModel) находится в точке (%.2f, %.2f)".format(x, y)
    }
}
