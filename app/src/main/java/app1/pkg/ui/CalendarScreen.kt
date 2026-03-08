package app1.pkg.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuOpen
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import app1.pkg.R
import app1.pkg.model.CalendarEvent
import app1.pkg.model.CalendarSettings
import app1.pkg.model.WeatherInfo
import app1.pkg.network.getWeatherDescriptionRes
import app1.pkg.viewmodel.CalendarViewModel
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.TemporalAdjusters
import java.util.Locale
import kotlin.math.ceil

enum class CalendarViewMode { MONTH, WEEK, DAY }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(viewModel: CalendarViewModel = viewModel()) {
    val events by viewModel.events
    val selectedDate by viewModel.selectedDate
    val weatherData by viewModel.weatherData
    val hourlyWeatherData by viewModel.hourlyWeatherData
    val isLoadingWeather by viewModel.isLoadingWeather
    val settings by viewModel.settings
    val datesWithEvents by viewModel.datesWithEvents
    
    var currentMonth by remember { mutableStateOf(YearMonth.from(selectedDate)) }
    var viewMode by remember { mutableStateOf(CalendarViewMode.MONTH) }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    var showEventDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var showWeatherDialog by remember { mutableStateOf(false) }
    var editingEvent by remember { mutableStateOf<CalendarEvent?>(null) }
    var weatherDate by remember { mutableStateOf(LocalDate.now()) }

