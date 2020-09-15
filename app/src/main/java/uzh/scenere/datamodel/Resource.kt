package uzh.scenere.datamodel

import java.util.*

open class Resource private constructor(id: String, scenarioId: String, name: String, description: String, val max: Int, val min: Int, val init: Int) : AbstractObject(id, scenarioId, name, description, true) {

    class ResourceBuilder(private val scenarioId: String, private val name: String, private val description: String) : AbstractObjectBuilder(scenarioId, name, description) {

        constructor(scenario: Scenario, name: String, description: String) : this(scenario.id, name, description)

        constructor(id: String, scenario: Scenario, name: String, description: String) : this(scenario.id, name, description) {
            this.id = id
        }

        constructor(id: String, scenarioId: String, name: String, description: String) : this(scenarioId, name, description) {
            this.id = id
        }

        private var max: Int = 0
        private var min: Int = 0
        private var init: Int = 0

        fun configure(min: Int, max: Int, init: Int): ResourceBuilder {
            this.max = max
            this.min = min
            this.init = init
            return this
        }

        override fun build(): Resource {
            val resource = Resource(id
                    ?: UUID.randomUUID().toString(), scenarioId, name, description, max, min, init)
            resource.attributes = this.attributes
            return resource
        }
    }

    class NullResource() : Resource("", "", "", "",0,0,0) {}
}