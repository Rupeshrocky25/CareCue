package com.example.carecue.data.local

import androidx.room.TypeConverter
import com.example.carecue.data.model.ActionType
import com.example.carecue.data.model.ChecklistItem
import com.example.carecue.data.model.ProposedAction
import com.example.carecue.data.model.TaskCategory
import org.json.JSONArray
import org.json.JSONObject

class Converters {

    @TypeConverter
    fun fromChecklistItems(items: List<ChecklistItem>?): String {
        if (items.isNullOrEmpty()) return "[]"
        val array = JSONArray()
        for (item in items) {
            val obj = JSONObject()
            obj.put("id", item.id)
            obj.put("text", item.text)
            obj.put("isCompleted", item.isCompleted)
            array.put(obj)
        }
        return array.toString()
    }

    @TypeConverter
    fun toChecklistItems(data: String?): List<ChecklistItem> {
        if (data.isNullOrBlank() || data == "[]") return emptyList()
        val list = mutableListOf<ChecklistItem>()
        try {
            val array = JSONArray(data)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    ChecklistItem(
                        id = obj.optString("id"),
                        text = obj.optString("text"),
                        isCompleted = obj.optBoolean("isCompleted", false)
                    )
                )
            }
        } catch (_: Exception) {}
        return list
    }

    @TypeConverter
    fun fromProposedActions(actions: List<ProposedAction>?): String {
        if (actions.isNullOrEmpty()) return "[]"
        val array = JSONArray()
        for (action in actions) {
            val obj = JSONObject()
            obj.put("id", action.id)
            obj.put("actionType", action.actionType.name)
            obj.put("title", action.title)
            obj.put("description", action.description ?: "")
            obj.put("details", action.details ?: "")
            obj.put("dueTimestamp", action.dueTimestamp ?: -1L)
            obj.put("timeZoneId", action.timeZoneId)
            obj.put("category", action.category.name)
            obj.put("targetTaskId", action.targetTaskId ?: "")
            obj.put("clarificationQuestion", action.clarificationQuestion ?: "")
            obj.put("originalQuery", action.originalQuery)

            val itemsArray = JSONArray()
            for (item in action.itemsToAdd) {
                itemsArray.put(item)
            }
            obj.put("itemsToAdd", itemsArray)
            array.put(obj)
        }
        return array.toString()
    }

    @TypeConverter
    fun toProposedActions(data: String?): List<ProposedAction> {
        if (data.isNullOrBlank() || data == "[]") return emptyList()
        val list = mutableListOf<ProposedAction>()
        try {
            val array = JSONArray(data)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val itemsList = mutableListOf<String>()
                val itemsArr = obj.optJSONArray("itemsToAdd")
                if (itemsArr != null) {
                    for (j in 0 until itemsArr.length()) {
                        itemsList.add(itemsArr.getString(j))
                    }
                }
                val dueTs = obj.optLong("dueTimestamp", -1L)
                list.add(
                    ProposedAction(
                        id = obj.optString("id"),
                        actionType = ActionType.valueOf(obj.optString("actionType", ActionType.CREATE_REMINDER.name)),
                        title = obj.optString("title"),
                        description = obj.optString("description").takeIf { it.isNotEmpty() },
                        details = obj.optString("details").takeIf { it.isNotEmpty() },
                        dueTimestamp = if (dueTs > 0) dueTs else null,
                        timeZoneId = obj.optString("timeZoneId", "UTC"),
                        category = TaskCategory.fromString(obj.optString("category", TaskCategory.GENERAL.name)),
                        targetTaskId = obj.optString("targetTaskId").takeIf { it.isNotEmpty() },
                        itemsToAdd = itemsList,
                        clarificationQuestion = obj.optString("clarificationQuestion").takeIf { it.isNotEmpty() },
                        originalQuery = obj.optString("originalQuery")
                    )
                )
            }
        } catch (_: Exception) {}
        return list
    }
}
