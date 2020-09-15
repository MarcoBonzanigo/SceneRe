package uzh.scenere.views

import android.annotation.SuppressLint
import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import uzh.scenere.R
import uzh.scenere.const.Constants.Companion.FRACTION
import uzh.scenere.const.Constants.Companion.NEW_LINE
import uzh.scenere.const.Constants.Companion.NEW_LINE_TOKEN
import uzh.scenere.const.Constants.Companion.NOTHING
import uzh.scenere.const.Constants.Companion.SPACE
import uzh.scenere.datamodel.Walkthrough
import uzh.scenere.datastructures.StatisticArrayList
import uzh.scenere.helpers.DipHelper
import uzh.scenere.helpers.NumberHelper
import uzh.scenere.helpers.StringHelper
import uzh.scenere.helpers.addOne
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

@SuppressLint("ViewConstructor")
class CommentAnalyticLayout(context: Context, vararg  val walkthroughs: Walkthrough) : LinearLayout(context) {

    private lateinit var comments: HashMap<String,ArrayList<CommentWrapper>>
    private lateinit var sortedStepList: ArrayList<String>
    private lateinit var stepRuns: HashMap<String,Int>
    private var activeStepName: String = NOTHING

    private var stepPointer: Int = 0

    fun previousStep(){
        if (!comments.isEmpty()){
            if (stepPointer>0){
                stepPointer--
            }else{
                stepPointer = comments.size-1
            }
        }
        visualizeOverview()
    }
    init {
        orientation = VERTICAL
        val params = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        params.setMargins(DipHelper.get(resources).dip5,0, DipHelper.get(resources).dip15,0)
        layoutParams = params
        createOverview()
    }

    fun nextStep(collectOnly: Boolean = false): ArrayList<Pair<String,String>> {
        if (!comments.isEmpty()) {
            if (stepPointer < comments.size-1) {
                stepPointer++
            } else {
                stepPointer = 0
            }
        }
        return visualizeOverview(collectOnly)
    }

    private fun createOverview() {
        comments = HashMap()
        stepRuns = HashMap()
        sortedStepList = ArrayList()
        val sortingMap = TreeMap<Float,String>()
        for (walkthrough in walkthroughs){
            walkthrough.load()
            val author = Walkthrough.WalkthroughProperty.WT_OWNER.get(String::class)
            val timestamp = Walkthrough.WalkthroughProperty.TIMESTAMP.getDisplayText()
            val stakeholderName = Walkthrough.WalkthroughProperty.STAKEHOLDER_NAME.getDisplayText()
            var stepNumber = 1
            for (stepId in Walkthrough.WalkthroughProperty.STEP_ID_LIST.getAll(String::class)){
                if (!comments.contains(stepId)){
                    comments[stepId] = ArrayList()
                }
                val stepComments = Walkthrough.WalkthroughStepProperty.STEP_COMMENTS.getAll(stepId, String::class)
                stepRuns.addOne(stepId)
                if (stepComments.isNullOrEmpty()){
                    stepNumber++
                    continue
                }
                val restoredComments = ArrayList<String>()
                for (comment in stepComments){
                    restoredComments.add(comment.replace(NEW_LINE_TOKEN,NEW_LINE))
                }
                val stepTitle = Walkthrough.WalkthroughStepProperty.STEP_TITLE.get(stepId, String::class)
                val stepTime = Walkthrough.WalkthroughStepProperty.STEP_TIME.get(stepId, Long::class)
                val stepText = Walkthrough.WalkthroughStepProperty.STEP_TEXT.get(stepId, String::class).replace(NEW_LINE_TOKEN,NEW_LINE)
                val wrapper = CommentWrapper(author,stepTime,restoredComments,stepTitle,stepText,stakeholderName,stepNumber,timestamp)
                if (!comments[stepId]!!.contains(wrapper)){
                    comments[stepId]?.add(wrapper)
                }else{
                    val index = comments[stepId]?.indexOf(wrapper)
                    if (index != null){
                        comments[stepId]?.get(index)?.stepRuns?.plus(1)
                    }
                }
                addStepToSortingMap(sortingMap,stepId,stepNumber.toFloat())
                stepNumber++
            }
        }
        //SORTING
        for (entry in sortingMap.entries){
            if (!sortedStepList.contains(entry.value)) {
                sortedStepList.add(entry.value)
            }
        }
        //CLEANUP
        val removalList = ArrayList<String>()
        for (entry in comments){
            var remove = true
            for (wrapper in entry.value){
                if (!wrapper.comments.isNullOrEmpty()){
                    remove = false
                }
            }
            if (remove){
                removalList.add(entry.key)
            }
        }
        for (stepId in removalList){
            sortedStepList.remove(stepId)
            comments.remove(stepId)
        }
        visualizeOverview()
    }

