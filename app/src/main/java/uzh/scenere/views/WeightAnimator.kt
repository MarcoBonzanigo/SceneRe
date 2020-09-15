package uzh.scenere.views

import android.animation.ObjectAnimator
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import kotlinx.android.synthetic.main.scroll_holder.*
import android.animation.Animator
import android.R.attr.level
import android.view.animation.Animation
import android.view.animation.Animation.AnimationListener



class WeightAnimator private constructor() {

    var animator: ObjectAnimator? = null

    constructor(view: View, weight: Float, duration: Long): this() {
        val animationWrapper = ViewWeightWrapper(view)
        animator = ObjectAnimator.ofFloat(animationWrapper,
                "weight",
                animationWrapper.getWeight(),
                weight)
        animator?.duration = duration
        animator?.addListener(object: Animator.AnimatorListener {
            override fun onAnimationRepeat(animation: Animator?) {
                //NOP
            }

            override fun onAnimationEnd(animation: Animator?) {
                val params = view.layoutParams as LinearLayout.LayoutParams
                params.weight = weight
                view.parent.requestLayout()
            }

            override fun onAnimationCancel(animation: Animator?) {
                //NOP
            }

            override fun onAnimationStart(animation: Animator?) {
                //NOP
            }
        })
    }

    fun play(){
        animator?.start()
    }

    private inner class ViewWeightWrapper(private val view: View) {
        init {
            if (view.layoutParams !is LinearLayout.LayoutParams){
                throw IllegalArgumentException("The Parent of this View needs to be of Type LinearLayout")
            }
        }

        fun setWeight(weight: Float) {
            val params = view.layoutParams as LinearLayout.LayoutParams
            params.weight = weight
            view.parent.requestLayout()
        }

        fun getWeight(): Float {
            return (view.layoutParams as LinearLayout.LayoutParams).weight
        }
    }
}