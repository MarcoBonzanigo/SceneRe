package uzh.scenere.activities

import android.content.Intent
import android.content.res.Configuration.ORIENTATION_LANDSCAPE
import android.content.res.Configuration.ORIENTATION_PORTRAIT
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ProgressBar
import kotlinx.android.synthetic.main.activity_editor.*
import uzh.scenere.R
import uzh.scenere.activities.EditorActivity.EditorState.*
import uzh.scenere.const.Constants
import uzh.scenere.const.Constants.Companion.ARROW_RIGHT
import uzh.scenere.const.Constants.Companion.ATTRIBUTE_TOKEN
import uzh.scenere.const.Constants.Companion.BOLD_END
import uzh.scenere.const.Constants.Companion.BOLD_START
import uzh.scenere.const.Constants.Companion.BREAK
import uzh.scenere.const.Constants.Companion.COMMA
import uzh.scenere.const.Constants.Companion.COORDINATES_PATTERN
import uzh.scenere.const.Constants.Companion.NEW_LINE
import uzh.scenere.const.Constants.Companion.NOTHING
import uzh.scenere.const.Constants.Companion.OBJECT_TOKEN
import uzh.scenere.const.Constants.Companion.O_A_TOKEN
import uzh.scenere.const.Constants.Companion.S1_A_TOKEN
import uzh.scenere.const.Constants.Companion.S1_O_A_TOKEN
import uzh.scenere.const.Constants.Companion.S1_O_TOKEN
import uzh.scenere.const.Constants.Companion.S1_S2_A_TOKEN
import uzh.scenere.const.Constants.Companion.S1_S2_O_A_TOKEN
import uzh.scenere.const.Constants.Companion.S1_S2_O_TOKEN
import uzh.scenere.const.Constants.Companion.S1_S2_TOKEN
import uzh.scenere.const.Constants.Companion.SINGLE_SELECT
import uzh.scenere.const.Constants.Companion.SINGLE_SELECT_WITH_PRESET_POSITION
import uzh.scenere.const.Constants.Companion.SPACE
import uzh.scenere.const.Constants.Companion.STAKEHOLDER_1_TOKEN
import uzh.scenere.const.Constants.Companion.STAKEHOLDER_2_TOKEN
import uzh.scenere.const.Constants.Companion.STATIC_TOKEN
import uzh.scenere.const.Constants.Companion.WHAT_IF_DATA
import uzh.scenere.datamodel.*
import uzh.scenere.datamodel.steps.*
import uzh.scenere.datamodel.triggers.AbstractTrigger
import uzh.scenere.datamodel.triggers.communication.BluetoothTrigger
import uzh.scenere.datamodel.triggers.communication.GpsTrigger
import uzh.scenere.datamodel.triggers.communication.NfcTrigger
import uzh.scenere.datamodel.triggers.communication.WifiTrigger
import uzh.scenere.datamodel.triggers.direct.*
import uzh.scenere.datamodel.triggers.indirect.CallTrigger
import uzh.scenere.datamodel.triggers.indirect.SmsTrigger
import uzh.scenere.datamodel.triggers.indirect.SoundTrigger
import uzh.scenere.datamodel.triggers.indirect.TimeTrigger
import uzh.scenere.datamodel.triggers.sensor.AccelerationTrigger
import uzh.scenere.datamodel.triggers.sensor.GyroscopeTrigger
import uzh.scenere.datamodel.triggers.sensor.LightTrigger
import uzh.scenere.datamodel.triggers.sensor.MagnetometerTrigger
import uzh.scenere.datastructures.MultiValueMap
import uzh.scenere.helpers.*
import uzh.scenere.views.*
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import kotlin.reflect.KClass


class EditorActivity : AbstractManagementActivity() {

    override fun getConfiguredRootLayout(): ViewGroup? {
        return editor_root
    }

    override fun isInEditMode(): Boolean {
        return editorState == EDIT
    }

    override fun isInAddMode(): Boolean {
        return editorState == ADD
    }

    override fun isInViewMode(): Boolean {
        return (editorState == EditorState.STEP || editorState == EditorState.TRIGGER || editorState == EditorState.INIT)
    }

    override fun getConfiguredInfoString(): Int {
        return R.string.icon_explain_editor
    }

    override fun getConfiguredLayout(): Int {
        return R.layout.activity_editor
    }

    override fun resetToolbar() {
        customizeToolbarId(R.string.icon_back,null,null,R.string.icon_glossary,null)
    }

    override fun isUsingNfc(): Boolean {
        return true
    }

    private val explanationMap: HashMap<Int, Map.Entry<Int, Int>> = HashMap<Int, Map.Entry<Int, Int>>()

    enum class EditorState {
        STEP, TRIGGER, INIT, ADD, EDIT
    }

