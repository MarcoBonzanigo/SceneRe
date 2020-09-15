package uzh.scenere.datamodel

import java.io.Serializable


interface IElement: Serializable {
    fun getElementId(): String
    fun getPreviousElementId(): String?
    fun getElementPathId(): String
    fun setPreviousElementId(id: String): IElement
}