package uzh.scenere.activities

import android.content.Intent
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.view.ViewGroup
import kotlinx.android.synthetic.main.activity_projects.*
import uzh.scenere.R
import uzh.scenere.const.Constants
import uzh.scenere.const.Constants.Companion.BUNDLE_PROJECT
import uzh.scenere.datamodel.Project
import uzh.scenere.datamodel.Scenario
import uzh.scenere.datamodel.Stakeholder
import uzh.scenere.helpers.*
import uzh.scenere.views.SreTutorialLayoutDialog
import uzh.scenere.views.SwipeButton
import uzh.scenere.views.SwipeButton.SwipeButtonExecution


class ProjectsActivity : AbstractManagementActivity() {

    override fun getConfiguredRootLayout(): ViewGroup? {
        return projects_root
    }

    override fun getConfiguredInfoString(): Int {
        return R.string.icon_explain_projects
    }
    override fun getConfiguredLayout(): Int {
        return R.layout.activity_projects
    }

    enum class ProjectsMode{
        VIEW, EDIT_CREATE, SCENARIO, STAKEHOLDER
    }
    private var projectsMode: ProjectsMode = ProjectsMode.VIEW
    override fun isInViewMode(): Boolean {
        return projectsMode == ProjectsMode.VIEW
    }

    override fun isInEditMode(): Boolean {
        return projectsMode == ProjectsMode.EDIT_CREATE
    }

    override fun isInAddMode(): Boolean {
        return projectsMode == ProjectsMode.EDIT_CREATE
    }

    override fun resetEditMode() {
        activeProject = null
        projectsMode = ProjectsMode.VIEW
    }

