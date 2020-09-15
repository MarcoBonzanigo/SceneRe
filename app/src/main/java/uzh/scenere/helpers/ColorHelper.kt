package uzh.scenere.helpers

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.support.v4.content.ContextCompat
import android.view.View
import uzh.scenere.R

class ColorHelper {
    companion object {
        fun percentToColor(percent: Float): Int {
            return Color.rgb(255 - NumberHelper.multiply(255, percent), NumberHelper.multiply(255, percent), 0)
        }

        fun isDark(color: Int): Boolean {
            return (Color.red(color) + Color.green(color) + Color.blue(color)) / 3.0 < 122
        }

        fun getTextColor(context: Context, backgroundColor: Int): Int {
            return if (ColorHelper.isDark(backgroundColor)) {
                getColorWithStyle(context, R.color.sreWhite)
            } else {
                getColorWithStyle(context, R.color.sreBlack)
            }
        }

        /**
         * Calculates the differences between Color a and b
         * Returns a value between 0 (identical) and 1 (complete different colors)
         */
        fun getColorDifferences(a: Int, b: Int): Float {
            return 1f / 765f * (Math.abs(Color.red(a) - Color.red(b)) +
                    Math.abs(Color.green(a) - Color.green(b)) +
                    Math.abs(Color.blue(a) - Color.blue(b)))

        }

        fun getColorToneDifferences(a: Int, b: Int): Float {
            val redDiff = Math.abs(Color.red(a) - Color.red(b)).toFloat()
            val greenDiff = Math.abs(Color.green(a) - Color.green(b)).toFloat()
            val blueDiff = Math.abs(Color.blue(a) - Color.blue(b)).toFloat()
            val finalDiffB = Math.abs(redDiff - greenDiff)
            val finalDiffR = Math.abs(blueDiff - greenDiff)
            val finalDiffG = Math.abs(blueDiff - redDiff)
            val max = NumberHelper.max(finalDiffB, finalDiffR, finalDiffG)
            return 1f / 255f * max
        }

        fun adjustColorToBackground(backgroundColor: Int, color: Int, differenceThreshold: Double): Int {
            var newColor = color
            while (getColorDifferences(backgroundColor, newColor) < differenceThreshold) {
                newColor = adjustColorToBrightness(backgroundColor, newColor)
            }
            return newColor
        }

        private fun adjustColorToBrightness(backgroundColor: Int, color: Int): Int {
            val step: Int
            var red = Color.red(color)
            var green = Color.green(color)
            var blue = Color.blue(color)
            if (isDark(backgroundColor)) {
                step = 10
            } else {
                step = -10
            }
            red = if (red + step > 255) 255 else if (red + step < 0) 0 else red + step
            green = if (green + step > 255) 255 else if (green + step < 0) 0 else green + step
            blue = if (blue + step > 255) 255 else if (blue + step < 0) 0 else blue + step
            return Color.rgb(red, green, blue)
        }

        fun toHexString(color: Int): String {
            var hexColour = Integer.toHexString(color and 0xffffff)
            if (hexColour.length < 6) {
                hexColour = "000000".substring(0, 6 - hexColour.length) + hexColour
            }
            return hexColour
        }

        fun fromHexString(color: String): Int {
            var c = color
            c = c.replace("#", "")
            return Integer.valueOf(c, 16)
        }

        fun getBackgroundColorOfView(v: View): Int {
            var color = Color.TRANSPARENT
            val background = v.background
            if (background is ColorDrawable) {
                color = background.color
            }
            return color
        }
    }
}