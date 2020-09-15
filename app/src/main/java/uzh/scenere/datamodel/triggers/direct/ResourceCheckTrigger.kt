package uzh.scenere.datamodel.triggers.direct

import android.content.Context
import uzh.scenere.R
import uzh.scenere.datamodel.Resource
import uzh.scenere.datamodel.triggers.AbstractTrigger
import uzh.scenere.datamodel.triggers.IDirectTrigger
import java.util.*
import kotlin.collections.ArrayList

class ResourceCheckTrigger(id: String?, previousId: String?, pathId: String): AbstractTrigger(id ?: UUID.randomUUID().toString(), previousId, pathId), IDirectTrigger {
    enum class CheckMode(val pos: Int) {
        ABOVE(1), BELOW(2), EQUAL(3), NOT_EQUAL(4);
        companion object {
            fun getModes(): Array<String> {
                val list = ArrayList<String>()
                for (mode in CheckMode.values()){
                    list.add(mode.toString())
                }
                return list.toTypedArray()
            }
            fun getFromPos(pos: Int): CheckMode?{
                for (mode in CheckMode.values()){
                    if (pos == mode.pos){
                        return mode
                    }
                }
                return null
            }
        }
        fun getSucceedString(res: String, value: Int, check: Int, context: Context): String{
            return when (this){
                ABOVE -> context.getString(R.string.resource_check_above,res,check,value)
                BELOW -> context.getString(R.string.resource_check_below,res,check,value)
                EQUAL -> context.getString(R.string.resource_check_equal,res,check,value)
                NOT_EQUAL -> context.getString(R.string.resource_check_not_equal,res,check,value)
            }
        }
    }


    var buttonLabel: String? = null
    var mode: CheckMode = CheckMode.EQUAL
    var resource: Resource? = null
    var checkValue: Int = 0
    var falseStepId: String? = null

    fun withButtonLabel(label: String?): ResourceCheckTrigger{
        this.buttonLabel = label
        return this
    }

    fun withResource(resource: Resource?): ResourceCheckTrigger{
        this.resource = resource
        return this
    }

    fun withMode(mode: String): ResourceCheckTrigger{
        this.mode = CheckMode.valueOf(mode)
        return this
    }

    fun withMode(mode: CheckMode): ResourceCheckTrigger{
        this.mode = mode
        return this
    }

    fun withModePos(pos: Int): ResourceCheckTrigger{
        val fromPos = CheckMode.getFromPos(pos)
        if (fromPos != null){
            this.mode = fromPos
        }
        return this
    }

    fun withCheckValue(value: Int): ResourceCheckTrigger{
        this.checkValue = value
        return this
    }

    fun withFalseStepId(stepId: String): ResourceCheckTrigger {
        this.falseStepId = stepId
        return this
    }
}