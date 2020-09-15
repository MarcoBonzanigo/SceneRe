package uzh.scenere.helpers

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import java.io.ByteArrayOutputStream
import java.util.*
import android.graphics.drawable.BitmapDrawable
import java.io.BufferedInputStream


class ImageHelper {

    companion object {
        fun getAssetImage(context: Context, filename: String, fileType: String = "png"): Bitmap {
            val assets = context.resources.assets
            val buffer = BufferedInputStream(assets.open("drawable/$filename.$fileType"))
            val bitmap = BitmapFactory.decodeStream(buffer)
            return BitmapDrawable(context.resources, bitmap).bitmap
        }




        fun calculateTopNColors(bitmap: Bitmap, nColors: Int, steps: Int): Array<Int> {
            val colorMap = HashMap<Int, ColorMapEntry>()
            val xStep = (bitmap.width / steps.toDouble()).toInt()
            val yStep = (bitmap.height / steps.toDouble()).toInt()
            var y = 0
            while (y < bitmap.height) {
                var x = 0
                while (x < bitmap.width) {
                    val c = bitmap.getPixel(x, y)
                    if (c == -12627531) { //Adapt to Primary
                        x += xStep
                        continue // taskbar
                    }
                    if (colorMap.containsKey(c)) {
                        colorMap.get(c)?.increment()
                    } else {
                        colorMap.put(c, ColorMapEntry(c))
                    }
                    x += xStep
                }
                y += yStep
            }
            val sortedColorMap = ArrayList<ColorMapEntry>()
            val finalColorList = ArrayList<Int>()
            val filteredColorList = ArrayList<Int>()
            // filter rare colors
            for (e in colorMap.entries) {
                if (e.value.count > 1) {
                    sortedColorMap.add(e.value)
                }
            }
            // sort to rarity
            sortedColorMap.sort()
            for (i in sortedColorMap) {
                finalColorList.add(i.color)
            }
            //filter similar colors
            for (i in finalColorList) {
                var similar = false
                for (ii in filteredColorList) {
                    if (i != ii && ColorHelper.getColorDifferences(i, ii) < 0.1) {
                        similar = true
                    }
                }
                if (!similar) {
                    filteredColorList.add(i)
                }
            }
            return CollectionHelper.subArray(Int::class.java, filteredColorList, 0, if (filteredColorList.size < nColors) filteredColorList.size else nColors)
        }

        /**
         * Returns an average color-intensity, stepDensity defines the coverage of pixels
         */
        fun calculateAverageColorIntensity(bitmap: Bitmap, stepDensity: Double): Double {
            val density = if (stepDensity <= 0 || stepDensity > 0.5) 0.5 else stepDensity
            val stepSize = (1.0 / density).toInt()
            return calculateAverageColorIntensity(bitmap, stepSize)
        }

        /**
         * Returns an average color-intensity, stepSize defines the probing distance
         */
        private fun calculateAverageColorIntensity(bitmap: Bitmap, stepSize: Int): Double {
            var redBucket: Long = 0
            var greenBucket: Long = 0
            var blueBucket: Long = 0
            var pixelCount = 0.0
            val step = if (stepSize <= 0 || stepSize >= if (bitmap.height > bitmap.width) bitmap.width else bitmap.height) 1 else stepSize
            var y = 0
            while (y < bitmap.height) {
                var x = 0
                while (x < bitmap.width) {
                    val c = bitmap.getPixel(x, y)

                    pixelCount++
                    redBucket += Color.red(c)
                    greenBucket += Color.green(c)
                    blueBucket += Color.blue(c)
                    x += step
                }
                y += step
            }
            if (pixelCount > 0) {
                val avgRed = redBucket / pixelCount
                val avgGreen = greenBucket / pixelCount
                val avgBlue = blueBucket / pixelCount
                return (avgRed + avgGreen + avgBlue) / 3
            }
            return 0.0
        }

        fun imageToBytes(bitmap: Bitmap?): ByteArray {
            if (bitmap == null) {
                return ByteArray(0)
            }

            val stream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            return stream.toByteArray()
        }

        fun bytesToImage(bytes: ByteArray?): Bitmap? {
            return if (bytes == null || bytes.isEmpty()) {
                null
            } else BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        }

        fun sizeOf(data: Bitmap?): Int {
            return if (data != null) {
                data.rowBytes * data.height
            } else 0
        }

        /**
         * This method scales the bitmap according to a maximum screenWidth and screenHeight keeping the aspect ratio.
         */
        fun scaleBitmap(bitmap: Bitmap, newWidth: Int, newHeight: Int): Bitmap {
            var width = bitmap.width
            var height = bitmap.height

            when {
                width > height -> {
                    // Landscape
                    val ratio = width.toFloat() / newWidth
                    width = newWidth
                    height = (height / ratio).toInt()
                }
                height > width -> {
                    // Portrait
                    val ratio = height.toFloat() / newHeight
                    height = newHeight
                    width = (width / ratio).toInt()
                }
                else -> {
                    // Square
                    height = newHeight
                    width = newWidth
                }
            }

            return Bitmap.createScaledBitmap(bitmap, width, height, true)
        }

        fun getImageFromUrl(url: String): Bitmap? {
            return bytesToImage(NetworkHelper.getBytesFromUrl(url))
        }
    }

    class ColorMapEntry(val color: Int) : Comparable<ColorMapEntry> {
        override fun compareTo(other: ColorMapEntry): Int {
            val comparedCount = other.count
            return if (comparedCount > count) 1 else if (comparedCount == count) 0 else -1
        }

        var count: Int = 0
            private set

        init {
            this.count = 1
        }

        fun increment() {
            this.count++
        }
    }
}