    // Dynamic color values based on whether a wallpaper is active
    val hasWallpaper = settings.wallpaperUri != null
    val onBgColor = if (hasWallpaper) Color.White else MaterialTheme.colorScheme.onBackground
    val secondaryOnBgColor = if (hasWallpaper) Color.White.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant
    val surfaceContainerColor = if (hasWallpaper) Color.White.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surface
    val surfaceBorderColor = if (hasWallpaper) Color.White.copy(alpha = 0.15f) else MaterialTheme.colorScheme.outlineVariant

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            SidebarContent(
                onClose = { scope.launch { drawerState.close() } },
                onAddEvent = {
                    editingEvent = null
                    showEventDialog = true
                    scope.launch { drawerState.close() }
                },
                onOpenSettings = {
                    showSettingsDialog = true
                    scope.launch { drawerState.close() }
                }
            )
        }
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Wallpaper Background
            if (hasWallpaper) {
                AsyncImage(
                    model = settings.wallpaperUri,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.5f))
                        .blur(20.dp)
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                )
            }

            Scaffold(
                containerColor = Color.Transparent,
                topBar = {
                    TopBar(
                        onMenuClick = { scope.launch { drawerState.open() } },
                        onViewModeChange = { viewMode = it },
                        viewMode = viewMode,
                        onBgColor = onBgColor
                    )
                }
            ) { innerPadding ->
                Column(
                    modifier = Modifier
                        .padding(innerPadding)
                        .fillMaxSize()
                ) {
                    // Main Calendar Card
                    Surface(
                        shape = RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp),
                        color = surfaceContainerColor,
                        border = BorderStroke(1.dp, surfaceBorderColor),
                        tonalElevation = 4.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            // Navigation Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(onClick = { currentMonth = currentMonth.minusMonths(1) }) {
                                        Icon(Icons.Default.ChevronLeft, contentDescription = "Prev", tint = onBgColor)
                                    }
                                    Text(
                                        text = currentMonth.format(DateTimeFormatter.ofPattern("MMM yyyy", Locale.getDefault())),
                                        color = onBgColor,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp)
                                    )
                                    IconButton(onClick = { currentMonth = currentMonth.plusMonths(1) }) {
                                        Icon(Icons.Default.ChevronRight, contentDescription = "Next", tint = onBgColor)
                                    }
                                }
                                
                                TextButton(onClick = {
                                    val today = LocalDate.now()
                                    viewModel.selectDate(today)
                                    currentMonth = YearMonth.from(today)
                                }) {
                                    Text(stringResource(R.string.today), color = onBgColor, fontWeight = FontWeight.SemiBold)
                                }
                            }

                            Box(modifier = Modifier.fillMaxWidth().animateContentSize()) {
                                when (viewMode) {
                                    CalendarViewMode.MONTH -> MonthGrid(
                                        currentMonth = currentMonth,
                                        selectedDate = selectedDate,
                                        datesWithEvents = datesWithEvents,
                                        onDateSelected = { viewModel.selectDate(it) },
                                        onDateDoubleClicked = {
                                            weatherDate = it
                                            showWeatherDialog = true
                                            viewModel.fetchWeatherForDate(it)
                                        },
                                        onBgColor = onBgColor
                                    )
                                    CalendarViewMode.WEEK -> WeekStrip(
                                        selectedDate = selectedDate,
                                        onDateSelected = { viewModel.selectDate(it) },
                                        onBgColor = onBgColor
                                    )
                                    CalendarViewMode.DAY -> DayHeader(selectedDate = selectedDate, onBgColor = onBgColor)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Events List / Timeline
                    Box(modifier = Modifier.weight(1f).padding(horizontal = 16.dp)) {
                        when (viewMode) {
                            CalendarViewMode.MONTH -> EventListSection(
                                selectedDate = selectedDate,
                                events = events,
                                weatherData = weatherData,
                                onDeleteEvent = { viewModel.deleteEvent(it) },
                                onEditEvent = {
                                    editingEvent = it
                                    showEventDialog = true
                                },
                                onToggleNotification = { viewModel.toggleNotification(it) },
                                onBgColor = onBgColor,
                                secondaryOnBgColor = secondaryOnBgColor
                            )
                            CalendarViewMode.WEEK, CalendarViewMode.DAY -> TimelineView(
                                selectedDate = selectedDate,
                                events = events,
                                onEventClick = {
                                    editingEvent = it
                                    showEventDialog = true
                                },
                                onBgColor = onBgColor
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.navigationBarsPadding())
                }
            }
        }
    }

    if (showEventDialog) {
        EventDialog(
            event = editingEvent,
            onDismiss = { showEventDialog = false },
            onSave = { title, time, description, isNotificationEnabled ->
                if (editingEvent != null) {
                    viewModel.updateEvent(editingEvent!!.copy(
                        title = title, 
                        time = time, 
                        description = description,
                        isNotificationEnabled = isNotificationEnabled
                    ))
                } else {
                    viewModel.addEvent(title, selectedDate, time, description, isNotificationEnabled = isNotificationEnabled)
                }
                showEventDialog = false
            }
        )
    }

    if (showSettingsDialog) {
        SettingsDialog(
            settings = settings,
            onDismiss = { showSettingsDialog = false },
            onUpdateSettings = { viewModel.updateSettings(it) },
            onSaveWallpaper = { viewModel.saveWallpaper(it) },
            onResetSettings = { viewModel.resetSettings() }
        )
    }

    if (showWeatherDialog) {
        WeatherDialog(
            weatherInfo = weatherData[weatherDate],
            hourlyWeatherData = hourlyWeatherData,
            weatherDate = weatherDate,
            isLoading = isLoadingWeather,
            onDismiss = { showWeatherDialog = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopBar(
    onMenuClick: () -> Unit,
    onViewModeChange: (CalendarViewMode) -> Unit,
    viewMode: CalendarViewMode,
    onBgColor: Color
) {
    TopAppBar(
        title = {
            Text(
                text = "DayPlan",
                style = MaterialTheme.typography.titleLarge,
                color = onBgColor,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Visible
            )
        },
        navigationIcon = {
            IconButton(onClick = onMenuClick) {
                Icon(Icons.Default.Menu, contentDescription = "Sidebar", tint = onBgColor)
            }
        },
        actions = {
            ViewModeSwitcher(
                currentMode = viewMode,
                onModeChange = onViewModeChange,
                modifier = Modifier.width(210.dp), // Reverted to previous scale
                onBgColor = onBgColor
            )
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.Transparent
        )
    )
}

@Composable
fun ViewModeSwitcher(
    currentMode: CalendarViewMode,
    onModeChange: (CalendarViewMode) -> Unit,
    modifier: Modifier = Modifier,
    onBgColor: Color
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = onBgColor.copy(alpha = 0.15f),
        modifier = modifier.padding(end = 8.dp)
    ) {
        Row(
            modifier = Modifier.padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            listOf(CalendarViewMode.MONTH, CalendarViewMode.WEEK, CalendarViewMode.DAY).forEach { mode ->
                val isSelected = currentMode == mode
                val label = when(mode) {
                    CalendarViewMode.MONTH -> stringResource(R.string.view_month)
                    CalendarViewMode.WEEK -> stringResource(R.string.view_week)
                    CalendarViewMode.DAY -> stringResource(R.string.view_day)
                }
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (isSelected) onBgColor else Color.Transparent,
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onModeChange(mode) }
                ) {
                    Text(
                        text = label,
                        modifier = Modifier.padding(vertical = 10.dp),
                        style = MaterialTheme.typography.labelLarge,
                        color = if (isSelected) {
                            if (onBgColor == Color.White) Color.Black else MaterialTheme.colorScheme.onPrimary
                        } else onBgColor,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        textAlign = TextAlign.Center,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
fun SidebarContent(
    onClose: () -> Unit,
    onAddEvent: () -> Unit,
    onOpenSettings: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxHeight().width(280.dp),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topEnd = 24.dp, bottomEnd = 24.dp)
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onClose) {
                    Icon(Icons.AutoMirrored.Filled.MenuOpen, contentDescription = "Close")
                }
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            Text("DayPlan", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            
            Spacer(modifier = Modifier.height(32.dp))
            
            SidebarItem(icon = Icons.Default.CalendarToday, label = stringResource(R.string.my_plans), isSelected = true, onClick = onClose)
            SidebarItem(icon = Icons.Default.Add, label = stringResource(R.string.add_new_event), onClick = onAddEvent)
            
            Spacer(modifier = Modifier.weight(1f))
            
            SidebarItem(icon = Icons.Default.Settings, label = stringResource(R.string.settings), onClick = onOpenSettings)
        }
    }
}

@Composable
fun SidebarItem(
    icon: ImageVector,
    label: String,
    isSelected: Boolean = false,
    onClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) else Color.Transparent)
            .clickable { onClick() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.width(16.dp))
        Text(label, color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
    }
}

@Composable
fun MonthGrid(
    currentMonth: YearMonth,
    selectedDate: LocalDate,
    datesWithEvents: Set<LocalDate>,
    onDateSelected: (LocalDate) -> Unit,
    onDateDoubleClicked: (LocalDate) -> Unit,
    onBgColor: Color
) {
    CalendarGrid(
        currentMonth = currentMonth,
        selectedDate = selectedDate,
        datesWithEvents = datesWithEvents,
        onDateSelected = onDateSelected,
        onDateDoubleClicked = onDateDoubleClicked,
        onBgColor = onBgColor
    )
}

@Composable
fun WeekStrip(
    selectedDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
    onBgColor: Color
) {
    val startOfWeek = selectedDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    val weekDates = (0..6).map { startOfWeek.plusDays(it.toLong()) }

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
        weekDates.forEach { date ->
            val isSelected = date == selectedDate
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
                    .clickable { onDateSelected(date) }
                    .padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    date.dayOfWeek.getDisplayName(TextStyle.NARROW, Locale.getDefault()),
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else onBgColor.copy(alpha = 0.6f),
                    style = MaterialTheme.typography.labelSmall
                )
                Text(
                    date.dayOfMonth.toString(),
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else onBgColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
        }
    }
}

