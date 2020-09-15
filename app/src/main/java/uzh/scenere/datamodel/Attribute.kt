package uzh.scenere.datamodel

import java.io.Serializable
import java.util.*

open class Attribute private constructor(val id: String, val refId: String, val key: String?, val value: String?): Serializable, IVersionItem {
    override var changeTimeMs: Long = 0

    var type: String? = null

    class AttributeBuilder(private val refId: String, private val key: String?, private val value: String?) {

        constructor(id: String, refId: String, key: String?, value: String?) : this(refId, key, value) {
            this.id = id
        }

        private var id: String? = null
        private var type: String? = null

        fun withAttributeType(type: String?): AttributeBuilder{
            this.type = type
            return this
        }
        fun copyId(attribute: Attribute): AttributeBuilder {
            this.id = attribute.id
            return this
        }

        fun buildAsLink(): Attribute {
            if (key != null){
                this.id = refId+key
            }
            return build()
        }

        fun build(): Attribute {
            val attribute = Attribute(id ?: UUID.randomUUID().toString(), refId, key, value)
            attribute.type = this.type
            return attribute
        }
    }

    fun getVersioningId(): String{
        if (key != null){
            return refId+key
        }
        return id
    }

    override fun equals(other: Any?): Boolean {
        if (other is Attribute) {
            return (id == other.id)
        }
        return false
    }

    override fun hashCode(): Int {
        return super.hashCode()
    }

    class NullAttribute(): Attribute("","","","") {}
}