package com.example.carecue.ui.lists

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.carecue.data.model.ChecklistItem
import com.example.carecue.data.model.TaskCategory
import com.example.carecue.ui.viewmodel.CareCueViewModel
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianDarkest
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.ObsidianSurfaceElevated
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SlateMuted
import com.example.ui.theme.StatusSuccess
import com.example.ui.theme.SunsetOrangePrimary

@Composable
fun ListsScreen(
    viewModel: CareCueViewModel,
    modifier: Modifier = Modifier
) {
    val tasks by viewModel.tasks.collectAsStateWithLifecycle()
    val shoppingLists = tasks.filter { it.category == TaskCategory.SHOPPING || it.checklistItems.isNotEmpty() }

    var newItemText by remember { mutableStateOf("") }
    var selectedListId by remember { mutableStateOf<String?>(null) }
    var showCreateListDialog by remember { mutableStateOf(false) }
    var newListName by remember { mutableStateOf("") }

    val activeList = shoppingLists.find { it.id == selectedListId } ?: shoppingLists.firstOrNull()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianDarkest)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(ObsidianSurface)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Checklists & Shopping",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = PureWhite
                            )
                        )
                        Text(
                            text = "Household essentials & care items",
                            style = MaterialTheme.typography.bodySmall.copy(color = SlateMuted)
                        )
                    }

                    Button(
                        onClick = { showCreateListDialog = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SunsetOrangePrimary,
                            contentColor = ObsidianDarkest
                        ),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("create_new_list_button")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("New List", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            if (shoppingLists.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(CircleShape)
                                .background(SunsetOrangePrimary.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.ShoppingCart,
                                contentDescription = null,
                                tint = SunsetOrangePrimary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No Checklists Yet",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = PureWhite
                            )
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Tell CareCue:\n“Add milk, eggs, and bread to shopping list”\nor tap New List above.",
                            style = MaterialTheme.typography.bodySmall.copy(color = SlateMuted),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            } else if (activeList != null) {
                // List Selector Pills if multiple
                if (shoppingLists.size > 1) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(ObsidianSurface)
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        shoppingLists.forEach { list ->
                            val isSelected = list.id == activeList.id
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSelected) SunsetOrangePrimary else ObsidianSurfaceElevated)
                                    .border(1.dp, if (isSelected) SunsetOrangePrimary else ObsidianBorder, RoundedCornerShape(12.dp))
                                    .clickable { selectedListId = list.id }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = list.title,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) ObsidianDarkest else PureWhite
                                )
                            }
                        }
                    }
                }

                // Add item row
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(ObsidianSurfaceElevated)
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = newItemText,
                            onValueChange = { newItemText = it },
                            placeholder = { Text("Add item (e.g. 2 gallons milk)...", color = SlateMuted, fontSize = 13.sp) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("add_checklist_item_input"),
                            shape = RoundedCornerShape(20.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = PureWhite,
                                unfocusedTextColor = PureWhite,
                                focusedBorderColor = SunsetOrangePrimary,
                                unfocusedBorderColor = ObsidianBorder,
                                focusedContainerColor = ObsidianDarkest,
                                unfocusedContainerColor = ObsidianDarkest
                            ),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        IconButton(
                            onClick = {
                                if (newItemText.isNotBlank()) {
                                    viewModel.addChecklistItem(activeList.id, newItemText)
                                    newItemText = ""
                                }
                            },
                            enabled = newItemText.isNotBlank(),
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(if (newItemText.isNotBlank()) SunsetOrangePrimary else ObsidianSurface)
                                .testTag("add_checklist_item_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Add item",
                                tint = if (newItemText.isNotBlank()) ObsidianDarkest else SlateMuted
                            )
                        }
                    }
                }

                // Checklist Items
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (activeList.checklistItems.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(ObsidianSurface)
                                    .padding(20.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No items yet. Type above or tell the assistant!",
                                    style = MaterialTheme.typography.bodySmall.copy(color = SlateMuted)
                                )
                            }
                        }
                    } else {
                        items(activeList.checklistItems, key = { it.id }) { item ->
                            ChecklistItemRow(
                                item = item,
                                onToggle = { viewModel.toggleChecklistItem(activeList.id, item.id) },
                                onDelete = { viewModel.removeChecklistItem(activeList.id, item.id) }
                            )
                        }
                    }
                }
            }
        }

        // New List Dialog
        if (showCreateListDialog) {
            Dialog(onDismissRequest = { showCreateListDialog = false }) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = ObsidianSurface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, ObsidianBorder),
                    modifier = Modifier.fillMaxWidth().testTag("create_list_dialog")
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            text = "Create Checklist",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = PureWhite
                            )
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = newListName,
                            onValueChange = { newListName = it },
                            label = { Text("List Name (e.g. Pharmacy, Groceries)") },
                            modifier = Modifier.fillMaxWidth().testTag("new_list_name_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = PureWhite,
                                unfocusedTextColor = PureWhite,
                                focusedBorderColor = SunsetOrangePrimary,
                                unfocusedBorderColor = ObsidianBorder
                            ),
                            shape = RoundedCornerShape(10.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            Button(
                                onClick = {
                                    if (newListName.isNotBlank()) {
                                        viewModel.quickCreateTask(
                                            title = newListName.trim(),
                                            description = "Household checklist",
                                            category = TaskCategory.SHOPPING,
                                            dueTimestamp = null
                                        )
                                        newListName = ""
                                        showCreateListDialog = false
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = SunsetOrangePrimary,
                                    contentColor = ObsidianDarkest
                                )
                            ) {
                                Text("Create List", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ChecklistItemRow(
    item: ChecklistItem,
    onToggle: () -> Unit,
    onDelete: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(ObsidianSurface)
            .border(1.dp, ObsidianBorder, RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp)
            .testTag("checklist_item_${item.id}")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .weight(1f)
                    .clickable { onToggle() }
            ) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(if (item.isCompleted) StatusSuccess else ObsidianDarkest)
                        .border(
                            1.5.dp,
                            if (item.isCompleted) StatusSuccess else SunsetOrangePrimary.copy(alpha = 0.6f),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (item.isCompleted) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Done",
                            tint = ObsidianDarkest,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Text(
                    text = item.text,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = if (item.isCompleted) SlateMuted else PureWhite,
                        textDecoration = if (item.isCompleted) TextDecoration.LineThrough else null,
                        fontWeight = if (item.isCompleted) FontWeight.Normal else FontWeight.Medium
                    )
                )
            }

            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(28.dp).testTag("delete_item_${item.id}")
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Delete item",
                    tint = SlateMuted,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
