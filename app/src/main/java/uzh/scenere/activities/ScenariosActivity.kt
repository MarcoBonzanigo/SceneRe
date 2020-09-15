package uzh.scenere.activities

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.support.v4.content.ContextCompat
import android.view.ViewGroup
import kotlinx.android.synthetic.main.activity_scenarios.*
import uzh.scenere.R
import uzh.scenere.const.Constants
import uzh.scenere.const.Constants.Companion.NOTHING
import uzh.scenere.const.Constants.Companion.SPACE
import uzh.scenere.datamodel.AbstractObject
import uzh.scenere.datamodel.Project
import uzh.scenere.datamodel.Scenario
import uzh.scenere.datamodel.Stakeholder
import uzh.scenere.helpers.*
import uzh.scenere.views.SreTutorialLayoutDialog
import uzh.scenere.views.SwipeButton

class ScenariosActivity : AbstractManagementActivity() {

    override fun getConfiguredRootLayout(): ViewGroup? {
        return scenarios_root
    }

    override fun getConfiguredInfoString(): Int {
        return R.string.icon_explain_scenarios
    }
    override fun getConfiguredLayout(): Int {
        return R.layout.activity_scenarios
    }

    enum class ScenarioMode {
        VIEW, EDIT_CREATE, OBJECTS, EDITOR
    }
    private var scenariosMode: ScenarioMode = ScenarioMode.VIEW
    override fun isInViewMode(): Boolean {
        return scenariosMode == ScenarioMode.VIEW
    }

    override fun isInEditMode(): Boolean {
        return scenariosMode == ScenarioMode.EDIT_CREATE
    }

    override fun isInAddMode(): Boolean {
        return scenariosMode == ScenarioMode.EDIT_CREATE
    }

    override fun resetEditMode() {
        activeScenario = null
        scenariosMode = ScenarioMode.VIEW
        if (isCopyEnabled){
            onToolbarCenterLeftClicked()
        }
    }

    override fun resetToolbar() {
        if (isCopyEnabled){
            customizeToolbarText(resources.getText(R.string.icon_back).toString(), resources.getText(R.string.icon_edit).toString(), getLockIcon(), resources.getText(R.string.icon_glossary).toString(), null)
        }else{
            customizeToolbarText(resources.getText(R.string.icon_back).toString(), resources.getText(R.string.icon_copy).toString(), getLockIcon(), resources.getText(R.string.icon_glossary).toString(), null)
        }
    }

