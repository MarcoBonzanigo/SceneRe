package uzh.scenere.views

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Handler
import android.view.ViewGroup
import android.view.Window
import android.widget.ImageView
import android.widget.RelativeLayout
import uzh.scenere.R
import uzh.scenere.const.Constants.Companion.NOTHING
import uzh.scenere.const.Constants.Companion.TUTORIAL_UID_IDENTIFIER
import uzh.scenere.helpers.DatabaseHelper
import uzh.scenere.helpers.ImageHelper

@SuppressLint("ViewConstructor")
class SreTutorialLayoutDialog(context: Context, private val screenWidth: Int, vararg drawableNames: String): RelativeLayout(context){

    private val nameList = ArrayList<String>()
    private var drawablePointer = -1
    private val closeButton = SreButton(context,this,NOTHING,null,null,SreButton.ButtonStyle.TUTORIAL)
    private val imageView = ImageView(context)
    private val dialog = Dialog(context)
    private lateinit  var endExecutable: () -> Unit

    init {
        if (!drawableNames.isEmpty()){
            for (name in drawableNames){
                val alreadySeen = DatabaseHelper.getInstance(context).read(TUTORIAL_UID_IDENTIFIER.plus(name), Boolean::class, false, DatabaseHelper.DataMode.PREFERENCES)
                if (!alreadySeen){
                    nameList.add(name)
                }
            }
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
            dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            val params = LayoutParams(screenWidth, ViewGroup.LayoutParams.MATCH_PARENT)
            layoutParams = params
            imageView.layoutParams = params
            closeButton.addRule(RelativeLayout.ALIGN_PARENT_END, TRUE)
            closeButton.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, TRUE)
            closeButton.translationY = -closeButton.getTopMargin().toFloat()
            addView(imageView)
            addView(closeButton)
            closeButton.setOnClickListener { execTutorialButtonClicked() }
            dialog.setCancelable(false)
            dialog.addContentView(this, this.layoutParams)
            execTutorialButtonClicked()
        }
    }

    private fun execTutorialButtonClicked() {
        if (drawablePointer >= 0){
            DatabaseHelper.getInstance(context).write(TUTORIAL_UID_IDENTIFIER.plus(nameList[drawablePointer]),true,DatabaseHelper.DataMode.PREFERENCES)
        }
        drawablePointer ++
        if (drawablePointer >= nameList.size){
            if (::endExecutable.isInitialized){
                endExecutable()
            }
            dialog.dismiss()
            cleanUp()
        }else{
            closeButton.text = if (drawablePointer == nameList.size-1) context.getString(R.string.tutorial_close) else context.getString(R.string.tutorial_next)
            imageView.setImageBitmap(ImageHelper.getAssetImage(context,nameList[drawablePointer]))
        }
    }

    fun show(tutorialOpen: Boolean, delayMilliseconds: Long = 0): Boolean{
        if (!tutorialOpen && !nameList.isEmpty()) {
            Handler().postDelayed({ dialog.show() }, delayMilliseconds)
            return true
        }
        return tutorialOpen
    }

    fun addEndExecutable(function: () -> Unit): SreTutorialLayoutDialog {
        endExecutable = function
        return this
    }

    private fun cleanUp(){
        System.gc()
    }
}