    private fun addStepToSortingMap(sortingMap: TreeMap<Float, String>, stepId: String, stepNumber: Float) {
        val fetchedStepId = sortingMap.get(stepNumber)
        if (fetchedStepId != null && fetchedStepId != stepId){
            addStepToSortingMap(sortingMap,stepId,stepNumber.plus(FRACTION))
        }else{
            sortingMap[stepNumber] = stepId
        }
    }

    private fun visualizeOverview(collectOnly: Boolean = false): ArrayList<Pair<String,String>> {
        val list = ArrayList<Pair<String,String>>()
        if (collectOnly){
            if (sortedStepList.isEmpty()) {
                return list
            }
            val stepId = sortedStepList[stepPointer]
            val wrapperList = comments[stepId]
            if (!wrapperList.isNullOrEmpty()) {
                val commentWrapper = wrapperList.first()
                activeStepName = commentWrapper.title
                list.add(createListEntry(context.getString(R.string.analytics_step_title), commentWrapper.title))
                list.add(createListEntry(context.getString(R.string.analytics_step_text), commentWrapper.text))
                val times = StatisticArrayList<Long>()
                var runs = 0
                var stakeholderName = NOTHING
                for (wrapper in wrapperList) {
                    times.add(wrapper.time)
                    runs += wrapper.stepRuns
                    if (StringHelper.hasText(wrapper.stakeholderName)) {
                        stakeholderName = wrapper.stakeholderName
                    }
                }
                if (StringHelper.hasText(stakeholderName)) {
                    list.add(createListEntry(context.getString(R.string.analytics_step_stakeholder), stakeholderName))
                }
                list.add(createListEntry(context.getString(R.string.analytics_step_number), wrapperList[0].stepNumber.toString()))
                list.add(createListEntry(context.getString(R.string.analytics_step_runs), NumberHelper.nvl(stepRuns[stepId], 0).toString()))
                val avg = times.avg()
                list.add(createListEntry(context.getString(R.string.analytics_avg_time), context.getString(R.string.analytics_x_seconds, avg)))
                for (wrapper in wrapperList) {
                    if (!wrapper.comments.isNullOrEmpty()) {
                        val comments = StringHelper.concatList(NEW_LINE, wrapper.comments)
                        list.add(createListEntry(context.getString(R.string.analytics_comment_of, wrapper.author), comments))
                        list.add(createListEntry(context.getString(R.string.analytics_comment_timestamp), wrapper.timestamp))
                    }
                }
            }
        }else {
            removeAllViews()
            if (sortedStepList.isEmpty()) {
                return list
            }
            val stepId = sortedStepList[stepPointer]
            val wrapperList = comments[stepId]
            if (!wrapperList.isNullOrEmpty()) {
                val commentWrapper = wrapperList.first()
                activeStepName = commentWrapper.title
                addView(createLine(context.getString(R.string.analytics_step_title), false, commentWrapper.title))
                addView(createLine(context.getString(R.string.analytics_step_text), false, commentWrapper.text))
                val times = StatisticArrayList<Long>()
                var runs = 0
                var stakeholderName = NOTHING
                for (wrapper in wrapperList) {
                    times.add(wrapper.time)
                    runs += wrapper.stepRuns
                    if (StringHelper.hasText(wrapper.stakeholderName)) {
                        stakeholderName = wrapper.stakeholderName
                    }
                }
                if (StringHelper.hasText(stakeholderName)) {
                    addView(createLine(context.getString(R.string.analytics_step_stakeholder), false, stakeholderName))
                }
                addView(createLine(context.getString(R.string.analytics_step_number), false, wrapperList[0].stepNumber.toString()))
                addView(createLine(context.getString(R.string.analytics_step_runs), false, NumberHelper.nvl(stepRuns[stepId], 0).toString()))
                val avg = times.avg()
                addView(createLine(context.getString(R.string.analytics_avg_time), false, context.getString(R.string.analytics_x_seconds, avg)))
                for (wrapper in wrapperList) {
                    if (!wrapper.comments.isNullOrEmpty()) {
                        val comments = StringHelper.concatList(NEW_LINE, wrapper.comments)
                        addView(createLine(context.getString(R.string.analytics_comment_of, wrapper.author), false, comments, SreTextView.TextStyle.MEDIUM))
                        addView(createLine(context.getString(R.string.analytics_comment_timestamp), false, wrapper.timestamp))
                    }
                }
            }
        }
        return list
    }

