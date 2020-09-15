package uzh.scenere.datamodel.steps

import uzh.scenere.datamodel.Resource
import java.util.*

class ResourceStep(id: String?, previousId: String?, pathId: String): AbstractStep(id ?: UUID.randomUUID().toString(), previousId, pathId) {
    var resource: Resource? = null
    var change: Int = 0

    fun withResource(resource: Resource?): ResourceStep{
        this.resource = resource
        return this
    }

    fun withChange(change: Int): ResourceStep{
        this.change = change
        return this
    }
}