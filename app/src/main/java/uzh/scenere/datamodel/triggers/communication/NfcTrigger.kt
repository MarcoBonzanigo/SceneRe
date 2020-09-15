package uzh.scenere.datamodel.triggers.communication

import uzh.scenere.datamodel.triggers.AbstractTrigger
import uzh.scenere.datamodel.triggers.ICommunicationTrigger
import java.util.*

class NfcTrigger(id: String?, previousId: String?, pathId: String): AbstractTrigger(id?: UUID.randomUUID().toString(), previousId, pathId), ICommunicationTrigger {

    var text: String? = null
    var message: String? = null

    fun withText(text: String?): NfcTrigger {
        this.text = text
        return this
    }

    fun withMessage(message: String?): NfcTrigger {
        this.message = message
        return this
    }
}