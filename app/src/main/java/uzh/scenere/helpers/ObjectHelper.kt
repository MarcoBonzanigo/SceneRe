package uzh.scenere.helpers

class ObjectHelper{
    companion object { //Static Reference
        fun <T> nvl(value: T?, valueIfNull: T): T {
            return value ?: valueIfNull; // Elvis Expression of Java number==null?valueIfNull:number
        }
    }
}