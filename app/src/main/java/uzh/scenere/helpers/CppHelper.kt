package uzh.scenere.helpers

import android.content.Context
import android.util.Log
import uzh.scenere.const.Constants.Companion.COLOR_BOUND
import java.util.*

class CppHelper {

    external fun evaluateAverage(grid: Array<IntArray>): Double

    companion object {
        // Used to load the 'native-lib' library on application startup.
        init {
            System.loadLibrary("native-lib")
        }

        fun evaluate(context: Context, grid: Int){
            val cpp = CppHelper()
            val gridSize = grid
            val runs = 1
            val bitmapGrid = array2dOfInt(gridSize, gridSize)
            for (w in 0 until gridSize) {
                for (h in 0 until gridSize) {
                    bitmapGrid[w][h] = Random().nextInt(COLOR_BOUND)
                }
            }
            for (i in 1..runs) {
                val startCpp = System.nanoTime()
                val cppResult = cpp.evaluateAverage(bitmapGrid)
                val startKotlin = System.nanoTime()
                val kotlinResult = evaluate(bitmapGrid)
                val startKotlinStream = System.nanoTime()
                val kotlinStreamResult = evaluateStream(bitmapGrid)
                val endTime = System.nanoTime()
                Log.d("JNI Performance", "To process ${gridSize * gridSize} values, \n" +
                        "C++ took ${startKotlin - startCpp} ns, outcome: $cppResult;\n" +
                        "Kotlin took ${startKotlinStream - startKotlin} ns, outcome $kotlinResult;\n" +
                        "Kotlin Streans took ${endTime - startKotlinStream} ns, outcome $kotlinStreamResult;")
                Log.i("JNI Performance", "${gridSize * gridSize};${startKotlin - startCpp};${startKotlinStream - startKotlin}" +
                        ";${endTime - startKotlinStream}")
            }
            System.gc()
        }

        private fun evaluate(grid: Array<IntArray>): Double {
            var totalAverage = 0.0
            for (array in grid){
                for (value in array){
                    totalAverage += value.toDouble().div(grid.size*array.size)
                }
            }
            return totalAverage
        }

        private fun evaluateStream(grid: Array<IntArray>): Double {
            var totalAverage = 0.0
            grid.forEach { a -> a.forEach { v -> totalAverage += v.toDouble().div(grid.size*a.size)}}
            return totalAverage
        }
    }
}