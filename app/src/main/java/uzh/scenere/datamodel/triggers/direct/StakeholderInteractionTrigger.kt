package uzh.scenere.datamodel.triggers.direct

import uzh.scenere.datamodel.triggers.AbstractTrigger
import uzh.scenere.datamodel.triggers.IDirectTrigger
import java.util.*

class StakeholderInteractionTrigger(id: String?, previousId: String?, pathId: String) : AbstractTrigger(id
        ?: UUID.randomUUID().toString(), previousId, pathId), IDirectTrigger {
    var text: String? = null
    var interactedStakeholderId: String? = null

    fun withText(text: String?): StakeholderInteractionTrigger {
        this.text = text
        return this
    }

    fun withInteractedStakeholderId(interactedStakeholderId: String?): StakeholderInteractionTrigger {
        this.interactedStakeholderId = interactedStakeholderId
        return this
    }

}