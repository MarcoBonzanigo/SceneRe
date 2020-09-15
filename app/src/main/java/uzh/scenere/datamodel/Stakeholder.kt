package uzh.scenere.datamodel

import uzh.scenere.const.Constants.Companion.NOTHING
import java.io.Serializable
import java.util.*

open class Stakeholder private constructor(val id: String,val projectId: String,val name: String, val description: String): Serializable, IVersionItem {
    override var changeTimeMs: Long = 0

    class StakeholderBuilder(private val projectId: String, private val name: String, private val description: String){

        constructor(project: Project, name: String, description: String): this(project.id,name,description)

        constructor(id: String, project: Project, name: String, description: String): this(project,name,description){
            this.id = id
        }

        constructor(id: String, projectId: String, name: String, description: String): this(projectId,name,description){
            this.id = id
        }

        private var id: String? = null

        fun build(): Stakeholder{
            return Stakeholder(id?: UUID.randomUUID().toString(),projectId,name,description)
        }

        fun copyId(stakeholder: Stakeholder) {
            this.id = stakeholder.id
        }
    }

    override fun equals(other: Any?): Boolean {
        if (other is Stakeholder){
            return (id == other.id)
        }
        return false
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }

    override fun toString(): String {
        return name
    }

    class NullStakeholder(stakeholderId: String = NOTHING): Stakeholder(stakeholderId,"","","") {}

    companion object {
        val name__ = "Stakeholder"
        val id_ = "id"
        val projectId_ = "projectId"
        val name_ = "name"
        val description_ = "description"
    }
}