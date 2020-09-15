package uzh.scenere.datamodel.triggers

import uzh.scenere.datamodel.IElement
import uzh.scenere.datamodel.IVersionItem

abstract class AbstractTrigger(val id: String, var previousId: String?, val pathId: String) : IElement, IVersionItem {
    override var changeTimeMs: Long = 0

    private val nextStepsIds: List<String> = ArrayList()

    fun hasSingleTransition(): Boolean{
        return nextStepsIds.size==1
    }

    fun getTransition(): String{
        return ""
    }

    override fun getElementId(): String {
        return id
    }

    override fun getPreviousElementId(): String? {
        return previousId
    }

    override fun getElementPathId(): String {
        return pathId
    }

    override fun setPreviousElementId(id: String): IElement {
        previousId = id
        return this
    }

    class NullTrigger(): AbstractTrigger("","","") {
        override var changeTimeMs: Long = 0
    }

}