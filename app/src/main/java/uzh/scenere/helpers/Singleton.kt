package uzh.scenere.helpers

import android.content.Context
import uzh.scenere.sensors.SensorHelper

class Singleton private constructor(context: Context) {

    companion object {
        // Volatile: writes to this field are immediately made visible to other threads.
        @Volatile private var instance : Singleton? = null

        fun getInstance(context: Context): Singleton {
            return when {
                instance != null -> instance!!
                else -> synchronized(this) {
                    if (instance == null) {
                        instance = Singleton(context)
                    }
                    instance!!
                }
            }
        }
    }
}