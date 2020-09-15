package uzh.scenere.helpers

import uzh.scenere.datamodel.*
import uzh.scenere.datamodel.steps.AbstractStep
import uzh.scenere.datamodel.triggers.AbstractTrigger
import java.io.Serializable
import kotlin.reflect.KClass

@Suppress("UNCHECKED_CAST")
class NullHelper private constructor(){
    companion object {
        fun <T: Serializable> get(clazz: KClass<T>): T{
            return when(clazz){
                Project::class -> Project.NullProject() as T
                Stakeholder::class -> Stakeholder.NullStakeholder() as T
                Scenario::class -> Scenario.NullScenario() as T
                Attribute::class -> Attribute.NullAttribute() as T
                ContextObject::class, AbstractObject::class -> ContextObject.NullContextObject() as T
                Resource::class -> Resource.NullResource() as T
                Path::class -> Path.NullPath() as T
                Walkthrough::class -> Walkthrough.NullWalkthrough() as T
                AbstractStep::class -> AbstractStep.NullStep() as T
                AbstractTrigger::class -> AbstractTrigger.NullTrigger() as T
                String::class -> "" as T
                Long::class -> 0 as T
                Float::class -> 0 as T
                Double::class -> 0 as T
                Integer::class -> 0 as T
                Short::class -> 0 as T
                Boolean::class -> false as T
                ByteArray::class -> ByteArray(0) as T
                else -> throw ClassNotFoundException()
            }
        }
    }
}