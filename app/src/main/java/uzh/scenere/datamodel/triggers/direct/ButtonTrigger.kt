package uzh.scenere.datamodel.triggers.direct

import uzh.scenere.datamodel.triggers.AbstractTrigger
import uzh.scenere.datamodel.triggers.IDirectTrigger
import java.util.*

class ButtonTrigger(id: String?, previousId: String?, pathId: String): AbstractTrigger(id ?: UUID.randomUUID().toString(), previousId, pathId), IDirectTrigger {
    var buttonLabel: String? = null

    fun withButtonLabel(label: String?): ButtonTrigger{
        buttonLabel = label
        return this
    }
}