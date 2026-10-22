package com.example.carecue.engine

import com.example.carecue.data.model.ActionType
import com.example.carecue.data.model.ProposedAction
import com.example.carecue.data.model.TaskCategory
import com.example.carecue.data.model.TaskItem
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import java.util.regex.Pattern

data class ParseResult(
    val replyText: String,
    val proposedActions: List<ProposedAction> = emptyList(),
    val isQuery: Boolean = false,
    val queryFilteredTasks: List<TaskItem>? = null,
    val requiresClarification: Boolean = false
)

class CareCueSmartEngine {

    /**
     * Parses user input deterministically using natural language patterns,
     * compound intent detection, and robust timezone-aware relative date parsing.
     */
    fun processInput(
        input: String,
        currentTasks: List<TaskItem>,
        timeZoneId: String = TimeZone.getDefault().id
    ): ParseResult {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) {
            return ParseResult(replyText = "I didn't catch that. Could you please specify a reminder or task?")
        }

        val tz = try {
            TimeZone.getTimeZone(timeZoneId)
        } catch (_: Exception) {
            TimeZone.getDefault()
        }

        val lower = trimmed.lowercase(Locale.ROOT)

        // 1. Query intents ("what do I have to do today?", "show my overdue tasks", "show schedule")
        if (isQueryIntent(lower)) {
            return handleQueryIntent(lower, currentTasks, tz)
        }

        // 2. Mark complete intent ("mark the grocery task complete", "complete call dad")
        if (isCompleteIntent(lower)) {
            return handleCompleteIntent(lower, currentTasks, timeZoneId)
        }

        // 3. Move / Reschedule intent ("move my call reminder to 8 PM", "reschedule dad call to tomorrow 6 PM")
        if (isRescheduleIntent(lower)) {
            return handleRescheduleIntent(trimmed, currentTasks, tz)
        }

        // 4. Delete intent ("delete the reminder I created for tomorrow", "delete call dad")
        if (isDeleteIntent(lower)) {
            return handleDeleteIntent(lower, currentTasks)
        }

        // 5. Compound Request Check ("... and add ... to shopping list")
        // e.g. "Remind me to call Dad at 7 PM tomorrow and add milk and bread to tomorrow's shopping list"
        val compoundParts = splitCompoundIntent(trimmed)
        if (compoundParts.size > 1) {
            val actions = mutableListOf<ProposedAction>()
            for (part in compoundParts) {
                val subResult = parseSingleAction(part, tz)
                actions.addAll(subResult)
            }
            if (actions.isNotEmpty()) {
                val clarification = actions.firstOrNull { it.clarificationQuestion != null }
                if (clarification?.clarificationQuestion != null) {
                    return ParseResult(
                        replyText = clarification.clarificationQuestion,
                        proposedActions = emptyList(),
                        requiresClarification = true
                    )
                }
                val summary = buildSummary(actions)
                return ParseResult(
                    replyText = "Here's what I extracted from your request:\n$summary\nWould you like me to save these?",
                    proposedActions = actions
                )
            }
        }

        // 6. Single Action Intent (Reminder or Shopping List)
        val actions = parseSingleAction(trimmed, tz)
        if (actions.isNotEmpty()) {
            val action = actions.first()
            if (action.clarificationQuestion != null) {
                return ParseResult(
                    replyText = action.clarificationQuestion,
                    proposedActions = emptyList(),
                    requiresClarification = true
                )
            }
            val summary = buildSummary(actions)
            return ParseResult(
                replyText = "I found this action:\n$summary\nSave this?",
                proposedActions = actions
            )
        }

