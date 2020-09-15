package uzh.scenere.helpers

import android.content.Context
import android.util.Log
import uzh.scenere.datamodel.Project

class TestingHelper private constructor () {
    companion object {
        fun testDataStorage(context: Context){
            DatabaseHelper.getInstance(context).clear()

            val valTrue1: String = "true"
            val valTrue2: Boolean = true
            val valTrue3: Short = -2
            val valTrue4: Int = -2
            val valTrue5: Float = -2.0f
            val valTrue6: Double = -2.0
            val valTrue7: Long = -2
            val valTrue8: Project = Project.ProjectBuilder("Vero", "Vero's Title", "Vero's Description").build()

            var successWrite = false

            DatabaseHelper.getInstance(context).write("Test1", valTrue1)
            DatabaseHelper.getInstance(context).write("Test2", valTrue2)
            DatabaseHelper.getInstance(context).write("Test3", valTrue3)
            DatabaseHelper.getInstance(context).write("Test4", valTrue4)
            DatabaseHelper.getInstance(context).write("Test5", valTrue5)
            DatabaseHelper.getInstance(context).write("Test6", valTrue6)
            DatabaseHelper.getInstance(context).write("Test7", valTrue7)
            DatabaseHelper.getInstance(context).write("Test8", valTrue8)


            successWrite = true

            val valFalse1: String = "false"
            val valFalse2: Boolean = false
            val valFalse3: Short = -1
            val valFalse4: Int = -1
            val valFalse5: Float = -1.0f
            val valFalse6: Double = -1.0
            val valFalse7: Long = -1
            val valFalse8: Project = Project.ProjectBuilder("Marco", "Marco's Title", "Marco's Title").build()

            val testWrite1 = DatabaseHelper.getInstance(context).read("Test1", String::class, valFalse1)
            val testWrite2 = DatabaseHelper.getInstance(context).read("Test2", Boolean::class, valFalse2)
            val testWrite3 = DatabaseHelper.getInstance(context).read("Test3", Short::class, valFalse3)
            val testWrite4 = DatabaseHelper.getInstance(context).read("Test4", Int::class, valFalse4)
            val testWrite5 = DatabaseHelper.getInstance(context).read("Test5", Float::class, valFalse5)
            val testWrite6 = DatabaseHelper.getInstance(context).read("Test6", Double::class, valFalse6)
            val testWrite7 = DatabaseHelper.getInstance(context).read("Test7", Long::class, valFalse7)
            val testWrite8 = DatabaseHelper.getInstance(context).read(valTrue8.id, Project::class, valFalse8)

            var successRead = false
            if (testWrite1 == valTrue1 &&
                    testWrite2 == valTrue2 &&
                    testWrite3 == valTrue3 &&
                    testWrite4 == valTrue4 &&
                    testWrite5 == valTrue5 &&
                    testWrite6 == valTrue6 &&
                    testWrite7 == valTrue7 &&
                    testWrite8 == valTrue8) {
                successRead = true
            }

            DatabaseHelper.getInstance(context).delete("Test1", String::class)
            DatabaseHelper.getInstance(context).delete("Test2", Boolean::class)
            DatabaseHelper.getInstance(context).delete("Test3", Short::class)
            DatabaseHelper.getInstance(context).delete("Test4", Int::class)
            DatabaseHelper.getInstance(context).delete("Test5", Float::class)
            DatabaseHelper.getInstance(context).delete("Test6", Double::class)
            DatabaseHelper.getInstance(context).delete("Test7", Long::class)
            DatabaseHelper.getInstance(context).delete("Test8", Project::class)


            val testDelete1 = DatabaseHelper.getInstance(context).read("Test1", String::class, valFalse1)
            val testDelete2 = DatabaseHelper.getInstance(context).read("Test2", Boolean::class, valFalse2)
            val testDelete3 = DatabaseHelper.getInstance(context).read("Test3", Short::class, valFalse3)
            val testDelete4 = DatabaseHelper.getInstance(context).read("Test4", Int::class, valFalse4)
            val testDelete5 = DatabaseHelper.getInstance(context).read("Test5", Float::class, valFalse5)
            val testDelete6 = DatabaseHelper.getInstance(context).read("Test6", Double::class, valFalse6)
            val testDelete7 = DatabaseHelper.getInstance(context).read("Test7", Long::class, valFalse7)
            val testDelete8 = DatabaseHelper.getInstance(context).read("Test8", Project::class, valFalse8)

            var successDelete = false
            if (testDelete1 == valFalse1 &&
                    testDelete2 == valFalse2 &&
                    testDelete3 == valFalse3 &&
                    testDelete4 == valFalse4 &&
                    testDelete5 == valFalse5 &&
                    testDelete6 == valFalse6 &&
                    testDelete7 == valFalse7 &&
                    testDelete8 == valFalse8) {
                successDelete = true
            }

            Log.d("DatabaseTest", "Write: $successWrite, Read: $successRead, Delete: $successDelete!")
        }
    }
}