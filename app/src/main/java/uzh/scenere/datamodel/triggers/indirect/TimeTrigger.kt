package uzh.scenere.datamodel.triggers.indirect

import uzh.scenere.datamodel.triggers.AbstractTrigger
import uzh.scenere.datamodel.triggers.IDirectTrigger
import uzh.scenere.helpers.NumberHelper
import java.util.*

class TimeTrigger(id: String?, previousId: String?, pathId: String): AbstractTrigger(id ?: UUID.randomUUID().toString(), previousId, pathId), IDirectTrigger {
    enum class TimeMode{
        FIXED_COUNTDOWN, RANDOM_COUNTDOWN, FIXED_TIME, RANDOM_TIME
    }

    val timeMode = TimeMode.FIXED_COUNTDOWN

    var text: String? = null
    var timeMs: Long? = null

    fun withText(text: String?): TimeTrigger {
        this.text = text
        return this
    }

    fun withTime(timeMs: Long?): TimeTrigger {
        this.timeMs = timeMs
        return this
    }

    fun withTimeSecond(time: String?): TimeTrigger {
        this.timeMs = NumberHelper.safeToNumber(time,0)*1000
        return this
    }

    fun withTimeMillisecondSecond(time: String?): TimeTrigger {
        this.timeMs = NumberHelper.safeToNumber(time,0)
        return this
    }

    fun getTimeSecond():Long{
        return NumberHelper.nvl(timeMs?.div(1000),0)
    }
}