        // 7. General fallback: Prompt user or create a general reminder
        val dueTime = parseRelativeDateTime(lower, tz)
        val defaultAction = ProposedAction(
            actionType = ActionType.CREATE_REMINDER,
            title = trimmed,
            dueTimestamp = dueTime,
            timeZoneId = tz.id,
            category = detectCategory(trimmed),
            originalQuery = trimmed
        )
        val timeLabel = formatDueTime(dueTime, tz)
        return ParseResult(
            replyText = "Would you like me to save a reminder for “$trimmed”$timeLabel?",
            proposedActions = listOf(defaultAction)
        )
    }

    private fun isQueryIntent(lower: String): Boolean {
        return lower.startsWith("what do i have") ||
                lower.startsWith("what is due") ||
                lower.startsWith("show my") ||
                lower.startsWith("list my") ||
                lower.contains("what's on my schedule") ||
                lower.contains("what is on my schedule") ||
                lower == "today" ||
                lower == "schedule" ||
                lower == "tasks"
    }

    private fun handleQueryIntent(
        lower: String,
        currentTasks: List<TaskItem>,
        tz: TimeZone
    ): ParseResult {
        val now = System.currentTimeMillis()

        if (lower.contains("overdue")) {
            val overdue = currentTasks.filter { !it.isCompleted && it.dueTimestamp != null && it.dueTimestamp < now }
            return if (overdue.isEmpty()) {
                ParseResult(replyText = "Great news! You have no overdue tasks right now.", isQuery = true)
            } else {
                val listStr = overdue.joinToString("\n") { "• ${it.title} (due ${formatDueTime(it.dueTimestamp, tz)})" }
                ParseResult(
                    replyText = "You have ${overdue.size} overdue task${if (overdue.size > 1) "s" else ""}:\n$listStr",
                    isQuery = true,
                    queryFilteredTasks = overdue
                )
            }
        }

        if (lower.contains("today") || lower.contains("do i have to do")) {
            val todayStart = Calendar.getInstance(tz).apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
            val todayEnd = todayStart + (24 * 3600 * 1000)

            val todayTasks = currentTasks.filter { task ->
                !task.isCompleted && task.dueTimestamp != null && task.dueTimestamp in todayStart..todayEnd
            }
            return if (todayTasks.isEmpty()) {
                ParseResult(
                    replyText = "You have no remaining tasks due today. Everything looks clear!",
                    isQuery = true
                )
            } else {
                val listStr = todayTasks.joinToString("\n") { "• ${it.title} at ${formatDueTime(it.dueTimestamp, tz)}" }
                ParseResult(
                    replyText = "Here are your tasks due today:\n$listStr",
                    isQuery = true,
                    queryFilteredTasks = todayTasks
                )
            }
        }

        if (lower.contains("shopping") || lower.contains("grocery")) {
            val shoppingTasks = currentTasks.filter { it.category == TaskCategory.SHOPPING }
            return if (shoppingTasks.isEmpty()) {
                ParseResult(replyText = "Your shopping list is currently empty.", isQuery = true)
            } else {
                val allItems = shoppingTasks.flatMap { it.checklistItems }
                val uncompleted = allItems.filter { !it.isCompleted }
                val itemsStr = if (uncompleted.isEmpty()) {
                    "All items checked off!"
                } else {
                    uncompleted.joinToString(", ") { it.text }
                }
                ParseResult(
                    replyText = "Shopping List items: $itemsStr",
                    isQuery = true,
                    queryFilteredTasks = shoppingTasks
                )
            }
        }

        // Generic schedule query
        val active = currentTasks.filter { !it.isCompleted }
        return if (active.isEmpty()) {
            ParseResult(replyText = "Your schedule is currently clear. No pending tasks!", isQuery = true)
        } else {
            val listStr = active.take(5).joinToString("\n") { "• ${it.title} (${it.category.displayName})" }
            ParseResult(
                replyText = "You have ${active.size} pending task${if (active.size > 1) "s" else ""}. Here are the upcoming ones:\n$listStr",
                isQuery = true,
                queryFilteredTasks = active
            )
        }
    }

    private fun isCompleteIntent(lower: String): Boolean {
        return lower.startsWith("mark ") ||
                lower.startsWith("complete ") ||
                lower.startsWith("finish ") ||
                lower.startsWith("check off ")
    }

    private fun handleCompleteIntent(
        lower: String,
        currentTasks: List<TaskItem>,
        timeZoneId: String
    ): ParseResult {
        val targetQuery = lower
            .removePrefix("mark ")
            .removePrefix("the ")
            .removePrefix("task ")
            .removePrefix("reminder ")
            .removeSuffix(" complete")
            .removeSuffix(" as complete")
            .removeSuffix(" done")
            .removePrefix("complete ")
            .removePrefix("finish ")
            .removePrefix("check off ")
            .trim()

        val matchedTask = findBestMatchingTask(targetQuery, currentTasks.filter { !it.isCompleted })
        return if (matchedTask != null) {
            val action = ProposedAction(
                actionType = ActionType.COMPLETE_TASK,
                title = "Mark “${matchedTask.title}” complete",
                targetTaskId = matchedTask.id,
                category = matchedTask.category,
                timeZoneId = timeZoneId,
                originalQuery = lower
            )
            ParseResult(
                replyText = "I found the matching task: “${matchedTask.title}”. Would you like me to mark it complete?",
                proposedActions = listOf(action)
            )
        } else {
            ParseResult(
                replyText = "I couldn't find a pending task matching “$targetQuery”. Please check your task list."
            )
        }
    }

    private fun isRescheduleIntent(lower: String): Boolean {
        return lower.startsWith("move ") ||
                lower.startsWith("reschedule ") ||
                lower.startsWith("postpone ") ||
                lower.startsWith("change ")
    }

    private fun handleRescheduleIntent(
        trimmed: String,
        currentTasks: List<TaskItem>,
        tz: TimeZone
    ): ParseResult {
        val lower = trimmed.lowercase(Locale.ROOT)
        // e.g. "Move my call reminder to 8 PM"
        val newTime = parseRelativeDateTime(lower, tz)
        if (newTime == null) {
            return ParseResult(
                replyText = "What time would you like to reschedule the task to?"
            )
        }

        // Find which task:
        val taskPart = lower
            .removePrefix("move ")
            .removePrefix("reschedule ")
            .removePrefix("postpone ")
            .removePrefix("change ")
            .removePrefix("my ")
            .substringBefore(" to ")
            .replace("reminder", "")
            .replace("task", "")
            .trim()

        val matchedTask = findBestMatchingTask(taskPart, currentTasks.filter { !it.isCompleted })
            ?: currentTasks.firstOrNull { !it.isCompleted && it.dueTimestamp != null }

        return if (matchedTask != null) {
            val action = ProposedAction(
                actionType = ActionType.RESCHEDULE_TASK,
                title = "Reschedule “${matchedTask.title}”",
                details = "New time: ${formatDueTime(newTime, tz)}",
                dueTimestamp = newTime,
                targetTaskId = matchedTask.id,
                category = matchedTask.category,
                timeZoneId = tz.id,
                originalQuery = trimmed
            )
            ParseResult(
                replyText = "Here is the proposed reschedule:\n• “${matchedTask.title}” → ${formatDueTime(newTime, tz)}\n\nConfirm new time?",
                proposedActions = listOf(action)
            )
        } else {
            ParseResult(
                replyText = "I couldn't find an existing reminder matching “$taskPart” to reschedule."
            )
        }
    }

    private fun isDeleteIntent(lower: String): Boolean {
        return lower.startsWith("delete ") ||
                lower.startsWith("remove ") ||
                lower.startsWith("cancel ")
    }

    private fun handleDeleteIntent(lower: String, currentTasks: List<TaskItem>): ParseResult {
        val target = lower
            .removePrefix("delete ")
            .removePrefix("remove ")
            .removePrefix("cancel ")
            .removePrefix("the ")
            .removePrefix("reminder ")
            .removePrefix("task ")
            .trim()

        val matchedTask = findBestMatchingTask(target, currentTasks)
        return if (matchedTask != null) {
            val action = ProposedAction(
                actionType = ActionType.DELETE_TASK,
                title = "Delete “${matchedTask.title}”",
                targetTaskId = matchedTask.id,
                category = matchedTask.category,
                originalQuery = lower
            )
            ParseResult(
                replyText = "Are you sure you want to delete “${matchedTask.title}”? This action cannot be undone.",
                proposedActions = listOf(action)
            )
        } else {
            ParseResult(replyText = "I couldn't find any task matching “$target” to delete.")
        }
    }

    private fun splitCompoundIntent(text: String): List<String> {
        val lower = text.lowercase(Locale.ROOT)
        // Check for " and " separating two actions (e.g. "... and add ... to shopping list")
        val pattern = Pattern.compile("\\s+and\\s+(?=add\\b|remind\\b|schedule\\b|put\\b)", Pattern.CASE_INSENSITIVE)
        val matcher = pattern.matcher(text)
        if (matcher.find()) {
            val part1 = text.substring(0, matcher.start()).trim()
            val part2 = text.substring(matcher.end()).trim()
            return listOf(part1, part2)
        }
        return listOf(text)
    }

    private fun parseSingleAction(text: String, tz: TimeZone): List<ProposedAction> {
        val lower = text.lowercase(Locale.ROOT).trim()

        // Shopping / grocery list intent
        if (lower.startsWith("add ") && (lower.contains("shopping") || lower.contains("grocery") || lower.contains("list"))) {
            val rawItems = lower
                .removePrefix("add ")
                .substringBefore(" to ")
                .replace(" and ", ", ")
                .split(",")
                .map { it.trim().removePrefix("and ").trim() }
                .filter { it.isNotEmpty() && !it.contains("shopping") && !it.contains("grocery") && !it.contains("list") }

            if (rawItems.isEmpty()) {
                return listOf(
                    ProposedAction(
                        actionType = ActionType.ADD_TO_SHOPPING_LIST,
                        title = "Add to Shopping List",
                        clarificationQuestion = "What items would you like me to add to your shopping list?",
                        originalQuery = text
                    )
                )
            }

            val dueTime = parseRelativeDateTime(lower, tz)
            return listOf(
                ProposedAction(
                    actionType = ActionType.ADD_TO_SHOPPING_LIST,
                    title = "Add ${rawItems.size} item${if (rawItems.size > 1) "s" else ""} to Shopping List",
                    itemsToAdd = rawItems,
                    dueTimestamp = dueTime,
                    category = TaskCategory.SHOPPING,
                    timeZoneId = tz.id,
                    originalQuery = text
                )
            )
        }

        // Reminder / task creation intent
        if (lower.startsWith("remind me to ") || lower.startsWith("remind me ") || lower.startsWith("schedule ")) {
            val content = text
                .replaceFirst("(?i)^remind\\s+me\\s+to\\s+".toRegex(), "")
                .replaceFirst("(?i)^remind\\s+me\\s+".toRegex(), "")
                .replaceFirst("(?i)^schedule\\s+".toRegex(), "")
                .trim()

            val dueTime = parseRelativeDateTime(content.lowercase(Locale.ROOT), tz)
            val cleanedTitle = cleanTaskTitle(content)

            // If user did not provide a time or gave an ambiguous time, ask them explicitly!
            val isAmbiguous = dueTime == null ||
                    content.lowercase(Locale.ROOT).contains(" later") ||
                    content.lowercase(Locale.ROOT).contains(" sometime")

            val clarification = if (isAmbiguous) {
                "When would you like to be reminded to “$cleanedTitle”? Please tell me the day and time (for example, “Tomorrow at 7 PM” or “Today at 5 PM”)."
            } else null

            return listOf(
                ProposedAction(
                    actionType = ActionType.CREATE_REMINDER,
                    title = cleanedTitle,
                    dueTimestamp = dueTime,
                    timeZoneId = tz.id,
                    category = detectCategory(cleanedTitle),
                    clarificationQuestion = clarification,
                    originalQuery = text
                )
            )
        }

        // "Add milk, bread to shopping list" without explicit "to my shopping list"
        if (lower.contains("shopping list") || lower.contains("grocery list")) {
            val rawItems = lower
                .removePrefix("add ")
                .substringBefore(" to ")
                .replace(" and ", ", ")
                .split(",")
                .map { it.trim().removePrefix("and ").trim() }
                .filter { it.isNotEmpty() }
            if (rawItems.isNotEmpty()) {
                return listOf(
                    ProposedAction(
                        actionType = ActionType.ADD_TO_SHOPPING_LIST,
                        title = "Add ${rawItems.size} item${if (rawItems.size > 1) "s" else ""} to Shopping List",
                        itemsToAdd = rawItems,
                        category = TaskCategory.SHOPPING,
                        timeZoneId = tz.id,
                        originalQuery = text
                    )
                )
            }
        }

        return emptyList()
    }

    private fun cleanTaskTitle(raw: String): String {
        return raw
            .replaceFirst("(?i)\\s+at\\s+\\d{1,2}(?::\\d{2})?\\s*(?:am|pm|a\\.m\\.|p\\.m\\.)?".toRegex(), "")
            .replaceFirst("(?i)\\s+tomorrow(?:\\s+at\\s+\\d{1,2}(?::\\d{2})?\\s*(?:am|pm)?)?".toRegex(), "")
            .replaceFirst("(?i)\\s+today(?:\\s+at\\s+\\d{1,2}(?::\\d{2})?\\s*(?:am|pm)?)?".toRegex(), "")
            .replaceFirst("(?i)\\s+tonight(?:\\s+at\\s+\\d{1,2}(?::\\d{2})?\\s*(?:am|pm)?)?".toRegex(), "")
            .replaceFirst("(?i)\\s+in\\s+\\d+\\s+hours?".toRegex(), "")
            .trim()
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }
    }

    /**
     * Parses relative date and times in the user's specific TimeZone.
     * Guaranteed never to silently shift dates or assume UTC is local.
     */
    fun parseRelativeDateTime(input: String, tz: TimeZone): Long? {
        val cal = Calendar.getInstance(tz)

        var hasDate = false
        var hasTime = false

        // Day offsets
        if (input.contains("tomorrow")) {
            cal.add(Calendar.DAY_OF_YEAR, 1)
            hasDate = true
        } else if (input.contains("today") || input.contains("tonight")) {
            hasDate = true
            if (input.contains("tonight") && !input.contains("pm")) {
                cal.set(Calendar.HOUR_OF_DAY, 20)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                hasTime = true
            }
        } else if (input.contains("day after tomorrow")) {
            cal.add(Calendar.DAY_OF_YEAR, 2)
            hasDate = true
        }

        // Relative hours offset ("in 2 hours")
        val inHoursPattern = Pattern.compile("in\\s+(\\d+)\\s+hours?", Pattern.CASE_INSENSITIVE)
        val inHoursMatcher = inHoursPattern.matcher(input)
        if (inHoursMatcher.find()) {
            val hours = inHoursMatcher.group(1)?.toIntOrNull() ?: 1
            return System.currentTimeMillis() + (hours * 3600 * 1000)
        }

        // Specific time: "at 7 PM", "7:30 pm", "10 am", "at 7"
        val timePattern = Pattern.compile("(?:at\\s+)?(\\d{1,2})(?::(\\d{2}))?\\s*(am|pm|a\\.m\\.|p\\.m\\.)?", Pattern.CASE_INSENSITIVE)
        val matcher = timePattern.matcher(input)

        // Find the last or best time match in string
        var matchedHour: Int? = null
        var matchedMin = 0
        var isPm: Boolean? = null

        while (matcher.find()) {
            val hStr = matcher.group(1)
            val mStr = matcher.group(2)
            val ampmStr = matcher.group(3)?.lowercase(Locale.ROOT)

            if (hStr != null) {
                val h = hStr.toIntOrNull() ?: continue
                if (h in 1..24) {
                    matchedHour = h
                    matchedMin = mStr?.toIntOrNull() ?: 0
                    if (ampmStr != null) {
                        isPm = ampmStr.contains("p")
                    }
                }
            }
        }

        if (matchedHour != null) {
            hasTime = true
            var hour = matchedHour
            if (isPm == true && hour < 12) {
                hour += 12
            } else if (isPm == false && hour == 12) {
                hour = 0
            } else if (isPm == null && hour in 1..6) {
                // Household default: 1-6 without AM/PM is usually PM for tasks
                hour += 12
            }
            cal.set(Calendar.HOUR_OF_DAY, hour)
            cal.set(Calendar.MINUTE, matchedMin)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)

            // If no explicit date was mentioned and the time is already past today, roll to tomorrow
            if (!hasDate && cal.timeInMillis < System.currentTimeMillis()) {
                cal.add(Calendar.DAY_OF_YEAR, 1)
            }
        } else if (hasDate && !hasTime) {
            // Default morning time: 9:00 AM
            cal.set(Calendar.HOUR_OF_DAY, 9)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
        }

        return if (hasDate || hasTime) cal.timeInMillis else null
    }

    private fun detectCategory(title: String): TaskCategory {
        val lower = title.lowercase(Locale.ROOT)
        return when {
            lower.contains("dad") || lower.contains("mom") || lower.contains("family") || lower.contains("call") || lower.contains("kid") -> TaskCategory.FAMILY
            lower.contains("milk") || lower.contains("bread") || lower.contains("grocer") || lower.contains("shopping") || lower.contains("buy") -> TaskCategory.SHOPPING
            lower.contains("doctor") || lower.contains("prescription") || lower.contains("medicine") || lower.contains("pill") || lower.contains("health") -> TaskCategory.HEALTH
            lower.contains("clean") || lower.contains("smoke") || lower.contains("detector") || lower.contains("trash") || lower.contains("repair") || lower.contains("house") -> TaskCategory.HOUSEHOLD
            else -> TaskCategory.GENERAL
        }
    }

    private fun findBestMatchingTask(query: String, tasks: List<TaskItem>): TaskItem? {
        val q = query.lowercase(Locale.ROOT).trim()
        if (q.isEmpty()) return null
        // 1. Exact match or title contains query
        val direct = tasks.firstOrNull { it.title.lowercase(Locale.ROOT).contains(q) }
        if (direct != null) return direct

        // 2. Query words overlap
        val qWords = q.split(" ").filter { it.length > 2 }
        return tasks.maxByOrNull { task ->
            val tLower = task.title.lowercase(Locale.ROOT)
            qWords.count { tLower.contains(it) }
        }?.takeIf { task ->
            val tLower = task.title.lowercase(Locale.ROOT)
            qWords.any { tLower.contains(it) }
        }
    }

    private fun buildSummary(actions: List<ProposedAction>): String {
        return actions.joinToString("\n") { action ->
            when (action.actionType) {
                ActionType.CREATE_REMINDER -> {
                    val timeStr = if (action.dueTimestamp != null) " — ${formatDueTime(action.dueTimestamp, TimeZone.getTimeZone(action.timeZoneId))}" else ""
                    "• ${action.title}$timeStr"
                }
                ActionType.ADD_TO_SHOPPING_LIST -> {
                    val items = action.itemsToAdd.joinToString(", ")
                    "• Shopping list — $items"
                }
                ActionType.RESCHEDULE_TASK -> "• ${action.title} (${action.details})"
                ActionType.COMPLETE_TASK -> "• ${action.title}"
                ActionType.DELETE_TASK -> "• ${action.title}"
                ActionType.QUERY_TASKS -> "• Query tasks"
            }
        }
    }

    fun formatDueTime(timestamp: Long?, tz: TimeZone): String {
        if (timestamp == null) return "No specific time"
        val cal = Calendar.getInstance(tz).apply { timeInMillis = timestamp }
        val now = Calendar.getInstance(tz)

        val isToday = cal.get(Calendar.YEAR) == now.get(Calendar.YEAR) &&
                cal.get(Calendar.DAY_OF_YEAR) == now.get(Calendar.DAY_OF_YEAR)

        val tomorrowCal = Calendar.getInstance(tz).apply { add(Calendar.DAY_OF_YEAR, 1) }
        val isTomorrow = cal.get(Calendar.YEAR) == tomorrowCal.get(Calendar.YEAR) &&
                cal.get(Calendar.DAY_OF_YEAR) == tomorrowCal.get(Calendar.DAY_OF_YEAR)

        val hour = cal.get(Calendar.HOUR).let { if (it == 0) 12 else it }
        val minute = cal.get(Calendar.MINUTE)
        val ampm = if (cal.get(Calendar.AM_PM) == Calendar.AM) "AM" else "PM"
        val timeStr = if (minute == 0) "$hour $ampm" else String.format(Locale.ROOT, "%d:%02d %s", hour, minute, ampm)

        return when {
            isToday -> "Today at $timeStr"
            isTomorrow -> "Tomorrow at $timeStr"
            else -> {
                val months = arrayOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
                "${months[cal.get(Calendar.MONTH)]} ${cal.get(Calendar.DAY_OF_MONTH)} at $timeStr"
            }
        }
    }
}
