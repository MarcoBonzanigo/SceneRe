package uzh.scenere.helpers

import android.content.Context
import uzh.scenere.const.Constants
import uzh.scenere.const.Constants.Companion.ATTRIBUTE_TOKEN
import uzh.scenere.const.Constants.Companion.NOTHING
import uzh.scenere.const.Constants.Companion.OBJECT_TOKEN
import uzh.scenere.const.Constants.Companion.O_A_TOKEN
import uzh.scenere.const.Constants.Companion.S1_A_TOKEN
import uzh.scenere.const.Constants.Companion.S1_O_A_TOKEN
import uzh.scenere.const.Constants.Companion.S1_O_TOKEN
import uzh.scenere.const.Constants.Companion.S1_S2_A_TOKEN
import uzh.scenere.const.Constants.Companion.S1_S2_O_A_TOKEN
import uzh.scenere.const.Constants.Companion.S1_S2_O_TOKEN
import uzh.scenere.const.Constants.Companion.S1_S2_TOKEN
import uzh.scenere.const.Constants.Companion.STAKEHOLDER_1_TOKEN
import uzh.scenere.const.Constants.Companion.STAKEHOLDER_2_TOKEN
import uzh.scenere.const.Constants.Companion.STATIC_TOKEN
import uzh.scenere.const.Constants.Companion.WHAT_IF_DATA
import uzh.scenere.const.Constants.Companion.WHAT_IF_SNAPSHOT_INTERMEDIATE_DATA
import uzh.scenere.const.Constants.Companion.WHAT_IF_SNAPSHOT_OLD_DATA
import uzh.scenere.const.Constants.Companion.WHAT_IF_SNAPSHOT_YOUNG_DATA
import uzh.scenere.datamodel.AbstractObject
import uzh.scenere.datamodel.IElement
import uzh.scenere.datamodel.Stakeholder
import uzh.scenere.datamodel.steps.AbstractStep
import uzh.scenere.datastructures.MultiValueMap

