package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.TaskItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("NinjaTugas", appName)
    }

    @Test
    fun `create homework task item model`() {
        val task = TaskItem(
            subject = "Matematika",
            questionText = "2x + 5 = 15",
            shortAnswer = "x = 5",
            detailedSteps = "1. Kurangi kedua ruas dengan 5: 2x = 10\n2. Bagi kedua ruas dengan 2: x = 5",
            keyFormulas = "Aljabar Linear",
            source = "TEXT"
        )
        assertNotNull(task)
        assertEquals("Matematika", task.subject)
        assertEquals("x = 5", task.shortAnswer)
    }
}
