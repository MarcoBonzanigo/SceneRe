package uzh.scenere.datamodel.pdf

class PdfLineConfiguration private constructor(){

    enum class Alignment {
        LEFT, RIGHT, CENTER, BOTTOM_LEFT, BOTTOM_RIGHT
    }

    enum class Direction {
        FORWARD, BACKWARD
    }

    private var text: String? = null
    private var alignment = Alignment.LEFT
    private var marginTop = 0f
    private var marginBottom = 0f
    private var marginLeft = 0f
    private var marginRight = 0f
    private var line = 0f
    private var direction: Direction? = null
    private var boldSegmentStart = -1
    private var boldSegmentEnd = -1

    fun getAlignment(): Alignment {
        return alignment
    }

    fun getMarginTop(): Float {
        return marginTop
    }

    fun getMarginBottom(): Float {
        return marginBottom
    }

    fun getMarginLeft(): Float {
        return marginLeft
    }

    fun getMarginRight(): Float {
        return marginRight
    }

    fun getLine(): Float {
        return line
    }

    fun getText(): String {
        return text!!
    }

    fun getDirection(): Direction? {
        return direction
    }

    class Builder(private val text: String, alignment: Alignment) {
        private var alignment = Alignment.LEFT
        private var marginTop = 0f
        private var marginBottom = 0f
        private var marginLeft = 0f
        private var marginRight = 0f
        private var line = 0f
        private var direction = Direction.FORWARD
        private var boldSegmentStart = -1
        private var boldSegmentEnd = -1

        init {
            this.alignment = alignment
        }

        fun withMargin(top: Float, bottom: Float, left: Float, right: Float): Builder {
            this.marginTop = top
            this.marginBottom = bottom
            this.marginLeft = left
            this.marginRight = right
            return this
        }

        fun onLine(line: Float): Builder {
            this.line = line
            return this
        }

        fun withDirection(direction: Direction): Builder {
            this.direction = direction
            return this
        }

        fun withBoldSection(start: Int, end: Int): Builder {
            this.boldSegmentStart = start
            this.boldSegmentEnd = end
            return this
        }

        fun build(): PdfLineConfiguration {
            val conf = PdfLineConfiguration()
            conf.text = this.text
            conf.alignment = this.alignment
            conf.marginTop = this.marginTop
            conf.marginBottom = this.marginBottom
            conf.marginLeft = this.marginLeft
            conf.marginRight = this.marginRight
            conf.line = this.line
            conf.direction = this.direction
            conf.boldSegmentStart = this.boldSegmentStart
            conf.boldSegmentEnd = this.boldSegmentEnd
            return conf
        }
    }

    fun setAlignment(alignment: Alignment) {
        this.alignment = alignment
    }

    fun setLine(line: Float) {
        this.line = line
    }

    fun setMargins(top: Float, bottom: Float, left: Float, right: Float) {
        this.marginTop = top
        this.marginBottom = bottom
        this.marginLeft = left
        this.marginRight = right
    }

    fun setBoldSegment(start: Int, end: Int) {
        this.boldSegmentStart = start
        this.boldSegmentEnd = end
    }

    fun getBoldSegmentStart(): Int {
        return this.boldSegmentStart
    }

    fun getBoldSegmentEnd(): Int {
        return this.boldSegmentEnd
    }


}