package uzh.scenere.datamodel.steps

import java.util.*

class SoundStep(id: String?, previousId: String?, pathId: String): AbstractStep(id ?: UUID.randomUUID().toString(), previousId, pathId) {
}