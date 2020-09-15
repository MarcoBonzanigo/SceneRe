package uzh.scenere.views

import android.animation.*
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Rect
import android.graphics.Typeface
import android.support.v4.content.ContextCompat
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.View.OnTouchListener
import android.view.ViewGroup
import android.view.ViewTreeObserver.OnGlobalLayoutListener
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import uzh.scenere.R
import uzh.scenere.helpers.*


class SwipeButton(context: Context, attributeSet: AttributeSet?, defStyleAttr: Int, defStyleRes: Int) : LinearLayout(context, attributeSet, defStyleAttr, defStyleRes) {
    enum class SwipeButtonState {
        LEFT, UP, DOWN, RIGHT, MIDDLE, LONG_CLICK
    }

    enum class SwipeButtonMode {
        DOUBLE, TRIPLE, QUADRUPLE
    }

    enum class Direction {
        X, Y
    }
    //Data
    public var dataObject: Any? = null
    public var outputObject: TextView? = null
    //Function
    private var exec: SwipeButtonExecution = object : SwipeButtonExecution {}
    var state: SwipeButtonState = SwipeButtonState.MIDDLE
    private var mode: SwipeButtonMode = SwipeButtonMode.QUADRUPLE
    //Grid
    private var topLayout: LinearLayout? = null
    private var middleLayout: RelativeLayout? = null
    private var bottomBackgroundLayout: RelativeLayout? = null
    //Button State
    private var initialX = 0f
    private var initialY = 0f
    private var initialEventY = -1f
    private var initialEventX = -1f
    private var initialButtonWidth = 0
    private var sliderButton: SwipeButtonIconTextView? = null

