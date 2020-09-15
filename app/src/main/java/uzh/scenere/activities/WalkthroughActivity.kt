package uzh.scenere.activities

import android.bluetooth.BluetoothDevice
import android.content.Intent
import android.content.pm.ActivityInfo
import android.net.wifi.ScanResult
import android.os.Bundle
import android.os.Handler
import android.support.v4.content.ContextCompat
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import kotlinx.android.synthetic.main.activity_walkthrough.*
import kotlinx.android.synthetic.main.holder.*
import uzh.scenere.R
import uzh.scenere.const.Constants
import uzh.scenere.const.Constants.Companion.COMPLETE_REMOVAL_DISABLED
import uzh.scenere.const.Constants.Companion.COMPLETE_REMOVAL_DISABLED_WITH_PRESET
import uzh.scenere.const.Constants.Companion.NONE
import uzh.scenere.const.Constants.Companion.NOTHING
import uzh.scenere.const.Constants.Companion.READ_ONLY
import uzh.scenere.const.Constants.Companion.SPACE
import uzh.scenere.const.Constants.Companion.WALKTHROUGH_PLAY_STATE
import uzh.scenere.const.Constants.Companion.WALKTHROUGH_PLAY_STATE_SHORTCUT
import uzh.scenere.datamodel.*
import uzh.scenere.datastructures.PlayState
import uzh.scenere.helpers.*
import uzh.scenere.views.SreContextAwareTextView
import uzh.scenere.views.SreTutorialLayoutDialog
import uzh.scenere.views.SwipeButton
import uzh.scenere.views.WalkthroughPlayLayout
import java.io.Serializable


class WalkthroughActivity : AbstractManagementActivity(), Serializable {

    override fun getConfiguredRootLayout(): ViewGroup? {
        return walkthrough_root
    }

    override fun isInEditMode(): Boolean {
        return CollectionHelper.oneOf(mode,WalkthroughMode.INFO,WalkthroughMode.WHAT_IF,WalkthroughMode.INPUT)
    }

    override fun isInAddMode(): Boolean {
        return CollectionHelper.oneOf(mode,WalkthroughMode.INFO,WalkthroughMode.WHAT_IF,WalkthroughMode.INPUT)
    }

    override fun isInViewMode(): Boolean {
        return !CollectionHelper.oneOf(mode,WalkthroughMode.INFO,WalkthroughMode.WHAT_IF,WalkthroughMode.INPUT)
    }

    override fun resetEditMode() {
        mode = WalkthroughMode.PLAY
        getInfoContentWrap().removeView(objectInfoSpinnerLayout)
        getInfoContentWrap().removeView(attributeInfoSpinnerLayout)
        getInfoContentWrap().removeView(selectedAttributeInfoLayout)
        objectInfoSpinnerLayout = null
        attributeInfoSpinnerLayout = null
        selectedAttributeInfoLayout = null
        selectedObject = null
        selectedAttribute = null
        selectedWhatIf = null
        getInfoContent().visibility = VISIBLE
        activeWalkthrough?.resetActiveness()
        resetToolbar()
    }

    override fun createEntity() {
        //NOP
    }

    override fun getConfiguredInfoString(): Int {
        return R.string.icon_explain_walkthrough
    }

    override fun getConfiguredLayout(): Int {
        return R.layout.activity_walkthrough
    }

    override fun getContentHolderLayout(): ViewGroup {
        return holder_linear_layout_holder
    }

    override fun getContentWrapperLayout(): ViewGroup {
        return holder_linear_layout_holder
    }

    override fun getInfoWrapper(): LinearLayout {
        return holder_layout_info
    }

    override fun getInfoTitle(): TextView {
        return if (mode == WalkthroughMode.PLAY) holder_text_info_title else walkthrough_text_selection_info
    }

    override fun getInfoContentWrap(): LinearLayout {
        return holder_text_info_content_wrap
    }

    override fun getInfoContent(): TextView {
        return holder_text_info_content
    }

    override fun isUsingNfc(): Boolean {
        return true
    }

    override fun isUsingWifi(): Boolean {
        return true
    }

