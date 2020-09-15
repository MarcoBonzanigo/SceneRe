package uzh.scenere.helpers

import android.content.Context
import android.content.Intent
import uzh.scenere.R
import uzh.scenere.activities.WalkthroughActivity
import uzh.scenere.const.Constants
import uzh.scenere.datastructures.PlayState
import java.lang.Exception

class ShortcutHelper {

    companion object {
        var enabled = false

        fun addShortcut(context: Context, walkthroughName: String, saveState: PlayState, notify: ((String,String?) -> Unit)){
            try {
                val shortcutIntent = Intent(context, WalkthroughActivity::class.java)
                shortcutIntent.action = Intent.ACTION_MAIN
                shortcutIntent.addCategory(Intent.CATEGORY_LAUNCHER)
                val flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED or Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT
                shortcutIntent.addFlags(flags)

                val addIntent = Intent()
                addIntent.putExtra(Constants.WALKTHROUGH_PLAY_STATE_SHORTCUT, DataHelper.toByteArray(saveState))
                addIntent.putExtra("duplicate", false)
                addIntent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent)
                addIntent.putExtra(Intent.EXTRA_SHORTCUT_NAME, walkthroughName)
                addIntent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, Intent.ShortcutIconResource
                        .fromContext(context, uzh.scenere.R.mipmap.ic_launcher))
                addIntent.action = "com.android.launcher.action.INSTALL_SHORTCUT"
                context.sendBroadcast(addIntent)
                notify(context.getString(R.string.walkthrough_shortcut_created), context.getString(R.string.walkthrough_shortcut_creation,walkthroughName))
            }catch(e: Exception){
                notify(context.getString(R.string.walkthrough_shortcut_creation_failed),null)
            }
        }
    }
}