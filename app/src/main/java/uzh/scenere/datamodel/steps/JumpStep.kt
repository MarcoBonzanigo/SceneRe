package uzh.scenere.datamodel.steps

import java.util.*

class JumpStep(id: String?, previousId: String?, pathId: String): AbstractStep(id ?: UUID.randomUUID().toString(), previousId, pathId) {
    var targetStepId: String? = null

    fun withTargetStep(targetStepId: String): JumpStep{
        this.targetStepId = targetStepId
        return this
    }
}