package uzh.scenere.datamodel

import uzh.scenere.const.Constants.Companion.STARTING_POINT
import uzh.scenere.datamodel.triggers.direct.IfElseTrigger
import uzh.scenere.helpers.NullHelper
import java.io.Serializable
import java.util.*

open class Path private constructor(val id: String, val scenarioId: String, val stakeholder: Stakeholder, val layer: Int): Serializable, IVersionItem {
    override var changeTimeMs: Long = 0

    val elements: HashMap<String,IElement> = HashMap()
    private val previousElements: HashMap<String,IElement> = HashMap()

    fun getStartingPoint(): IElement?{
        return previousElements[STARTING_POINT]
    }

    fun getEndPoint(): IElement? {
        var element = getStartingPoint()
        do{
            val nextElement = getNextElement(element?.getElementId())
            if (nextElement != null){
                element = nextElement
            }
        }while(nextElement != null)
        return element
    }

    fun getNextElement(currentElementId: String?): IElement?{
        return previousElements[currentElementId]
    }

    fun getNextElement(currentElement: IElement?): IElement?{
        if (currentElement == null){
            return null
        }
        return previousElements[currentElement.getElementId()]
    }

    fun remove(element: IElement){
        elements.remove(element.getElementId())
        previousElements.remove(element.getPreviousElementId())
    }

    fun add(element: IElement) {
        elements[element.getElementId()] = element
        if (element.getPreviousElementId() == null){
            previousElements[STARTING_POINT] = element.setPreviousElementId(STARTING_POINT)
        }else{
            previousElements[element.getPreviousElementId()!!] = element
        }
    }

    fun countIfElse(addOne: Boolean): Int{
        var isIfElse = 0
        for (element in elements){
            if (element.value is IfElseTrigger){
                isIfElse++
            }
        }
        return isIfElse + if (addOne) 1 else 0
    }

    class PathBuilder(private val scenarioId: String, private val stakeholder: Stakeholder, private val layer: Int) {

        constructor(id: String, scenarioId: String, stakeholder: Stakeholder, layer: Int): this(scenarioId, stakeholder, layer){
            this.id = id
        }

        private var id: String? = null

        fun build(): Path {
            return Path(id ?: UUID.randomUUID().toString(),scenarioId, stakeholder, layer)
        }
    }

    class NullPath(): Path("","",NullHelper.get(Stakeholder::class),0) {}

    companion object {
        val name__ = "Path"
        val id_ = "id"
        val scenarioId_ = "scenarioId"
        val stakeholder_ = "stakeholder"
        val layer_ = "layer"
    }
}