    override fun resetToolbar() {
        if (mode == WalkthroughMode.PLAY){
            val value = ObjectHelper.nvl(activeWalkthrough?.state,WalkthroughPlayLayout.WalkthroughState.STARTED)
            customizeToolbarId(R.string.icon_back,
                    if (ObjectHelper.nvl(activeWalkthrough?.getActiveWhatIfs()?.isNotEmpty(),false) && activeWalkthrough?.state != WalkthroughPlayLayout.WalkthroughState.STARTED) R.string.icon_what_if else null,
                    if (CollectionHelper.oneOf(value,WalkthroughPlayLayout.WalkthroughState.STARTED,WalkthroughPlayLayout.WalkthroughState.FINISHED)) null else R.string.icon_input,
                    if (value != WalkthroughPlayLayout.WalkthroughState.PLAYING) null else  R.string.icon_object, null)
        }else{
            customizeToolbarId(R.string.icon_back, null, null, R.string.icon_glossary, null)
        }
    }

    enum class WalkthroughMode {
        SELECT_PROJECT, SELECT_SCENARIO, SELECT_STAKEHOLDER, PLAY, INFO, WHAT_IF, INPUT
    }

    private var mode: WalkthroughMode = WalkthroughMode.SELECT_PROJECT
    private val loadedProjects = ArrayList<Project>()
    private val loadedScenarios = ArrayList<Scenario>()
    private val loadedStakeholders = ArrayList<Stakeholder>()
    private var pointer: Int? = null
    private var projectPointer: Int? = null
    private var scenarioPointer: Int? = null
    private var scenarioContext: Scenario? = null
    //Play
    private var activeWalkthrough: WalkthroughPlayLayout? = null
    //Info
    private var objectInfoSpinnerLayout: View? = null
    private var attributeInfoSpinnerLayout: View? = null
    private var selectedAttributeInfoLayout: View? = null
    private var selectedObject: AbstractObject? = null
    private var selectedAttribute: Attribute? = null

