package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.carecue.data.local.CareCueDatabase
import com.example.carecue.data.repository.SettingsRepository
import com.example.carecue.data.repository.TaskRepository
import com.example.carecue.notifications.CareCueNotificationHelper
import com.example.carecue.ui.assistant.AssistantScreen
import com.example.carecue.ui.components.WaveformAnimation
import com.example.carecue.ui.lists.ListsScreen
import com.example.carecue.ui.schedule.ScheduleScreen
import com.example.carecue.ui.settings.SettingsScreen
import com.example.carecue.ui.viewmodel.CareCueViewModel
import com.example.ui.theme.CareCueTheme
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianDarkest
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.ObsidianSurfaceElevated
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SlateMuted
import com.example.ui.theme.SunsetOrangeContainer
import com.example.ui.theme.SunsetOrangePrimary

data class NavDestination(
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val tag: String
)

class MainActivity : ComponentActivity() {

    private val viewModel: CareCueViewModel by viewModels {
        val db = CareCueDatabase.getDatabase(applicationContext)
        val taskRepo = TaskRepository(db.taskDao(), db.chatMessageDao())
        val settingsRepo = SettingsRepository(applicationContext)
        CareCueViewModel.Factory(taskRepo, settingsRepo)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        CareCueNotificationHelper.createNotificationChannel(applicationContext)

        setContent {
            CareCueTheme {
                CareCueApp(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun CareCueApp(viewModel: CareCueViewModel) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    var selectedIndex by rememberSaveable { mutableIntStateOf(0) }
    var isSettingsOpen by rememberSaveable { mutableStateOf(false) }

    // If onboarding is not completed, ask user for their details first!
    if (!settings.isOnboardingCompleted) {
        com.example.carecue.ui.components.OnboardingDialog(
            initialTimezoneId = settings.timeZoneId,
            onComplete = { userName, focus, timeZoneId ->
                viewModel.completeOnboarding(userName, focus, timeZoneId)
            }
        )
    }

    // Clean back stack handling
    BackHandler(enabled = isSettingsOpen || selectedIndex != 0) {
        if (isSettingsOpen) {
            isSettingsOpen = false
        } else {
            selectedIndex = 0
        }
    }

    // Clean, limited 3 core navigation destinations
    val destinations = listOf(
        NavDestination("Assistant", Icons.Filled.ChatBubble, Icons.Outlined.ChatBubbleOutline, "nav_assistant"),
        NavDestination("Schedule", Icons.Filled.DateRange, Icons.Outlined.DateRange, "nav_schedule"),
        NavDestination("Lists", Icons.Filled.ShoppingCart, Icons.Outlined.ShoppingCart, "nav_lists")
    )

    BoxWithConstraints(modifier = Modifier.fillMaxSize().background(ObsidianDarkest)) {
        val isWideScreen = maxWidth >= 600.dp

        Scaffold(
            containerColor = ObsidianDarkest,
            topBar = {
                // Sleek Top App Bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(ObsidianSurface)
                        .border(width = 0.5.dp, color = ObsidianBorder, shape = RoundedCornerShape(0.dp))
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isSettingsOpen) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = { isSettingsOpen = false },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = "Back",
                                        tint = SunsetOrangePrimary
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Settings & Controls",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = PureWhite
                                    )
                                )
                            }
                        } else {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(SunsetOrangeContainer)
                                        .border(1.dp, SunsetOrangePrimary.copy(alpha = 0.5f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    WaveformAnimation(
                                        isListening = false,
                                        barCount = 3,
                                        maxHeight = 16.dp,
                                        minHeight = 6.dp,
                                        activeColor = SunsetOrangePrimary,
                                        inactiveColor = SunsetOrangePrimary
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "CareCue",
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                fontWeight = FontWeight.ExtraBold,
                                                color = PureWhite,
                                                letterSpacing = 0.3.sp
                                            )
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(SunsetOrangePrimary.copy(alpha = 0.15f))
                                                .padding(horizontal = 5.dp, vertical = 1.dp)
                                        ) {
                                            Text(
                                                text = "Alexa+",
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = SunsetOrangePrimary
                                            )
                                        }
                                    }
                                    Text(
                                        text = "Small things remembered",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = SlateMuted,
                                            fontSize = 11.sp
                                        )
                                    )
                                }
                            }

                            IconButton(
                                onClick = { isSettingsOpen = true },
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(ObsidianSurfaceElevated)
                                    .testTag("open_settings_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = "Settings",
                                    tint = SlateMuted,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            },
            bottomBar = {
                if (!isSettingsOpen && !isWideScreen) {
                    NavigationBar(
                        containerColor = ObsidianSurface,
                        contentColor = PureWhite,
                        tonalElevation = 0.dp,
                        modifier = Modifier
                            .navigationBarsPadding()
                            .border(width = 0.5.dp, color = ObsidianBorder, shape = RoundedCornerShape(0.dp))
                            .testTag("carecue_bottom_nav")
                    ) {
                        destinations.forEachIndexed { index, dest ->
                            val isSelected = selectedIndex == index
                            NavigationBarItem(
                                selected = isSelected,
                                onClick = { selectedIndex = index },
                                icon = {
                                    Icon(
                                        imageVector = if (isSelected) dest.selectedIcon else dest.unselectedIcon,
                                        contentDescription = dest.title,
                                        tint = if (isSelected) ObsidianDarkest else SlateMuted,
                                        modifier = Modifier.size(20.dp)
                                    )
                                },
                                label = {
                                    Text(
                                        text = dest.title,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) SunsetOrangePrimary else SlateMuted
                                    )
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    indicatorColor = SunsetOrangePrimary
                                ),
                                modifier = Modifier.testTag(dest.tag)
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (isSettingsOpen) {
                    SettingsScreen(viewModel = viewModel)
                } else if (isWideScreen) {
                    Row(modifier = Modifier.fillMaxSize()) {
                        NavigationRail(
                            containerColor = ObsidianSurface,
                            contentColor = PureWhite,
                            modifier = Modifier.fillMaxHeight().testTag("carecue_nav_rail")
                        ) {
                            destinations.forEachIndexed { index, dest ->
                                val isSelected = selectedIndex == index
                                NavigationRailItem(
                                    selected = isSelected,
                                    onClick = { selectedIndex = index },
                                    icon = {
                                        Icon(
                                            imageVector = if (isSelected) dest.selectedIcon else dest.unselectedIcon,
                                            contentDescription = dest.title,
                                            tint = if (isSelected) ObsidianDarkest else SlateMuted,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    },
                                    label = {
                                        Text(
                                            text = dest.title,
                                            fontSize = 11.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) SunsetOrangePrimary else SlateMuted
                                        )
                                    },
                                    colors = NavigationRailItemDefaults.colors(
                                        indicatorColor = SunsetOrangePrimary
                                    ),
                                    modifier = Modifier.testTag(dest.tag)
                                )
                            }
                        }

                        Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                            MainContentScreen(
                                selectedIndex = selectedIndex,
                                viewModel = viewModel
                            )
                        }
                    }
                } else {
                    MainContentScreen(
                        selectedIndex = selectedIndex,
                        viewModel = viewModel
                    )
                }
            }
        }
    }
}

@Composable
fun MainContentScreen(
    selectedIndex: Int,
    viewModel: CareCueViewModel
) {
    when (selectedIndex) {
        0 -> AssistantScreen(viewModel = viewModel)
        1 -> ScheduleScreen(viewModel = viewModel)
        2 -> ListsScreen(viewModel = viewModel)
        else -> AssistantScreen(viewModel = viewModel)
    }
}
