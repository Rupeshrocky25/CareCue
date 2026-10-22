package com.example

import com.example.carecue.data.model.ActionType
import com.example.carecue.data.model.TaskCategory
import com.example.carecue.data.model.TaskItem
import com.example.carecue.data.model.TaskStatus
import com.example.carecue.engine.CareCueSmartEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class CareCueEngineTest {

    private val engine = CareCueSmartEngine()
    private val testTz = TimeZone.getTimeZone("America/New_York")

    @Test
    fun testCompoundCommandExtraction() {
        val input = "Remind me to call Dad at 7 PM tomorrow and add milk and bread to tomorrow's shopping list"
        val result = engine.processInput(input, emptyList(), testTz.id)

        assertEquals(2, result.proposedActions.size)
        
        val reminderAction = result.proposedActions[0]
        assertEquals(ActionType.CREATE_REMINDER, reminderAction.actionType)
        assertTrue(reminderAction.title.contains("Call Dad", ignoreCase = true))
        assertNotNull(reminderAction.dueTimestamp)

        val shoppingAction = result.proposedActions[1]
        assertEquals(ActionType.ADD_TO_SHOPPING_LIST, shoppingAction.actionType)
        assertTrue(shoppingAction.itemsToAdd.any { it.contains("milk", ignoreCase = true) })
        assertTrue(shoppingAction.itemsToAdd.any { it.contains("bread", ignoreCase = true) })
    }

    @Test
    fun testSingleReminderParsing() {
        val input = "Remind me to call Dad tomorrow at 7 PM"
        val result = engine.processInput(input, emptyList(), testTz.id)

        assertEquals(1, result.proposedActions.size)
        val action = result.proposedActions[0]
        assertEquals(ActionType.CREATE_REMINDER, action.actionType)
        assertTrue(action.title.contains("Call Dad", ignoreCase = true))
        assertNotNull(action.dueTimestamp)

        val cal = Calendar.getInstance(testTz).apply { timeInMillis = action.dueTimestamp!! }
        assertEquals(19, cal.get(Calendar.HOUR_OF_DAY))
        assertEquals(0, cal.get(Calendar.MINUTE))
    }

    @Test
    fun testShoppingListExtraction() {
        val input = "Add milk, bread and eggs to my shopping list"
        val result = engine.processInput(input, emptyList(), testTz.id)

        assertEquals(1, result.proposedActions.size)
        val action = result.proposedActions[0]
        assertEquals(ActionType.ADD_TO_SHOPPING_LIST, action.actionType)
        assertEquals(3, action.itemsToAdd.size)
    }

    @Test
    fun testQueryTodayTasks() {
        val todayCal = Calendar.getInstance(testTz).apply {
            set(Calendar.HOUR_OF_DAY, 14)
            set(Calendar.MINUTE, 0)
        }
        val sampleTasks = listOf(
            TaskItem(
                id = "1",
                title = "Check Mom's prescription",
                dueTimestamp = todayCal.timeInMillis,
                timeZoneId = testTz.id
            )
        )
        val result = engine.processInput("What do I have to do today?", sampleTasks, testTz.id)
        assertTrue(result.isQuery)
        assertTrue(result.replyText.contains("Check Mom's prescription"))
    }

    @Test
    fun testMarkCompleteIntent() {
        val sampleTasks = listOf(
            TaskItem(
                id = "task_grocery",
                title = "Pick up grocery order",
                category = TaskCategory.SHOPPING,
                isCompleted = false
            )
        )
        val result = engine.processInput("Mark the grocery task complete", sampleTasks, testTz.id)
        assertEquals(1, result.proposedActions.size)
        val action = result.proposedActions[0]
        assertEquals(ActionType.COMPLETE_TASK, action.actionType)
        assertEquals("task_grocery", action.targetTaskId)
    }

    @Test
    fun testRescheduleIntent() {
        val sampleTasks = listOf(
            TaskItem(
                id = "call_reminder_id",
                title = "Call Dad",
                category = TaskCategory.FAMILY,
                dueTimestamp = System.currentTimeMillis() + 3600000
            )
        )
        val result = engine.processInput("Move my call reminder to 8 PM", sampleTasks, testTz.id)
        assertEquals(1, result.proposedActions.size)
        val action = result.proposedActions[0]
        assertEquals(ActionType.RESCHEDULE_TASK, action.actionType)
        assertEquals("call_reminder_id", action.targetTaskId)
    }

    @Test
    fun testDerivedTaskStatus() {
        val pastTime = System.currentTimeMillis() - (3600 * 1000)
        val futureTime = System.currentTimeMillis() + (3600 * 1000)

        val overdueTask = TaskItem(title = "Overdue task", dueTimestamp = pastTime, isCompleted = false)
        assertEquals(TaskStatus.OVERDUE, overdueTask.getStatus())

        val completedOverdueTask = TaskItem(title = "Completed task", dueTimestamp = pastTime, isCompleted = true)
        assertEquals(TaskStatus.COMPLETED, completedOverdueTask.getStatus())

        val upcomingTask = TaskItem(title = "Upcoming task", dueTimestamp = futureTime, isCompleted = false)
        assertEquals(TaskStatus.PENDING, upcomingTask.getStatus())
    }

    @Test
    fun testClarificationWhenTimeIsMissing() {
        val input = "Remind me to call Dad"
        val result = engine.processInput(input, emptyList(), testTz.id)
        assertTrue(result.requiresClarification)
        assertTrue(result.replyText.contains("When would you like to be reminded to “Call Dad”?", ignoreCase = true))
    }
}