@Composable
fun DayHeader(selectedDate: LocalDate, onBgColor: Color) {
    Text(
        text = selectedDate.format(DateTimeFormatter.ofPattern("EEEE, d MMMM")),
        color = onBgColor,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.fillMaxWidth(),
        textAlign = TextAlign.Center
    )
}

@Composable
fun TimelineView(
    selectedDate: LocalDate,
    events: List<CalendarEvent>,
    onEventClick: (CalendarEvent) -> Unit,
    onBgColor: Color
) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        val hours = (0..23)
        items(hours.toList()) { hour ->
            Row(modifier = Modifier.height(IntrinsicSize.Min)) {
                Text(
                    text = String.format(Locale.getDefault(), "%02d:00", hour),
                    color = onBgColor.copy(alpha = 0.6f),
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.width(45.dp).padding(top = 12.dp)
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 8.dp)
                        .drawBehindGrid(onBgColor)
                        .padding(8.dp)
                ) {
                    val hourlyEvents = events.filter { 
                        it.date == selectedDate && parseEventTime(it.time)?.hour == hour
                    }
                    if (hourlyEvents.isNotEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            hourlyEvents.forEach { event ->
                                CompactEventItem(event, onEventClick)
                            }
                        }
                    } else {
                        Spacer(modifier = Modifier.height(40.dp))
                    }
                }
            }
        }
    }
}

private fun parseEventTime(timeStr: String): LocalTime? {
    return try {
        if (timeStr == "All Day" || timeStr == "Half Day") null
        else LocalTime.parse(timeStr, DateTimeFormatter.ofPattern("hh:mm a", Locale.ENGLISH))
    } catch (e: Exception) {
        null
    }
}

