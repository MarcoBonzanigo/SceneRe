package uzh.scenere.datamodel.steps

import android.content.Context
import uzh.scenere.R
import uzh.scenere.const.Constants

class OutroStep(context: Context, scenarioId: String, pathId: String, description: String): AbstractStep(Constants.OUTRO_IDENTIFIER.plus(scenarioId),null,pathId) {

    init {
        title = context.getString(R.string.walkthrough_outro)
        text = description
    }
}