    private var sliderLane: RelativeLayout? = null
    private var firstAction: Boolean = true
    private val animationDuration: Long = 200L
    //Layout
    private var labelText: TextView? = null
    private var topIconText: TextView? = null
    private var topLabelText: TextView? = null
    private var topBg: TextView? = null
    private var bottomIconText: TextView? = null
    private var bottomLabelText: TextView? = null
    private var bottomBg: TextView? = null
    private var leftIconText: TextView? = null
    private var rightIconText: TextView? = null
    //Position
    private var firstPosition: Boolean = false
    //Configuration
    private var lIdx: Int = 0
    private var rIdx: Int = 1
    private var tIdx: Int = 2
    private var bIdx: Int = 3
    private var cIdx: Int = 4
    private var icons: IntArray = intArrayOf(R.string.icon_delete, R.string.icon_edit, R.string.icon_comment, R.string.icon_lock,R.string.icon_null)
    private var react: BooleanArray = booleanArrayOf(true, true, true, true, false)
    private var colors: IntArray = intArrayOf(getColorWithStyle(context,R.color.srePrimaryPastel), getColorWithStyle(context,R.color.srePrimaryPastel), getColorWithStyle(context,R.color.srePrimaryPastel), getColorWithStyle(context,R.color.srePrimaryPastel), getColorWithStyle(context,R.color.srePrimaryPastel))
    //State
    private var initialized: Boolean = false
    private var active: Boolean = false
    private var activeColor: Int = getColorWithStyle(context,R.color.srePrimaryPastel)
    private var inactiveColor: Int = ContextCompat.getColor(context,R.color.srePrimaryDisabled)
    private var individualColor: Boolean = false
    var interacted: Boolean = false
    private var autoCollapse: Boolean = false
    //Measurement
    private var dpiText = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 5f, resources.displayMetrics)
    private var dpiPadding = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8f, resources.displayMetrics).toInt()
    private var dpiPaddingIconLabelTop = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 9f, resources.displayMetrics).toInt()
    private var dpiPaddingIconLabelLeft = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 30f, resources.displayMetrics).toInt()
    private var dpiPaddingSmall = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 4f, resources.displayMetrics).toInt()
    private val dpiSliderMargin = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 10f, resources.displayMetrics).toInt()
    private val dpiHeight = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 150f, resources.displayMetrics).toInt()
    private val dpiMargin = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 5f, resources.displayMetrics).toInt()

    init {
        init(context)
        ISreView.adaptStyle(context,this)
    }

    //XML Constructors
    constructor(context: Context) : this(context, null, -1, -1) {
        init(context)
    }

    constructor(context: Context, attributeSet: AttributeSet?) : this(context, attributeSet, -1, -1) {
        init(context)
    }

    constructor(context: Context, attributeSet: AttributeSet?, defStyleAttr: Int) : this(context, attributeSet, defStyleAttr, -1) {
        init(context)
    }

    //Inline Constructors
    constructor(context: Context, label: String?) : this(context, null, -1, -1) {
        init(context)
        labelText?.text = label
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun init(context: Context) {
        if (initialized) {
            return
        }

        //Master Layout Params
        this.orientation = LinearLayout.VERTICAL
        adaptMasterLayoutParams()

        //Grid, Top Box (30%)
        val topLayout = LinearLayout(context)
        this.topLayout = topLayout
        topLayout.background = ContextCompat.getDrawable(context, R.drawable.sre_swipe_button_bg_top)
        val topLayoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0)
        topLayoutParams.weight = 2f
        topLayout.layoutParams = topLayoutParams
        addView(topLayout)

        //Grid, Middle Box (60%)
        val middleLinearLayout = LinearLayout(context)
        val middleLayoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0)
        middleLayoutParams.weight = 6f
        middleLinearLayout.layoutParams = middleLayoutParams
        addView(middleLinearLayout)

        //Grid, Bottom Box (10%)
        val bottomLinearLayout = LinearLayout(context)
        val bottomLayoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0)
        bottomLinearLayout.background = ContextCompat.getDrawable(context, R.drawable.sre_swipe_button_bg_bottom)
        bottomLayoutParams.weight = 1f
        bottomLinearLayout.layoutParams = bottomLayoutParams
        addView(bottomLinearLayout)

        //Content Wrapper for Middle Box
        val middleLayout = RelativeLayout(context)
        this.middleLayout = middleLayout
        middleLinearLayout.addView(middleLayout)

        //Background
        val bottomBackgroundLayout = RelativeLayout(context)
        this.bottomBackgroundLayout = bottomBackgroundLayout
        val backgroundLayoutParams = createParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT, RelativeLayout.CENTER_HORIZONTAL)
        bottomBackgroundLayout.background = ContextCompat.getDrawable(context, R.drawable.sre_swipe_button_bg_mid)
        middleLayout.addView(bottomBackgroundLayout, backgroundLayoutParams)

        //Label
        val labelText = TextView(context)
        this.labelText = labelText
        val labelTextParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        labelText.layoutParams = labelTextParams
        labelText.textAlignment = View.TEXT_ALIGNMENT_CENTER
        labelText.gravity = Gravity.CENTER
        topLayout.addView(labelText, labelTextParams)

        //Slider-Lane
        val sliderLane = RelativeLayout(context)
        this.sliderLane = sliderLane
        val sliderLaneParams = createParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, RelativeLayout.CENTER_IN_PARENT)
        sliderLaneParams.marginStart = dpiSliderMargin
        sliderLaneParams.marginEnd = dpiSliderMargin
        sliderLane.background = ContextCompat.getDrawable(context, R.drawable.sre_swipe_button_slider_mid)
        middleLayout.addView(sliderLane, sliderLaneParams)

        //Slider-Button
        val sliderButton = SwipeButtonIconTextView(context)
        this.sliderButton = sliderButton
        val sliderButtonParams = createParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, RelativeLayout.ALIGN_PARENT_LEFT, RelativeLayout.CENTER_VERTICAL)
        sliderButton.text = null
        sliderButton.setPadding(dpiPadding * 8, dpiPadding, dpiPadding * 8, dpiPadding)
        sliderButton.background = ContextCompat.getDrawable(context, R.drawable.sre_swipe_button_slider_button)
        middleLayout.addView(sliderButton, sliderButtonParams)

        //Top Icon
        val topIconText = SwipeButtonIconTextView(context)
        this.topIconText = topIconText
        val topIconTextParams = createParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, RelativeLayout.ALIGN_PARENT_TOP, RelativeLayout.CENTER_HORIZONTAL)
        topIconTextParams.setMargins(dpiPaddingSmall,dpiPaddingSmall,dpiPaddingSmall,dpiPaddingSmall)
        topIconText.background = ContextCompat.getDrawable(context, R.drawable.sre_swipe_button_slider_top)

        //Top Label
        val topLabelText = SwipeButtonIconTextView(context)
        this.topLabelText = topLabelText
        val topLabelTextParams = createParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, RelativeLayout.ALIGN_PARENT_TOP, RelativeLayout.CENTER_HORIZONTAL)
        topLabelTextParams.setMargins(dpiPaddingSmall,dpiPaddingSmall,dpiPaddingSmall,dpiPaddingSmall)
        topLabelText.setPadding(dpiPaddingIconLabelLeft,dpiPaddingIconLabelTop,0,0)
        topLabelText.setTypeface(null,Typeface.BOLD)

        //Top Background
        val topBg = SwipeButtonIconTextView(context)
        this.topBg = topBg
        val topBgParams = createParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, RelativeLayout.ALIGN_PARENT_TOP, RelativeLayout.CENTER_HORIZONTAL)
        topBgParams.setMargins(dpiPaddingSmall,dpiPaddingSmall,dpiPaddingSmall,dpiPaddingSmall)
        topBg.setPadding(dpiPadding * 4, dpiPadding, dpiPadding * 4, dpiPadding*2)
        topBg.background = ContextCompat.getDrawable(context, R.drawable.sre_swipe_button_slider_top)
        bottomBackgroundLayout.addView(topBg, topBgParams)
        bottomBackgroundLayout.addView(topIconText, topIconTextParams)
        bottomBackgroundLayout.addView(topLabelText, topLabelTextParams)

        //Bottom Icon
        val bottomIconText = SwipeButtonIconTextView(context)
        this.bottomIconText = bottomIconText
        val bottomIconTextParams = createParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.CENTER_HORIZONTAL)
        bottomIconTextParams.setMargins(dpiPaddingSmall,dpiPaddingSmall,dpiPaddingSmall,dpiPaddingSmall)
        bottomIconText.background = ContextCompat.getDrawable(context, R.drawable.sre_swipe_button_slider_bottom)

        //Top Label
        val bottomLabelText = SwipeButtonIconTextView(context)
        this.bottomLabelText = bottomLabelText
        val bottomLabelTextParams = createParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.CENTER_HORIZONTAL)
        bottomLabelTextParams.setMargins(dpiPaddingSmall,dpiPaddingSmall,dpiPaddingSmall,dpiPaddingSmall)
        bottomLabelText.setPadding(dpiPaddingIconLabelLeft,dpiPaddingIconLabelTop,0,0)
        bottomLabelText.setTypeface(null,Typeface.BOLD)

        //Bottom Background
        val bottomBg = SwipeButtonIconTextView(context)
        this.bottomBg = bottomBg
        val bottomBgParams = createParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.CENTER_HORIZONTAL)
        bottomBgParams.setMargins(dpiPaddingSmall,dpiPaddingSmall,dpiPaddingSmall,dpiPaddingSmall)
        bottomBg.setPadding(dpiPadding * 4, dpiPadding*2, dpiPadding * 4, dpiPadding)
        bottomBg.background = ContextCompat.getDrawable(context, R.drawable.sre_swipe_button_slider_bottom)
        bottomBackgroundLayout.addView(bottomBg, bottomBgParams)
        bottomBackgroundLayout.addView(bottomIconText, bottomIconTextParams)
        bottomBackgroundLayout.addView(bottomLabelText, bottomLabelTextParams)

        //Left Icon
        val leftIconText = SwipeButtonIconTextView(context)
        this.leftIconText = leftIconText
        val leftIconTextParams = createParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, RelativeLayout.ALIGN_PARENT_LEFT)
        leftIconText.setTextSize(TypedValue.COMPLEX_UNIT_DIP, dpiText)
        sliderLane.addView(leftIconText, leftIconTextParams)

        //Right Icon
        val rightIconText = SwipeButtonIconTextView(context)
        this.rightIconText = rightIconText
        val rightIconTextParams = createParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, RelativeLayout.ALIGN_PARENT_RIGHT)
        rightIconText.setTextSize(TypedValue.COMPLEX_UNIT_DIP, dpiText)
        sliderLane.addView(rightIconText, rightIconTextParams)

        //Touch Listener
        middleLayout.setOnTouchListener(getButtonTouchListener())

        //Initializer
        val layout = this
        val viewTreeObserver = layout.viewTreeObserver
        viewTreeObserver.addOnGlobalLayoutListener(object : OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                layout.viewTreeObserver.removeOnGlobalLayoutListener(this)
                val width = layout.measuredWidth
                val initialX = width / 2f - sliderButton.width / 2f
                layout.initialX = initialX
                sliderButton.x = initialX
                layout.initialY = sliderButton.y
            }
        })
        //Update Buttons
        updateViews(true)
        //Finish Initialization
        initialized = true
    }

    private fun getButtonTouchListener(): View.OnTouchListener? {
        return OnTouchListener { _, event ->
            val hitBox = Rect()
            sliderButton?.getHitRect(hitBox)
            val x = NumberHelper.nvl(event.x.toInt(), 0f).toInt()
            val y = NumberHelper.nvl(event.y.toInt(), 0f).toInt()
            val margin = 0.25f //Easier clickable with bigger Hitbox
            if (!(x * (1 + margin) >= hitBox.left && x * (1 - margin) < hitBox.right
                            && y * (1 + margin) >= hitBox.top && y * (1 - margin) < hitBox.bottom)
                    && event.action != MotionEvent.ACTION_UP) {
                return@OnTouchListener true
            }
            //Calculation Values
            val sliderWidth = NumberHelper.nvl(sliderButton?.width, 0f).toFloat()
            val sliderHeight = NumberHelper.nvl(sliderButton?.height, 0f).toFloat()
            val sliderX = NumberHelper.nvl(sliderButton?.x, 0f).toFloat()
            val sliderY = NumberHelper.nvl(sliderButton?.y, 0f).toFloat()
            val h = NumberHelper.nvl(middleLayout?.height, 0f).toFloat()
            val w = NumberHelper.nvl(middleLayout?.width, 0f).toFloat() - dpiSliderMargin
            val sliderSpanX = (width - sliderWidth) / 2 - (dpiSliderMargin * 2)
            val sliderSpanY = (height - sliderHeight) / 2
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    val rect = Rect()
                    sliderButton?.getHitRect(rect)
                    if (rect.contains(event.x.toInt(), event.y.toInt())) {
                        interacted = true
                        initialEventY = event.y
                        initialEventX = event.x
                        if (initialX == sliderX && initialY == sliderY){
                            initialButtonWidth = sliderWidth.toInt()
                        }
                        return@OnTouchListener true
                    }
                    return@OnTouchListener false
                }
                MotionEvent.ACTION_MOVE -> {
                    if (Math.abs(initialEventX - event.x) > Math.abs(initialEventY - event.y) && sliderButton?.y == initialY) {
                        /*HORIZONTAL*/
                        //Handle text transparency
                        val alpha = 1 - 1.3f * Math.abs(sliderSpanX - sliderX) / sliderSpanX
                        topIconText?.alpha = alpha
                        bottomIconText?.alpha = alpha
                        leftIconText?.alpha = alpha
                        rightIconText?.alpha = alpha
                        //Handle general slide to the left || right
                        if (event.x > sliderWidth / 2 + dpiSliderMargin && event.x + sliderWidth / 2 < w) {
                            sliderButton?.x = (event.x - sliderWidth / 2)
                        }
                        //Handle corner conditions on the left
                        if (event.x < sliderWidth / 2 + dpiSliderMargin && sliderX + sliderWidth / 2 + dpiSliderMargin > 0) {
                            sliderButton?.x = dpiSliderMargin.toFloat()
                        }
                        //Handle corner conditions on the right
                        if (event.x + sliderWidth / 2 > w && sliderX + sliderWidth / 2 < w) {
                            sliderButton?.x = (w - sliderWidth)
                        }
                    } else if (Math.abs(initialEventX - event.x) < Math.abs(initialEventY - event.y) && sliderButton?.x == initialX && mode == SwipeButtonMode.QUADRUPLE) {
                        /*VERTICAL*/
                        //Handle text transparency
                        val alpha = 1 - 1.3f * Math.abs((sliderSpanY / 2) - sliderY) / (sliderSpanY / 2)
                        topIconText?.alpha = alpha
                        bottomIconText?.alpha = alpha
                        leftIconText?.alpha = alpha
                        rightIconText?.alpha = alpha
                        //Handle general slide to up || down
                        if (event.y > sliderHeight / 2 && event.y + sliderHeight / 2 < h) {
                            sliderButton?.y = (event.y - sliderHeight / 2)
                        }
                        //Handle corner conditions on up
                        if (event.y < sliderHeight / 2 && sliderY + sliderHeight / 2 > 0) {
                            sliderButton?.y = 0f
                        }
                        //Handle corner conditions on down
                        if (event.y > (h - sliderHeight / 2) && sliderY + sliderHeight / 2 < h) {
                            sliderButton?.y = h - sliderHeight
                        }
                    }
                    return@OnTouchListener true
                }
                MotionEvent.ACTION_UP -> {
                    interacted = false
                    if (active) {
                        collapse()
                    } else {
                        when {
                            sliderX + sliderWidth > w * 0.85 && react[rIdx] -> expand(icons[rIdx], SwipeButtonState.RIGHT)
                            sliderX < w * 0.15 && react[lIdx] -> expand(icons[lIdx], SwipeButtonState.LEFT)
                            sliderY + sliderHeight > h * 0.975 && react[bIdx] -> expand(icons[bIdx], SwipeButtonState.DOWN)
                            sliderY < h * 0.025 && react[tIdx] -> expand(icons[tIdx], SwipeButtonState.UP)
                            react[cIdx]&&Math.abs(sliderX-initialX)<(sliderSpanX/10) && Math.abs(sliderY-initialY)<(sliderSpanY/10) &&event.eventTime-event.downTime>500 -> {
                                state = SwipeButtonState.LONG_CLICK
                                execute()
                            }
                            else -> reset()
                        }
                    }
                    return@OnTouchListener true
                }
            }
            false
        }
    }

    //Configuration
    fun setButtonIcons(lIcon: Int?, rIcon: Int?, tIcon: Int?, bIcon: Int?, cIcon: Int?): SwipeButton {
        icons[lIdx] = lIcon ?: icons[lIdx]
        icons[rIdx] = rIcon ?: icons[rIdx]
        icons[tIdx] = tIcon ?: icons[tIdx]
        icons[bIdx] = bIcon ?: icons[bIdx]
        icons[cIdx] = cIcon ?: icons[cIdx]
        return this
    }

    fun setColors(active: Int, inactive: Int): SwipeButton {
        individualColor = false
        activeColor = active
        inactiveColor = inactive
        return this
    }

    fun setButtonStates(lActive: Boolean, rActive: Boolean, tActive: Boolean, bActive: Boolean, cActive: Boolean = false): SwipeButton {
        react[lIdx] = lActive
        react[rIdx] = rActive
        react[tIdx] = tActive
        react[bIdx] = bActive
        react[cIdx] = cActive
        return this
    }

    fun setIndividualButtonColors(lColor: Int, rColor: Int, tColor: Int, bColor: Int, cColor: Int = getColorWithStyle(context,R.color.srePrimaryPastel)): SwipeButton {
        individualColor = true
        colors[lIdx] = lColor
        colors[rIdx] = rColor
        colors[tIdx] = tColor
        colors[bIdx] = bColor
        colors[cIdx] = cColor
        return this
    }

    fun setButtonMode(mode: SwipeButtonMode): SwipeButton {
        this.mode = mode
        return this
    }

    fun setExecutable(executable: SwipeButtonExecution): SwipeButton {
        exec = executable
        return this
    }

    fun setCounter(topCounter: Int?, bottomCounter: Int?, color: Int = ContextCompat.getColor(context,R.color.srePrimaryWarn)): SwipeButton {
        topLabelText?.text = topCounter?.toString()
        bottomLabelText?.text = bottomCounter?.toString()
        topLabelText?.setTextColor(color)
        bottomLabelText?.setTextColor(color)
        return this
    }

    fun setAutoCollapse(isAutoCollapse: Boolean): SwipeButton{
        autoCollapse = isAutoCollapse
        return this
    }

    fun setText(text: String): SwipeButton{
        labelText?.text = text
        return this
    }

    fun getText(): String?{
        return labelText?.text.toString()
    }

    fun setFirstPosition(): SwipeButton{
        firstPosition = true
        return this
    }

    fun isFirstPosition(): Boolean{
        return firstPosition
    }

    fun adaptMasterLayoutParams(borderless: Boolean = false): SwipeButton {
        val masterLayoutParams = LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dpiHeight)
        if (!borderless) {
            masterLayoutParams.marginStart = dpiMargin//TODO Adapt to existing scroll bar or not
            masterLayoutParams.marginEnd = dpiMargin//+NumberHelper.nvl(context?.resources?.getDimension(R.dimen.dimScrollbar),0).toInt()
            masterLayoutParams.topMargin = dpiMargin / 5
        }
        masterLayoutParams.weight = 12f
        layoutParams = masterLayoutParams
        return this
    }

    fun updateViews(init: Boolean): SwipeButton {
        if (init) {
            var modifier: Float = 1.5f
            if (mode == SwipeButtonMode.DOUBLE) {
                dpiText *= 1.5f
                dpiPadding *= 2
                topIconText?.visibility = View.GONE
                topLabelText?.visibility = View.GONE
                topBg?.visibility = View.GONE
                bottomIconText?.visibility = View.GONE
                bottomLabelText?.visibility = View.GONE
                bottomBg?.visibility = View.GONE
                modifier = 2f
            }
            leftIconText?.setTextSize(TypedValue.COMPLEX_UNIT_DIP, dpiText)
            rightIconText?.setTextSize(TypedValue.COMPLEX_UNIT_DIP, dpiText)

            topIconText?.setTextSize(TypedValue.COMPLEX_UNIT_DIP, dpiText)
            bottomIconText?.setTextSize(TypedValue.COMPLEX_UNIT_DIP, dpiText)
            sliderButton?.setTextSize(TypedValue.COMPLEX_UNIT_DIP, dpiText * modifier)
        }
        //Color & States
        sliderButton?.setTextColor(activeColor)
        labelText?.setTextColor(activeColor)
        leftIconText?.setTextColor(if (individualColor) colors[lIdx] else (if (react[lIdx]) activeColor else inactiveColor))
        rightIconText?.setTextColor(if (individualColor) colors[rIdx] else (if (react[rIdx]) activeColor else inactiveColor))
        sliderButton?.setTextColor(if (individualColor) colors[cIdx] else activeColor)
        topIconText?.setTextColor(if (individualColor) colors[tIdx] else (if (react[tIdx]) activeColor else inactiveColor))
        bottomIconText?.setTextColor(if (individualColor) colors[bIdx] else (if (react[bIdx]) activeColor else inactiveColor))
        //Text
        leftIconText?.text = resources.getText(icons[lIdx])
        rightIconText?.text = resources.getText(icons[rIdx])
        topIconText?.text = resources.getText(icons[tIdx])
        bottomIconText?.text = resources.getText(icons[bIdx])
        sliderButton?.text = resources.getText(icons[cIdx])
        //Icons
        rightIconText?.setPadding(dpiPadding, dpiPadding, dpiPadding, dpiPadding)
        leftIconText?.setPadding(dpiPadding, dpiPadding, dpiPadding, dpiPadding)
        topIconText?.setPadding(dpiPadding, dpiPaddingSmall, dpiPadding, dpiPadding)
        bottomIconText?.setPadding(dpiPadding, dpiPadding, dpiPadding,dpiPaddingSmall)
        ISreView.adaptStyle(context,this)
        return this
    }

    //Animations
    private fun expand(icon: Int, state: SwipeButtonState) {
        this.state = state
        val sliderX = NumberHelper.nvl(sliderButton?.x, 0f).toFloat()
        val sliderWidth = NumberHelper.nvl(sliderButton?.width, 0).toInt()
        val positionAnimator = ValueAnimator.ofFloat(sliderX, dpiSliderMargin.toFloat())
        positionAnimator.addUpdateListener {
            val x = positionAnimator.animatedValue as Float
            sliderButton?.x = x
        }
        val widthAnimator = ValueAnimator.ofInt(sliderWidth, width - (dpiSliderMargin * 2))
        widthAnimator.addUpdateListener {
            val params = sliderButton?.layoutParams
            params?.width = widthAnimator.animatedValue as Int
            sliderButton?.layoutParams = params
        }
        val positionYAnimator = createAnimator(Direction.Y, animationDuration)
        val listener = object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                super.onAnimationEnd(animation)
                active = true
                if (!autoCollapse){
                    sliderButton?.text = resources.getText(icon)
                }
            }
        }

        playAnimations(listener, positionAnimator, widthAnimator, positionYAnimator)
        execute()
    }

    fun collapse() {
        this.state = SwipeButtonState.MIDDLE
        initialEventX = -1f
        initialEventY = -1f
        val sliderWidth = NumberHelper.nvl(sliderButton?.width, 0).toInt()
        val widthAnimator = ValueAnimator.ofInt(sliderWidth, initialButtonWidth)
        val positionAnimator = ValueAnimator.ofFloat(0f, initialX)
        positionAnimator.addUpdateListener {
            val x = positionAnimator.animatedValue as Float
            sliderButton?.x = x
        }
        widthAnimator.addUpdateListener {
            val params = sliderButton?.layoutParams
            params?.width = widthAnimator.animatedValue as Int
            sliderButton?.layoutParams = params
        }
        widthAnimator.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                super.onAnimationEnd(animation)
                active = false
                reset()
            }
        })

        val leftTextAnimator = ObjectAnimator.ofFloat(leftIconText as TextView, "alpha", 1f)
        val rightTextAnimator = ObjectAnimator.ofFloat(rightIconText as TextView, "alpha", 1f)
        val topTextAnimator = ObjectAnimator.ofFloat(topIconText as TextView, "alpha", 1f)
        val bottomTextAnimator = ObjectAnimator.ofFloat(bottomIconText as TextView, "alpha", 1f)
        val listener = object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                super.onAnimationEnd(animation)
                sliderButton?.text = resources.getText(icons[cIdx])
            }
        }
        playAnimations(listener, positionAnimator, widthAnimator, leftTextAnimator, rightTextAnimator, topTextAnimator, bottomTextAnimator)
        execute()
    }

    fun reset() {
        val positionXAnimator = createAnimator(Direction.X, animationDuration)
        val positionYAnimator = createAnimator(Direction.Y, animationDuration)

        val rightTextAnimator = ObjectAnimator.ofFloat(rightIconText as TextView, "alpha", 1f)
        val leftTextAnimator = ObjectAnimator.ofFloat(leftIconText as TextView, "alpha", 1f)
        val topTextAnimator = ObjectAnimator.ofFloat(topIconText as TextView, "alpha", 1f)
        val bottomTextAnimator = ObjectAnimator.ofFloat(bottomIconText as TextView, "alpha", 1f)

        playAnimations(null, positionXAnimator, positionYAnimator, leftTextAnimator, rightTextAnimator, topTextAnimator, bottomTextAnimator)
    }

    private fun createAnimator(dir: Direction, duration: Long): ValueAnimator? {
        val sliderPos = NumberHelper.nvl(if (dir == Direction.X) sliderButton?.x else sliderButton?.y, 0f).toFloat()
        val positionAnimator = ValueAnimator.ofFloat(sliderPos, if (dir == Direction.X) initialX else initialY)
        positionAnimator.interpolator = AccelerateDecelerateInterpolator()
        positionAnimator.addUpdateListener {
            val value = positionAnimator.animatedValue as Float
            if (dir == Direction.X) {
                sliderButton?.x = value
            } else {
                sliderButton?.y = value
            }
        }
        positionAnimator.duration = duration
        return positionAnimator
    }

    private fun <T : Animator?> playAnimations(listener: AnimatorListenerAdapter?, vararg animators: T) {
        val animatorSet = AnimatorSet()
        if (listener != null) {
            animatorSet.addListener(listener)
        }
        animatorSet.playTogether(animators.asList())
        animatorSet.start()
    }

    private fun createParams(width: Int, height: Int, vararg rules: Int): RelativeLayout.LayoutParams {
        val params = RelativeLayout.LayoutParams(width, height)
        for (rule in rules.asList()) {
            params.addRule(rule)
        }
        return params
    }

    private fun execute() {
        if (autoCollapse && !CollectionHelper.oneOf(state,SwipeButtonState.MIDDLE,SwipeButtonState.LONG_CLICK)){
            android.os.Handler().postDelayed({ collapse() }, 250)
        }
        when (state) {
            SwipeButtonState.LEFT -> exec.execLeft()
            SwipeButtonState.RIGHT -> exec.execRight()//Toast.makeText(context,"",Toast.LENGTH_SHORT).show()
            SwipeButtonState.UP -> exec.execUp()
            SwipeButtonState.DOWN -> exec.execDown()
            SwipeButtonState.MIDDLE -> exec.execReset()
            SwipeButtonState.LONG_CLICK -> {
                exec.execLongClick()
                state = SwipeButtonState.MIDDLE
            }
        }
    }

    interface SwipeButtonExecution {
        fun execLeft() {/*NOP*/}
        fun execRight() {/*NOP*/}
        fun execUp() {/*NOP*/}
        fun execDown() {/*NOP*/}
        fun execReset() {/*NOP*/}
        fun execLongClick(){/*NOP*/}
    }

    class SwipeButtonIconTextView(context: Context): TextView(context){
        init {
            typeface = Typeface.createFromAsset(context.assets, "FontAwesome900.otf")
            gravity = Gravity.CENTER
        }
    }
}