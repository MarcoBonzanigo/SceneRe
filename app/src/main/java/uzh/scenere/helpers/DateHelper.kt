package uzh.scenere.helpers

import java.text.SimpleDateFormat
import java.util.*


class DateHelper {

    companion object {
        fun getCurrentTimestamp(pattern: String = "dd-MM-yyyy_HHmmss"): String{
            return toTimestamp(System.currentTimeMillis(),pattern)
        }

        fun toTimestamp(timeMs: Long, pattern: String = "dd-MM-yyyy_HH_mm_ss"): String{
            val date = Date(timeMs)
            val format = SimpleDateFormat(pattern)
            return format.format(date)
        }
    }
}