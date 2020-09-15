package uzh.scenere.views

import android.annotation.SuppressLint
import android.content.Context
import android.view.View
import android.widget.LinearLayout
import uzh.scenere.R
import uzh.scenere.const.Constants
import uzh.scenere.datamodel.Project
import uzh.scenere.datamodel.Scenario
import uzh.scenere.datamodel.Stakeholder
import uzh.scenere.datamodel.Walkthrough
import uzh.scenere.helpers.*

@SuppressLint("ViewConstructor")
class WalkthroughAnalyticLayout(context: Context, val walkthrough: Walkthrough, private val topLayer: Boolean, private val function: () -> Unit) : LinearLayout(context) {

    init {
        orientation = VERTICAL
        val params = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        params.setMargins(DipHelper.get(resources).dip5,0, DipHelper.get(resources).dip15,0)
        layoutParams = params
        createOverview()
    }

    private fun createOverview(collectOnly: Boolean = false): ArrayList<Pair<String,String>> {
        val list = ArrayList<Pair<String,String>>()
        if (collectOnly){
            val stakeholder = walkthrough.stakeholder
            val scenario = DatabaseHelper.getInstance(context).read(walkthrough.scenarioId, Scenario::class, NullHelper.get(Scenario::class))
            val project = DatabaseHelper.getInstance(context).read(scenario.projectId, Project::class, NullHelper.get(Project::class))
            if (stakeholder is Stakeholder.NullStakeholder){
                list.add(createListEntry(stakeholder.className(),context.getString(R.string.analytics_unknown)))
                list.add(createListEntry(scenario.className(),context.getString(R.string.analytics_unknown)))
                list.add(createListEntry(project.className(),context.getString(R.string.analytics_unknown)))
            }else{
                list.add(createListEntry(stakeholder.className(),stakeholder.name))
                list.add(createListEntry(scenario.className(),scenario.title))
                list.add(createListEntry(project.className(),project.title))
            }
            walkthrough.load()
            if (topLayer){
                for (property in Walkthrough.WalkthroughProperty.values()) {
                    if (property.isStatisticalValue){
                        list.add(createListEntry(property.label,property.getDisplayText()))
                    }
                }
            }else{
                for (stepId in Walkthrough.WalkthroughProperty.STEP_ID_LIST.getAll(Walkthrough.WalkthroughProperty.STEP_ID_LIST.type)){
                    for (property in (Walkthrough.WalkthroughStepProperty.values())) {
                        if (property.isStatisticalValue){
                            list.add(createListEntry(property.label,property.getDisplayText(stepId as String)))
                        }
                    }
                }
            }
            addView(createDeleteLine())
        }else{
            val stakeholder = walkthrough.stakeholder
            val scenario = DatabaseHelper.getInstance(context).read(walkthrough.scenarioId, Scenario::class, NullHelper.get(Scenario::class))
            val project = DatabaseHelper.getInstance(context).read(scenario.projectId, Project::class, NullHelper.get(Project::class))
            if (stakeholder is Stakeholder.NullStakeholder){
                addView(createLine(stakeholder.className(),false,context.getString(R.string.analytics_unknown)))
                addView(createLine(scenario.className(),false,context.getString(R.string.analytics_unknown)))
                addView(createLine(project.className(),false,context.getString(R.string.analytics_unknown)))
            }else{
                addView(createLine(stakeholder.className(),false,stakeholder.name))
                addView(createLine(scenario.className(),false,scenario.title))
                addView(createLine(project.className(),false,project.title))
            }
            walkthrough.load()
            if (topLayer){
                for (property in Walkthrough.WalkthroughProperty.values()) {
                    if (property.isStatisticalValue){
                        addView(createLine(property.label,false,property.getDisplayText(),CollectionHelper.containsOneOf(property.getDisplayText(),context.getString(R.string.walkthrough_final_state_cancelled_check))))
                    }
                }
            }else{
                for (stepId in Walkthrough.WalkthroughProperty.STEP_ID_LIST.getAll(Walkthrough.WalkthroughProperty.STEP_ID_LIST.type)){
                    for (property in (Walkthrough.WalkthroughStepProperty.values())) {
                        if (property.isStatisticalValue){
                            addView(createLine(property.label,false,property.getDisplayText(stepId as String)))
                        }
                    }
                }
            }
            addView(createDeleteLine())
        }
        return list
    }

    private fun createLine(labelText: String, multiLine: Boolean = false, presetValue: String? = null, attention: Boolean = false): View? {
        val layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        val wrapper = LinearLayout(context)
        wrapper.layoutParams = layoutParams
        wrapper.weightSum = 2f
        wrapper.orientation = if (multiLine) LinearLayout.VERTICAL else LinearLayout.HORIZONTAL
        val label = SreTextView(context,wrapper,context.getString(R.string.label, labelText))
        val weightedParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT)
        weightedParams.weight = 1f
        label.setWeight(weightedParams)
        val text = SreTextView(context,wrapper,presetValue,if (attention) SreTextView.TextStyle.ATTENTION else SreTextView.TextStyle.DARK)
        text.setWeight(1f)
        text.textAlignment = if (multiLine) View.TEXT_ALIGNMENT_TEXT_START else View.TEXT_ALIGNMENT_TEXT_END
        text.setSingleLine(multiLine)
        wrapper.addView(label)
        wrapper.addView(text)
        return wrapper
    }

    private fun createDeleteLine(): View? {
        val layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        val wrapper = LinearLayout(context)
        wrapper.layoutParams = layoutParams
        val button = SreButton(context,wrapper,context.getString(R.string.walkthrough_delete),null,null,SreButton.ButtonStyle.ATTENTION)
        button.layoutParams = layoutParams
        button.setExecutable(function)
        button.setLongClickOnly(true)
        return button
    }

    private fun createListEntry(labelText: String, presetValue: String): Pair<String,String>{
        return Pair(labelText,presetValue)
    }

    fun getExportIntroduction(total: Int): String{
        return context.getString(R.string.analytics_export_walkthrough,total)
    }

    fun getExportData(transpose: Boolean = false): ArrayList<Array<String>>{
        val list = ArrayList<Array<String>>()
        list.add(arrayOf(Constants.SPACE, Constants.SPACE))
        if (transpose){
            val first = ArrayList<String>()
            val second = ArrayList<String>()
            for (line in createOverview(true)){
                first.add(line.first)
                second.add(line.second)
            }
            list.add(first.toTypedArray())
            list.add(second.toTypedArray())
        }else{
            for (line in createOverview(true)){
                list.add(arrayOf(line.first,line.second))
            }
        }
        return list
    }
}