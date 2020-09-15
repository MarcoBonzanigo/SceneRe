package uzh.scenere.datamodel.triggers.indirect

import uzh.scenere.datamodel.triggers.AbstractTrigger
import uzh.scenere.datamodel.triggers.IIndirectTrigger
import java.util.*

class SmsTrigger(id: String?, previousId: String?, pathId: String): AbstractTrigger(id ?: UUID.randomUUID().toString(), previousId, pathId), IIndirectTrigger {
    enum class SmsMode{
        FIXED, RANDOM
    }

    val smsMode = SmsMode.FIXED

    var text: String? = null
    var telephoneNr: String? = null

    fun withText(text: String?): SmsTrigger {
        this.text = text
        return this
    }

    fun withTelephoneNr(telephoneNr: String?): SmsTrigger {
        this.telephoneNr = telephoneNr
        return this
    }
}