package uzh.scenere.datamodel

import android.content.Context
import uzh.scenere.helpers.DatabaseHelper
import java.io.Serializable
import java.util.*
import kotlin.collections.ArrayList

open class Project private constructor(val id: String, val creator: String, val title: String, val description: String): Serializable, IVersionItem {
    override var changeTimeMs: Long = 0

    var scenarios: List<Scenario> = ArrayList()
    var stakeholders: List<Stakeholder> = ArrayList()

    fun getStakeholderPositionById(id: String, ignoredStakeholder: Stakeholder): Int{
        var pointer = 0
        for (stakeholder in stakeholders){
            if (stakeholder.id == id) return pointer
            if (stakeholder != ignoredStakeholder){
                pointer++
            }
        }
        return -1
    }

    fun getStakeholdersExcept(stakeholder: Stakeholder): ArrayList<Stakeholder> {
        val list = ArrayList<Stakeholder>()
        for (s in stakeholders){
            if (s != stakeholder){
                list.add(s)
            }
        }
        return list
    }

    fun getNextStakeholder(stakeholder: Stakeholder? = null): Stakeholder?{
        if (stakeholders.isEmpty()){
            return null
        }
        if (stakeholder == null){
            return stakeholders[0]
        }
        for (s in 0 until stakeholders.size){
            if (stakeholders[s] == stakeholder){
                return if ((s+1) == stakeholders.size){
                    getNextStakeholder()
                }else{
                    stakeholders[s+1]
                }
            }
        }
        return null
    }

    fun getPreviousStakeholder(stakeholder: Stakeholder? = null): Stakeholder?{
        if (stakeholders.isEmpty()){
            return null
        }
        if (stakeholder == null){
            return stakeholders[0]
        }
        for (s in 0 until stakeholders.size){
            if (stakeholders[s] == stakeholder){
                return if (s == 0){
                    stakeholders[stakeholders.size-1]
                }else{
                    stakeholders[s-1]
                }
            }
        }
        return null
    }

    class ProjectBuilder(private val creator: String, private val  title: String, private val  description: String){

        constructor(id: String, creator: String, title: String, description: String) : this(creator, title, description) {
            this.id = id
        }

        private var scenarios: List<Scenario> = ArrayList()
        private var stakeholders: List<Stakeholder> = ArrayList()
        private var id: String? = null

        fun addScenarios(vararg scenario: Scenario): ProjectBuilder{
            this.scenarios = scenarios.plus(scenario)
            return this
        }
        fun addStakeholders(vararg stakeholder: Stakeholder): ProjectBuilder{
            this.stakeholders = stakeholders.plus(stakeholder)
            return this
        }
        fun copyId(project: Project): ProjectBuilder{
            this.id = project.id
            return this
        }
        fun build(): Project{
            val project = Project(id?: UUID.randomUUID().toString(),creator,title,description)
            project.scenarios = this.scenarios
            project.stakeholders = this.stakeholders
            return project
        }
    }

    override fun equals(other: Any?): Boolean {
        if (other is Project){
            return (id == other.id)
        }
        return false
    }

    fun reloadScenarios(context: Context): Project{
        val list = ArrayList<Scenario>()
        for (scenario in scenarios){
            val s = DatabaseHelper.getInstance(context).readFull(scenario.id, Scenario::class)
            if (s != null){
                list.add(s)
            }
        }
        scenarios = list
        return this
    }

    override fun hashCode(): Int {
        return super.hashCode()
    }

    class NullProject(): Project("","","","") {}
}