package uzh.scenere.datamodel.steps

import android.content.Context
import uzh.scenere.R
import uzh.scenere.const.Constants

class IntroStep(context: Context, scenarioId: String, pathId: String, description: String): AbstractStep(Constants.INTRO_IDENTIFIER.plus(scenarioId),null,pathId) {

    init {
        title = context.getString(R.string.walkthrough_intro)
        text = description
    }
}