package uzh.scenere.views

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.support.v4.content.ContextCompat
import android.view.View
import uzh.scenere.R
import uzh.scenere.helpers.getColorWithStyle

@SuppressLint("ViewConstructor")
class GraphView (context: Context,
                 var values: FloatArray,
                 var title: String,
                 var horizontalLabels: Array<String>,
                 var verticalLabels: Array<String>,
                 var type: GraphType,
                 var max: Float?,
                 var min: Float?,
                 var span: Float?) : View(context) {
        enum class GraphType(){
            BAR, LINE
        }

        init {
            if (horizontalLabels.size == 1 && verticalLabels.size == 1){
                horizontalLabels = arrayOf("", "", "", "", "", horizontalLabels[0], "", "", "", "", "")
                verticalLabels = arrayOf("", "", "", "", "", verticalLabels[0], "", "", "", "", "")
            }
        }
        private val space: Float = 1.05f
        private val paint: Paint = Paint()
        private val adaptive: Boolean = max==null

        override fun onDraw(canvas: Canvas) {
            val border = 20f
            val horizontalStart = border * 2
            val height = height.toFloat()
            val width = (width - 1).toFloat()
            val max = getMax()
            val min = getMin()
            val graphHeight = height - 2 * border
            val graphWidth = width - 2 * border

            paint.textAlign = Paint.Align.LEFT
            val verticalSize = verticalLabels.size - 1
            for (i in verticalLabels.indices) {
                paint.color = getColorWithStyle(context,R.color.srePrimary)

                val y = graphHeight / verticalSize * i + border
                canvas.drawLine(horizontalStart, y, width, y, paint)
                paint.color = getColorWithStyle(context,R.color.sreBlack)
                canvas.drawText(verticalLabels[i], 0f, y, paint)
            }
            val horizontalSize = horizontalLabels.size - 1
            for (i in horizontalLabels.indices) {
                paint.color = getColorWithStyle(context,R.color.srePrimary)
                val x = graphWidth / horizontalSize * i + horizontalStart
                canvas.drawLine(x, height - border, x, border, paint)
                paint.textAlign = Paint.Align.CENTER
                if (i == horizontalLabels.size - 1)
                    paint.textAlign = Paint.Align.RIGHT
                if (i == 0)
                    paint.textAlign = Paint.Align.LEFT
                paint.color = getColorWithStyle(context,R.color.sreBlack)
                canvas.drawText(horizontalLabels[i], x, height - 4, paint)
            }

            paint.textAlign = Paint.Align.CENTER
            canvas.drawText(title, graphWidth / 2 + horizontalStart, border - 4, paint)

            if (max != min) {
                val negativeColor = getColorWithStyle(context, R.color.sreSecondaryLight)
                val positiveColor = getColorWithStyle(context, R.color.srePrimaryLight)
                if (type == GraphType.BAR) {
                    val dataLength = values.size.toFloat()
                    val colWidth = (width - 2 * border) / dataLength
                    for (v in values.indices) {
                        paint.color = if (values[v]>0) positiveColor else negativeColor
                        val ratio = Math.abs(values[v]).div(max)
                        val h = graphHeight * ratio
                        canvas.drawRect(v * colWidth + horizontalStart, border - h + graphHeight, v * colWidth + horizontalStart + (colWidth - 1), height - (border - 1), paint)
                    }
                } else {
                    val diff = max - min
                    val dataLength = values.size.toFloat()
                    val colWidth = (width - 2 * border) / dataLength
                    val halfCol = colWidth / 2
                    var lastH = 0f
                    for (v in values.indices) {
                        val value = values[v].minus(min)
                        paint.color = if (value>0) positiveColor else negativeColor
                        val ratio = Math.abs(value).div(diff)
                        val h = graphHeight * ratio
                        if (v > 0)
                            canvas.drawLine((v - 1) * colWidth + (horizontalStart + 1) + halfCol, border - lastH + graphHeight, v * colWidth + (horizontalStart + 1) + halfCol, border - h + graphHeight, paint)
                        lastH = h
                    }
                }
            }
        }

        private fun getMax(): Float {
            if (max != null && !adaptive) return max!!
            var largest = Integer.MIN_VALUE.toFloat()
            for (value in values) {
                val finValue = if (type == GraphType.BAR) Math.abs(value) else value
                largest = if (finValue*space > largest) finValue*space else largest
            }
            if (span == null) {
                return largest
            }
            if (span!! < 0) { //percentage
                largest -= largest / 100 * span!!
                return largest
            }
            largest += span!!
            return largest
        }

        private fun getMin(): Float {
            if (type == GraphType.BAR) return 0f
            if (min != null) return min!!
            var smallest = Integer.MAX_VALUE.toFloat()
            for (i in values.indices)
                if (values[i] < smallest)
                    smallest = values[i]
            if (span == null) {
                return smallest
            }
            if (span!! < 0) { //percentage
                smallest += smallest / 100 * span!!
                return smallest
            }
            smallest -= span!!
            return smallest
        }
    }