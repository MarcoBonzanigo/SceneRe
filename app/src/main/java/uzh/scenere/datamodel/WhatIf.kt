package uzh.scenere.datamodel

import java.io.Serializable

class WhatIf private constructor(private val previousId: String, private val description: String): Serializable, IVersionItem {
    override var changeTimeMs: Long = 0

    class WhatIfBuilder(val previousId: String, val description: String){

        fun build(): WhatIf{
            val whatIf  = WhatIf(previousId, description)
            return whatIf
        }
    }

}