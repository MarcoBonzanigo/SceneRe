package uzh.scenere.datamodel.triggers.communication

import uzh.scenere.datamodel.triggers.AbstractTrigger
import uzh.scenere.datamodel.triggers.ICommunicationTrigger
import java.util.*


@Deprecated("No Application yet")
class MobileNetworkTrigger(id: String?, previousId: String?, pathId: String): AbstractTrigger(id ?: UUID.randomUUID().toString(), previousId, pathId), ICommunicationTrigger {
}