package uzh.scenere.helpers

import uzh.scenere.const.Constants.Companion.EARTH_RADIUS_M
import kotlin.math.PI

class NumberHelper {
    companion object { //Static Reference
        fun <T : Number> nvl(value: T?, valueIfNull: T): T {
            return value ?: valueIfNull; // Elvis Expression of Java number==null?valueIfNull:number
        }

        fun divide(a: Int, b: Int): Int {
            return divide(a, b.toFloat())
        }

        fun divide(a: Int, b: Float): Int {
            if (b == 0f) {
                throw ArithmeticException("Division by zero.")
            }
            return (a.toFloat() / b).toInt()
        }

        fun multiply(a: Int, b: Float): Int {
            return (a * b).toInt()
        }

        fun multiply(a: Int, b: Double): Int {
            return (a * b).toInt()
        }

        private fun multiply(a: Long, b: Double): Long {
            return (a * b).toLong()
        }

        fun randomLong(): Long {
            return multiply(java.lang.Long.MAX_VALUE, Math.random())
        }

        fun floor(num: Double, digits: Int): Double {
            return Math.floor(num * digits) / digits
        }

        fun ceil(num: Double, digits: Int): Double {
            return Math.ceil(num * digits) / digits
        }

        fun floor(num: Float, digits: Int): Float {
            return (Math.floor((num * digits).toDouble()) / digits).toFloat()
        }

        fun ceil(num: Float, digits: Int): Float {
            return (Math.ceil((num * digits).toDouble()) / digits).toFloat()
        }

        fun createApplicationIdFromString(applicationString: String?): Long {
            if (applicationString == null || applicationString.contains("\\.")) {
                throw IllegalArgumentException("Something went wrong, did you register a Application-Name and Package-Location? Current Application-Id: " + applicationString!!)
            }
            val pts = applicationString.split("\\.".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            val type = if (pts.size - 1 > 3) 3 else pts.size - 1
            val pointer = arrayOf(intArrayOf(0, 0, 0, 0), intArrayOf(0, 1, 0, 1), intArrayOf(0, 1, 2, 0), intArrayOf(0, 1, 2, 3))
            val head = "1"
            val length = fillWithZeros(applicationString.length.toString(), 3)
            val application = stringToLongString(pts[pts.size - 1], 3)
            val package1 = stringToLongString(pts[pointer[type][0]], 3)
            val package2 = stringToLongString(pts[pointer[type][1]], 3)
            val package3 = stringToLongString(pts[pointer[type][2]], 3)
            val package4 = stringToLongString(pts[pointer[type][3]], 3)
            return -java.lang.Long.valueOf(head + length + application + package1 + package2 + package3 + package4)
        }

        private fun stringToLongString(s: String, maxDigits: Int): String {
            val stringLong = stringToLong(s, maxDigits)
            return fillWithZeros(stringLong.toString(), maxDigits)
        }

        private fun fillWithZeros(number: String, digits: Int): String {
            var nr = number
            for (i in nr.length until digits) {
                nr = "0$nr"
            }
            return nr
        }

        private fun stringToLong(s: String, maxDigits: Int): Long {
            var l: Long = 0
            for (c in s.toCharArray()) {
                l += c.toLong()
            }
            val valWithDigits = maxValWithDigits(maxDigits)
            return if (valWithDigits <= l) l % valWithDigits else l
        }

        private fun maxValWithDigits(maxDigits: Int): Long {
            return Math.pow(10.0, maxDigits.toDouble()).toLong()
        }

        fun max(vararg numbers: Float): Float {
            var max = numbers[0]
            for (n in numbers) {
                max = if (n > max) n else max
            }
            return max
        }

        fun capAt(i: Float, low: Float, high: Float): Float {
            return when {
                i < low -> low
                i > high -> high
                else -> i
            }
        }


        fun capAt(i: Int?, low: Int, high: Int): Int {
            val nvl = nvl(i,0)
            return when {
                nvl < low -> low
                nvl > high -> high
                else -> nvl
            }
        }

        fun capAtHigh(i: Int, high: Int): Int {
            return when {
                i > high -> high
                else -> i
            }
        }

        fun capAtLow(i: Int, low: Int): Int {
            return when {
                i < low -> low
                else -> i
            }
        }

        fun safeToNumber(numberString: String?, returnIfFailed: Long): Long {
            if (numberString != null) {
                return try {
                    val long = numberString.toLong()
                    long
                } catch (e: Exception) {
                    returnIfFailed
                }
            }
            return returnIfFailed
        }

        fun getDistanceInMeter(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
            val degreeRadianFraction = PI / 180.0
            val distanceLatitude = degreeRadianFraction.times(lat2 - lat1)
            val distanceLongitude = degreeRadianFraction.times(lon2 - lon1)

            val radianLatitude1 = degreeRadianFraction.times(lat1)
            val radianLatitude2 = degreeRadianFraction.times(lat2)

            val part = Math.sin(distanceLatitude / 2) * Math.sin(distanceLatitude / 2) +
                    Math.sin(distanceLongitude / 2) * Math.sin(distanceLongitude / 2) *
                    Math.cos(radianLatitude1) * Math.cos(radianLatitude2)
            val meterFraction = 2 * Math.atan2(Math.sqrt(part), Math.sqrt(1 - part))
            return floor(EARTH_RADIUS_M * meterFraction,2)
        }
    }
}