    private lateinit var inputLabelTitle: String
    private lateinit var inputLabelDescription: String
    private var activeProject: Project? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        inputLabelTitle = getString(uzh.scenere.R.string.project_title)
        inputLabelDescription = getString(uzh.scenere.R.string.project_description)
        creationButton =
                SwipeButton(this,getString(R.string.project_create))
                        .setButtonMode(SwipeButton.SwipeButtonMode.DOUBLE)
                        .setColors(getColorWithStyle(applicationContext,R.color.srePrimaryPastel),ContextCompat.getColor(applicationContext,R.color.srePrimaryDisabled))
                        .setButtonStates(false,true,false,false)
                        .setButtonIcons(R.string.icon_null,R.string.icon_edit,null,null,R.string.icon_project)
                        .setFirstPosition()
                        .updateViews(true )
        creationButton!!.setExecutable(generateCreationExecutable(creationButton!!))
        getContentHolderLayout().addView(creationButton)
        createTitle("",getContentHolderLayout())
        for (project in DatabaseHelper.getInstance(applicationContext).readBulk(Project::class,null)){
            addProjectToList(project)
        }
        getInfoTitle().text = StringHelper.styleString(getSpannedStringFromId(R.string.icon_explain_projects),fontAwesome)
        resetToolbar()
        tutorialOpen = SreTutorialLayoutDialog(this@ProjectsActivity,screenWidth,"info_creation","info_bars", "info_toolbar","info_lock_glossary").addEndExecutable { tutorialOpen = false }.show(tutorialOpen)
    }

    private fun addProjectToList(project: Project) {
        val swipeButton = SwipeButton(this, project.title)
                .setColors(getColorWithStyle(applicationContext,R.color.srePrimaryPastel), ContextCompat.getColor(applicationContext,R.color.srePrimaryDisabled))
                .setButtonIcons(R.string.icon_delete, R.string.icon_edit, R.string.icon_stakeholder, R.string.icon_scenario, null)
                .setButtonStates(lockState == LockState.UNLOCKED, true, true, true)
                .updateViews(true)
        swipeButton.dataObject = project
        swipeButton.setCounter(DatabaseHelper.getInstance(applicationContext).readBulk(Stakeholder::class,project).size,
                DatabaseHelper.getInstance(applicationContext).readBulk(Scenario::class,project).size)
        swipeButton.setExecutable(generateProjectExecutable(swipeButton, project))
        getContentHolderLayout().addView(swipeButton)
    }

    private fun generateCreationExecutable(button: SwipeButton, project: Project? = null): SwipeButtonExecution {
        return object: SwipeButtonExecution{
            override fun execRight() {
                activeButton = button
                openInput(ProjectsMode.EDIT_CREATE)
            }
        }
    }

    private fun generateProjectExecutable(button: SwipeButton, project: Project? = null): SwipeButtonExecution {
        return object: SwipeButtonExecution{
            override fun execLeft() {
                if (project!=null){
                    removeProject(project,true)
                    showDeletionConfirmation(project.title)
                }
            }
            override fun execRight() {
                activeButton = button
                openInput(ProjectsMode.EDIT_CREATE,project)
            }
            override fun execUp() {
                activeButton = button
                openInput(ProjectsMode.STAKEHOLDER,project)
            }
            override fun execDown() {
                activeButton = button
                openInput(ProjectsMode.SCENARIO,project)
            }
            override fun execReset() {
                resetEditMode()
            }
        }
    }

    override fun createEntity() {
        val title = inputMap[inputLabelTitle]!!.getStringValue()
        val description = inputMap[inputLabelDescription]!!.getStringValue()
        val projectBuilder = Project.ProjectBuilder(DatabaseHelper.getInstance(applicationContext).read(Constants.USER_NAME,String::class, Constants.ANONYMOUS), title, description)
        if (activeProject != null){
            removeProject(activeProject!!)
            projectBuilder.copyId(activeProject!!)
        }
        val project = projectBuilder.build()
        DatabaseHelper.getInstance(applicationContext).write(project.id,project)
        addProjectToList(project)
        tutorialOpen = SreTutorialLayoutDialog(this@ProjectsActivity,screenWidth,"info_entity_item").addEndExecutable { tutorialOpen = false }.show(tutorialOpen)
    }

    private fun openInput(projectsMode: ProjectsMode, project: Project? = null) {
        activeProject = project
        this.projectsMode = projectsMode
        when(projectsMode){
            ProjectsMode.VIEW -> {}//NOP
            ProjectsMode.EDIT_CREATE -> {
                cleanInfoHolder(if (activeProject==null) getString(R.string.projects_create) else getString(R.string.projects_edit))
                getInfoContentWrap().addView(createLine(inputLabelTitle, LineInputType.SINGLE_LINE_EDIT, project?.title, false, -1))
                getInfoContentWrap().addView(createLine(inputLabelDescription, LineInputType.MULTI_LINE_EDIT, project?.description, false, -1))
            }
            ProjectsMode.SCENARIO -> {
                val intent = Intent(this,ScenariosActivity::class.java)
                intent.putExtra(BUNDLE_PROJECT,project)
                startActivity(intent)
                return
            }
            ProjectsMode.STAKEHOLDER -> {
                val intent = Intent(this,StakeholdersActivity::class.java)
                intent.putExtra(BUNDLE_PROJECT,project)
                startActivity(intent)
                return
            }
        }

        execMorphInfoBar(InfoState.MAXIMIZED)
    }

    private fun removeProject(project: Project, dbRemoval: Boolean = false) {
        for (viewPointer in 0 until getContentHolderLayout().childCount){
            if (getContentHolderLayout().getChildAt(viewPointer) is SwipeButton &&
                    (getContentHolderLayout().getChildAt(viewPointer) as SwipeButton).dataObject == project){
                getContentHolderLayout().removeViewAt(viewPointer)
                if (dbRemoval){
                    DatabaseHelper.getInstance(applicationContext).delete(project.id, Project::class)
                }
                return
            }
        }
    }

    override fun onToolbarCenterRightClicked() {
        if (!isInputOpen()) {
            val intent = Intent(this, GlossaryActivity::class.java)
            intent.putExtra(Constants.BUNDLE_GLOSSARY_TOPIC, "Project")
            startActivity(intent)
        }
    }
}