package uzh.scenere.sensors

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import uzh.scenere.R
import uzh.scenere.helpers.NumberHelper
import uzh.scenere.helpers.SensorValueHelper
import uzh.scenere.views.GraphView


class SensorHelper private constructor(context: Context) {
    private val sensorCount = 40
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val sensorTypes: HashMap<String,String> = HashMap()
    private val sensorArray: ArrayList<Sensor> = ArrayList(sensorCount)
    private var textGraphEventListener: TextGraphEventListener? = null

    var intervals = arrayListOf<Int>(
            SensorManager.SENSOR_DELAY_FASTEST,
            SensorManager.SENSOR_DELAY_GAME,
            SensorManager.SENSOR_DELAY_UI,
            SensorManager.SENSOR_DELAY_NORMAL
    ) //Documentation
    private val selectedSensors: HashMap<Int,String> = hashMapOf(
            Sensor.TYPE_ACCELEROMETER to "Accelerometer",
            Sensor.TYPE_LINEAR_ACCELERATION to "Linear Acceleration",
            Sensor.TYPE_ROTATION_VECTOR to "Rotation",
            Sensor.TYPE_MAGNETIC_FIELD to "Magnetic Field",
            Sensor.TYPE_GRAVITY to "Gravity",
            Sensor.TYPE_LIGHT to "Light"
    ) //Documentation

    companion object {
        // Volatile: writes to this field are immediately made visible to other threads.
        @Volatile private var instance : SensorHelper? = null

        fun getInstance(context: Context): SensorHelper {
            return when {
                instance != null -> instance!!
                else -> synchronized(this) {
                    if (instance == null) {
                        instance = SensorHelper(context)
                    }
                    instance!!
                }
            }
        }
    }

    fun createSensorArray() : String{
        sensorArray.clear()
        for (i in selectedSensors){
            val sensorElement = sensorManager.getDefaultSensor(i.key)
            if (sensorElement != null){
                Log.i("SensorArray",i.toString()+"-"+sensorElement.name)
                sensorTypes[sensorElement.name] = i.value
                sensorArray.add(sensorElement)
            }
        }
        return sensorArray.size.toString()+" Sensors found and initialized."
    }

    fun getSensorArray(): ArrayList<Sensor>{
        if (sensorArray.isEmpty()){
            createSensorArray()
        }
        return sensorArray
    }

    fun getTypeName(sensorName: String): String{
        return sensorTypes[sensorName] ?: ""
    }

    fun registerTextGraphListener(sensor: Sensor, outputText: TextView){
        unregisterTextGraphListener()
        textGraphEventListener = TextGraphEventListener(outputText,sensor)
        sensorManager.registerListener(textGraphEventListener,sensor,SensorManager.SENSOR_DELAY_GAME)
    }
    fun unregisterTextGraphListener(){
        textGraphEventListener?.reset()
        sensorManager.unregisterListener(textGraphEventListener)
        textGraphEventListener = null;
    }

    class TextGraphEventListener(private val outputText: TextView, private val filteredSensor: Sensor, private val smoothValues: Boolean = true) : SensorEventListener{

        private var graphView: GraphView? = null
        private var cachedLayoutParams: ViewGroup.LayoutParams? = null
        fun reset() {
            if (outputText.parent is LinearLayout && graphView != null){
                (outputText.parent as LinearLayout).removeView(graphView)
                outputText.layoutParams = cachedLayoutParams
                graphView = null
            }
        }

        override fun onSensorChanged(event: SensorEvent?) {
            if (event==null || (event.sensor != filteredSensor)) return
            // Checked Values
            if (smoothValues){
                outputText.text = SensorValueHelper.smoothValuesAndWrap(event)
                val values = SensorValueHelper.smoothValues(event)
                if (outputText.parent is LinearLayout && graphView == null){
                    //Cache and Adjust Text
                    cachedLayoutParams = outputText.layoutParams
                    val outputTextLp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT, 2f)
                    val margin = NumberHelper.nvl(outputText.resources?.getDimension(R.dimen.dpi5), 0).toInt()
                    outputTextLp.setMargins(margin,margin,margin,margin)
                    outputText.layoutParams = outputTextLp
                    //Create and add Graph
                    graphView = GraphView(outputText.context, values, "Sensor-Values", arrayOf("Sensor-Node"), arrayOf("Value"), GraphView.GraphType.BAR, null, null, null)
                    val graphViewLp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT, 1f)
                    graphViewLp.setMargins(margin,margin,margin,margin)
                    graphView!!.layoutParams = graphViewLp
                    (outputText.parent as LinearLayout).addView(graphView)
                }else if (graphView != null){
                    graphView?.values = values
                    graphView?.invalidate()
                }
            }else{
                var valText = event.sensor.name+" Values:\n"
                for (value in event.values) valText += (value.toString()+"\n")
                outputText.text = valText
            }
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
            //NOP
        }

    }
}