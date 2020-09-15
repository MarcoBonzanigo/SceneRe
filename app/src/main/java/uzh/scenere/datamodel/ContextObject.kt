package uzh.scenere.datamodel

import uzh.scenere.datamodel.AbstractObject.AbstractObjectBuilder
import java.util.*

open class ContextObject private constructor(id: String, scenarioId: String, name: String, description: String) : AbstractObject(id, scenarioId, name, description, false) {

    class ContextObjectBuilder(private val scenarioId: String, private val name: String, private val description: String) : AbstractObjectBuilder(scenarioId, name, description) {

        constructor(scenario: Scenario, name: String, description: String) : this(scenario.id, name, description)

        constructor(id: String, scenario: Scenario, name: String, description: String) : this(scenario.id, name, description) {
            this.id = id
        }

        constructor(id: String, scenarioId: String, name: String, description: String) : this(scenarioId, name, description) {
            this.id = id
        }

        override fun build(): ContextObject {
            val resource = ContextObject(id?:UUID.randomUUID().toString(), scenarioId, name, description)
            resource.attributes = this.attributes
            return resource
        }
    }

    class NullContextObject() : ContextObject("", "", "", "") {}
}