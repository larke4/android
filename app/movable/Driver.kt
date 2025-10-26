class Driver(
    fullName: String,
    age: Int,
    speed: Double,
    val carModel: String
) : Human(fullName, age, speed) {

    override fun move() {
        // Прямолинейное движение вдоль оси X
        x += speed
    }

    override fun toString(): String {
        return "$fullName ($age лет, водитель $carModel) находится в точке (%.2f, %.2f)".format(x, y)
    }
}
