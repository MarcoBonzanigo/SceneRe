package uzh.scenere.datastructures

import uzh.scenere.datamodel.*
import uzh.scenere.views.WalkthroughPlayLayout
import java.io.Serializable

class PlayState(val scenario: Scenario,
                val stakeholder: Stakeholder,
                val walkthrough: Walkthrough,
                val startingTime: Long,
                val infoTime: Long,
                val whatIfTime: Long,
                val inputTime: Long,
                val layer: Int,
                val paths: HashMap<Int, Path>?,
                val first: IElement?,
                val second: IElement?,
                val comments: ArrayList<String>,
                val mode: WalkthroughPlayLayout.WalkthroughPlayMode,
                val backupState: WalkthroughPlayLayout.WalkthroughState,
                val state: WalkthroughPlayLayout.WalkthroughState,
                val refresh: Boolean,
                val wifiDiscovered: Boolean,
                val activeResources: HashMap<Resource, Int>) : Serializable