    private fun createListEntry(labelText: String, presetValue: String): Pair<String,String>{
        return Pair(labelText,presetValue)
    }

    private fun createLine(labelText: String, multiLine: Boolean = false, presetValue: String? = null, specialStyle: SreTextView.TextStyle = SreTextView.TextStyle.DARK): View? {
        val layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        val wrapper = LinearLayout(context)
        wrapper.layoutParams = layoutParams
        wrapper.weightSum = 2f
        wrapper.orientation = if (multiLine) LinearLayout.VERTICAL else LinearLayout.HORIZONTAL
        val label = SreTextView(context,wrapper,context.getString(R.string.label, labelText))
        val weightedParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT)
        weightedParams.weight = 1f
        label.setWeight(weightedParams)
        val text = SreTextView(context,wrapper,presetValue,specialStyle)
        text.setWeight(1f)
        text.textAlignment = if (multiLine) View.TEXT_ALIGNMENT_TEXT_START else View.TEXT_ALIGNMENT_TEXT_END
        text.setSingleLine(multiLine)
        wrapper.addView(label)
        wrapper.addView(text)
        return wrapper
    }

    fun addTo(viewGroup: ViewGroup): Boolean {
        if (parent == null){
            viewGroup.addView(this)
            return true
        }
        return false
    }

    fun getStepCount(): Int {
        return sortedStepList.size
    }

    fun getStepsWithCommentsCount(): Int {
        return comments.size
    }

    fun getActiveStepName(): String {
        return activeStepName
    }

    private class CommentWrapper(val author: String, val time: Long, val comments: List<String>, val title: String, val text: String, val stakeholderName: String, val stepNumber: Int, val timestamp: String){

        var stepRuns = 1

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as CommentWrapper

            if (time != other.time) return false
            if (text != other.text) return false

            return true
        }

        override fun hashCode(): Int {
            var result = time.hashCode()
            result = 31 * result + text.hashCode()
            return result
        }
    }

    fun getExportIntroduction(): String{
        return context.getString(R.string.analytics_comments_export,comments.size,sortedStepList.size)
    }
    
    fun getExportData(transpose: Boolean = false): ArrayList<Array<String>>{
        val list = ArrayList<Array<String>>()
        list.add(arrayOf(SPACE,SPACE))
        if (!comments.isEmpty()){
            do{
                if (transpose){
                    val first = ArrayList<String>()
                    val second = ArrayList<String>()
                    for (line in nextStep(true)){
                        first.add(line.first)
                        second.add(line.second)
                    }
                    list.add(first.toTypedArray())
                    list.add(second.toTypedArray())
                }else{
                    for (line in nextStep(true)){
                        list.add(arrayOf(line.first,line.second))
                    }
                }
                list.add(arrayOf(SPACE,SPACE))
            }while(stepPointer != 0)
        }
        if (!transpose){
            list.removeAt(list.size-1)
        }
        return list
    }
}