class WhatIfAiHelper {
    companion object {
        @Suppress("UNCHECKED_CAST")
        fun process(context: Context) {
            try {
                val count = DatabaseHelper.getInstance(context).read(Constants.WHAT_IF_PROPOSAL_COUNT, Int::class, 3,DatabaseHelper.DataMode.PREFERENCES)+1
                DatabaseHelper.getInstance(context).write(Constants.WHAT_IF_PROPOSAL_COUNT, count, DatabaseHelper.DataMode.PREFERENCES)
                val check = DatabaseHelper.getInstance(context).read(Constants.WHAT_IF_PROPOSAL_CHECK, Int::class, 1, DatabaseHelper.DataMode.PREFERENCES)
                val initialized = DatabaseHelper.getInstance(context).read(Constants.WHAT_IF_INITIALIZED, Boolean::class, false, DatabaseHelper.DataMode.PREFERENCES)
                if (!initialized || count < check) { //Only execute when enough Proposals have been made
                    return
                }
                //All Elements
                val steps = DatabaseHelper.getInstance(context).readBulk(IElement::class, null, true)
                //All Stakeholder
                val stakeholders = DatabaseHelper.getInstance(context).readBulk(Stakeholder::class, null, true)
                //All Objects
                val objects = DatabaseHelper.getInstance(context).readBulk(AbstractObject::class, null, true)
                //All Existing Data
                var currentWhatIfs = MultiValueMap<String, String>()
                var snapshotYoungData = MultiValueMap<String, String>()
                var snapshotIntermediateData = MultiValueMap<String, String>()
                var snapshotOldData = MultiValueMap<String, String>()
                val bytesExisting = DatabaseHelper.getInstance(context).read(Constants.WHAT_IF_DATA, ByteArray::class, NullHelper.get(ByteArray::class))
                val bytesSnapshotYoung = DatabaseHelper.getInstance(context).read(Constants.WHAT_IF_SNAPSHOT_YOUNG_DATA, ByteArray::class, NullHelper.get(ByteArray::class))
                val bytesSnapshotIntermediate = DatabaseHelper.getInstance(context).read(Constants.WHAT_IF_SNAPSHOT_INTERMEDIATE_DATA, ByteArray::class, NullHelper.get(ByteArray::class))
                val bytesSnapshotOld = DatabaseHelper.getInstance(context).read(Constants.WHAT_IF_SNAPSHOT_OLD_DATA, ByteArray::class, NullHelper.get(ByteArray::class))
                if (bytesExisting.isNotEmpty()) {
                    try {
                        currentWhatIfs = (DataHelper.toObject(bytesExisting, MultiValueMap::class) as MultiValueMap<String, String>)
                    } catch (e: Exception) {/*NOP*/
                    }
                }
                if (bytesSnapshotYoung.isNotEmpty()) {
                    try {
                        snapshotYoungData = (DataHelper.toObject(bytesExisting, MultiValueMap::class) as MultiValueMap<String, String>)
                    } catch (e: Exception) {/*NOP*/
                    }
                }
                if (bytesSnapshotIntermediate.isNotEmpty()) {
                    try {
                        snapshotIntermediateData = (DataHelper.toObject(bytesExisting, MultiValueMap::class) as MultiValueMap<String, String>)
                    } catch (e: Exception) {/*NOP*/
                    }
                }
                if (bytesSnapshotOld.isNotEmpty()) {
                    try {
                        snapshotOldData = (DataHelper.toObject(bytesExisting, MultiValueMap::class) as MultiValueMap<String, String>)
                    } catch (e: Exception) {/*NOP*/
                    }
                }
                //Create Mapping for Data-Types
                val mapping = MultiValueMap<String, String>()
                for (stakeholder in stakeholders) {
                    mapping.put(stakeholder.name.toLowerCase(), STAKEHOLDER_1_TOKEN)
                }
                for (obj in objects) {
                    mapping.put(obj.name.toLowerCase(), OBJECT_TOKEN)
                    for (attribute in obj.attributes) {
                        if (attribute.key != null) {
                            mapping.put(attribute.key.toLowerCase(), ATTRIBUTE_TOKEN)
                        }
                    }
                }
                //New Data Preparation
                var stepsWithWhatIfs = 0
                var stepsTotal = 0
                val createdWhatIfs = HashMap<String, Int>()
                for (step in steps) {
                    if (step is AbstractStep) {
                        stepsTotal++
                        if (step.whatIfs.isNotEmpty()) {
                            stepsWithWhatIfs++
                            for (whatIf in step.whatIfs) {
                                val list = normalize(whatIf, mapping)
                                for (normalizedWhatIf in list) {
                                    //Normalize What Ifs
                                    createdWhatIfs.addOne(normalizedWhatIf)
                                }
                            }
                        }
                    }
                }
                //Final Proposals
                val reusedWhatIfs = HashMap<String, Int>()
                val newWhatIfs = HashMap<String, Int>()
                val discardedWhatIfs = HashMap<String, Int>()
                //Scan for Frequency if Snapshots exist
                if (snapshotYoungData.map.isNotEmpty() && snapshotIntermediateData.map.isNotEmpty() && snapshotOldData.map.isNotEmpty()) {
                    var min = 999999
                    var max = 0
                    val lowFrequencyThreshold = 3
                    for (entry in createdWhatIfs) {
                        min = if (entry.value < min) entry.value else min
                        max = if (entry.value > max) entry.value else max
                    }
                    if (max - min > lowFrequencyThreshold) { //Big Frequency difference and Snapshots exist, check Existence in oldest Snapshots
                        val oldestValues = snapshotOldData.getAllValues()
                        for (entry in createdWhatIfs) {
                            if (entry.value < lowFrequencyThreshold && oldestValues.contains(entry.key)) {
                                //Old and barely used
                                discardedWhatIfs.addOne(entry.key)
                            }
                        }
                    }
                }
                //Iterate Proposals
                for (data in currentWhatIfs.getAllValues()) {
                    //Check Used What Ifs
                    if (createdWhatIfs.containsKey(data)) {
                        reusedWhatIfs.addOne(data)
                    } else {
                        discardedWhatIfs.addOne(data)
                    }
                }
                for (data in createdWhatIfs) {
                    if (!reusedWhatIfs.containsKey(data.key)) {
                        newWhatIfs.addOne(data.key)
                    }
                }
                //Categorize new and discarded Proposals
                val finalProposals = MultiValueMap<String, String>()
                val finalProposalRejects = MultiValueMap<String, String>()
                for (whatIf in newWhatIfs) {
                    //Static
                    if (!whatIf.key.contains(STAKEHOLDER_1_TOKEN) && !whatIf.key.contains(STAKEHOLDER_2_TOKEN) && !whatIf.key.contains(OBJECT_TOKEN) && !whatIf.key.contains(ATTRIBUTE_TOKEN)) {
                        finalProposals.put(STATIC_TOKEN, whatIf.key)
                    }
                    //Stakeholder 1 only
                    if (whatIf.key.contains(STAKEHOLDER_1_TOKEN) && !whatIf.key.contains(STAKEHOLDER_2_TOKEN) && !whatIf.key.contains(OBJECT_TOKEN) && !whatIf.key.contains(ATTRIBUTE_TOKEN)) {
                        finalProposals.put(STAKEHOLDER_1_TOKEN, whatIf.key)
                    }
                    //Stakeholder 1 and Object
                    if (whatIf.key.contains(STAKEHOLDER_1_TOKEN) && !whatIf.key.contains(STAKEHOLDER_2_TOKEN) && whatIf.key.contains(OBJECT_TOKEN) && !whatIf.key.contains(ATTRIBUTE_TOKEN)) {
                        finalProposals.put(S1_O_TOKEN, whatIf.key)
                    }
                    //Stakeholder 1 and Attribute
                    if (whatIf.key.contains(STAKEHOLDER_1_TOKEN) && !whatIf.key.contains(STAKEHOLDER_2_TOKEN) && !whatIf.key.contains(OBJECT_TOKEN) && whatIf.key.contains(ATTRIBUTE_TOKEN)) {
                        finalProposals.put(S1_A_TOKEN, whatIf.key)
                    }
                    //Stakeholder 1 and Object and Attribute
                    if (whatIf.key.contains(STAKEHOLDER_1_TOKEN) && !whatIf.key.contains(STAKEHOLDER_2_TOKEN) && whatIf.key.contains(OBJECT_TOKEN) && whatIf.key.contains(ATTRIBUTE_TOKEN)) {
                        finalProposals.put(S1_O_A_TOKEN, whatIf.key)
                    }
                    //Stakeholder 1 and 2
                    if (whatIf.key.contains(STAKEHOLDER_1_TOKEN) && whatIf.key.contains(STAKEHOLDER_2_TOKEN) && !whatIf.key.contains(OBJECT_TOKEN) && !whatIf.key.contains(ATTRIBUTE_TOKEN)) {
                        finalProposals.put(S1_S2_TOKEN, whatIf.key)
                    }
                    //Stakeholder 1 and 2 and Object
                    if (whatIf.key.contains(STAKEHOLDER_1_TOKEN) && whatIf.key.contains(STAKEHOLDER_2_TOKEN) && whatIf.key.contains(OBJECT_TOKEN) && !whatIf.key.contains(ATTRIBUTE_TOKEN)) {
                        finalProposals.put(S1_S2_O_TOKEN, whatIf.key)
                    }
                    //Stakeholder 1 and 2 and Attribute
                    if (whatIf.key.contains(STAKEHOLDER_1_TOKEN) && whatIf.key.contains(STAKEHOLDER_2_TOKEN) && !whatIf.key.contains(OBJECT_TOKEN) && whatIf.key.contains(ATTRIBUTE_TOKEN)) {
                        finalProposals.put(S1_S2_A_TOKEN, whatIf.key)
                    }
                    //Stakeholder 1 and 2 and Object and Attribute
                    if (whatIf.key.contains(STAKEHOLDER_1_TOKEN) && whatIf.key.contains(STAKEHOLDER_2_TOKEN) && whatIf.key.contains(OBJECT_TOKEN) && whatIf.key.contains(ATTRIBUTE_TOKEN)) {
                        finalProposals.put(S1_S2_O_A_TOKEN, whatIf.key)
                    }
                    //Object Only
                    if (!whatIf.key.contains(STAKEHOLDER_1_TOKEN) && !whatIf.key.contains(STAKEHOLDER_2_TOKEN) && whatIf.key.contains(OBJECT_TOKEN) && !whatIf.key.contains(ATTRIBUTE_TOKEN)) {
                        finalProposals.put(OBJECT_TOKEN, whatIf.key)
                    }
                    //Attribute Only
                    if (!whatIf.key.contains(STAKEHOLDER_1_TOKEN) && !whatIf.key.contains(STAKEHOLDER_2_TOKEN) && !whatIf.key.contains(OBJECT_TOKEN) && whatIf.key.contains(ATTRIBUTE_TOKEN)) {
                        finalProposals.put(ATTRIBUTE_TOKEN, whatIf.key)
                    }
                    //Object and Attribute
                    if (!whatIf.key.contains(STAKEHOLDER_1_TOKEN) && !whatIf.key.contains(STAKEHOLDER_2_TOKEN) && whatIf.key.contains(OBJECT_TOKEN) && whatIf.key.contains(ATTRIBUTE_TOKEN)) {
                        finalProposals.put(O_A_TOKEN, whatIf.key)
                    }
                }
                for (whatIf in discardedWhatIfs) {
                    //Static
                    if (!whatIf.key.contains(STAKEHOLDER_1_TOKEN) && !whatIf.key.contains(STAKEHOLDER_2_TOKEN) && !whatIf.key.contains(OBJECT_TOKEN) && !whatIf.key.contains(ATTRIBUTE_TOKEN)) {
                        finalProposalRejects.put(STATIC_TOKEN, whatIf.key)
                    }
                    //Stakeholder 1 only
                    if (whatIf.key.contains(STAKEHOLDER_1_TOKEN) && !whatIf.key.contains(STAKEHOLDER_2_TOKEN) && !whatIf.key.contains(OBJECT_TOKEN) && !whatIf.key.contains(ATTRIBUTE_TOKEN)) {
                        finalProposalRejects.put(STAKEHOLDER_1_TOKEN, whatIf.key)
                    }
                    //Stakeholder 1 and Object
                    if (whatIf.key.contains(STAKEHOLDER_1_TOKEN) && !whatIf.key.contains(STAKEHOLDER_2_TOKEN) && whatIf.key.contains(OBJECT_TOKEN) && !whatIf.key.contains(ATTRIBUTE_TOKEN)) {
                        finalProposalRejects.put(S1_O_TOKEN, whatIf.key)
                    }
                    //Stakeholder 1 and Attribute
                    if (whatIf.key.contains(STAKEHOLDER_1_TOKEN) && !whatIf.key.contains(STAKEHOLDER_2_TOKEN) && !whatIf.key.contains(OBJECT_TOKEN) && whatIf.key.contains(ATTRIBUTE_TOKEN)) {
                        finalProposalRejects.put(S1_A_TOKEN, whatIf.key)
                    }
                    //Stakeholder 1 and Object and Attribute
                    if (whatIf.key.contains(STAKEHOLDER_1_TOKEN) && !whatIf.key.contains(STAKEHOLDER_2_TOKEN) && whatIf.key.contains(OBJECT_TOKEN) && whatIf.key.contains(ATTRIBUTE_TOKEN)) {
                        finalProposalRejects.put(S1_O_A_TOKEN, whatIf.key)
                    }
                    //Stakeholder 1 and 2
                    if (whatIf.key.contains(STAKEHOLDER_1_TOKEN) && whatIf.key.contains(STAKEHOLDER_2_TOKEN) && !whatIf.key.contains(OBJECT_TOKEN) && !whatIf.key.contains(ATTRIBUTE_TOKEN)) {
                        finalProposalRejects.put(S1_S2_TOKEN, whatIf.key)
                    }
                    //Stakeholder 1 and 2 and Object
                    if (whatIf.key.contains(STAKEHOLDER_1_TOKEN) && whatIf.key.contains(STAKEHOLDER_2_TOKEN) && whatIf.key.contains(OBJECT_TOKEN) && !whatIf.key.contains(ATTRIBUTE_TOKEN)) {
                        finalProposalRejects.put(S1_S2_O_TOKEN, whatIf.key)
                    }
                    //Stakeholder 1 and 2 and Attribute
                    if (whatIf.key.contains(STAKEHOLDER_1_TOKEN) && whatIf.key.contains(STAKEHOLDER_2_TOKEN) && !whatIf.key.contains(OBJECT_TOKEN) && whatIf.key.contains(ATTRIBUTE_TOKEN)) {
                        finalProposalRejects.put(S1_S2_A_TOKEN, whatIf.key)
                    }
                    //Stakeholder 1 and 2 and Object and Attribute
                    if (whatIf.key.contains(STAKEHOLDER_1_TOKEN) && whatIf.key.contains(STAKEHOLDER_2_TOKEN) && whatIf.key.contains(OBJECT_TOKEN) && whatIf.key.contains(ATTRIBUTE_TOKEN)) {
                        finalProposalRejects.put(S1_S2_O_A_TOKEN, whatIf.key)
                    }
                    //Object Only
                    if (!whatIf.key.contains(STAKEHOLDER_1_TOKEN) && !whatIf.key.contains(STAKEHOLDER_2_TOKEN) && whatIf.key.contains(OBJECT_TOKEN) && !whatIf.key.contains(ATTRIBUTE_TOKEN)) {
                        finalProposalRejects.put(OBJECT_TOKEN, whatIf.key)
                    }
                    //Attribute Only
                    if (!whatIf.key.contains(STAKEHOLDER_1_TOKEN) && !whatIf.key.contains(STAKEHOLDER_2_TOKEN) && !whatIf.key.contains(OBJECT_TOKEN) && whatIf.key.contains(ATTRIBUTE_TOKEN)) {
                        finalProposalRejects.put(ATTRIBUTE_TOKEN, whatIf.key)
                    }
                    //Object and Attribute
                    if (!whatIf.key.contains(STAKEHOLDER_1_TOKEN) && !whatIf.key.contains(STAKEHOLDER_2_TOKEN) && whatIf.key.contains(OBJECT_TOKEN) && whatIf.key.contains(ATTRIBUTE_TOKEN)) {
                        finalProposalRejects.put(O_A_TOKEN, whatIf.key)
                    }
                }
                //Only Change the ones affected by the Mode
                val whatIfMode = WhatIfMode.valueOf(DatabaseHelper.getInstance(context).read(Constants.WHAT_IF_MODE, String::class, WhatIfMode.ALL.toString(), DatabaseHelper.DataMode.PREFERENCES))
                val finalWhatIfs = MultiValueMap<String, String>(currentWhatIfs)
                when (whatIfMode) {
                    WhatIfMode.ALL -> {
                        //Addition
                        finalWhatIfs.putAll(STAKEHOLDER_1_TOKEN, *finalProposals.get(STAKEHOLDER_1_TOKEN).toTypedArray())
                        finalWhatIfs.putAll(OBJECT_TOKEN, *finalProposals.get(OBJECT_TOKEN).toTypedArray())
                        finalWhatIfs.putAll(ATTRIBUTE_TOKEN, *finalProposals.get(ATTRIBUTE_TOKEN).toTypedArray())
                        finalWhatIfs.putAll(STATIC_TOKEN, *finalProposals.get(STATIC_TOKEN).toTypedArray())
                        finalWhatIfs.putAll(S1_S2_TOKEN, *finalProposals.get(S1_S2_TOKEN).toTypedArray())
                        finalWhatIfs.putAll(S1_S2_O_TOKEN, *finalProposals.get(S1_S2_O_TOKEN).toTypedArray())
                        finalWhatIfs.putAll(S1_S2_A_TOKEN, *finalProposals.get(S1_S2_A_TOKEN).toTypedArray())
                        finalWhatIfs.putAll(S1_S2_O_A_TOKEN, *finalProposals.get(S1_S2_O_A_TOKEN).toTypedArray())
                        finalWhatIfs.putAll(S1_O_TOKEN, *finalProposals.get(S1_O_TOKEN).toTypedArray())
                        finalWhatIfs.putAll(S1_A_TOKEN, *finalProposals.get(S1_A_TOKEN).toTypedArray())
                        finalWhatIfs.putAll(S1_O_A_TOKEN, *finalProposals.get(S1_O_A_TOKEN).toTypedArray())
                        finalWhatIfs.putAll(O_A_TOKEN, *finalProposals.get(O_A_TOKEN).toTypedArray())
                        //Removal
                        finalWhatIfs.remove(STAKEHOLDER_1_TOKEN, *finalProposalRejects.get(STAKEHOLDER_1_TOKEN).toTypedArray())
                        finalWhatIfs.remove(OBJECT_TOKEN, *finalProposalRejects.get(OBJECT_TOKEN).toTypedArray())
                        finalWhatIfs.remove(ATTRIBUTE_TOKEN, *finalProposalRejects.get(ATTRIBUTE_TOKEN).toTypedArray())
                        finalWhatIfs.remove(STATIC_TOKEN, *finalProposalRejects.get(STATIC_TOKEN).toTypedArray())
                        finalWhatIfs.remove(S1_S2_TOKEN, *finalProposalRejects.get(S1_S2_TOKEN).toTypedArray())
                        finalWhatIfs.remove(S1_S2_O_TOKEN, *finalProposalRejects.get(S1_S2_O_TOKEN).toTypedArray())
                        finalWhatIfs.remove(S1_S2_A_TOKEN, *finalProposalRejects.get(S1_S2_A_TOKEN).toTypedArray())
                        finalWhatIfs.remove(S1_S2_O_A_TOKEN, *finalProposalRejects.get(S1_S2_O_A_TOKEN).toTypedArray())
                        finalWhatIfs.remove(S1_O_TOKEN, *finalProposalRejects.get(S1_O_TOKEN).toTypedArray())
                        finalWhatIfs.remove(S1_A_TOKEN, *finalProposalRejects.get(S1_A_TOKEN).toTypedArray())
                        finalWhatIfs.remove(S1_O_A_TOKEN, *finalProposalRejects.get(S1_O_A_TOKEN).toTypedArray())
                        finalWhatIfs.remove(O_A_TOKEN, *finalProposalRejects.get(O_A_TOKEN).toTypedArray())
                    }
                    WhatIfMode.DYNAMIC -> {
                        //Addition
                        finalWhatIfs.putAll(STAKEHOLDER_1_TOKEN, *finalProposals.get(STAKEHOLDER_1_TOKEN).toTypedArray())
                        finalWhatIfs.putAll(OBJECT_TOKEN, *finalProposals.get(OBJECT_TOKEN).toTypedArray())
                        finalWhatIfs.putAll(ATTRIBUTE_TOKEN, *finalProposals.get(ATTRIBUTE_TOKEN).toTypedArray())
                        finalWhatIfs.putAll(S1_S2_TOKEN, *finalProposals.get(S1_S2_TOKEN).toTypedArray())
                        finalWhatIfs.putAll(S1_S2_O_TOKEN, *finalProposals.get(S1_S2_O_TOKEN).toTypedArray())
                        finalWhatIfs.putAll(S1_S2_A_TOKEN, *finalProposals.get(S1_S2_A_TOKEN).toTypedArray())
                        finalWhatIfs.putAll(S1_S2_O_A_TOKEN, *finalProposals.get(S1_S2_O_A_TOKEN).toTypedArray())
                        finalWhatIfs.putAll(S1_O_TOKEN, *finalProposals.get(S1_O_TOKEN).toTypedArray())
                        finalWhatIfs.putAll(S1_A_TOKEN, *finalProposals.get(S1_A_TOKEN).toTypedArray())
                        finalWhatIfs.putAll(S1_O_A_TOKEN, *finalProposals.get(S1_O_A_TOKEN).toTypedArray())
                        finalWhatIfs.putAll(O_A_TOKEN, *finalProposals.get(O_A_TOKEN).toTypedArray())
                        //Removal
                        finalWhatIfs.remove(STAKEHOLDER_1_TOKEN, *finalProposalRejects.get(STAKEHOLDER_1_TOKEN).toTypedArray())
                        finalWhatIfs.remove(OBJECT_TOKEN, *finalProposalRejects.get(OBJECT_TOKEN).toTypedArray())
                        finalWhatIfs.remove(ATTRIBUTE_TOKEN, *finalProposalRejects.get(ATTRIBUTE_TOKEN).toTypedArray())
                        finalWhatIfs.remove(S1_S2_TOKEN, *finalProposalRejects.get(S1_S2_TOKEN).toTypedArray())
                        finalWhatIfs.remove(S1_S2_O_TOKEN, *finalProposalRejects.get(S1_S2_O_TOKEN).toTypedArray())
                        finalWhatIfs.remove(S1_S2_A_TOKEN, *finalProposalRejects.get(S1_S2_A_TOKEN).toTypedArray())
                        finalWhatIfs.remove(S1_S2_O_A_TOKEN, *finalProposalRejects.get(S1_S2_O_A_TOKEN).toTypedArray())
                        finalWhatIfs.remove(S1_O_TOKEN, *finalProposalRejects.get(S1_O_TOKEN).toTypedArray())
                        finalWhatIfs.remove(S1_A_TOKEN, *finalProposalRejects.get(S1_A_TOKEN).toTypedArray())
                        finalWhatIfs.remove(S1_O_A_TOKEN, *finalProposalRejects.get(S1_O_A_TOKEN).toTypedArray())
                        finalWhatIfs.remove(O_A_TOKEN, *finalProposalRejects.get(O_A_TOKEN).toTypedArray())
                    }
                    WhatIfMode.STAKEHOLDER -> {
                        //Addition
                        finalWhatIfs.putAll(STAKEHOLDER_1_TOKEN, *finalProposals.get(STAKEHOLDER_1_TOKEN).toTypedArray())
                        finalWhatIfs.putAll(S1_S2_TOKEN, *finalProposals.get(S1_S2_TOKEN).toTypedArray())
                        //Removal
                        finalWhatIfs.remove(STAKEHOLDER_1_TOKEN, *finalProposalRejects.get(STAKEHOLDER_1_TOKEN).toTypedArray())
                        finalWhatIfs.remove(S1_S2_TOKEN, *finalProposalRejects.get(S1_S2_TOKEN).toTypedArray())
                    }
                    WhatIfMode.OBJECTS -> {
                        //Addition
                        finalWhatIfs.putAll(OBJECT_TOKEN, *finalProposals.get(OBJECT_TOKEN).toTypedArray())
                        finalWhatIfs.putAll(ATTRIBUTE_TOKEN, *finalProposals.get(ATTRIBUTE_TOKEN).toTypedArray())
                        finalWhatIfs.putAll(O_A_TOKEN, *finalProposals.get(O_A_TOKEN).toTypedArray())
                        //Removal
                        finalWhatIfs.remove(OBJECT_TOKEN, *finalProposalRejects.get(OBJECT_TOKEN).toTypedArray())
                        finalWhatIfs.remove(ATTRIBUTE_TOKEN, *finalProposalRejects.get(ATTRIBUTE_TOKEN).toTypedArray())
                        finalWhatIfs.remove(O_A_TOKEN, *finalProposalRejects.get(O_A_TOKEN).toTypedArray())
                    }
                    WhatIfMode.STATIC -> {
                        //Addition
                        finalWhatIfs.putAll(STATIC_TOKEN, *finalProposals.get(STATIC_TOKEN).toTypedArray())
                        //Removal
                        finalWhatIfs.remove(STATIC_TOKEN, *finalProposalRejects.get(STATIC_TOKEN).toTypedArray())
                    }
                    WhatIfMode.NONE -> {
                        //Do Nothing
                    }
                }
                //Persist
                DatabaseHelper.getInstance(context).write(WHAT_IF_DATA, DataHelper.toByteArray(finalWhatIfs))
                DatabaseHelper.getInstance(context).write(WHAT_IF_SNAPSHOT_YOUNG_DATA, DataHelper.toByteArray(currentWhatIfs))
                DatabaseHelper.getInstance(context).write(WHAT_IF_SNAPSHOT_INTERMEDIATE_DATA, DataHelper.toByteArray(snapshotYoungData))
                DatabaseHelper.getInstance(context).write(WHAT_IF_SNAPSHOT_OLD_DATA, DataHelper.toByteArray(snapshotIntermediateData))
                //Reset Proposal Counter
                DatabaseHelper.getInstance(context).write(Constants.WHAT_IF_PROPOSAL_COUNT, 0, DatabaseHelper.DataMode.PREFERENCES)
            }catch (e: Exception){
                /*NOP*/
            }
        }

        private fun normalize(whatIf: String, mapping: MultiValueMap<String, String>): List<String> {
            var map = HashMap<String,Int>()
            map[whatIf] = 1
            for (entry in mapping.map.entries){
                val newMap = HashMap<String,Int>()
                for (branchedWhatIf in map){
                    for (type in entry.value){
                        var replaceAllIgnoreCase = NOTHING
                        if (branchedWhatIf.key.contains(STAKEHOLDER_1_TOKEN) && type == STAKEHOLDER_1_TOKEN){
                            replaceAllIgnoreCase = branchedWhatIf.key.replaceAllIgnoreCase(entry.key, STAKEHOLDER_2_TOKEN)
                        }else{
                            replaceAllIgnoreCase = branchedWhatIf.key.replaceAllIgnoreCase(entry.key, type)
                        }
                        newMap[replaceAllIgnoreCase] = 1
                    }
                }
                //Copy Map to deal with similar Names for different Data-Types
                map = newMap
            }
            val list = ArrayList<String>()
            for (normalizedWhatIf in map){
                list.add(normalizedWhatIf.key)
            }
            return list
        }

    }
}