    private lateinit var inputLabelTitle: String
    private lateinit var inputLabelIntro: String
    private lateinit var inputLabelOutro: String
    private var activeProject: Project? = null
    private var activeScenario: Scenario? = null
    private var isCopyEnabled = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        inputLabelTitle = getString(R.string.scenario_name)
        inputLabelIntro = getString(R.string.scenario_intro)
        inputLabelOutro = getString(R.string.scenario_outro)
        activeProject = intent.getSerializableExtra(Constants.BUNDLE_PROJECT) as Project
        creationButton =
                SwipeButton(this, getString(R.string.scenario_create))
                        .setButtonMode(SwipeButton.SwipeButtonMode.DOUBLE)
                        .setColors(getColorWithStyle(applicationContext,R.color.srePrimaryPastel), ContextCompat.getColor(applicationContext,R.color.srePrimaryDisabled))
                        .setButtonStates(false, true, false, false)
                        .setButtonIcons(R.string.icon_null, R.string.icon_edit, null, null, R.string.icon_scenario)
                        .setFirstPosition()
                        .updateViews(true)
        creationButton!!.setExecutable(generateCreationExecutable(creationButton!!))
        getContentHolderLayout().addView(creationButton)
        createTitle("", getContentHolderLayout())
        for (scenario in DatabaseHelper.getInstance(applicationContext).readBulk(Scenario::class, activeProject)) {
            addScenarioToList(scenario)
        }
        getInfoTitle().text = StringHelper.styleString(getSpannedStringFromId(R.string.icon_explain_scenarios), fontAwesome)
        resetToolbar()
        tutorialOpen = SreTutorialLayoutDialog(this@ScenariosActivity,screenWidth,"info_copy").addEndExecutable { tutorialOpen = false }.show(tutorialOpen)
    }

    private fun addScenarioToList(scenario: Scenario) {
        val swipeButton = SwipeButton(this, scenario.title)
                .setColors(getColorWithStyle(applicationContext,R.color.srePrimaryPastel), ContextCompat.getColor(applicationContext,R.color.srePrimaryDisabled))
                .setButtonMode(SwipeButton.SwipeButtonMode.QUADRUPLE)
                .setButtonIcons(R.string.icon_delete, R.string.icon_edit, R.string.icon_object, R.string.icon_path_editor, null)
                .setButtonStates(lockState == LockState.UNLOCKED, true, true, true)
                .updateViews(true)
        swipeButton.dataObject = scenario
        swipeButton.setCounter(DatabaseHelper.getInstance(applicationContext).readBulk(AbstractObject::class,scenario).size,null)
        swipeButton.setExecutable(generateScenarioExecutable(swipeButton, scenario))
        getContentHolderLayout().addView(swipeButton)
    }

    private fun generateCreationExecutable(button: SwipeButton, scenario: Scenario? = null): SwipeButton.SwipeButtonExecution {
        return object : SwipeButton.SwipeButtonExecution {
            override fun execRight() {
                activeButton = button
                openInput(ScenarioMode.EDIT_CREATE)
            }
        }
    }

    private fun generateScenarioExecutable(button: SwipeButton, scenario: Scenario? = null): SwipeButton.SwipeButtonExecution {
        return object : SwipeButton.SwipeButtonExecution {
            override fun execLeft() {
                if (scenario != null) {
                    removeScenario(scenario,true)
                    showDeletionConfirmation(scenario.title)
                }
            }
            override fun execRight() {
                activeButton = button
                openInput(ScenarioMode.EDIT_CREATE, scenario)
            }
            override fun execUp() {
                activeButton = button
                openInput(ScenarioMode.OBJECTS, scenario)
            }
            override fun execDown() {
                activeButton = button
                openInput(ScenarioMode.EDITOR, scenario)
            }
            override fun execReset() {
                resetEditMode()
            }
        }
    }

    override fun createEntity() {
        val title = inputMap[inputLabelTitle]!!.getStringValue()
        val intro = inputMap[inputLabelIntro]!!.getStringValue()
        val outro = inputMap[inputLabelOutro]!!.getStringValue()
        val scenarioBuilder = Scenario.ScenarioBuilder(activeProject!!, title, intro, outro)
        if (isCopyEnabled && activeScenario != null){
            val fullScenario = DatabaseHelper.getInstance(applicationContext).readFull(activeScenario!!.id, Scenario::class)
            //Special Copy Routine
            var copy: Scenario? = null
            if (fullScenario != null && activeProject != null){
                val stakeholders = DatabaseHelper.getInstance(applicationContext).readBulk(Stakeholder::class,activeProject!!)
                copy = CopyHelper.copy(fullScenario,*addToArrayBefore(CollectionHelper.toIdStringList(stakeholders).toTypedArray(),fullScenario.projectId))
            }
            if (copy != null){
                for (o in copy.objects) {
                    DatabaseHelper.getInstance(applicationContext).write(o.id, o)
                }
                for (shPath in copy.paths.entries) {
                    for (path in shPath.value.entries) {
                        DatabaseHelper.getInstance(applicationContext).write(path.value.id, path.value)
                    }
                }
                scenarioBuilder.copyId(copy)
            }
        }else if (activeScenario != null) {
            removeScenario(activeScenario!!)
            scenarioBuilder.copyId(activeScenario!!)
        }
        val scenario = scenarioBuilder.build()
        DatabaseHelper.getInstance(applicationContext).write(scenario.id, scenario)
        addScenarioToList(scenario)
    }

    private fun openInput(scenariosMode: ScenarioMode, scenario: Scenario? = null) {
        activeScenario = scenario
        this.scenariosMode = scenariosMode
        when (scenariosMode) {
            ScenarioMode.VIEW -> {}//NOP
            ScenarioMode.EDIT_CREATE -> {
                cleanInfoHolder(if (activeScenario == null) getString(R.string.scenarios_create) else if (isCopyEnabled) getString(R.string.scenarios_copy) else getString(R.string.scenarios_edit))
                getInfoContentWrap().addView(createLine(inputLabelTitle, LineInputType.SINGLE_LINE_EDIT, if (scenario != null) (scenario.title.plus(if (isCopyEnabled) SPACE+getString(R.string.literal_copy) else NOTHING)) else null, false, -1))
                getInfoContentWrap().addView(createLine(inputLabelIntro, LineInputType.MULTI_LINE_EDIT, scenario?.intro, false, -1))
                getInfoContentWrap().addView(createLine(inputLabelOutro, LineInputType.MULTI_LINE_EDIT, scenario?.outro, false, -1))
            }
            ScenarioMode.OBJECTS -> {
                val intent = Intent(this, ObjectsActivity::class.java)
                intent.putExtra(Constants.BUNDLE_SCENARIO, activeScenario)
                startActivity(intent)
                return
            }
            ScenarioMode.EDITOR -> {
                val intent = Intent(this, EditorActivity::class.java)
                intent.putExtra(Constants.BUNDLE_SCENARIO, activeScenario)
                Handler().postDelayed({
                startActivity(intent)},0)
                return
            }
        }

        execMorphInfoBar(InfoState.MAXIMIZED)
    }

    private fun removeScenario(scenario: Scenario, dbRemoval: Boolean = false) {
        for (viewPointer in 0 until getContentHolderLayout().childCount) {
            if (getContentHolderLayout().getChildAt(viewPointer) is SwipeButton &&
                    (getContentHolderLayout().getChildAt(viewPointer) as SwipeButton).dataObject == scenario) {
                getContentHolderLayout().removeViewAt(viewPointer)
                if (dbRemoval){
                    DatabaseHelper.getInstance(applicationContext).delete(scenario.id, Scenario::class)
                }
                return
            }
        }
    }

    override fun onToolbarCenterRightClicked() {
        if (!isInputOpen()) {
            val intent = Intent(this, GlossaryActivity::class.java)
            intent.putExtra(Constants.BUNDLE_GLOSSARY_TOPIC, "Scenario")
            startActivity(intent)
        }
    }

    override fun onToolbarCenterLeftClicked() {
        if (!isInputOpen()) {
            isCopyEnabled = !isCopyEnabled
            resetToolbar()
            for (v in 0 until getContentHolderLayout().childCount){
                val view = getContentHolderLayout().getChildAt(v)
                if (view is SwipeButton && view != creationButton){
                    view.setButtonIcons(R.string.icon_delete, if (isCopyEnabled) R.string.icon_copy else R.string.icon_edit, R.string.icon_object, R.string.icon_path_editor, null).updateViews(false)
                }
            }
        }
    }
}