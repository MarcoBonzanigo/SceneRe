package uzh.scenere.datamodel.triggers.indirect

import uzh.scenere.datamodel.triggers.AbstractTrigger
import uzh.scenere.datamodel.triggers.IIndirectTrigger
import java.util.*

class CallTrigger(id: String?, previousId: String?, pathId: String): AbstractTrigger(id ?: UUID.randomUUID().toString(), previousId, pathId), IIndirectTrigger {

    enum class CallMode{
        FIXED, RANDOM
    }

    val callMode = CallMode.FIXED

    var text: String? = null
    var telephoneNr: String? = null

    fun withText(text: String?): CallTrigger {
        this.text = text
        return this
    }

    fun withTelephoneNr(telephoneNr: String?): CallTrigger {
        this.telephoneNr = telephoneNr
        return this
    }
}