@Composable
fun CompactEventItem(event: CalendarEvent, onClick: (CalendarEvent) -> Unit) {
    Surface(
        color = event.color.copy(alpha = 0.2f),
        border = BorderStroke(1.dp, event.color.copy(alpha = 0.5f)),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth().clickable { onClick(event) }
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(event.color))
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(event.title, color = MaterialTheme.colorScheme.onSurface, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                Text(event.time, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
            }
        }
    }
}

fun Modifier.drawBehindGrid(onBgColor: Color) = this.then(
    Modifier.background(
        color = onBgColor.copy(alpha = 0.05f),
        shape = RoundedCornerShape(8.dp)
    ).border(
        width = 0.5.dp,
        color = onBgColor.copy(alpha = 0.1f),
        shape = RoundedCornerShape(8.dp)
    )
)

@Composable
fun EventListSection(
    selectedDate: LocalDate,
    events: List<CalendarEvent>,
    weatherData: Map<LocalDate, WeatherInfo>,
    onDeleteEvent: (CalendarEvent) -> Unit,
    onEditEvent: (CalendarEvent) -> Unit,
    onToggleNotification: (CalendarEvent) -> Unit,
    onBgColor: Color,
    secondaryOnBgColor: Color
) {
    Column {
        Text(
            text = stringResource(R.string.events),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = onBgColor
        )

        Spacer(modifier = Modifier.height(8.dp))

        val dailyEvents = events.filter { it.date == selectedDate }
        
        if (dailyEvents.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxWidth().height(100.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.no_events),
                    style = MaterialTheme.typography.bodyLarge,
                    color = secondaryOnBgColor
                )
            }
        } else {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                dailyEvents.forEach { event ->
                    EventItem(
                        event = event,
                        weatherInfo = weatherData[event.date],
                        onDelete = { onDeleteEvent(event) },
                        onClick = { onEditEvent(event) },
                        onToggleNotification = { onToggleNotification(event) },
                        onBgColor = onBgColor,
                        secondaryOnBgColor = secondaryOnBgColor
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
fun CalendarGrid(
    currentMonth: YearMonth,
    selectedDate: LocalDate,
    datesWithEvents: Set<LocalDate>,
    onDateSelected: (LocalDate) -> Unit,
    onDateDoubleClicked: (LocalDate) -> Unit,
    onBgColor: Color
) {
    val daysInMonth = currentMonth.lengthOfMonth()
    val firstDayOfMonth = currentMonth.atDay(1).dayOfWeek.value % 7 // 0 for Sunday
    
    val totalCells = ceil((daysInMonth + firstDayOfMonth) / 7.0).toInt() * 7
    
    Column {
        Row(modifier = Modifier.fillMaxWidth()) {
            val weekdays = listOf("S", "M", "T", "W", "T", "F", "S")
            weekdays.forEach { day ->
                Text(
                    text = day,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelSmall,
                    color = onBgColor.copy(alpha = 0.6f),
                    fontWeight = FontWeight.Bold
                )
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        for (i in 0 until totalCells step 7) {
            Row(modifier = Modifier.fillMaxWidth()) {
                for (j in 0 until 7) {
                    val dayIndex = i + j
                    val dayOfMonth = dayIndex - firstDayOfMonth + 1
                    
                    if (dayOfMonth in 1..daysInMonth) {
                        val date = currentMonth.atDay(dayOfMonth)
                        val isSelected = date == selectedDate
                        val isToday = date == LocalDate.now()
                        val hasEvents = datesWithEvents.contains(date)
                        
                        var lastClickTime by remember { mutableLongStateOf(0L) }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .padding(2.dp)
                                .clip(CircleShape)
                                .background(
                                    when {
                                        isSelected -> MaterialTheme.colorScheme.primary
                                        isToday -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                                        else -> Color.Transparent
                                    }
                                )
                                .clickable {
                                    val currentTime = System.currentTimeMillis()
                                    if (currentTime - lastClickTime < 300) {
                                        onDateDoubleClicked(date)
                                    } else {
                                        onDateSelected(date)
                                    }
                                    lastClickTime = currentTime
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = dayOfMonth.toString(),
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = when {
                                        isSelected -> MaterialTheme.colorScheme.onPrimary
                                        isToday -> MaterialTheme.colorScheme.primary
                                        else -> onBgColor
                                    },
                                    fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 14.sp
                                )
                                if (hasEvents) {
                                    Box(
                                        modifier = Modifier
                                            .size(4.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (isSelected) MaterialTheme.colorScheme.onPrimary
                                                else MaterialTheme.colorScheme.primary
                                            )
                                    )
                                }
                            }
                        }
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun EventItem(
    event: CalendarEvent,
    weatherInfo: WeatherInfo?,
    onDelete: () -> Unit,
    onClick: () -> Unit,
    onToggleNotification: () -> Unit,
    onBgColor: Color,
    secondaryOnBgColor: Color
) {
    var showMenu by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = { showMenu = true }
            ),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = onBgColor.copy(alpha = 0.1f)
        ),
        border = BorderStroke(1.dp, onBgColor.copy(alpha = 0.15f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(12.dp).clip(CircleShape).background(event.color))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = event.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = onBgColor
                    )
                }
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = event.time,
                        style = MaterialTheme.typography.labelMedium,
                        color = secondaryOnBgColor
                    )
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    IconButton(
                        onClick = onToggleNotification,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = if (event.isNotificationEnabled) Icons.Default.NotificationsActive else Icons.Default.NotificationsOff,
                            contentDescription = "Toggle Notification",
                            tint = if (event.isNotificationEnabled) MaterialTheme.colorScheme.primary else secondaryOnBgColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
            
            if (event.description.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = event.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = secondaryOnBgColor,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (weatherInfo != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            onBgColor.copy(alpha = 0.05f),
                            RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    AsyncImage(
                        model = "https://openweathermap.org/img/wn/${weatherInfo.icon}.png",
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${weatherInfo.temperature}°C - ${stringResource(getWeatherDescriptionRes(weatherInfo.weatherCode))}",
                        style = MaterialTheme.typography.labelSmall,
                        color = secondaryOnBgColor
                    )
                }
            }
        }
    }

    if (showMenu) {
        AlertDialog(
            onDismissRequest = { showMenu = false },
            confirmButton = {
                TextButton(onClick = {
                    onDelete()
                    showMenu = false
                }) {
                    Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showMenu = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
            title = { Text(stringResource(R.string.delete_event_title)) },
            text = { Text(stringResource(R.string.delete_event_confirm)) }
        )
    }
}

@Composable
fun SettingsDialog(
    settings: CalendarSettings,
    onDismiss: () -> Unit,
    onUpdateSettings: (CalendarSettings) -> Unit,
    onSaveWallpaper: (Uri) -> Unit,
    onResetSettings: () -> Unit
) {
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            onSaveWallpaper(uri)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(stringResource(R.string.dark_mode))
                    Switch(
                        checked = settings.isDarkMode,
                        onCheckedChange = { onUpdateSettings(settings.copy(isDarkMode = it)) }
                    )
                }
                
                Button(
                    onClick = { launcher.launch("image/*") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text(stringResource(R.string.change_wallpaper))
                }

                Button(
                    onClick = { onResetSettings() },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary,
                        contentColor = MaterialTheme.colorScheme.onSecondary
                    )
                ) {
                    Text(stringResource(R.string.reset_visuals))
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.close))
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeatherDialog(
    weatherInfo: WeatherInfo?,
    hourlyWeatherData: Map<String, WeatherInfo>,
    weatherDate: LocalDate,
    isLoading: Boolean,
    onDismiss: () -> Unit
) {
    var selectedTime by remember { mutableStateOf(LocalTime.now()) }
    var showTimePicker by remember { mutableStateOf(false) }
    
    val displayWeather = remember(weatherDate, selectedTime, hourlyWeatherData, weatherInfo) {
        val dateTimeKey = LocalDateTime.of(weatherDate, selectedTime.withMinute(0).withSecond(0)).toString().substring(0, 13) + ":00"
        hourlyWeatherData.entries.find { it.key.startsWith(dateTimeKey.substring(0, 13)) }?.value ?: weatherInfo
    }

    BasicAlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        content = {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .wrapContentHeight(),
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stringResource(R.string.weather_details),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    val timeFormatter = DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH)
                    val isNight = selectedTime.hour >= 18 || selectedTime.hour < 6
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .padding(bottom = 16.dp)
                            .clickable { showTimePicker = true }
                            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = selectedTime.format(timeFormatter),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isNight) stringResource(R.string.night_time) else stringResource(R.string.day_time),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }

                    if (isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(stringResource(R.string.loading_weather))
                    } else if (displayWeather != null) {
                        AsyncImage(
                            model = "https://openweathermap.org/img/wn/${displayWeather.icon}@2x.png",
                            contentDescription = null,
                            modifier = Modifier.size(80.dp)
                        )
                        Text(
                            text = "${displayWeather.temperature}°C",
                            style = MaterialTheme.typography.displaySmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = stringResource(getWeatherDescriptionRes(displayWeather.weatherCode)),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            WeatherDetailItem(
                                icon = Icons.Default.WaterDrop,
                                label = stringResource(R.string.humidity),
                                value = if (displayWeather.humidity != null) "${displayWeather.humidity}%" else "--"
                            )
                            WeatherDetailItem(
                                icon = Icons.Default.Air,
                                label = stringResource(R.string.wind),
                                value = if (displayWeather.windSpeed != null) "${displayWeather.windSpeed} m/s" else "--"
                            )
                        }
                    } else {
                        Text(stringResource(R.string.weather_not_available))
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.onSurface,
                            contentColor = MaterialTheme.colorScheme.surface
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(stringResource(R.string.close))
                    }
                }
            }
        }
    )

    if (showTimePicker) {
        val timePickerState = rememberTimePickerState(
            initialHour = selectedTime.hour,
            initialMinute = selectedTime.minute
        )
        
        Dialog(onDismissRequest = { showTimePicker = false }) {
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(stringResource(R.string.select_weather_time), style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(16.dp))
                    TimePicker(state = timePickerState)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showTimePicker = false }) {
                            Text(stringResource(R.string.cancel))
                        }
                        TextButton(onClick = {
                            val selectedLocalTime = LocalTime.of(timePickerState.hour, 0)
                            selectedTime = selectedLocalTime
                            showTimePicker = false
                        }) {
                            Text(stringResource(R.string.confirm))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun WeatherDetailItem(icon: ImageVector, label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventDialog(
    event: CalendarEvent?,
    onDismiss: () -> Unit,
    onSave: (String, String, String, Boolean) -> Unit
) {
    var title by remember { mutableStateOf(event?.title ?: "") }
    var time by remember { mutableStateOf(event?.time ?: "12:00 PM") }
    var description by remember { mutableStateOf(event?.description ?: "") }
    var isNotificationEnabled by remember { mutableStateOf(event?.isNotificationEnabled ?: true) }
    var showTimePicker by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (event == null) stringResource(R.string.add_event) else stringResource(R.string.edit_event)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(stringResource(R.string.event_title)) },
                    modifier = Modifier.fillMaxWidth()
                )
                
                OutlinedTextField(
                    value = time,
                    onValueChange = { },
                    label = { Text(stringResource(R.string.event_time)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showTimePicker = true },
                    enabled = false,
                    colors = OutlinedTextFieldDefaults.colors(
                        disabledTextColor = MaterialTheme.colorScheme.onSurface,
                        disabledBorderColor = MaterialTheme.colorScheme.outline,
                        disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(stringResource(R.string.enable_notification))
                    Switch(
                        checked = isNotificationEnabled,
                        onCheckedChange = { isNotificationEnabled = it }
                    )
                }

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text(stringResource(R.string.event_description)) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { if (title.isNotEmpty()) onSave(title, time, description, isNotificationEnabled) },
                enabled = title.isNotEmpty()
            ) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )

    if (showTimePicker) {
        val initialHour = try { 
            val t = time.split(" ")[0].split(":")
            var h = t[0].toInt()
            if (time.contains("PM") && h < 12) h += 12
            if (time.contains("AM") && h == 12) h = 0
            h
        } catch (e: Exception) { 12 }
        
        val initialMinute = try { 
            time.split(" ")[0].split(":")[1].toInt()
        } catch (e: Exception) { 0 }
        
        val timePickerState = rememberTimePickerState(
            initialHour = initialHour,
            initialMinute = initialMinute
        )

        Dialog(onDismissRequest = { showTimePicker = false }) {
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    TimePicker(state = timePickerState)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showTimePicker = false }) {
                            Text(stringResource(R.string.cancel))
                        }
                        TextButton(onClick = {
                            val selectedLocalTime = LocalTime.of(timePickerState.hour, timePickerState.minute)
                            time = DateTimeFormatter.ofPattern("hh:mm a", Locale.ENGLISH).format(selectedLocalTime)
                            showTimePicker = false
                        }) {
                            Text(stringResource(R.string.confirm))
                        }
                    }
                }
            }
        }
    }
}
