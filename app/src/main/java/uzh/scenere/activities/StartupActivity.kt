package uzh.scenere.activities

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.view.View
import android.view.View.INVISIBLE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.view.animation.AlphaAnimation
import android.widget.TextView
import kotlinx.android.synthetic.main.activity_startup.*
import uzh.scenere.R
import uzh.scenere.const.Constants
import uzh.scenere.const.Constants.Companion.NOTHING
import uzh.scenere.helpers.*
import uzh.scenere.views.WeightAnimator
import java.util.*
import kotlin.random.Random

class StartupActivity : AbstractBaseActivity() {

    override fun getConfiguredRootLayout(): ViewGroup? {
        return startup_root
    }

    override fun getConfiguredLayout(): Int {
        return R.layout.activity_startup
    }

    private var complete = 0
    private var total = 0
    private var userName: String? = null
    private var userId: String = NOTHING
    private var interrupted: Boolean = false
    private var closing: Boolean = false
    private var cleanupFinished = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        userName = DatabaseHelper.getInstance(applicationContext).readAndMigrate(Constants.USER_NAME, String::class, NOTHING, false)
        userId = DatabaseHelper.getInstance(applicationContext).readAndMigrate(Constants.USER_ID, String::class, NOTHING, false)

        executeAsyncTask({
            DatabaseHelper.getInstance(applicationContext).recreateTableStatistics()
            cleanupFinished = true
        },{cleanupFinished = true},{cleanupFinished = true})

        morphRandom(startup_text_1 as TextView,'S')
        morphRandom(startup_text_2 as TextView,'c')
        morphRandom(startup_text_3 as TextView,'e')
        morphRandom(startup_text_4 as TextView,'n')
        morphRandom(startup_text_5 as TextView,'e')
        morphRandom(startup_text_6 as TextView,'-')
        morphRandom(startup_text_7 as TextView,'R')
        morphRandom(startup_text_8 as TextView,'E')

        if (!StringHelper.hasText(userId)){
            userId = UUID.randomUUID().toString()
            DatabaseHelper.getInstance(applicationContext).write(Constants.USER_ID, userId)
        }
    }

    private fun morphRandom(text: TextView, returnChar: Char, init: Boolean = true, offset: Int = 0) {
        total += if (init) 1 else 0
        var character: Char = '#'
        if (StringHelper.hasText(text.text)){
            character = text.text[0]
        }
        val dist = 255-NumberHelper.capAt(Math.abs(returnChar.toInt()-character.toInt())*5,0,239)
        val colorString = getString(R.string.color_alpha_rgb, dist.toString(16), "004d40")
        val color = Color.parseColor(colorString)
        text.setTextColor(color)
        if (character == returnChar){
            return complete()
        }
        val charDistance = 40
        val chanceOfNewCharacter = 0.85
        val lowerBound = 32
        val low = NumberHelper.capAt((returnChar.toInt() - (charDistance-offset)),lowerBound,returnChar.toInt())
        text.text = (low + Random.nextSafeInt(2*(returnChar.toInt()-low))).toChar().toString()
        val offsetNew = if (offset < charDistance && Math.random()< chanceOfNewCharacter) (offset+1) else offset
        Handler().postDelayed({morphRandom(text,returnChar, false, offsetNew)},(0L + Random.nextSafeInt((charDistance-offsetNew))))
    }

    private fun complete() {
        complete++
        WeightAnimator(startup_layout_progress_bar, complete.toFloat(), 250).play()
        if (complete == total){
            logoDisplayFinished()
        }
    }

    fun onStartupInterrupt(v: View){
        if (StringHelper.hasText(userName)){
            interrupted = true
            startup_edit_name.isEnabled = true
            startup_button_continue.visibility = VISIBLE
            startup_text_name.text = getString(R.string.startup_name_change)
            fadeIn()
        }
    }

    private fun logoDisplayFinished() {
        val primary = getColorWithStyle(this, R.color.srePrimaryDeepDark)
        val secondary = getColorWithStyle(this, R.color.srePrimaryLight)
        (startup_text_1 as TextView).setTextColor(primary)
        (startup_text_2 as TextView).setTextColor(primary)
        (startup_text_3 as TextView).setTextColor(primary)
        (startup_text_4 as TextView).setTextColor(primary)
        (startup_text_5 as TextView).setTextColor(primary)
        (startup_text_7 as TextView).setTextColor(secondary)
        (startup_text_8 as TextView).setTextColor(secondary)
        fadeIn()
        if (StringHelper.hasText(userName)){
            Handler().postDelayed({
                fadeOut()
            },1000)
            startup_text_name.text = getString(R.string.startup_welcome_back)
            startup_edit_name.setText(userName)
            startup_edit_name.isEnabled = false
            Handler().postDelayed({checkInputAndNext()},2000)
        }else{
            startup_button_continue.visibility = VISIBLE
        }
    }

    private fun fadeIn() {
        startup_edit_name.visibility = VISIBLE
        startup_text_name.visibility = VISIBLE
        val fadeIn = AlphaAnimation(0.0f, 1.0f)
        fadeIn.duration = 1000
        startup_text_name.startAnimation(fadeIn)
        startup_edit_name.startAnimation(fadeIn)
    }

    private fun fadeOut() {
        val fadeOut = AlphaAnimation(1.0f, 0.0f)
        fadeOut.duration = 1000
        startup_text_name.startAnimation(fadeOut)
        startup_edit_name.startAnimation(fadeOut)
        startup_edit_name.visibility = INVISIBLE
        startup_text_name.visibility = INVISIBLE
        if (startup_button_continue.visibility == VISIBLE){
            startup_button_continue.startAnimation(fadeOut)
            startup_button_continue.visibility = INVISIBLE
        }
    }

    override fun onNavigationButtonClicked(view: View) {
        when (view.id) {
            R.id.startup_button_continue -> {
                checkInputAndNext()
            }
        }
    }

    private fun checkInputAndNext() {
        if (interrupted || closing){
            interrupted = false
            return
        }
        if (!StringHelper.hasText(startup_edit_name.text)) {
            notify(getString(R.string.startup_name_enter))
            return
        }
        closing = true
        if (startup_edit_name.isEnabled){
            startup_edit_name.isEnabled = false
            DatabaseHelper.getInstance(applicationContext).write(Constants.USER_NAME, startup_edit_name.text.toString())
            fadeOut()
            Handler().postDelayed({startMainMenu()},1000)
        }else{
            startMainMenu()
        }
    }

    private fun startMainMenu(){
        if (cleanupFinished){
            startActivity(Intent(this, MainMenuActivity::class.java))
        }else{
            Handler().postDelayed({startMainMenu()},300)
        }
    }
}