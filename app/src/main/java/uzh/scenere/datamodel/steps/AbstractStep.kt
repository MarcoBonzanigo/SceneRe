package uzh.scenere.datamodel.steps

import uzh.scenere.const.Constants.Companion.NOTHING
import uzh.scenere.const.Constants.Companion.SEMI_COLON
import uzh.scenere.datamodel.*
import uzh.scenere.helpers.StringHelper
import kotlin.collections.ArrayList

abstract class AbstractStep(val id: String, var previousId: String?, val pathId: String): IElement, IVersionItem {
    override var changeTimeMs: Long = 0

    var comments = ArrayList<String>() //Not saved in Element, only holder
    var whatIfs = ArrayList<String>()
    var objects =  ArrayList<AbstractObject>()
    var title: String? = null
    var text: String? = null
    var time: Long = 0

    fun withTitle(title: String): AbstractStep {
        this.title = title
        return this
    }

    fun withText(text: String): AbstractStep {
        this.text = text
        return this
    }

    fun withTime(time: Long): AbstractStep {
        this.time = time
        return this
    }

    fun withObjects(objectList: ArrayList<AbstractObject>): AbstractStep{
        this.objects.addAll(objectList)
        return this
    }

    fun withObject(obj: AbstractObject): AbstractStep{
        this.objects.add(obj)
        return this
    }

    fun withWhatIfs(whatIfs: List<String>? = null): AbstractStep{
        if (whatIfs != null){
            this.whatIfs.clear()
            this.whatIfs.addAll(whatIfs)
        }
        return this
    }

    fun withComments(comments: List<String>? = null): AbstractStep{
        if (!comments.isNullOrEmpty()){
            this.comments.clear()
            this.comments.addAll(comments)
        }
        return this
    }

    override fun getElementId(): String {
        return id
    }

    override fun getElementPathId(): String {
        return pathId
    }


    override fun getPreviousElementId(): String? {
        return previousId
    }

    override fun setPreviousElementId(id: String): IElement {
        previousId = id
        return this
    }

    fun getObjectNames(): String {
        if (objects.isEmpty()){
            return NOTHING
        }
        return StringHelper.concatTokens(SEMI_COLON, *objects.toTypedArray())
    }

    class NullStep(): AbstractStep("","","") {
        override var changeTimeMs: Long = 0
    }
}