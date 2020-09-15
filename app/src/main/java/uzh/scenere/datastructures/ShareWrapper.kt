package uzh.scenere.datastructures

import uzh.scenere.const.Constants.Companion.NOTHING
import uzh.scenere.const.Constants.Companion.NOT_VALIDATED
import uzh.scenere.datamodel.Project
import uzh.scenere.datamodel.Walkthrough
import java.io.Serializable

class ShareWrapper() : Serializable {
    var validationCode: Int = NOT_VALIDATED
    var oldItems: Int = 0
    var totalItems: Int = 0
    var statistics: String = NOTHING
    var timeMs: Long = 0L
    var owner: String = ""
    var projectArray: Array<Project> = emptyArray()
    var walkthroughArray: Array<Walkthrough> = emptyArray()

    fun withTimestamp(): ShareWrapper {
        timeMs = System.currentTimeMillis()
        return this
    }

    fun withProjects(projects: Array<Project>): ShareWrapper {
        projectArray = projects
        return this
    }

    fun withOwner(user: String): ShareWrapper {
        owner = user
        return this
    }

    fun withWalkthroughs(walkthroughs: Array<Walkthrough>, wipeAllOther: Boolean = false): ShareWrapper {
        walkthroughArray = walkthroughs
        if (wipeAllOther){
            projectArray = emptyArray()
        }
        return this
    }

    fun validate(validation: Int): ShareWrapper {
        validationCode = validation
        return this
    }
}