    private var projectLabel: SreContextAwareTextView? =  null
    private var scenarioLabel: SreContextAwareTextView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        this.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        getInfoTitle().text = StringHelper.styleString(getSpannedStringFromId(getConfiguredInfoString()), fontAwesome)
        loadedProjects.clear()
        loadedProjects.addAll(DatabaseHelper.getInstance(applicationContext).readBulk(Project::class, null))
        creationButton = SwipeButton(this, createButtonLabel(loadedProjects, getString(R.string.literal_projects)))
                .setColors(getColorWithStyle(applicationContext, R.color.srePrimaryPastel), ContextCompat.getColor(applicationContext,R.color.srePrimaryDisabled))
                .setButtonMode(SwipeButton.SwipeButtonMode.QUADRUPLE)
                .setButtonIcons(R.string.icon_backward, R.string.icon_forward, R.string.icon_undo, R.string.icon_check, null)
                .setButtonStates(!loadedProjects.isEmpty(), !loadedProjects.isEmpty(), false, false)
                .adaptMasterLayoutParams(true)
                .setFirstPosition()
                .setAutoCollapse(true)
                .updateViews(true)
        creationButton?.setExecutable(createControlExecutable())
        walkthrough_layout_selection_content.addView(creationButton)
        resetToolbar()
        projectLabel = SreContextAwareTextView(applicationContext, walkthrough_layout_selection_orientation, arrayListOf(getString(R.string.walkthrough_selected_project,NOTHING)), ArrayList())
        scenarioLabel = SreContextAwareTextView(applicationContext, walkthrough_layout_selection_orientation, arrayListOf(getString(R.string.walkthrough_selected_scenario,NOTHING)), ArrayList())
        val weightedParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0)
        weightedParams.weight = 2f
        projectLabel?.setWeight(weightedParams)
        scenarioLabel?.setWeight(weightedParams)
        projectLabel?.text = getString(R.string.walkthrough_selected_project,NONE)
        scenarioLabel?.text = getString(R.string.walkthrough_selected_scenario,NONE)
        walkthrough_layout_selection_orientation.addView(projectLabel)
        walkthrough_layout_selection_orientation.addView(scenarioLabel)
        tutorialOpen = SreTutorialLayoutDialog(this@WalkthroughActivity, screenWidth, "info_walkthrough").addEndExecutable { tutorialOpen = false }.show(tutorialOpen)
        restoreWalkthroughIfPossible()
    }

    private fun restoreWalkthroughIfPossible() {
        if (activeWalkthrough == null) {
            val shortcutSave = intent.getByteArrayExtra(WALKTHROUGH_PLAY_STATE_SHORTCUT)
            val bytes = DatabaseHelper.getInstance(applicationContext).read(WALKTHROUGH_PLAY_STATE, ByteArray::class, NullHelper.get(ByteArray::class), DatabaseHelper.DataMode.PREFERENCES)
            if (ShortcutHelper.enabled && shortcutSave != null && shortcutSave.isNotEmpty()) {
                play(DataHelper.toObject(shortcutSave, PlayState::class))
            } else if (bytes.isNotEmpty()) {
                play(DataHelper.toObject(bytes, PlayState::class))
            }
        }else{
            if (!CollectionHelper.oneOf(activeWalkthrough?.state,WalkthroughPlayLayout.WalkthroughState.STARTED,WalkthroughPlayLayout.WalkthroughState.FINISHED)){
                activeWalkthrough?.resolveStepAndTrigger()
            }
        }
        DatabaseHelper.getInstance(applicationContext).delete(WALKTHROUGH_PLAY_STATE, ByteArray::class, DatabaseHelper.DataMode.PREFERENCES)
    }

    private fun backupWalkthroughIfNecessary() {
        if (activeWalkthrough != null && activeWalkthrough?.state != WalkthroughPlayLayout.WalkthroughState.FINISHED) {
            activeWalkthrough?.onPause()
            val serialized = DataHelper.toByteArray(WalkthroughPlayLayout.saveState(activeWalkthrough!!))
            if (serialized.isNotEmpty()) {
                DatabaseHelper.getInstance(applicationContext).write(WALKTHROUGH_PLAY_STATE, serialized, DatabaseHelper.DataMode.PREFERENCES)
            }
        }
    }

    override fun onDestroy() {
        backupWalkthroughIfNecessary()
        super.onDestroy()
    }

    override fun onPause() {
        backupWalkthroughIfNecessary()
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
        restoreWalkthroughIfPossible()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        restoreWalkthroughIfPossible()
    }

    private fun <T : Serializable> createButtonLabel(selectedList: ArrayList<T>, label: String): String {
        if (selectedList.isEmpty()) {
            var l = label
            if (mode == WalkthroughMode.SELECT_STAKEHOLDER){
                l = getString(R.string.literal_paths)
            }
            return getString(R.string.walkthrough_button_label_failure, l)
        }
        return getString(R.string.walkthrough_button_label, selectedList.size, label)
    }

    private fun createControlExecutable(): SwipeButton.SwipeButtonExecution {
        return object : SwipeButton.SwipeButtonExecution {
            override fun execLeft() {
                execPrev()
            }

            override fun execRight() {
                execNext()
            }

            override fun execDown() {
                execSelect()
            }

            override fun execUp() {
                execBack()
            }
        }
    }

    private fun execNext() {
        when (mode) {
            WalkthroughMode.SELECT_PROJECT -> select(loadedProjects, true)
            WalkthroughMode.SELECT_SCENARIO -> select(loadedScenarios, true)
            WalkthroughMode.SELECT_STAKEHOLDER -> {
                select(loadedStakeholders, true)
                showSave()
            }
            else -> return
        }
    }

    private fun execPrev() {
        when (mode) {
            WalkthroughMode.SELECT_PROJECT -> select(loadedProjects, false)
            WalkthroughMode.SELECT_SCENARIO -> select(loadedScenarios, false)
            WalkthroughMode.SELECT_STAKEHOLDER -> {
                select(loadedStakeholders, false)
                showSave()
            }
            else -> return
        }
    }

    private fun showSave() {
        if (ShortcutHelper.enabled){
            if (pointer != null) {
                customizeToolbarId(R.string.icon_back, R.string.icon_save, null, R.string.icon_glossary, null)
            } else {
                customizeToolbarId(R.string.icon_back, null, null, R.string.icon_glossary, null)
            }
        }
    }

    private fun execSelect() {
        when (mode) {
            WalkthroughMode.SELECT_PROJECT -> {
                creationButton?.setButtonStates(false,false,false, false)
                        ?.updateViews(false)
                executeAsyncTask({
                    mode = WalkthroughMode.SELECT_SCENARIO
                    loadedScenarios.clear()
                    loadedScenarios.addAll(DatabaseHelper.getInstance(applicationContext).readBulk(Scenario::class, loadedProjects[pointer!!]))
                    projectPointer = pointer
                    pointer = null
                },{
                    projectLabel?.text = getString(R.string.walkthrough_selected_project, loadedProjects[projectPointer!!].title)
                    creationButton?.setButtonStates(!loadedScenarios.isEmpty(), !loadedScenarios.isEmpty(), true, false)
                            ?.setText(createButtonLabel(loadedScenarios, "Scenarios"))
                            ?.updateViews(false)
                })
            }
            WalkthroughMode.SELECT_SCENARIO -> {
                creationButton?.setButtonStates(false,false,false, false)
                        ?.updateViews(false)
                executeAsyncTask({
                mode = WalkthroughMode.SELECT_STAKEHOLDER
                loadedStakeholders.clear()
                scenarioContext = DatabaseHelper.getInstance(applicationContext).readFull(loadedScenarios[pointer!!].id, Scenario::class)
                scenarioPointer = pointer
                pointer = null
                val stakeholders = DatabaseHelper.getInstance(applicationContext).readBulk(Stakeholder::class, loadedProjects[projectPointer!!])
                for (stakeholder in stakeholders) {
                    if (scenarioContext!!.hasStakeholderPath(stakeholder)) {
                        loadedStakeholders.add(stakeholder)
                    }
                }
                },{
                    scenarioLabel?.text = getString(R.string.walkthrough_selected_scenario, loadedScenarios[scenarioPointer!!].title)
                    creationButton?.setButtonStates(!loadedStakeholders.isEmpty(), !loadedStakeholders.isEmpty(), true, false)
                            ?.setButtonIcons(R.string.icon_backward, R.string.icon_forward, R.string.icon_undo, R.string.icon_play, null)
                            ?.setText(createButtonLabel(loadedStakeholders, getString(R.string.literal_stakeholders)))
                            ?.updateViews(false)
                })
            }
            WalkthroughMode.SELECT_STAKEHOLDER -> play()
            else -> return
        }
    }

    /**
     * next = false --> previous
     */
    private fun <T : Serializable> select(selectedList: ArrayList<T>, next: Boolean) {
        if (selectedList.isEmpty()) {
            return
        }
        if (next) {
            if (pointer != null && selectedList.size > (pointer!! + 1)) {
                pointer = pointer!! + 1
            } else {
                pointer = 0
            }
        } else {
            if (pointer != null && 0 <= (pointer!! - 1)) {
                pointer = pointer!! - 1
            } else {
                pointer = (selectedList.size - 1)
            }
        }

        when (selectedList[pointer!!]) {
            is Project -> creationButton?.setButtonStates(true, true, false, true)?.setText((selectedList[pointer!!] as Project).title)?.updateViews(false)
            is Scenario -> creationButton?.setButtonStates(true, true, true, true)?.setText((selectedList[pointer!!] as Scenario).title)?.updateViews(false)
            is Stakeholder -> {
                val hasPath = scenarioContext!!.hasStakeholderPath((selectedList[pointer!!] as Stakeholder))
                creationButton?.setButtonStates(true, true, true, hasPath)?.setText((selectedList[pointer!!] as Stakeholder).name)?.updateViews(false)
            }
        }
    }

    private fun execBack() {
        when (mode) {
            WalkthroughMode.SELECT_SCENARIO -> {
                mode = WalkthroughMode.SELECT_PROJECT
                loadedProjects.clear()
                loadedProjects.addAll(DatabaseHelper.getInstance(applicationContext).readBulk(Project::class, null))
                pointer = null
                projectPointer = null
                projectLabel?.text = getString(R.string.walkthrough_selected_project,NONE)
                creationButton?.setButtonStates(!loadedProjects.isEmpty(), !loadedProjects.isEmpty(), false, false)?.setText(createButtonLabel(loadedProjects, getString(R.string.literal_projects)))?.updateViews(false)
            }
            WalkthroughMode.SELECT_STAKEHOLDER -> {
                mode = WalkthroughMode.SELECT_SCENARIO
                loadedScenarios.clear()
                loadedScenarios.addAll(DatabaseHelper.getInstance(applicationContext).readBulk(Scenario::class, loadedProjects[projectPointer!!]))
                scenarioContext = null
                pointer = null
                scenarioPointer = null
                scenarioLabel?.text = getString(R.string.walkthrough_selected_scenario,NONE)
                creationButton?.setButtonStates(!loadedScenarios.isEmpty(), !loadedScenarios.isEmpty(), true, false)
                        ?.setButtonIcons(R.string.icon_backward, R.string.icon_forward, R.string.icon_undo, R.string.icon_check, null)
                        ?.setText(createButtonLabel(loadedScenarios, getString(R.string.literal_scenarios)))
                        ?.updateViews(false)
                customizeToolbarId(R.string.icon_back, null, null, R.string.icon_info, null)
            }
            else -> return
        }
    }

    private fun play(playState: PlayState? = null) {
        mode = WalkthroughMode.PLAY
        walkthrough_layout_selection_content.visibility = GONE
        walkthrough_layout_selection.visibility = GONE
        walkthrough_holder.visibility = VISIBLE
        if (playState != null){
            activeWalkthrough = WalkthroughPlayLayout(applicationContext,NullHelper.get(Scenario::class), NullHelper.get(Stakeholder::class), { resetToolbar() }, { stop() }, notifyExecutable)
            WalkthroughPlayLayout.loadState(activeWalkthrough!!,playState)
        }else {
            activeWalkthrough = WalkthroughPlayLayout(applicationContext, scenarioContext!!, loadedStakeholders[pointer!!], { resetToolbar() }, { stop() }, notifyExecutable)
        }
        activeWalkthrough?.connectActivity(this)
        getContentHolderLayout().addView(activeWalkthrough?.prepareLayout())
        tutorialOpen = SreTutorialLayoutDialog(this@WalkthroughActivity,screenWidth,"info_walkthrough_context").addEndExecutable { tutorialOpen = false }.show(tutorialOpen)
    }

    private fun stop() {
        val walkthroughStatistics = activeWalkthrough?.getWalkthrough()
        if (walkthroughStatistics != null) {
            walkthroughStatistics.toXml(applicationContext)
            DatabaseHelper.getInstance(applicationContext).write(walkthroughStatistics.id, walkthroughStatistics)
        }
        objectInfoSpinnerLayout = null
        mode = WalkthroughMode.SELECT_STAKEHOLDER
        walkthrough_layout_selection_content.visibility = VISIBLE
        walkthrough_layout_selection.visibility = VISIBLE
        walkthrough_holder.visibility = GONE
        activeWalkthrough = null
        getContentHolderLayout().removeAllViews()
        resetToolbar()
        creationButton?.reset()
    }

    private fun objectInfoSelected() {
        if (objectInfoSpinnerLayout != null) {
            val spinner = searchForLayout(objectInfoSpinnerLayout!!, Spinner::class)
            val objectName = spinner?.selectedItem.toString()
            val obj = scenarioContext?.getObjectByName(objectName)
            //Cleanup
            getInfoContentWrap().removeView(attributeInfoSpinnerLayout)
            getInfoContentWrap().removeView(selectedAttributeInfoLayout)
            selectedObject = null
            selectedAttribute = null
            if (obj != null && obj != selectedObject) {
                Walkthrough.WalkthroughProperty.INFO_OBJECT.set(objectName, String::class)
                selectedObject = obj
                attributeInfoSpinnerLayout = createLine(getString(R.string.literal_attribute), LineInputType.LOOKUP, null, false, -1, obj.getAttributeNames(""), { attributeInfoSelected() })
                getInfoContentWrap().addView(attributeInfoSpinnerLayout, 1)
            }
        }
    }

    private fun attributeInfoSelected() {
        if (attributeInfoSpinnerLayout != null) {
            val spinner = searchForLayout(attributeInfoSpinnerLayout!!, Spinner::class)
            val attributeName = spinner?.selectedItem.toString()
            val attr = selectedObject?.getAttributeByName(attributeName)
            //Cleanup
            getInfoContentWrap().removeView(selectedAttributeInfoLayout)
            selectedAttribute = null
            if (attr != null && attr != selectedAttribute) {
                Walkthrough.WalkthroughProperty.INFO_ATTRIBUTE.set(attributeName, String::class)
                selectedAttribute = attr
                selectedAttributeInfoLayout = createLine(getString(R.string.literal_value), LineInputType.MULTI_LINE_TEXT, attr.value, false, -1)
                getInfoContentWrap().addView(selectedAttributeInfoLayout, 2)
            }
        }
    }

    // DATA LINK
    // - NFC
    override fun execUseNfcData(data: String) {
        val message = activeWalkthrough?.execUseNfcData(data)
        if (StringHelper.hasText(message)){
            notify(message)
        }
    }
    override fun execNoDataRead() {
        notify(applicationContext.getString(R.string.nfc_no_data))
    }

    // - WiFi
    override fun execUseWifiScanResult(scanResult: ScanResult) {
        activeWalkthrough?.execUseWifiData(scanResult)
    }

    override fun handlePhoneNumber(phoneNumber: String): Boolean {
        if (activeWalkthrough != null){
            return activeWalkthrough!!.handlePhoneNumber(phoneNumber)
        }
        return super.handlePhoneNumber(phoneNumber)
    }

    override fun handleSmsData(phoneNumber: String, message: String): Boolean {
        if (activeWalkthrough != null){
            return activeWalkthrough!!.handleSmsData(phoneNumber,message)
        }
        return super.handleSmsData(phoneNumber, message)
    }

    override fun handleBluetoothData(devices: List<BluetoothDevice>): Boolean {
        if (activeWalkthrough != null){
            return activeWalkthrough!!.handleBluetoothData(devices)
        }
        return super.handleBluetoothData(devices)
    }

    private var awaitingBackConfirmation = false
    override fun onBackPressed() {
        if (!CollectionHelper.oneOf(mode,WalkthroughMode.PLAY,WalkthroughMode.INPUT,WalkthroughMode.INFO,WalkthroughMode.WHAT_IF) || awaitingBackConfirmation) {
            activeWalkthrough?.saveAndLoadNew(true)
            super.onBackPressed()
        } else {
            notify(getString(R.string.walkthrough_confirm))
            awaitingBackConfirmation = true
            Handler().postDelayed({ awaitingBackConfirmation = false }, 2000)
        }
    }

    override fun onToolbarLeftClicked() {
        if (mode != WalkthroughMode.PLAY) {
            super.onToolbarLeftClicked()
        } else {
            onBackPressed()
        }
    }

    override fun onToolbarCenterLeftClicked() {
        if (ShortcutHelper.enabled && mode == WalkthroughMode.SELECT_STAKEHOLDER){
            if (scenarioPointer != null && pointer != null){
                val saveState = WalkthroughPlayLayout.saveState(WalkthroughPlayLayout(applicationContext, NullHelper.get(Scenario::class), NullHelper.get(Stakeholder::class), { resetToolbar() }, { stop() }, notifyExecutable))
                ShortcutHelper.addShortcut(applicationContext,"${loadedScenarios[scenarioPointer!!].title}-${loadedStakeholders[pointer!!].name}",saveState,notifyExtendedExecutable)
            }
            //WHAT IFS
        }else  if (CollectionHelper.oneOf(mode,WalkthroughMode.PLAY,WalkthroughMode.INPUT) && activeWalkthrough?.state != WalkthroughPlayLayout.WalkthroughState.STARTED) {
            if (mode == WalkthroughMode.INPUT){
                activeWalkthrough?.resetActiveness()
            }
            getInfoContent().visibility = GONE
            getInfoTitle().text = getString(R.string.walkthrough_what_if_browse)
            customizeToolbarId(null, null, R.string.icon_input, null, R.string.icon_cross)
            execMorphInfoBar(InfoState.MAXIMIZED)
            getInfoContentWrap().removeAllViews()
            getInfoContentWrap().addView(createLine(getString(R.string.literal_what_if), LineInputType.MULTI_TEXT, READ_ONLY, false, -1, activeWalkthrough?.getActiveWhatIfs()?.toTypedArray(),null, answerWhatIf))
            activeWalkthrough?.setWhatIfActive(true)
            mode = WalkthroughMode.WHAT_IF
            tutorialOpen = SreTutorialLayoutDialog(this@WalkthroughActivity, screenWidth, "info_what_if_reply").addEndExecutable { tutorialOpen = false }.show(tutorialOpen)
        }
    }

    override fun onToolbarCenterClicked() {
        //INPUT
        if (CollectionHelper.oneOf(mode,WalkthroughMode.PLAY,WalkthroughMode.WHAT_IF) &&
                !(CollectionHelper.oneOf(activeWalkthrough?.state,WalkthroughPlayLayout.WalkthroughState.STARTED,WalkthroughPlayLayout.WalkthroughState.FINISHED))) {
            if (mode == WalkthroughMode.WHAT_IF){
                activeWalkthrough?.resetActiveness()
            }
            getInfoContent().visibility = GONE
            getInfoTitle().text = getString(R.string.walkthrough_contribute_comments)
            holder_text_info_title.text = getString(R.string.walkthrough_contribute_comments)
            customizeToolbarId(null, if (activeWalkthrough?.getActiveWhatIfs().isNullOrEmpty()) null else R.string.icon_what_if, null, null, R.string.icon_cross)
            execMorphInfoBar(InfoState.MAXIMIZED)
            getInfoContentWrap().removeAllViews()
            if (selectedWhatIf != null){
                getInfoTitle().text = getString(R.string.walkthrough_contribute_what_if_reply)
                holder_text_info_title.text = getString(R.string.walkthrough_contribute_what_if_reply)
                val preset = COMPLETE_REMOVAL_DISABLED_WITH_PRESET.plus(getString(R.string.literal_what_if_x,selectedWhatIf, SPACE))
                getInfoContentWrap().addView(createLine(getString(R.string.literal_comment), LineInputType.MULTI_TEXT, preset, false, -1, activeWalkthrough?.getComments(), addComment, removeComment))
            }else{
                getInfoContentWrap().addView(createLine(getString(R.string.literal_comment), LineInputType.MULTI_TEXT, COMPLETE_REMOVAL_DISABLED, false, -1, activeWalkthrough?.getComments(), addComment, removeComment))
            }
            activeWalkthrough?.setInputActive(true)
            selectedWhatIf = null
            mode = WalkthroughMode.INPUT
        }
    }

    override fun onToolbarCenterRightClicked() {
        if (mode == WalkthroughMode.PLAY && activeWalkthrough?.state == WalkthroughPlayLayout.WalkthroughState.PLAYING) {
            getInfoContent().visibility = GONE
            val objects = activeWalkthrough!!.getObjectNames("")
            val contextInfoAvailable = objects.size > 1
            getInfoTitle().text = getString(if (contextInfoAvailable) R.string.walkthrough_selection_info else R.string.walkthrough_selection_no_info)
            customizeToolbarId(null, null, null, null, R.string.icon_cross)
            execMorphInfoBar(InfoState.MAXIMIZED)
            if (contextInfoAvailable) {
                getInfoContentWrap().removeAllViews()
                objectInfoSpinnerLayout = createLine(getString(R.string.literal_object), LineInputType.LOOKUP, null, false, -1, objects, { objectInfoSelected() })
                getInfoContentWrap().addView(objectInfoSpinnerLayout, 0)
            }
            activeWalkthrough?.setInfoActive(true)
            mode = WalkthroughMode.INFO
        } else if (CollectionHelper.oneOf(mode,WalkthroughMode.SELECT_STAKEHOLDER,WalkthroughMode.SELECT_PROJECT,WalkthroughMode.SELECT_SCENARIO)){
            val intent = Intent(this, GlossaryActivity::class.java)
            intent.putExtra(Constants.BUNDLE_GLOSSARY_TOPIC, getText(R.string.literal_walkthrough))
            startActivity(intent)
        }
    }

    var selectedWhatIf: String? = null
    private val answerWhatIf: (String?) -> Unit = {
        if (StringHelper.hasText(it)){
            selectedWhatIf = it
            onToolbarCenterClicked()
        }
    }

    private val addComment: (String?) -> Unit = {
        if (StringHelper.hasText(it)){
            activeWalkthrough?.addComment(it!!)
        }
    }

    private val removeComment: (String?) -> Unit = {
        if (StringHelper.hasText(it)) {
            activeWalkthrough?.removeComment(it!!)
        }
    }

    private val notifyExtendedExecutable: (String,String?) -> Unit = { title: String, content:String? ->
        notify(title,content)
    }
}

