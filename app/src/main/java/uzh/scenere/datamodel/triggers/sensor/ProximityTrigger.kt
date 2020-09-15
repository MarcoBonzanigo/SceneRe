package uzh.scenere.datamodel.triggers.sensor

import uzh.scenere.datamodel.triggers.AbstractTrigger
import uzh.scenere.datamodel.triggers.ISensorTrigger
import java.util.*

@Deprecated("Most Devices don't support this Sensor")
class ProximityTrigger(id: String?, previousId: String?, pathId: String): AbstractTrigger(id ?: UUID.randomUUID().toString(), previousId, pathId), ISensorTrigger {
}