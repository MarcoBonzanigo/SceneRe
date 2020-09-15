package uzh.scenere.helpers

import android.hardware.SensorEvent
import uzh.scenere.const.Constants.Companion.HUNDRED
import uzh.scenere.const.Constants.Companion.MILLION_D
import java.math.BigDecimal
import java.math.RoundingMode

class SensorValueHelper private constructor () {
    companion object {
        private var sensorType: String? = null
        private const val gridSize = 150 //TODO> Configurable
        private const val roundingPrecision = 2
        private var gridPointer = 0
        private var grid: Array<FloatArray>? = null
        private var cachedValues: Array<BigDecimal>? = null

        fun smoothValuesAndWrap(event: SensorEvent): String{
            checkResetNecessary(event)
            val values = cacheAndSmoothValues(event)
            return wrap(values,event)
        }
        fun smoothValues(event: SensorEvent): FloatArray{
            checkResetNecessary(event)
            return cacheAndSmoothValues(event)
        }
        fun wrap(values: FloatArray, event: SensorEvent): String{
            val time = System.nanoTime()
            if (gridPointer > gridSize) {
                var text = event.sensor.name + " Values:\n"
                for (value in values) text +=  value.toString() + "\n"
                return text + floor(((System.nanoTime() - time).toDouble()/MILLION_D),roundingPrecision).toString() + " ms."
            }
            return "Gathering Data... ("+floor((HUNDRED/gridSize.toDouble()*gridPointer),roundingPrecision)+"%)"
        }
        private fun checkResetNecessary(event: SensorEvent) {
            if (sensorType != event.sensor.name || grid == null || cachedValues == null) {
                //Reset Caching Mechanism
                sensorType = event.sensor.name
                grid = array2dOfFloat(event.values.size, gridSize)
                cachedValues = Array(event.values.size){BigDecimal.ZERO}
                gridPointer = 0
            }
        }
        private fun cacheAndSmoothValues(event: SensorEvent): FloatArray{
            val values = FloatArray(event.values.size)
            if (gridPointer == gridSize && cachedValues != null){
                //calculate once
                for (p in 0 until event.values.size) {
                    var sum = BigDecimal.ZERO
                    for (value in grid!![p]) {
                        sum = sum.add(BigDecimal(value.toDouble()))
                    }
                    cachedValues!![p] = sum
                }
            }
            if (gridPointer >= gridSize){
                for (p in 0 until event.values.size) {
                    cachedValues!![p] = cachedValues!![p]
                            .add(BigDecimal(event.values[p].toDouble()))
                            .subtract(BigDecimal(grid!![p][gridPointer % gridSize].toDouble()))
                }
            }
            for (p in 0 until event.values.size) {
                grid!![p][gridPointer % gridSize] = event.values[p]
                values[p] = cachedValues!![p].divide(BigDecimal(gridSize), roundingPrecision, RoundingMode.HALF_UP).toFloat()
            }
            gridPointer++
            return values
        }
    }
}