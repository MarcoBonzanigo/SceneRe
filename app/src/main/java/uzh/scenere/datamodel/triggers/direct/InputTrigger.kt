package uzh.scenere.datamodel.triggers.direct

import uzh.scenere.datamodel.triggers.AbstractTrigger
import uzh.scenere.datamodel.triggers.IDirectTrigger
import java.util.*

class InputTrigger(id: String?, previousId: String?, pathId: String) : AbstractTrigger(id?: UUID.randomUUID().toString(), previousId, pathId), IDirectTrigger {
    var text: String? = null
    var input: String? = null

    fun withText(text: String?): InputTrigger {
        this.text = text
        return this
    }

    fun withInput(input: String?): InputTrigger {
        this.input = input
        return this
    }

}