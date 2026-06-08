package dhp.thl.tpl.unlock.photos

data class PointData(val xPct: Float, val yPct: Float)

fun serializePoints(points: List<PointData>): String {
    return points.joinToString(";") { "${it.xPct},${it.yPct}" }
}

fun deserializePoints(str: String?): List<PointData> {
    if (str.isNullOrBlank()) return emptyList()
    return str.split(";").mapNotNull {
        val parts = it.split(",")
        if (parts.size == 2) {
            try {
                PointData(parts[0].toFloat(), parts[1].toFloat())
            } catch (e: Exception) {
                null
            }
        } else null
    }
}