    private var editorState: EditorState = EditorState.INIT
    private val elementAttributes: Array<String> = arrayOf(NOTHING, NOTHING, NOTHING, NOTHING, NOTHING, NOTHING, NOTHING, NOTHING, NOTHING, NOTHING)
    private var creationUnitClass: KClass<out IElement>? = null
    private var editUnit: IElement? = null
    //Context
    private var activeScenario: Scenario? = null
    private var projectContext: Project? = null
    private var activePath: Path? = null
    private var activeStepIds: ArrayList<String> = ArrayList()
    private val pathList = ArrayList<Int>()
    private val pathNameList = ArrayList<String>()
    private var whatIfProposal = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        var stakeholder: Stakeholder? = null
        val progressBar = ProgressBar(applicationContext)
        getContentHolderLayout().addView(progressBar)
        executeAsyncTask({
            activeScenario = intent.getSerializableExtra(Constants.BUNDLE_SCENARIO) as Scenario?
            if (activeScenario != null) {
                projectContext = DatabaseHelper.getInstance(applicationContext).readFull(activeScenario!!.projectId, Project::class)
                activeScenario = DatabaseHelper.getInstance(applicationContext).readFull(activeScenario!!.id, Scenario::class)
            }
            if (projectContext != null && !projectContext!!.stakeholders.isNullOrEmpty()) {
                stakeholder = projectContext?.getNextStakeholder()
                activePath = activeScenario?.getPath(stakeholder!!, applicationContext, 0)
            }
        }, {
            getContentWrapperLayout().setBackgroundColor(getColorWithStyle(applicationContext, R.color.sreWhite))
            populateExplanationMap()
            execAdaptToOrientationChange()

            refreshState()

            creationButton = SwipeButton(this, stakeholder?.name
                    ?: getString(R.string.editor_no_stakeholder))
                    .setColors(getColorWithStyle(applicationContext, R.color.srePrimaryPastel), ContextCompat.getColor(applicationContext,R.color.srePrimaryDisabled))
                    .setButtonMode(SwipeButton.SwipeButtonMode.QUADRUPLE)
                    .setButtonIcons(R.string.icon_backward, R.string.icon_forward, R.string.icon_null, R.string.icon_plus, null)
                    .setButtonStates(sideEnabled(), sideEnabled(), false, false)
                    .adaptMasterLayoutParams(true)
                    .setFirstPosition()
                    .setAutoCollapse(true)
                    .updateViews(true)
            creationButton?.setExecutable(createControlExecutable())
            editor_linear_layout_control.addView(creationButton)

            getInfoTitle().text = StringHelper.styleString(getSpannedStringFromId(getConfiguredInfoString()), fontAwesome)
            resetToolbar()
            getInfoTitle().textSize = DipHelper.get(resources).dip2_5.toFloat()
            tutorialOpen = true
            visualizeActivePath()
            tutorialOpen = false
            getContentHolderLayout().setOnHierarchyChangeListener(object : ViewGroup.OnHierarchyChangeListener {
                override fun onChildViewRemoved(parent: View?, child: View?) {
                    refreshState(child)
                }

                override fun onChildViewAdded(parent: View?, child: View?) {
                    //NOP
                }

            })
            getContentHolderLayout().removeView(progressBar)
        })
        tutorialOpen = SreTutorialLayoutDialog(this@EditorActivity, screenWidth, "info_editor_stakeholder", "info_editor_element").addEndExecutable { tutorialOpen = false }.show(tutorialOpen)
    }

    private fun sideEnabled() = ObjectHelper.nvl(projectContext?.stakeholders?.size, 0) > 1

    private fun visualizeActivePath() {
        if (activePath != null) {
            getContentHolderLayout().removeAllViews()
            var element = activePath?.getStartingPoint()
            while (element != null) {
                renderElement(element)
                element = activePath?.getNextElement(element.getElementId())
            }
        }
        refreshState()
        execScroll()
    }

    private fun addAndRenderElement(element: IElement, edit: Boolean = (editorState==EDIT)) {
        activePath?.add(element)
        DatabaseHelper.getInstance(applicationContext).write(element.getElementId(), element)
        if (edit){
            updateRenderedElement(element)
            execScroll(true)
        }else{
            renderElement(element)
        }
    }

    private fun renderElement(iElement: IElement) {
        var title: String? = null
        val tutorialDrawable = ArrayList<String>()
        if (iElement is AbstractStep) {
            title = BOLD_START + iElement.readableClassName() + BOLD_END+ BREAK + iElement.title
            tutorialDrawable.add("info_what_if")
        }
        if (iElement is AbstractTrigger) title = BOLD_START + iElement.readableClassName() + BOLD_END
        val previousAvailable = getContentHolderLayout().childCount != 0
        if (previousAvailable) {
            connectPreviousToNext()
        }else{
            tutorialDrawable.add("info_element")
        }
        val (right, left) = resolveLeftRight(iElement)
        val element = Element(applicationContext, iElement, previousAvailable, left, right, false).withLabel(StringHelper.fromHtml(title))
        element.setEditExecutable { openInput(iElement) }
        element.setDeleteExecutable {
            DatabaseHelper.getInstance(applicationContext).delete(iElement.getElementId(),IElement::class)
            val prevPosition = getContentHolderLayout().indexOfChild(element)-1
            if (prevPosition >= 0){
                (getContentHolderLayout().getChildAt(prevPosition) as Element).disconnectFromNext()
            }
            getContentHolderLayout().removeView(element)
            activePath?.remove(iElement)
            if (iElement is IfElseTrigger){
                //Delete all Paths associated with this Element
                for (entry in iElement.optionLayerLink){
                    if (entry.value != activePath?.layer){ //Don't delete the current path
                        val path = activeScenario?.removePath(activePath!!.stakeholder,entry.value)
                        if (path != null){
                            DatabaseHelper.getInstance(applicationContext).delete(path.id,Path::class)
                        }
                    }
                }
                pathNameList.remove(pathNameList.last())
                renderAndNotifyPath(activePath?.layer != 0)
            }
        }
        when (iElement){
            is IfElseTrigger -> {
                element.setPathData(iElement.getOptions())
                        .setOnPathIndexSelectedExecutable(onPathSelected)
                        .setInitSelectionExecutable(onPathSelectionInit)
                        .setAddExecutable {onPathAdded(iElement)}
                        .setRemoveExecutable {onPathRemoved(iElement)}
                tutorialDrawable.add("info_if_else_config")
            }
            is NfcTrigger -> {
                tutorialDrawable.add("info_nfc_writing")
                element.setNfcExecutable { toggleNfcData(iElement,element) }
            }
            is AbstractStep -> {
                element.setWhatIfExecutable { openWhatIfCreation(iElement) }
            }
        }
        getContentHolderLayout().addView(element)
        colorizeZebraPattern()
        if (!tutorialDrawable.isEmpty()){
            tutorialOpen = SreTutorialLayoutDialog(this@EditorActivity,screenWidth,*tutorialDrawable.toTypedArray()).addEndExecutable { tutorialOpen = false }.show(tutorialOpen)
        }
    }

    private fun resolveLeftRight(iElement: IElement): Pair<Boolean, Boolean> {
        var right = false
        var left = false
        if (iElement is StakeholderInteractionTrigger) {
            var localStakeholder = 0
            var foreignStakeholder = 0
            for (s in 0 until projectContext!!.stakeholders.size) {
                if (projectContext!!.stakeholders[s] == activePath!!.stakeholder) {
                    localStakeholder = s
                }
                if (projectContext!!.stakeholders[s].id == iElement.interactedStakeholderId) {
                    foreignStakeholder = s
                }
            }
            right = (localStakeholder < foreignStakeholder)
            left = (localStakeholder > foreignStakeholder)
        }
        return Pair(right, left)
    }

    private fun onPathAdded(iElement: IElement?) {
        if (iElement != null){
            openInput(iElement,InputMode.ADD)
        }
    }

    private fun onPathRemoved(iElement: IElement?) {
        if (iElement != null){
            openInput(iElement,InputMode.REMOVE)
        }
    }

    private fun openWhatIfCreation(iElement: IElement?) {
        if (iElement != null){
            openInput(iElement,InputMode.WHAT_IF)
        }
    }

    private var activeNfcTriggerElement: Element? = null
    private fun toggleNfcData(nfcTrigger: NfcTrigger, element: Element) {
        val enabledPrior = element.nfcDataLoaded
        var enabled = true
        for (view in 0 until getContentHolderLayout().childCount){
            val child = getContentHolderLayout().getChildAt(view)
            if (child is Element){
                child.setNfcLoaded(false)
                enabled = child.checkNfcEnabled()
            }
        }
        if (enabled){
            if (!enabledPrior){
                setDataToWrite(nfcTrigger.id)
                element.setNfcLoaded(true)
            }
            activeNfcTriggerElement = element
            notify(getString(R.string.editor_nfc_data), if (element.nfcDataLoaded) getString(R.string.editor_nfc_loaded) else getString(R.string.editor_nfc_cleared))
        } else {
            notify(getString(R.string.editor_nfc_data), getString(R.string.editor_nfc_disabled))
        }
    }

    override fun onDataWriteExecuted(returnValues: Pair<Boolean, String>){
        super.onDataWriteExecuted(returnValues)
        activeNfcTriggerElement?.setNfcLoaded(false)
    }

    private val onPathSelectionInit: (String) -> Unit = {
        if (activePath != null){
            val layer = activePath!!.layer
            if (!pathNameList.contains(StringHelper.concatWithIdBrackets(it,layer))){
                pathNameList.add(StringHelper.concatWithIdBrackets(it,layer))
            }
            renderAndNotifyPath(false)
        }
    }

    private val onPathSelected: (Int,Any?) -> Unit = { index: Int, data: Any? ->
        if (activePath != null && data is IfElseTrigger){
            val layer = data.getLayerForOption(index)
            if (layer != 0){
                pathNameList.remove(pathNameList.last())
            }
            pathList.add(activePath!!.layer)
            activePath = activeScenario?.getPath(activePath!!.stakeholder,this, layer)
            visualizeActivePath()
            creationButton?.setButtonIcons(R.string.icon_backward, R.string.icon_forward, if (layer != 0) R.string.icon_undo else R.string.icon_null, R.string.icon_plus, null)
                    ?.setButtonStates(sideEnabled(), sideEnabled(), (layer != 0), false)
                    ?.updateViews(true)
            val pathName = data.pathOptions[layer]
            if (pathName != null && !pathNameList.contains(StringHelper.concatWithIdBrackets(pathName,layer))){
                pathNameList.add(StringHelper.concatWithIdBrackets(pathName,layer))
                renderAndNotifyPath(true)
            }
            if (layer != 0){
                tutorialOpen = SreTutorialLayoutDialog(this@EditorActivity,screenWidth,"info_path_switch").addEndExecutable { tutorialOpen = false }.show(tutorialOpen)
            }
        }
    }

    private var callCount = 0
    private var ifElseCount = -1
    private fun renderAndNotifyPath(alternativePath: Boolean, init: Boolean = false) {
        if (pathNameList.isEmpty()){
            notify(getString(R.string.editor_main_path))
            creationButton?.setText(activePath!!.stakeholder.name)?.updateViews(true)
        }else{
            if (ifElseCount == -1){
                ifElseCount = activePath!!.countIfElse(alternativePath)
            }
            callCount++
            val pathName = StringHelper.concatListWithoutIdBrackets(ARROW_RIGHT, pathNameList)
            if (callCount == ifElseCount) {
                notify(if (alternativePath) getString(R.string.editor_alternative_path) else getString(R.string.editor_main_path),pathName)
                callCount = 0
                ifElseCount = -1
            }
            creationButton?.setText(activePath!!.stakeholder.name + " [$pathName]")?.updateViews(true)
        }
    }

    private fun updateRenderedElement(iElement: IElement) {
        var title: String? = null
        if (iElement is AbstractStep) title = BOLD_START + iElement.readableClassName() + BOLD_END+BREAK + iElement.title
        if (iElement is AbstractTrigger) title = BOLD_START + iElement.readableClassName() + BOLD_END
        for (v in 0 until getContentHolderLayout().childCount){
            if (getContentHolderLayout().getChildAt(v) is Element && (getContentHolderLayout().getChildAt(v) as Element).containsElement(iElement)){
                (getContentHolderLayout().getChildAt(v) as Element).withLabel(StringHelper.fromHtml(title)).updateElement(iElement).setEditExecutable { openInput(iElement) }
                when (iElement){
                    is IfElseTrigger -> {
                        (getContentHolderLayout().getChildAt(v) as Element).setPathData(iElement.getOptions())
                                .resetSelectCount()
                                .setOnPathIndexSelectedExecutable(onPathSelected)
                                .setAddExecutable {onPathAdded(iElement)}
                                .setRemoveExecutable {onPathRemoved(iElement)}
                    }
                    is StakeholderInteractionTrigger -> {
                        val (right, left) = resolveLeftRight(iElement)
                        (getContentHolderLayout().getChildAt(v) as Element).updateElement(iElement)
                                .updateRight(right)
                                .updateLeft(left)
                                .updateInteraction(iElement.interactedStakeholderId!!)
                    }
                    is AbstractStep -> {
                        (getContentHolderLayout().getChildAt(v) as Element).setWhatIfExecutable { openWhatIfCreation(iElement) }
                    }
                }
            }
        }
    }

    private fun refreshState(view: View? = null) {
        if (view != null && view is Element){
            editorState = if ((view as Element).isStep()) EditorState.STEP else EditorState.TRIGGER
        }else if (getContentHolderLayout().childCount > 0){
            for (v in 0 until getContentHolderLayout().childCount) {
                if (getContentHolderLayout().getChildAt(v) is Element) {
                    editorState = if ((getContentHolderLayout().getChildAt(v) as Element).isStep()) EditorState.TRIGGER else EditorState.STEP
                }
            }
        }else{
            editorState = STEP
        }
        when (editorState) {
            EditorState.INIT -> {
                updateSpinner(R.array.spinner_steps)
                editorState = EditorState.STEP
            }
            EditorState.STEP -> updateSpinner(R.array.spinner_steps)
            EditorState.TRIGGER -> updateSpinner(R.array.spinner_triggers)
            else -> return
        }
    }

    private fun populateExplanationMap() {
        explanationMap[R.array.spinner_steps] = AbstractMap.SimpleEntry(R.string.explanation_steps_title, R.string.explanation_steps_content)
        explanationMap[R.string.step_standard] = AbstractMap.SimpleEntry(R.string.step_standard, R.string.explanation_step_standard)
    }

    private fun createControlExecutable(): SwipeButton.SwipeButtonExecution {
        return object : SwipeButton.SwipeButtonExecution {
            override fun execLeft() {
                val stakeholder: Stakeholder? = projectContext?.getPreviousStakeholder(activePath?.stakeholder)
                if (stakeholder != null) {
                    activePath = activeScenario?.getPath(stakeholder, applicationContext, 0)
                    pathList.clear()
                    pathNameList.clear()
                    creationButton?.setText(stakeholder.name)
                            ?.setButtonIcons(R.string.icon_backward, R.string.icon_forward, R.string.icon_null, R.string.icon_plus, null)
                            ?.setButtonStates(sideEnabled(), sideEnabled(), false, false)
                            ?.updateViews(true)
                    visualizeActivePath()
                }
            }

            override fun execRight() {
                val stakeholder: Stakeholder? = projectContext?.getNextStakeholder(activePath?.stakeholder)
                if (stakeholder != null) {
                    activePath = activeScenario?.getPath(stakeholder, applicationContext, 0)
                    pathList.clear()
                    pathNameList.clear()
                    creationButton?.setText(stakeholder.name)
                            ?.setButtonIcons(R.string.icon_backward, R.string.icon_forward, R.string.icon_null, R.string.icon_plus, null)
                            ?.setButtonStates(sideEnabled(), sideEnabled(), false, false)
                            ?.updateViews(true)
                    visualizeActivePath()
                }
            }

            override fun execDown() {
                openInput()
            }

            override fun execUp() {
                if (activePath?.layer != 0){
                    val lastPathLayer = pathList.last()
                    pathList.remove(lastPathLayer)
                    activePath = activeScenario!!.getPath(activePath!!.stakeholder, applicationContext, lastPathLayer)
                    visualizeActivePath()
                    pathNameList.remove(pathNameList.last())
                }
                if (activePath?.layer == 0 && !pathNameList.isEmpty()){
                    pathNameList.remove(pathNameList.last())
                }
            }
        }
    }


    enum class InputMode{
        UNSPECIFIED, ADD, REMOVE, WHAT_IF
    }
    private fun openInput(element: IElement? = null, inputMode: InputMode = InputMode.UNSPECIFIED) {
        getInfoTitle().textSize = DipHelper.get(resources).dip3_5.toFloat()
        editorState = if (element == null) ADD else EDIT
        editor_spinner_selection?.visibility = View.GONE
        creationButton?.visibility = View.GONE
        //All Resources
        val allResources = activeScenario!!.getAllResources()
        val allUsedResources = activeScenario!!.getAllUsedResources()
        //All Steps
        val (stepTitles, stepIds) = activeScenario!!.getAllStepTitlesAndIds(activePath!!.stakeholder)
        activeStepIds = stepIds
        if (element != null) {//LOAD
            cleanInfoHolder("Edit " + element.readableClassName())
            editUnit = element
            when (element) {
                //STEPS
                is StandardStep -> {
                    creationUnitClass = StandardStep::class
                    if (inputMode == InputMode.WHAT_IF){
                        cleanInfoHolder("Edit " + getString(R.string.literal_what_ifs))
                        val pointer = adaptAttributes(getString(R.string.literal_what_if))
                        getInfoContentWrap().addView(createLine(elementAttributes[pointer], LineInputType.MULTI_TEXT, null, false, -1, createWhatIfs(element)))
                        (uncheckedMap[elementAttributes[pointer]] as SreMultiAutoCompleteTextView).setObjects(collectContextObjects(true))
                    }else{
                        var pointer = adaptAttributes(*resources.getStringArray(R.array.editor_attributes_step_standard))
                        getInfoContentWrap().addView(createLine(elementAttributes[pointer++], LineInputType.SINGLE_LINE_EDIT, element.title, false, -1))
                        getInfoContentWrap().addView(createLine(elementAttributes[pointer], LineInputType.MULTI_LINE_CONTEXT_EDIT, element.text, false, -1))
                        (inputMap[elementAttributes[pointer]] as SreMultiAutoCompleteTextView).setObjects(collectContextObjects())
                    }
                    execMorphInfoBar(InfoState.MAXIMIZED)
                }
                is JumpStep -> {
                    creationUnitClass = JumpStep::class
                    if (inputMode == InputMode.WHAT_IF){
                        val pointer = adaptAttributes(getString(R.string.literal_what_if))
                        cleanInfoHolder("Edit " + getString(R.string.literal_what_ifs))
                        getInfoContentWrap().addView(createLine(elementAttributes[pointer], LineInputType.MULTI_TEXT, null, false, -1, createWhatIfs(element)))
                        (uncheckedMap[elementAttributes[pointer]] as SreMultiAutoCompleteTextView).setObjects(collectContextObjects(true))
                    }else{
                        val stepId = activeStepIds.indexOf(element.targetStepId)+1
                        var pointer = adaptAttributes(*resources.getStringArray(R.array.editor_attributes_step_jump))
                        getInfoContentWrap().addView(createLine(elementAttributes[pointer++], LineInputType.SINGLE_LINE_EDIT, element.title, false, -1))
                        getInfoContentWrap().addView(createLine(elementAttributes[pointer], LineInputType.MULTI_LINE_CONTEXT_EDIT, element.text, false, -1))
                        (inputMap[elementAttributes[pointer++]] as SreMultiAutoCompleteTextView).setObjects(collectContextObjects())
                        getInfoContentWrap().addView(createLine(elementAttributes[pointer], LineInputType.LOOKUP, if (stepId > 0) SINGLE_SELECT_WITH_PRESET_POSITION.plus(stepId) else null, false, -1,addToArrayBefore(stepTitles.toTypedArray(),NOTHING)))
                    }
                    execMorphInfoBar(InfoState.MAXIMIZED)
                }
                is SoundStep -> {
                    creationUnitClass = SoundStep::class
                    if (inputMode == InputMode.WHAT_IF){
                        val pointer = adaptAttributes(getString(R.string.literal_what_if))
                        cleanInfoHolder("Edit " + getString(R.string.literal_what_ifs))
                        getInfoContentWrap().addView(createLine(elementAttributes[pointer], LineInputType.MULTI_TEXT, null, false, -1, createWhatIfs(element)))
                        (uncheckedMap[elementAttributes[pointer]] as SreMultiAutoCompleteTextView).setObjects(collectContextObjects(true))
                    }else{
                        var pointer = adaptAttributes(*resources.getStringArray(R.array.editor_attributes_step_sound))
                        getInfoContentWrap().addView(createLine(elementAttributes[pointer++], LineInputType.SINGLE_LINE_EDIT, element.title, false, -1))
                        getInfoContentWrap().addView(createLine(elementAttributes[pointer], LineInputType.MULTI_LINE_CONTEXT_EDIT, element.text, false, -1))
                        (inputMap[elementAttributes[pointer]] as SreMultiAutoCompleteTextView).setObjects(collectContextObjects())
                    }
                    execMorphInfoBar(InfoState.MAXIMIZED)
                }
                is VibrationStep -> {
                    creationUnitClass = VibrationStep::class
                    if (inputMode == InputMode.WHAT_IF){
                        val pointer = adaptAttributes(getString(R.string.literal_what_if))
                        cleanInfoHolder("Edit " + getString(R.string.literal_what_ifs))
                        getInfoContentWrap().addView(createLine(elementAttributes[pointer], LineInputType.MULTI_TEXT, null, false, -1, createWhatIfs(element)))
                        (uncheckedMap[elementAttributes[pointer]] as SreMultiAutoCompleteTextView).setObjects(collectContextObjects(true))
                    }else{
                        var pointer = adaptAttributes(*resources.getStringArray(R.array.editor_attributes_step_vibration))
                        getInfoContentWrap().addView(createLine(elementAttributes[pointer++], LineInputType.SINGLE_LINE_EDIT, element.title, false, -1))
                        getInfoContentWrap().addView(createLine(elementAttributes[pointer], LineInputType.MULTI_LINE_CONTEXT_EDIT, element.text, false, -1))
                        (inputMap[elementAttributes[pointer]] as SreMultiAutoCompleteTextView).setObjects(collectContextObjects())
                    }
                    execMorphInfoBar(InfoState.MAXIMIZED)
                }
                is ResourceStep -> {
                    if (allResources.size == 0){
                        notify(getString(R.string.editor_no_resources_title),getString(R.string.editor_no_resource_step_text))
                    }
                    creationUnitClass = ResourceStep::class
                    if (inputMode == InputMode.WHAT_IF){
                        val pointer = adaptAttributes(getString(R.string.literal_what_if))
                        cleanInfoHolder("Edit " + getString(R.string.literal_what_ifs))
                        getInfoContentWrap().addView(createLine(elementAttributes[pointer], LineInputType.MULTI_TEXT, null, false, -1, createWhatIfs(element)))
                        (uncheckedMap[elementAttributes[pointer]] as SreMultiAutoCompleteTextView).setObjects(collectContextObjects(true))
                    }else {
                        var index = adaptAttributes(*resources.getStringArray(R.array.editor_attributes_step_resource))
                        val position = allResources.indexOf(element.resource)+1
                        getInfoContentWrap().addView(createLine(elementAttributes[index++], LineInputType.SINGLE_LINE_EDIT, element.title, false, -1))
                        getInfoContentWrap().addView(createLine(elementAttributes[index++], LineInputType.LOOKUP, SINGLE_SELECT_WITH_PRESET_POSITION.plus(position), false, -1,addToArrayBefore(allResources.toStringArray(),NOTHING)))
                        getInfoContentWrap().addView(createLine(elementAttributes[index++], LineInputType.NUMBER_SIGNED_EDIT, element.change.toString(), true, 9))
                        getInfoContentWrap().addView(createLine(elementAttributes[index], LineInputType.MULTI_LINE_CONTEXT_EDIT, element.text, false, -1))
                        (inputMap[elementAttributes[index]] as SreMultiAutoCompleteTextView).setObjects(collectContextObjects())
                    }
                    execMorphInfoBar(InfoState.MAXIMIZED)
                }
                //TRIGGERS
                is ButtonTrigger -> {
                    creationUnitClass = ButtonTrigger::class
                    adaptAttributes(*resources.getStringArray(R.array.editor_attributes_trigger_button))
                    getInfoContentWrap().addView(createLine(elementAttributes[0], LineInputType.SINGLE_LINE_EDIT, element.buttonLabel, false, -1))
                    execMorphInfoBar(InfoState.MAXIMIZED)
                }
                is IfElseTrigger -> {
                    creationUnitClass = IfElseTrigger::class
                    var tutorialDrawable: String? = null
                    if (inputMode == InputMode.REMOVE){
                        val index = adaptAttributes(*resources.getStringArray(R.array.editor_attributes_trigger_if_else_removal))
                        getInfoContentWrap().addView(createLine(elementAttributes[index], LineInputType.LOOKUP, null, false, -1, addToArrayBefore(element.getDeletableIndexedOptions(),"")))
                        tutorialDrawable = "info_option_removal"
                    }else {
                        if (pathNameList.isNotEmpty()){
                            pathNameList.remove(pathNameList.last())
                        }
                        var index = adaptAttributes(*resources.getStringArray(R.array.editor_attributes_trigger_if_else))
                        getInfoContentWrap().addView(createLine(elementAttributes[index++], LineInputType.SINGLE_LINE_EDIT, element.text, false, -1))
                        for (string in element.getOptions()){
                            getInfoContentWrap().addView(createLine(elementAttributes[index++], LineInputType.SINGLE_LINE_EDIT, string, false, -1))
                        }
                        if (inputMode == InputMode.ADD){
                            getInfoContentWrap().addView(createLine(elementAttributes[index], LineInputType.SINGLE_LINE_EDIT, null, false, -1))
                            tutorialDrawable = "info_option_add"
                        }
                    }
                    execMorphInfoBar(InfoState.MAXIMIZED)
                    if (tutorialDrawable != null){
                        tutorialOpen = SreTutorialLayoutDialog(this@EditorActivity,screenWidth, tutorialDrawable).addEndExecutable { tutorialOpen = false }.show(tutorialOpen)
                    }
                }
                is StakeholderInteractionTrigger -> {
                    creationUnitClass = StakeholderInteractionTrigger::class
                    val stakeholderPositionById = projectContext!!.getStakeholderPositionById(element.interactedStakeholderId!!,activePath!!.stakeholder!!)+1
                    var index = adaptAttributes(*resources.getStringArray(R.array.editor_attributes_trigger_stakeholder_interaction))
                    getInfoContentWrap().addView(createLine(elementAttributes[index++], LineInputType.SINGLE_LINE_EDIT, element.text, false, -1))
                    getInfoContentWrap().addView(createLine(elementAttributes[index], LineInputType.LOOKUP, SINGLE_SELECT_WITH_PRESET_POSITION.plus(stakeholderPositionById), false, -1, addToArrayBefore(projectContext!!.getStakeholdersExcept(activePath!!.stakeholder).toStringArray(),NOTHING)))
                    execMorphInfoBar(InfoState.MAXIMIZED)
                }
                is InputTrigger -> {
                    creationUnitClass = InputTrigger::class
                    var index = adaptAttributes(*resources.getStringArray(R.array.editor_attributes_trigger_input))
                    getInfoContentWrap().addView(createLine(elementAttributes[index++], LineInputType.SINGLE_LINE_EDIT, element.text, false, -1))
                    getInfoContentWrap().addView(createLine(elementAttributes[index], LineInputType.SINGLE_LINE_EDIT, element.input, false, -1))
                    execMorphInfoBar(InfoState.MAXIMIZED)
                }
                is ResourceCheckTrigger -> {
                    if (allUsedResources.size == 0){
                        notify(getString(R.string.editor_no_resources_title),getString(R.string.editor_no_resource_trigger_text))
                    }
                    creationUnitClass = ResourceCheckTrigger::class
                    var index = adaptAttributes(*resources.getStringArray(R.array.editor_attributes_trigger_resource))
                    val resPos = allResources.indexOf(element.resource)+1
                    val stepPos = activeStepIds.indexOf(element.falseStepId)+1
                    getInfoContentWrap().addView(createLine(elementAttributes[index++], LineInputType.SINGLE_LINE_EDIT, element.buttonLabel, false, -1))
                    getInfoContentWrap().addView(createLine(elementAttributes[index++], LineInputType.LOOKUP, SINGLE_SELECT_WITH_PRESET_POSITION.plus(resPos), false, -1, addToArrayBefore(allResources.toStringArray(),NOTHING)))
                    getInfoContentWrap().addView(createLine(elementAttributes[index++], LineInputType.LOOKUP, SINGLE_SELECT_WITH_PRESET_POSITION.plus(element.mode.pos), false, -1,addToArrayBefore(ResourceCheckTrigger.CheckMode.getModes(),NOTHING)))
                    getInfoContentWrap().addView(createLine(elementAttributes[index++], LineInputType.NUMBER_SIGNED_EDIT, element.checkValue.toString(), false, 9))
                    getInfoContentWrap().addView(createLine(elementAttributes[index], LineInputType.LOOKUP, SINGLE_SELECT_WITH_PRESET_POSITION.plus(stepPos), false, -1,addToArrayBefore(stepTitles.toTypedArray(),NOTHING)))
                    execMorphInfoBar(InfoState.MAXIMIZED)
                }
                is TimeTrigger -> {
                    creationUnitClass = TimeTrigger::class
                    var index = adaptAttributes(*resources.getStringArray(R.array.editor_attributes_trigger_time))
                    getInfoContentWrap().addView(createLine(elementAttributes[index++], LineInputType.SINGLE_LINE_EDIT, element.text, false, -1))
                    getInfoContentWrap().addView(createLine(elementAttributes[index], LineInputType.NUMBER_EDIT, element.getTimeSecond().toString(), false, -1))
                    execMorphInfoBar(InfoState.MAXIMIZED)
                }
                is SoundTrigger -> {
                    creationUnitClass = SoundTrigger::class
                    var index = adaptAttributes(*resources.getStringArray(R.array.editor_attributes_trigger_sound))
                    getInfoContentWrap().addView(createLine(elementAttributes[index++], LineInputType.SINGLE_LINE_EDIT, element.text, false, -1))
                    getInfoContentWrap().addView(createLine(elementAttributes[index], LineInputType.NUMBER_SIGNED_EDIT, NumberHelper.nvl(element.dB,0).toString(), false, -1))
                    execMorphInfoBar(InfoState.MAXIMIZED)
                }
                is BluetoothTrigger -> {
                    creationUnitClass = BluetoothTrigger::class
                    var index = adaptAttributes(*resources.getStringArray(R.array.editor_attributes_trigger_bt))
                    getInfoContentWrap().addView(createLine(elementAttributes[index++], LineInputType.SINGLE_LINE_EDIT, element.text, false, -1))
                    getInfoContentWrap().addView(createLine(elementAttributes[index], LineInputType.SINGLE_LINE_EDIT, element.deviceId, false, -1))
                    execMorphInfoBar(InfoState.MAXIMIZED)
                }
                is GpsTrigger -> {
                    creationUnitClass = GpsTrigger::class
                    var index = adaptAttributes(*resources.getStringArray(R.array.editor_attributes_trigger_gps))
                    getInfoContentWrap().addView(createLine(elementAttributes[index++], LineInputType.SINGLE_LINE_EDIT, element.text, false, -1))
                    getInfoContentWrap().addView(createLine(elementAttributes[index++], LineInputType.NUMBER_EDIT, element.getRadius().toString(), false, -1))
                    getInfoContentWrap().addView(createLine(elementAttributes[index++], LineInputType.SINGLE_LINE_EDIT, element.getLatitudeLongitude(), false, -1))
                    getInfoContentWrap().addView(createLine(elementAttributes[index], LineInputType.BUTTON, null, false, -1,{startActivity(Intent(this, PickerActivity::class.java))}))
                    execMorphInfoBar(InfoState.MAXIMIZED)
                }
                is NfcTrigger -> {
                    creationUnitClass = NfcTrigger::class
                    var index = adaptAttributes(*resources.getStringArray(R.array.editor_attributes_trigger_nfc))
                    getInfoContentWrap().addView(createLine(elementAttributes[index++], LineInputType.SINGLE_LINE_EDIT, element.text, false, -1))
                    getInfoContentWrap().addView(createLine(elementAttributes[index], LineInputType.MULTI_LINE_EDIT, element.message, true, -1))
                    execMorphInfoBar(InfoState.MAXIMIZED)
                }
                is WifiTrigger -> {
                    creationUnitClass = WifiTrigger::class
                    var index = adaptAttributes(*resources.getStringArray(R.array.editor_attributes_trigger_wifi))
                    getInfoContentWrap().addView(createLine(elementAttributes[index++], LineInputType.SINGLE_LINE_EDIT, element.text, false, -1))
                    getInfoContentWrap().addView(createLine(elementAttributes[index++], LineInputType.SINGLE_LINE_EDIT, element.getSsid(), false, -1))
                    val array = addToArrayBefore(CommunicationHelper.Companion.WiFiStrength.getStringValues().toTypedArray(),NOTHING)
                    var position = 0
                    val strength = element.getStrength()
                    for (p in array.indices){
                        if (array[p] == strength){
                            position = p
                        }
                    }
                    getInfoContentWrap().addView(createLine(elementAttributes[index], LineInputType.LOOKUP, SINGLE_SELECT_WITH_PRESET_POSITION.plus(position), false, -1, array))
                    execMorphInfoBar(InfoState.MAXIMIZED)
                }
                is CallTrigger -> {
                    creationUnitClass = CallTrigger::class
                    var index = adaptAttributes(*resources.getStringArray(R.array.editor_attributes_trigger_call))
                    getInfoContentWrap().addView(createLine(elementAttributes[index++], LineInputType.SINGLE_LINE_EDIT, element.text, false, -1))
                    getInfoContentWrap().addView(createLine(elementAttributes[index], LineInputType.SINGLE_LINE_EDIT, element.telephoneNr, false, -1))
                    execMorphInfoBar(InfoState.MAXIMIZED)
                }
                is SmsTrigger -> {
                    creationUnitClass = SmsTrigger::class
                    var index = adaptAttributes(*resources.getStringArray(R.array.editor_attributes_trigger_sms))
                    getInfoContentWrap().addView(createLine(elementAttributes[index++], LineInputType.SINGLE_LINE_EDIT, element.text, false, -1))
                    getInfoContentWrap().addView(createLine(elementAttributes[index], LineInputType.SINGLE_LINE_EDIT, element.telephoneNr, false, -1))
                    execMorphInfoBar(InfoState.MAXIMIZED)
                }
                is AccelerationTrigger -> {/*TODO*/}
                is GyroscopeTrigger -> {/*TODO*/}
                is LightTrigger -> {/*TODO*/}
                is MagnetometerTrigger -> {/*TODO*/}
            }
        } else {//CREATE
            cleanInfoHolder("Add " + editor_spinner_selection.selectedItem)
            when ((editor_spinner_selection.selectedItem as String)) {
                //STEP
                resources.getString(R.string.step_standard) -> {
                    creationUnitClass = StandardStep::class
                    var index = adaptAttributes(*resources.getStringArray(R.array.editor_attributes_step_standard))
                    getInfoContentWrap().addView(createLine(elementAttributes[index++], LineInputType.SINGLE_LINE_EDIT, null, false, -1))
                    getInfoContentWrap().addView(createLine(elementAttributes[index], LineInputType.MULTI_LINE_CONTEXT_EDIT, null, false, -1))
                    (inputMap[elementAttributes[index]] as SreMultiAutoCompleteTextView).setObjects(collectContextObjects())
                    execMorphInfoBar(InfoState.MAXIMIZED)
                    tutorialOpen = SreTutorialLayoutDialog(this@EditorActivity,screenWidth,"info_editor_context").addEndExecutable { tutorialOpen = false }.show(tutorialOpen)
                }
                resources.getString(R.string.step_jump) -> {
                    if (activeStepIds.size == 0){
                        notify(getString(R.string.editor_no_steps_title), getString(R.string.editor_no_steps_text))
                    }
                    creationUnitClass = JumpStep::class
                    var index = adaptAttributes(*resources.getStringArray(R.array.editor_attributes_step_jump))
                    getInfoContentWrap().addView(createLine(elementAttributes[index++], LineInputType.SINGLE_LINE_EDIT, null, false, -1))
                    getInfoContentWrap().addView(createLine(elementAttributes[index], LineInputType.MULTI_LINE_CONTEXT_EDIT, null, false, -1))
                    (inputMap[elementAttributes[index++]] as SreMultiAutoCompleteTextView).setObjects(collectContextObjects())
                    getInfoContentWrap().addView(createLine(elementAttributes[index], LineInputType.LOOKUP, SINGLE_SELECT, false, -1,addToArrayBefore(stepTitles.toTypedArray(),NOTHING)))
                    execMorphInfoBar(InfoState.MAXIMIZED)
                    tutorialOpen = SreTutorialLayoutDialog(this@EditorActivity,screenWidth,"info_editor_context").addEndExecutable { tutorialOpen = false }.show(tutorialOpen)
                }
                resources.getString(R.string.step_sound) -> {
                    creationUnitClass = SoundStep::class
                    var index = adaptAttributes(*resources.getStringArray(R.array.editor_attributes_step_sound))
                    getInfoContentWrap().addView(createLine(elementAttributes[index++], LineInputType.SINGLE_LINE_EDIT, null, false, -1))
                    getInfoContentWrap().addView(createLine(elementAttributes[index], LineInputType.MULTI_LINE_CONTEXT_EDIT, null, false, -1))
                    (inputMap[elementAttributes[index]] as SreMultiAutoCompleteTextView).setObjects(collectContextObjects())
                    execMorphInfoBar(InfoState.MAXIMIZED)
                    tutorialOpen = SreTutorialLayoutDialog(this@EditorActivity,screenWidth,"info_editor_context").addEndExecutable { tutorialOpen = false }.show(tutorialOpen)
                }
                resources.getString(R.string.step_vibration) -> {
                    creationUnitClass = VibrationStep::class
                    var index = adaptAttributes(*resources.getStringArray(R.array.editor_attributes_step_vibration))
                    getInfoContentWrap().addView(createLine(elementAttributes[index++], LineInputType.SINGLE_LINE_EDIT, null, false, -1))
                    getInfoContentWrap().addView(createLine(elementAttributes[index], LineInputType.MULTI_LINE_CONTEXT_EDIT, null, false, -1))
                    (inputMap[elementAttributes[index]] as SreMultiAutoCompleteTextView).setObjects(collectContextObjects())
                    execMorphInfoBar(InfoState.MAXIMIZED)
                    tutorialOpen = SreTutorialLayoutDialog(this@EditorActivity,screenWidth,"info_editor_context").addEndExecutable { tutorialOpen = false }.show(tutorialOpen)
                }
                resources.getString(R.string.step_resource) -> {
                    if (allResources.size == 0){
                        notify(getString(R.string.editor_no_resources_title),getString(R.string.editor_no_resource_step_text))
                    }
                    creationUnitClass = ResourceStep::class
                    var index = adaptAttributes(*resources.getStringArray(R.array.editor_attributes_step_resource))
                    getInfoContentWrap().addView(createLine(elementAttributes[index++], LineInputType.SINGLE_LINE_EDIT, null, false, -1))
                    getInfoContentWrap().addView(createLine(elementAttributes[index++], LineInputType.LOOKUP, SINGLE_SELECT, false, -1,addToArrayBefore(allResources.toStringArray(),NOTHING)))
                    getInfoContentWrap().addView(createLine(elementAttributes[index++], LineInputType.NUMBER_SIGNED_EDIT, null, true, 9))
                    getInfoContentWrap().addView(createLine(elementAttributes[index], LineInputType.MULTI_LINE_CONTEXT_EDIT, null, false, -1))
                    (inputMap[elementAttributes[index]] as SreMultiAutoCompleteTextView).setObjects(collectContextObjects())
                    execMorphInfoBar(InfoState.MAXIMIZED)
                    tutorialOpen = SreTutorialLayoutDialog(this@EditorActivity,screenWidth,"info_editor_context").addEndExecutable { tutorialOpen = false }.show(tutorialOpen)
                }
                //TRIGGER
                resources.getString(R.string.trigger_button) -> {
                    creationUnitClass = ButtonTrigger::class
                    val index = adaptAttributes(*resources.getStringArray(R.array.editor_attributes_trigger_button))
                    getInfoContentWrap().addView(createLine(elementAttributes[index], LineInputType.SINGLE_LINE_EDIT, null, false, -1))
                    execMorphInfoBar(InfoState.MAXIMIZED)
                }
                resources.getString(R.string.trigger_if_else) -> {
                    creationUnitClass = IfElseTrigger::class
                    var index = adaptAttributes(*resources.getStringArray(R.array.editor_attributes_trigger_if_else_init))
                    getInfoContentWrap().addView(createLine(elementAttributes[index++], LineInputType.SINGLE_LINE_EDIT, null, false, -1))
                    getInfoContentWrap().addView(createLine(elementAttributes[index], LineInputType.SINGLE_LINE_EDIT, null, false, -1))
                    execMorphInfoBar(InfoState.MAXIMIZED)
                    tutorialOpen = SreTutorialLayoutDialog(this@EditorActivity,screenWidth,"info_if_else_element").addEndExecutable { tutorialOpen = false }.show(tutorialOpen)
                }
                resources.getString(R.string.trigger_stakeholder_interaction) -> {
                    creationUnitClass = StakeholderInteractionTrigger::class
                    var index = adaptAttributes(*resources.getStringArray(R.array.editor_attributes_trigger_stakeholder_interaction))
                    getInfoContentWrap().addView(createLine(elementAttributes[index++], LineInputType.SINGLE_LINE_EDIT, null, false, -1))
                    getInfoContentWrap().addView(createLine(elementAttributes[index], LineInputType.LOOKUP, SINGLE_SELECT, false, -1, addToArrayBefore(projectContext!!.getStakeholdersExcept(activePath!!.stakeholder).toStringArray(),NOTHING)))
                    execMorphInfoBar(InfoState.MAXIMIZED)
                }
                resources.getString(R.string.trigger_input) -> {
                    creationUnitClass = InputTrigger::class
                    var index = adaptAttributes(*resources.getStringArray(R.array.editor_attributes_trigger_input))
                    getInfoContentWrap().addView(createLine(elementAttributes[index++], LineInputType.SINGLE_LINE_EDIT, null, false, -1))
                    getInfoContentWrap().addView(createLine(elementAttributes[index], LineInputType.SINGLE_LINE_EDIT, null, false, -1))
                    execMorphInfoBar(InfoState.MAXIMIZED)
                }
                resources.getString(R.string.trigger_resource) -> {
                    if (allUsedResources.size == 0){
                        notify(getString(R.string.editor_no_resources_title),getString(R.string.editor_no_resource_trigger_text))
                    }
                    creationUnitClass = ResourceCheckTrigger::class
                    var index = adaptAttributes(*resources.getStringArray(R.array.editor_attributes_trigger_resource))
                    getInfoContentWrap().addView(createLine(elementAttributes[index++], LineInputType.SINGLE_LINE_EDIT, null, false, -1))
                    getInfoContentWrap().addView(createLine(elementAttributes[index++], LineInputType.LOOKUP, SINGLE_SELECT, false, -1, addToArrayBefore(allResources.toStringArray(),NOTHING)))
                    getInfoContentWrap().addView(createLine(elementAttributes[index++], LineInputType.LOOKUP, SINGLE_SELECT, false, -1,addToArrayBefore(ResourceCheckTrigger.CheckMode.getModes(),NOTHING)))
                    getInfoContentWrap().addView(createLine(elementAttributes[index++], LineInputType.NUMBER_SIGNED_EDIT, null, false, 9))
                    getInfoContentWrap().addView(createLine(elementAttributes[index], LineInputType.LOOKUP, SINGLE_SELECT, false, -1,addToArrayBefore(stepTitles.toTypedArray(),NOTHING)))
                    execMorphInfoBar(InfoState.MAXIMIZED)
                }
                resources.getString(R.string.trigger_timer) -> {
                    creationUnitClass = TimeTrigger::class
                    var index = adaptAttributes(*resources.getStringArray(R.array.editor_attributes_trigger_time))
                    getInfoContentWrap().addView(createLine(elementAttributes[index++], LineInputType.SINGLE_LINE_EDIT, null, false, -1))
                    getInfoContentWrap().addView(createLine(elementAttributes[index], LineInputType.NUMBER_EDIT, null, false, -1))
                    execMorphInfoBar(InfoState.MAXIMIZED)
                }
                resources.getString(R.string.trigger_bt) -> {
                    creationUnitClass = BluetoothTrigger::class
                    var index = adaptAttributes(*resources.getStringArray(R.array.editor_attributes_trigger_bt))
                    getInfoContentWrap().addView(createLine(elementAttributes[index++], LineInputType.SINGLE_LINE_EDIT, null, false, -1))
                    getInfoContentWrap().addView(createLine(elementAttributes[index], LineInputType.SINGLE_LINE_EDIT, null, false, -1))
                    execMorphInfoBar(InfoState.MAXIMIZED)
                }
                resources.getString(R.string.trigger_gps) -> {
                    creationUnitClass = GpsTrigger::class
                    var index = adaptAttributes(*resources.getStringArray(R.array.editor_attributes_trigger_gps))
                    getInfoContentWrap().addView(createLine(elementAttributes[index++], LineInputType.SINGLE_LINE_EDIT, null, false, -1))
                    getInfoContentWrap().addView(createLine(elementAttributes[index++], LineInputType.NUMBER_EDIT, null, false, -1))
                    getInfoContentWrap().addView(createLine(elementAttributes[index++], LineInputType.SINGLE_LINE_EDIT, null, false, -1))
                    getInfoContentWrap().addView(createLine(elementAttributes[index], LineInputType.BUTTON, null, false, -1,{startActivity(Intent(this, PickerActivity::class.java))}))
                    execMorphInfoBar(InfoState.MAXIMIZED)
                }
                resources.getString(R.string.trigger_nfc) -> {
                    creationUnitClass = NfcTrigger::class
                    var index = adaptAttributes(*resources.getStringArray(R.array.editor_attributes_trigger_nfc))
                    getInfoContentWrap().addView(createLine(elementAttributes[index++], LineInputType.SINGLE_LINE_EDIT, null, false, -1))
                    getInfoContentWrap().addView(createLine(elementAttributes[index], LineInputType.MULTI_LINE_EDIT, null, true, 25))
                    execMorphInfoBar(InfoState.MAXIMIZED)
                }
                resources.getString(R.string.trigger_wifi) -> {
                    creationUnitClass = WifiTrigger::class
                    var index = adaptAttributes(*resources.getStringArray(R.array.editor_attributes_trigger_wifi))
                    getInfoContentWrap().addView(createLine(elementAttributes[index++], LineInputType.SINGLE_LINE_EDIT, null, false, -1))
                    getInfoContentWrap().addView(createLine(elementAttributes[index++], LineInputType.SINGLE_LINE_EDIT, null, false, -1))
                    val array = addToArrayBefore(CommunicationHelper.Companion.WiFiStrength.getStringValues().toTypedArray(),NOTHING)
                    getInfoContentWrap().addView(createLine(elementAttributes[index], LineInputType.LOOKUP, SINGLE_SELECT, false, -1, array))
                    execMorphInfoBar(InfoState.MAXIMIZED)
                }
                resources.getString(R.string.trigger_call) -> {
                    creationUnitClass = CallTrigger::class
                    var index = adaptAttributes(*resources.getStringArray(R.array.editor_attributes_trigger_call))
                    getInfoContentWrap().addView(createLine(elementAttributes[index++], LineInputType.SINGLE_LINE_EDIT, null, false, -1))
                    getInfoContentWrap().addView(createLine(elementAttributes[index], LineInputType.SINGLE_LINE_EDIT, null, false, -1))
                    execMorphInfoBar(InfoState.MAXIMIZED)
                }
                resources.getString(R.string.trigger_sms) -> {
                    creationUnitClass = SmsTrigger::class
                    var index = adaptAttributes(*resources.getStringArray(R.array.editor_attributes_trigger_sms))
                    getInfoContentWrap().addView(createLine(elementAttributes[index++], LineInputType.SINGLE_LINE_EDIT, null, false, -1))
                    getInfoContentWrap().addView(createLine(elementAttributes[index], LineInputType.SINGLE_LINE_EDIT, null, false, -1))
                    execMorphInfoBar(InfoState.MAXIMIZED)
                }
                resources.getString(R.string.trigger_sound) -> {
                    creationUnitClass = SoundTrigger::class
                    var index = adaptAttributes(*resources.getStringArray(R.array.editor_attributes_trigger_sound))
                    getInfoContentWrap().addView(createLine(elementAttributes[index++], LineInputType.SINGLE_LINE_EDIT, null, false, -1))
                    getInfoContentWrap().addView(createLine(elementAttributes[index], LineInputType.SINGLE_LINE_EDIT, null, false, -1))
                    execMorphInfoBar(InfoState.MAXIMIZED)
                }
                resources.getString(R.string.trigger_acceleration) -> {/*TODO*/ }
                resources.getString(R.string.trigger_gyroscope) -> {/*TODO*/ }
                resources.getString(R.string.trigger_light) -> {/*TODO*/ }
                resources.getString(R.string.trigger_magnetic) -> {/*TODO*/ }
            }
        }
    }

    private fun collectContextObjects(includeStakeholder: Boolean = false): ArrayList<java.io.Serializable>{
        val list = ArrayList<java.io.Serializable>()
        for (obj in activeScenario?.objects!!){
            list.add(obj)
            list.addAll(obj.attributes)
        }
        if (includeStakeholder){
            list.addAll(projectContext!!.stakeholders)
        }
        return list
    }

    @Suppress("UNCHECKED_CAST")
    private fun createWhatIfs(element: AbstractStep): Array<String>{
        if (element.whatIfs.isEmpty()) {
            whatIfProposal = true
            val whatIfMode = WhatIfMode.valueOf(DatabaseHelper.getInstance(applicationContext).read(Constants.WHAT_IF_MODE, String::class, WhatIfMode.ALL.toString(),DatabaseHelper.DataMode.PREFERENCES))
            val initialized = DatabaseHelper.getInstance(applicationContext).read(Constants.WHAT_IF_INITIALIZED, Boolean::class, false,DatabaseHelper.DataMode.PREFERENCES)
            var map = MultiValueMap<String,String>()
            val bytes = DatabaseHelper.getInstance(applicationContext).read(WHAT_IF_DATA,ByteArray::class,NullHelper.get(ByteArray::class))
            if (bytes.isNotEmpty()){
                try{
                    map = (DataHelper.toObject(bytes,MultiValueMap::class) as MultiValueMap<String,String>)
                }catch(e: Exception){/*NOP*/}
            }
            if (!initialized){
                //OBJECTS
                for (i in 1 .. 4){
                    map.put(OBJECT_TOKEN,getGenericStringWithIdAndTemplate(i,R.string.what_if_object_0, OBJECT_TOKEN))
                }
                //ATTRIBUTES
                for (i in 1 .. 2){
                    map.put(O_A_TOKEN,getGenericStringWithIdAndTemplate(i,R.string.what_if_attribute_0, ATTRIBUTE_TOKEN, OBJECT_TOKEN))
                }
                //STAKEHOLDER RELATION
                for (i in 1 .. 3){
                    map.put(S1_S2_TOKEN,getGenericStringWithIdAndTemplate(i,R.string.what_if_stakeholder_2_0, STAKEHOLDER_1_TOKEN, STAKEHOLDER_2_TOKEN))
                }
                //STAKEHOLDER
                for (i in 1 .. 1){
                    map.put(STAKEHOLDER_1_TOKEN,getGenericStringWithIdAndTemplate(i,R.string.what_if_stakeholder_1_0,STAKEHOLDER_1_TOKEN))
                }
                //FIXED
                for (i in 1 .. 22){
                    map.put(STATIC_TOKEN,getGenericStringWithIdAndTemplate(i,R.string.what_if_0))
                }
                DatabaseHelper.getInstance(applicationContext).write(WHAT_IF_DATA,DataHelper.toByteArray(map))
                DatabaseHelper.getInstance(applicationContext).write(Constants.WHAT_IF_INITIALIZED, true, DatabaseHelper.DataMode.PREFERENCES)
            }
            val stakeholder1 = StringHelper.nvl(activePath?.stakeholder?.name,NOTHING)
            var stakeholder2 = "Stakeholder2"
            val whatIfList = ArrayList<String>()
            val stakeholderCount = NumberHelper.nvl(projectContext?.stakeholders?.size, 0)
            //Objects & Attributes
            when (whatIfMode) {
                WhatIfMode.ALL -> {
                    whatIfList.addAll(map.get(STATIC_TOKEN))
                    whatIfList.addAll(map.get(STAKEHOLDER_1_TOKEN))
                    if (stakeholderCount > 1){
                        whatIfList.addAll(map.get(S1_S2_TOKEN))
                    }
                    if (element.objects.isNotEmpty()){
                        whatIfList.addAll(map.get(OBJECT_TOKEN))
                        whatIfList.addAll(map.get(ATTRIBUTE_TOKEN))
                        whatIfList.addAll(map.get(O_A_TOKEN))
                        if (stakeholderCount > 1) {
                            whatIfList.addAll(map.get(S1_S2_O_TOKEN))
                            whatIfList.addAll(map.get(S1_S2_A_TOKEN))
                            whatIfList.addAll(map.get(S1_S2_O_A_TOKEN))
                        }
                        whatIfList.addAll(map.get(S1_O_TOKEN))
                        whatIfList.addAll(map.get(S1_A_TOKEN))
                        whatIfList.addAll(map.get(S1_O_A_TOKEN))
                    }
                }
                WhatIfMode.DYNAMIC -> {
                    whatIfList.addAll(map.get(STAKEHOLDER_1_TOKEN))
                    if (stakeholderCount > 1){
                        whatIfList.addAll(map.get(S1_S2_TOKEN))
                    }
                    if (element.objects.isNotEmpty()){
                        whatIfList.addAll(map.get(OBJECT_TOKEN))
                        whatIfList.addAll(map.get(ATTRIBUTE_TOKEN))
                        whatIfList.addAll(map.get(O_A_TOKEN))
                        if (stakeholderCount > 1) {
                            whatIfList.addAll(map.get(S1_S2_O_TOKEN))
                            whatIfList.addAll(map.get(S1_S2_A_TOKEN))
                            whatIfList.addAll(map.get(S1_S2_O_A_TOKEN))
                        }
                        whatIfList.addAll(map.get(S1_O_TOKEN))
                        whatIfList.addAll(map.get(S1_A_TOKEN))
                        whatIfList.addAll(map.get(S1_O_A_TOKEN))
                    }
                }
                WhatIfMode.STAKEHOLDER -> {
                    whatIfList.addAll(map.get(STAKEHOLDER_1_TOKEN))
                    if (stakeholderCount > 1){
                        whatIfList.addAll(map.get(S1_S2_TOKEN))
                    }
                }
                WhatIfMode.OBJECTS -> {
                    if (element.objects.isNotEmpty()){
                        whatIfList.addAll(map.get(OBJECT_TOKEN))
                        whatIfList.addAll(map.get(ATTRIBUTE_TOKEN))
                        whatIfList.addAll(map.get(O_A_TOKEN))
                    }
                }
                WhatIfMode.STATIC -> {
                    whatIfList.addAll(map.get(STATIC_TOKEN))
                }
                WhatIfMode.NONE -> {
                    //Do Nothing
                }
            }
            //Fill Data
            val finalWhatIfs = ArrayList<String>()
            for (whatIf in whatIfList) {
                var replacedWhatIf = whatIf
                //Objects
                for (o in element.objects) {
                    replacedWhatIf = replacedWhatIf.replace(OBJECT_TOKEN,o.name)
                    for (a in activeScenario!!.getAttributesToObject(o)) {
                        if (a.key != null){
                            replacedWhatIf = replacedWhatIf.replace(ATTRIBUTE_TOKEN, a.key)
                        }
                    }
                }
                //Stakeholders
                replacedWhatIf = replacedWhatIf.replace(STAKEHOLDER_1_TOKEN, stakeholder1)
                if (stakeholderCount > 1){
                    for (s in 0 until stakeholderCount){
                        stakeholder2 = projectContext!!.stakeholders[s].name
                        if (stakeholder2 != stakeholder1) {
                            replacedWhatIf = replacedWhatIf.replace(STAKEHOLDER_2_TOKEN, stakeholder2)
                        }
                    }
                }
                finalWhatIfs.add(replacedWhatIf)
            }
            return finalWhatIfs.toTypedArray()
        }
        return element.whatIfs.toTypedArray()
    }

    /**
     * Prepares the Input and returns an Index
     */
    private fun adaptAttributes(vararg attributeNames: String): Int {
        for (i in elementAttributes.indices) {
            elementAttributes[i] = ""
        }
        for (name in attributeNames.indices) {
            if (name > elementAttributes.size - 1) {
                return elementAttributes.size - 1
            }
            elementAttributes[name] = attributeNames[name]
        }
        return 0
    }

    override fun createEntity() {
        if (activePath == null) {
            return
        }
        val whatIfs = ArrayList<String>()
        if (elementAttributes[0] == getString(R.string.literal_what_if) && inputMap[elementAttributes[0]] == null && multiInputMap[getString(R.string.literal_what_if)] != null){
            val rawWhatIfs = multiInputMap[getString(R.string.literal_what_if)]
            for (view in rawWhatIfs!!){
                whatIfs.add(view.text.toString())
            }
        }
        if (editUnit != null){
            DatabaseHelper.getInstance(applicationContext).delete(editUnit!!.getElementId(),IElement::class)
            activePath?.remove(editUnit!!)
        }
        if ( editUnit is AbstractStep && (!whatIfs.isEmpty() || //Add new What-Ifs
                (whatIfs.isEmpty() && inputMap[elementAttributes[0]] == null))){ //Clear What-Ifs
            if (whatIfProposal){
                WhatIfAiHelper.process(applicationContext)
            }
            whatIfProposal = false
            DatabaseHelper.getInstance(applicationContext).write(editUnit!!.getElementId(),(editUnit as AbstractStep).withWhatIfs(whatIfs))
            return
        }
        val endPoint = if (editUnit != null) editUnit?.getPreviousElementId() else activePath?.getEndPoint()?.getElementId()

        when (creationUnitClass) {
            //Steps
            StandardStep::class -> {
                val title = inputMap[elementAttributes[0]]!!.getStringValue()
                val objects = activeScenario?.getObjectsWithNames((inputMap[elementAttributes[1]]!! as SreMultiAutoCompleteTextView).getUsedObjectLabels())
                val text = inputMap[elementAttributes[1]]!!.getStringValue()
                addAndRenderElement(StandardStep(editUnit?.getElementId(), endPoint, activePath!!.id).withTitle(title).withText(text).withObjects(objects!!).withWhatIfs((editUnit as AbstractStep?)?.whatIfs))
            }
            JumpStep::class -> {
                val title = inputMap[elementAttributes[0]]!!.getStringValue()
                val objects = activeScenario?.getObjectsWithNames((inputMap[elementAttributes[1]]!! as SreMultiAutoCompleteTextView).getUsedObjectLabels())
                val text = inputMap[elementAttributes[1]]!!.getStringValue()
                val targetStepIdPosition = ((inputMap[elementAttributes[2]] as SreButton).data as Int) - 1
                addAndRenderElement(JumpStep(editUnit?.getElementId(), endPoint, activePath!!.id).withTargetStep(activeStepIds[targetStepIdPosition]).withTitle(title).withText(text).withObjects(objects!!).withWhatIfs((editUnit as AbstractStep?)?.whatIfs))
            }
            SoundStep::class -> {
                val title = inputMap[elementAttributes[0]]!!.getStringValue()
                val objects = activeScenario?.getObjectsWithNames((inputMap[elementAttributes[1]]!! as SreMultiAutoCompleteTextView).getUsedObjectLabels())
                val text = inputMap[elementAttributes[1]]!!.getStringValue()
                addAndRenderElement(SoundStep(editUnit?.getElementId(), endPoint, activePath!!.id).withTitle(title).withText(text).withObjects(objects!!).withWhatIfs((editUnit as AbstractStep?)?.whatIfs))
            }
            VibrationStep::class -> {
                val title = inputMap[elementAttributes[0]]!!.getStringValue()
                val objects = activeScenario?.getObjectsWithNames((inputMap[elementAttributes[1]]!! as SreMultiAutoCompleteTextView).getUsedObjectLabels())
                val text = inputMap[elementAttributes[1]]!!.getStringValue()
                addAndRenderElement(VibrationStep(editUnit?.getElementId(), endPoint, activePath!!.id).withTitle(title).withText(text).withObjects(objects!!).withWhatIfs((editUnit as AbstractStep?)?.whatIfs))
            }
            ResourceStep::class -> {
                val title = inputMap[elementAttributes[0]]!!.getStringValue()
                val resourcePosition = ((inputMap[elementAttributes[1]] as SreButton).data as Int)-1
                val resource = activeScenario!!.getAllResources()[resourcePosition]
                val changeString = uncheckedMap[elementAttributes[2]]!!.getStringValue()
                var change = 0
                if (StringHelper.hasText(changeString)){
                    change = changeString.toInt()
                }
                val objects = activeScenario?.getObjectsWithNames((inputMap[elementAttributes[3]]!! as SreMultiAutoCompleteTextView).getUsedObjectLabels())
                val text = inputMap[elementAttributes[3]]!!.getStringValue()
                addAndRenderElement(ResourceStep(editUnit?.getElementId(), endPoint, activePath!!.id).withResource(resource).withChange(change).withTitle(title).withText(text).withObjects(objects!!).withWhatIfs((editUnit as AbstractStep?)?.whatIfs))
            }
            //Triggers
            ButtonTrigger::class -> {
                val buttonLabel = inputMap[elementAttributes[0]]!!.getStringValue()
                addAndRenderElement(ButtonTrigger(editUnit?.getElementId(), endPoint, activePath!!.id).withButtonLabel(buttonLabel))
            }
            IfElseTrigger::class -> {
                if (inputMap[elementAttributes[0]] == null) {
                    //Removal
                    val selection = multiInputMap[elementAttributes[0]]
                    if (!selection.isNullOrEmpty()) {
                        for (editText in selection) {
                            val option = (editUnit as IfElseTrigger).getOptionFromIndexedString(editText.getStringValue())
                            val layer = (editUnit as IfElseTrigger).removePathOption(option)
                            val removedPath = activeScenario?.removePath(activePath!!.stakeholder, layer)
                            if (removedPath != null) {
                                DatabaseHelper.getInstance(applicationContext).delete(removedPath.id, Path::class)
                            }
                        }
                        addAndRenderElement(editUnit!!)
                    }
                } else {
                    //Add Edit
                    val text = inputMap[elementAttributes[0]]!!.getStringValue()
                    val defaultOption = inputMap[elementAttributes[1]]!!.getStringValue()
                    val option1 = inputMap[elementAttributes[2]]?.getStringValue()
                    val option2 = inputMap[elementAttributes[3]]?.getStringValue()
                    val option3 = inputMap[elementAttributes[4]]?.getStringValue()
                    val option4 = inputMap[elementAttributes[5]]?.getStringValue()
                    val option5 = inputMap[elementAttributes[6]]?.getStringValue()
                    val newOptionCount = countNonNull(defaultOption, option1, option2, option3, option4, option5)
                    val element = IfElseTrigger(editUnit?.getElementId(), endPoint, activePath!!.id, text, defaultOption)
                            .addPathOption(defaultOption, activePath!!.layer, 0)
                    if (newOptionCount > 1) {
                        val oldOptionCount = (editUnit as IfElseTrigger).getOptionCount()
                        if (oldOptionCount < newOptionCount) {
                            val newPath = activeScenario?.getPath(activePath!!.stakeholder, applicationContext)
                            element.addPathOption(inputMap[elementAttributes[newOptionCount]]?.getStringValue(), newPath!!.layer, newOptionCount - 1)
                        }
                        element.addPathOption(option1, (editUnit as IfElseTrigger).getLayerForOption(1), 1)
                                .addPathOption(option2, (editUnit as IfElseTrigger).getLayerForOption(2), 2)
                                .addPathOption(option3, (editUnit as IfElseTrigger).getLayerForOption(3), 3)
                                .addPathOption(option4, (editUnit as IfElseTrigger).getLayerForOption(4), 4)
                                .addPathOption(option5, (editUnit as IfElseTrigger).getLayerForOption(5), 5)
                    }
                    addAndRenderElement(element)
                }
            }
            StakeholderInteractionTrigger::class -> {
                val text = inputMap[elementAttributes[0]]!!.getStringValue()
                val textView = inputMap[elementAttributes[1]]
                if (textView is SreButton){
                    val stakeholderPos = (textView.data  as Int)-1
                    val stakeholderId = projectContext!!.getStakeholdersExcept(activePath!!.stakeholder)[stakeholderPos].id
                    addAndRenderElement(StakeholderInteractionTrigger(editUnit?.getElementId(), endPoint, activePath!!.id).withText(text).withInteractedStakeholderId(stakeholderId))
                }
            }
            InputTrigger::class -> {
                val text = inputMap[elementAttributes[0]]!!.getStringValue()
                val input = inputMap[elementAttributes[1]]!!.getStringValue()
                addAndRenderElement(InputTrigger(editUnit?.getElementId(), endPoint, activePath!!.id).withText(text).withInput(input))
            }
            ResourceCheckTrigger::class -> {
                val buttonLabel = inputMap[elementAttributes[0]]!!.getStringValue()
                val resourcePosition = ((inputMap[elementAttributes[1]] as SreButton).data as Int)-1
                val modePosition = ((inputMap[elementAttributes[2]] as SreButton).data as Int) //No -1, its the position already
                val checkValue = inputMap[elementAttributes[3]]!!.getStringValue().toInt()
                val stepPosition = ((inputMap[elementAttributes[4]] as SreButton).data as Int)-1
                addAndRenderElement(ResourceCheckTrigger(editUnit?.getElementId(), endPoint, activePath!!.id).withButtonLabel(buttonLabel)
                        .withResource(activeScenario!!.getAllResources()[resourcePosition]).withModePos(modePosition).withCheckValue(checkValue).withFalseStepId(activeStepIds[stepPosition]))
            }
            TimeTrigger::class -> {
                val text = inputMap[elementAttributes[0]]!!.getStringValue()
                val timeSecond = inputMap[elementAttributes[1]]!!.getStringValue()
                addAndRenderElement(TimeTrigger(editUnit?.getElementId(), endPoint, activePath!!.id).withText(text).withTimeSecond(timeSecond))
            }
            SoundTrigger::class -> {
                val text = inputMap[elementAttributes[0]]!!.getStringValue()
                val dB = inputMap[elementAttributes[1]]!!.getStringValue()
                addAndRenderElement(SoundTrigger(editUnit?.getElementId(), endPoint, activePath!!.id).withText(text).withDb(dB))
            }
            BluetoothTrigger::class -> {
                val text = inputMap[elementAttributes[0]]!!.getStringValue()
                val ssid = inputMap[elementAttributes[1]]!!.getStringValue()
                addAndRenderElement(BluetoothTrigger(editUnit?.getElementId(), endPoint, activePath!!.id).withText(text).withDeviceId(ssid))
            }
            GpsTrigger::class -> {
                val text = inputMap[elementAttributes[0]]!!.getStringValue()
                val radius = inputMap[elementAttributes[1]]!!.getStringValue()
                val latitudeLongitude = inputMap[elementAttributes[2]]!!.getStringValue()
                if (!latitudeLongitude.matches(COORDINATES_PATTERN)){
                    notify(getString(R.string.editor_gps_warning),getString(R.string.editor_gps_warning_text))
                }else{
                    val split = latitudeLongitude.split(COMMA)
                    val latitude = split[0]
                    val longitude = split[1]
                    addAndRenderElement(GpsTrigger(editUnit?.getElementId(), endPoint, activePath!!.id).withText(text).withGpsData(radius,latitude,longitude))
                }
            }
            NfcTrigger::class -> {
                val text = inputMap[elementAttributes[0]]!!.getStringValue()
                val message = uncheckedMap[elementAttributes[1]]!!.getStringValue().replace(NEW_LINE, SPACE)
                addAndRenderElement(NfcTrigger(editUnit?.getElementId(), endPoint, activePath!!.id).withText(text).withMessage(message))
            }
            WifiTrigger::class -> {
                val text = inputMap[elementAttributes[0]]!!.getStringValue()
                val ssid = inputMap[elementAttributes[1]]!!.getStringValue()
                val strength = inputMap[elementAttributes[2]]!!.getStringValue()
                addAndRenderElement(WifiTrigger(editUnit?.getElementId(), endPoint, activePath!!.id).withText(text).withSsidAndStrength(ssid,strength))
            }
            CallTrigger::class -> {
                val text = inputMap[elementAttributes[0]]!!.getStringValue()
                val telephoneNr = inputMap[elementAttributes[1]]!!.getStringValue()
                addAndRenderElement(CallTrigger(editUnit?.getElementId(), endPoint, activePath!!.id).withText(text).withTelephoneNr(telephoneNr))
            }
            SmsTrigger::class -> {
                val text = inputMap[elementAttributes[0]]!!.getStringValue()
                val telephoneNr = inputMap[elementAttributes[1]]!!.getStringValue()
                addAndRenderElement(SmsTrigger(editUnit?.getElementId(), endPoint, activePath!!.id).withText(text).withTelephoneNr(telephoneNr))
            }
            AccelerationTrigger::class -> {/*TODO*/}
            GyroscopeTrigger::class -> {/*TODO*/}
            LightTrigger::class -> {/*TODO*/}
            MagnetometerTrigger::class -> {/*TODO*/}
        }
        editUnit = null
        creationUnitClass = null
    }

    private fun connectPreviousToNext() {
        for (v in 0 until getContentHolderLayout().childCount) {
            if (getContentHolderLayout().getChildAt(v) is Element) {
                (getContentHolderLayout().getChildAt(v) as Element).connectToNext()
            }
        }
    }

    private fun colorizeZebraPattern() {
        for (v in 0 until getContentHolderLayout().childCount) {
            if (getContentHolderLayout().getChildAt(v) is Element) {
                (getContentHolderLayout().getChildAt(v) as Element).setZebraPattern(v%2==0)
            }
        }
    }

    override fun resetEditMode() {
        editor_spinner_selection?.visibility = View.VISIBLE
        creationButton?.visibility = View.VISIBLE
        editUnit = null
        inputMap.clear()
        multiInputMap.clear()
        creationUnitClass = null
        refreshState()
        getInfoTitle().textSize = DipHelper.get(resources).dip2_5.toFloat()
    }

    private fun updateSpinner(arrayResource: Int) {
        val viewResource = resolveSpinnerLayoutStyle(applicationContext)
        val spinnerArrayAdapter = ArrayAdapter<String>(this, viewResource, resources.getStringArray(arrayResource))
        spinnerArrayAdapter.setDropDownViewResource(viewResource)
        editor_spinner_selection.adapter = spinnerArrayAdapter
        editor_spinner_selection.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                creationButton?.setButtonIcons(R.string.icon_backward, R.string.icon_forward, if (pathList.isEmpty()) R.string.icon_null else R.string.icon_undo, R.string.icon_plus, null)
                        ?.setButtonStates(sideEnabled(), sideEnabled(), !pathList.isEmpty(), activePath != null && !(editor_spinner_selection.selectedItem as String).contains("["))?.updateViews(false)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                //NOP
            }
        }
    }

    override fun execAdaptToOrientationChange() {
        if (resources.configuration.orientation == ORIENTATION_LANDSCAPE) {
            contentDefaultMaxLines = 2
        } else if (resources.configuration.orientation == ORIENTATION_PORTRAIT) {
            contentDefaultMaxLines = 4
        }
    }

    override fun onBackPressed() {
        if (isInputOpen()){
            onToolbarRightClicked()
        }else{
            super.onBackPressed()
        }
    }

    override fun onToolbarCenterRightClicked() {
        if (!isInputOpen()) {
            val intent = Intent(this, GlossaryActivity::class.java)
            intent.putExtra(Constants.BUNDLE_GLOSSARY_TOPIC, "Editor")
            intent.putExtra(Constants.BUNDLE_GLOSSARY_ADDITIONAL_TOPICS, arrayOf("Step","Trigger"))
            startActivity(intent)
        }
    }
}