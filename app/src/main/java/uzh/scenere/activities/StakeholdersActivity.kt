package uzh.scenere.activities

import android.content.Intent
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.view.ViewGroup
import kotlinx.android.synthetic.main.activity_stakeholders.*
import uzh.scenere.R
import uzh.scenere.const.Constants
import uzh.scenere.const.Constants.Companion.BUNDLE_PROJECT
import uzh.scenere.datamodel.Project
import uzh.scenere.datamodel.Stakeholder
import uzh.scenere.helpers.DatabaseHelper
import uzh.scenere.helpers.StringHelper
import uzh.scenere.helpers.getColorWithStyle
import uzh.scenere.helpers.getStringValue
import uzh.scenere.views.SwipeButton
import uzh.scenere.views.SwipeButton.SwipeButtonExecution


class StakeholdersActivity : AbstractManagementActivity() {

    override fun getConfiguredRootLayout(): ViewGroup? {
        return stakeholders_root
    }

    override fun getConfiguredInfoString(): Int {
        return R.string.icon_explain_stakeholders
    }
    override fun getConfiguredLayout(): Int {
        return R.layout.activity_stakeholders
    }

    enum class StakeholderMode{
        VIEW, EDIT_CREATE
    }
    private var stakeholdersMode: StakeholderMode = StakeholderMode.VIEW
    override fun isInViewMode(): Boolean {
        return stakeholdersMode == StakeholderMode.VIEW
    }

    override fun isInEditMode(): Boolean {
        return stakeholdersMode == StakeholderMode.EDIT_CREATE
    }

    override fun isInAddMode(): Boolean {
        return stakeholdersMode == StakeholderMode.EDIT_CREATE
    }
    override fun resetEditMode() {
        activeStakeholder = null
        stakeholdersMode = StakeholderMode.VIEW
    }

    private lateinit var inputLabelName: String
    private lateinit var inputLabelDescription: String
    private var activeProject: Project? = null
    private var activeStakeholder: Stakeholder? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        inputLabelName = getString(R.string.stakeholder_name)
        inputLabelDescription = getString(R.string.stakeholder_description)
        activeProject = intent.getSerializableExtra(BUNDLE_PROJECT) as Project
        creationButton =
                SwipeButton(this,getString(R.string.stakeholder_create))
                        .setButtonMode(SwipeButton.SwipeButtonMode.DOUBLE)
                        .setColors(getColorWithStyle(applicationContext,R.color.srePrimaryPastel),ContextCompat.getColor(applicationContext,R.color.srePrimaryDisabled))
                        .setButtonStates(false,true,false,false)
                        .setButtonIcons(R.string.icon_null,R.string.icon_edit,null,null,R.string.icon_stakeholder)
                        .setFirstPosition()
                        .updateViews(true )
        creationButton!!.setExecutable(generateCreationExecutable(creationButton!!))
        getContentHolderLayout().addView(creationButton)
        createTitle("",getContentHolderLayout())
        for (stakeholder in DatabaseHelper.getInstance(applicationContext).readBulk(Stakeholder::class,activeProject)){
            addStakeholderToList(stakeholder)
        }
        getInfoTitle().text = StringHelper.styleString(getSpannedStringFromId(R.string.icon_explain_stakeholders),fontAwesome)
        resetToolbar()
    }

    private fun addStakeholderToList(stakeholder: Stakeholder) {
        val swipeButton = SwipeButton(this, stakeholder.name)
                .setColors(getColorWithStyle(applicationContext,R.color.srePrimaryPastel), ContextCompat.getColor(applicationContext,R.color.srePrimaryDisabled))
                .setButtonMode(SwipeButton.SwipeButtonMode.DOUBLE)
                .setButtonIcons(R.string.icon_delete, R.string.icon_edit, null, null, null)
                .setButtonStates(lockState == LockState.UNLOCKED, true, false, false)
                .updateViews(true)
        swipeButton.dataObject = stakeholder
        swipeButton.setExecutable(generateStakeholderExecutable(swipeButton, stakeholder))
        getContentHolderLayout().addView(swipeButton)
    }

    private fun generateCreationExecutable(button: SwipeButton, stakeholder: Stakeholder? = null): SwipeButtonExecution {
        return object: SwipeButtonExecution{
            override fun execRight() {
                activeButton = button
                openInput(StakeholderMode.EDIT_CREATE)
            }
        }
    }

    private fun generateStakeholderExecutable(button: SwipeButton, stakeholder: Stakeholder? = null): SwipeButtonExecution {
        return object: SwipeButtonExecution{
            override fun execLeft() {
                if (stakeholder!=null){
                    removeStakeholder(stakeholder,true)
                    showDeletionConfirmation(stakeholder.name)
                }
            }
            override fun execRight() {
                activeButton = button
                openInput(StakeholderMode.EDIT_CREATE,stakeholder)
            }
            override fun execReset() {
                resetEditMode()
            }
        }
    }

    override fun createEntity() {
        val name = inputMap[inputLabelName]!!.getStringValue()
        val introduction = inputMap[inputLabelDescription]!!.getStringValue()
        val stakeholderBuilder = Stakeholder.StakeholderBuilder(activeProject!!,name, introduction)
        if (activeStakeholder != null){
            removeStakeholder(activeStakeholder!!)
            stakeholderBuilder.copyId(activeStakeholder!!)
        }
        val stakeholder = stakeholderBuilder.build()
        DatabaseHelper.getInstance(applicationContext).write(stakeholder.id,stakeholder)
        addStakeholderToList(stakeholder)
    }

    private fun openInput(stakeholdersMode: StakeholderMode, stakeholder: Stakeholder? = null) {
        activeStakeholder = stakeholder
        this.stakeholdersMode = stakeholdersMode
        cleanInfoHolder(if (activeStakeholder==null) getString(R.string.stakeholders_create) else getString(R.string.stakeholders_edit))
        when(stakeholdersMode){
            StakeholderMode.EDIT_CREATE -> {
                getInfoContentWrap().addView(createLine(inputLabelName, LineInputType.SINGLE_LINE_EDIT, stakeholder?.name, false, -1))
                getInfoContentWrap().addView(createLine(inputLabelDescription, LineInputType.MULTI_LINE_EDIT, stakeholder?.description, false, -1))
            }
        }

        execMorphInfoBar(InfoState.MAXIMIZED)
    }

    private fun removeStakeholder(stakeholder: Stakeholder, dbRemoval: Boolean = false) {
        for (viewPointer in 0 until getContentHolderLayout().childCount){
            if (getContentHolderLayout().getChildAt(viewPointer) is SwipeButton &&
                    (getContentHolderLayout().getChildAt(viewPointer) as SwipeButton).dataObject == stakeholder){
                getContentHolderLayout().removeViewAt(viewPointer)
                if (dbRemoval){
                    DatabaseHelper.getInstance(applicationContext).delete(stakeholder.id, Stakeholder::class)
                }
                return
            }
        }
    }

    override fun onToolbarCenterRightClicked() {
        if (!isInputOpen()) {
            val intent = Intent(this, GlossaryActivity::class.java)
            intent.putExtra(Constants.BUNDLE_GLOSSARY_TOPIC, "Stakeholder")
            startActivity(intent)
        }
    }
}