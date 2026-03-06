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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
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
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.*
import kotlin.math.ceil

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    viewModel: CalendarViewModel = viewModel()
) {
    val selectedDate by viewModel.selectedDate
    val datesWithEvents by viewModel.datesWithEvents
    val dayEvents by viewModel.selectedDayEvents
    val settings by viewModel.settings
    val weatherData by viewModel.weatherData
    val hourlyWeatherData by viewModel.hourlyWeatherData
    val isLoadingWeather by viewModel.isLoadingWeather

    var currentMonth by remember { mutableStateOf(YearMonth.now()) }
    var showAddEventDialog by remember { mutableStateOf(false) }
    var editingEvent by remember { mutableStateOf<CalendarEvent?>(null) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var showWeatherDialog by remember { mutableStateOf(false) }
    var weatherDate by remember { mutableStateOf(LocalDate.now()) }

    val isDateInPast = remember(selectedDate) { selectedDate.isBefore(LocalDate.now()) }
    
    // Pagination for events
    var currentPage by remember { mutableIntStateOf(0) }
    val paginatedEvents = remember(dayEvents, currentPage) {
        if (dayEvents.isEmpty()) emptyList()
        else listOf(dayEvents[currentPage % dayEvents.size])
    }

    LaunchedEffect(dayEvents) {
        if (currentPage >= dayEvents.size && dayEvents.isNotEmpty()) {
            currentPage = 0
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (settings.wallpaperUri != null) {
            AsyncImage(
                model = settings.wallpaperUri,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.3f))
                    .blur(10.dp)
            )
        }

        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            currentMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault())),
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onBackground,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { currentMonth = currentMonth.minusMonths(1) }) {
                            Icon(Icons.Default.ChevronLeft, contentDescription = stringResource(R.string.previous_month))
                        }
                    },
                    actions = {
                        IconButton(onClick = { currentMonth = currentMonth.plusMonths(1) }) {
                            Icon(Icons.Default.ChevronRight, contentDescription = stringResource(R.string.next_month))
                        }
                        IconButton(onClick = { showSettingsDialog = true }) {
                            Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.settings))
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = Color.Transparent
                    )
                )
            }
        ) { innerPadding ->
            Surface(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 16.dp),
                shape = RoundedCornerShape(24.dp),
                color = if (settings.wallpaperUri != null) 
                    MaterialTheme.colorScheme.surface.copy(alpha = 0.7f) 
                    else MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f)),
                tonalElevation = 4.dp
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                    ) {
                        if (settings.bannerUri != null) {
                            AsyncImage(
                                model = settings.bannerUri,
                                contentDescription = null,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(100.dp)
                                    .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)),
                                contentScale = ContentScale.Crop,
                                alpha = 0.8f
                            )
                        }

                        Column(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                        ) {
                            CalendarGrid(
                                currentMonth = currentMonth,
                                selectedDate = selectedDate,
                                datesWithEvents = datesWithEvents,
                                onDateSelected = { viewModel.onDateSelected(it) },
                                onDateDoubleClicked = { 
                                    weatherDate = it
                                    showWeatherDialog = true
                                    viewModel.fetchWeatherForDate(it)
                                }
                            )
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            Text(
                                stringResource(R.string.today_plan),
                                style = MaterialTheme.typography.headlineSmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.ExtraBold
                            )
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            if (dayEvents.isEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(200.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            stringResource(R.string.no_tasks),
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        if (!isDateInPast) {
                                            Spacer(modifier = Modifier.height(16.dp))
                                            FloatingActionButton(
                                                onClick = { showAddEventDialog = true },
                                                containerColor = MaterialTheme.colorScheme.primary,
                                                contentColor = MaterialTheme.colorScheme.onPrimary,
                                                shape = RoundedCornerShape(16.dp)
                                            ) {
                                                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_event))
                                            }
                                        }
                                    }
                                }
                            } else {
                                Column {
                                    paginatedEvents.forEach { eventItem ->
                                        EventItem(
                                            event = eventItem,
                                            weatherInfo = weatherData[eventItem.date],
                                            onEdit = { editingEvent = it },
                                            onDelete = { viewModel.deleteEvent(eventItem.id) }
                                        )
                                        Spacer(modifier = Modifier.height(16.dp))
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(bottom = 120.dp), 
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        if (dayEvents.size > 1) {
                                            LazyRow(
                                                modifier = Modifier.weight(1f),
                                                horizontalArrangement = Arrangement.Center,
                                                contentPadding = PaddingValues(horizontal = 8.dp)
                                            ) {
                                                itemsIndexed(dayEvents) { index, event ->
                                                    val isSelected = index == currentPage
                                                    Box(
                                                        modifier = Modifier
                                                            .padding(horizontal = 4.dp)
                                                            .size(width = 32.dp, height = 12.dp)
                                                            .clip(RoundedCornerShape(6.dp))
                                                            .background(if (isSelected) event.color else event.color.copy(alpha = 0.3f))
                                                            .clickable { currentPage = index }
                                                            .border(
                                                                width = if (isSelected) 1.dp else 0.dp,
                                                                color = if (isSelected) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                                                                shape = RoundedCornerShape(6.dp)
                                                            )
                                                    )
                                                }
                                            }
                                        } else {
                                            Spacer(modifier = Modifier.weight(1f))
                                        }

                                        if (!isDateInPast) {
                                            FloatingActionButton(
                                                onClick = { showAddEventDialog = true },
                                                containerColor = MaterialTheme.colorScheme.primary,
                                                contentColor = MaterialTheme.colorScheme.onPrimary,
                                                shape = RoundedCornerShape(16.dp),
                                                modifier = Modifier.size(48.dp)
                                            ) {
                                                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_event), modifier = Modifier.size(24.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        if (showAddEventDialog) {
            EventDialog(
                selectedDate = selectedDate,
                onDismiss = { showAddEventDialog = false },
                onConfirm = { title, color, time, description ->
                    viewModel.addEvent(title, selectedDate, color, time, description)
                    showAddEventDialog = false
                }
            )
        }

        editingEvent?.let { eventToEdit ->
            EventDialog(
                event = eventToEdit,
                selectedDate = eventToEdit.date,
                onDismiss = { editingEvent = null },
                onConfirm = { title, color, time, description ->
                    viewModel.updateEvent(eventToEdit.copy(title = title, color = color, time = time, description = description))
                    editingEvent = null
                }
            )
        }

        if (showSettingsDialog) {
            SettingsDialog(
                settings = settings,
                onDismiss = { showSettingsDialog = false },
                onUpdateSettings = { viewModel.updateSettings(it) },
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
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun EventDialog(
    event: CalendarEvent? = null,
    selectedDate: LocalDate,
    onDismiss: () -> Unit,
    onConfirm: (String, Color, String, String) -> Unit
) {
    var title by remember { mutableStateOf(event?.title ?: "") }
    var timeOption by remember { mutableStateOf(
        when (event?.time) {
            "All Day" -> "All Day"
            "Half Day" -> "Half Day"
            null -> "All Day"
            else -> "Specific"
        }
    ) }
    
    val initialTime = remember(event, timeOption) {
        if (timeOption == "Specific") {
            try {
                LocalTime.parse(event?.time, DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH))
            } catch (_: Exception) {
                LocalTime.now()
            }
        } else {
            LocalTime.now()
        }
    }
    
    var selectedTime by remember { mutableStateOf(initialTime) }
    var showTimePicker by remember { mutableStateOf(false) }
    var description by remember { mutableStateOf(event?.description ?: "") }
    
    val colors = remember { listOf(Color(0xFF000000), Color(0xFF5856D6), Color(0xFFFF9500), Color(0xFFFF3B30), Color(0xFF34C759)) }
    var selectedColor by remember { mutableStateOf(event?.color ?: colors[0]) }

    val isDateInPast = remember(selectedDate) { selectedDate.isBefore(LocalDate.now()) }
    val isToday = remember(selectedDate) { selectedDate.isEqual(LocalDate.now()) }
    
    val isTimeInPast = if (isToday && timeOption == "Specific") {
        selectedTime.isBefore(LocalTime.now().plusMinutes(1))
    } else false

    val isReadOnly = isDateInPast && event != null
    val canConfirm = title.isNotBlank() && !isDateInPast && !isTimeInPast

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Text(
                if (event == null) stringResource(R.string.new_plan) else if (isReadOnly) stringResource(R.string.view_plan) else stringResource(R.string.edit_plan), 
                style = MaterialTheme.typography.titleLarge
            ) 
        },
        text = {
            val dateDisplayString = remember(selectedDate) {
                selectedDate.format(DateTimeFormatter.ofPattern("d MMMM", Locale.getDefault()))
            }
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = dateDisplayString,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )

                OutlinedTextField(
                    value = title,
                    onValueChange = { if (!isReadOnly) title = it },
                    label = { Text(stringResource(R.string.event_title)) },
                    modifier = Modifier.fillMaxWidth(),
                    readOnly = isReadOnly,
                    singleLine = true
                )

                Text(stringResource(R.string.time), style = MaterialTheme.typography.labelLarge)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("All Day", "Half Day", "Specific").forEach { option ->
                        FilterChip(
                            selected = timeOption == option,
                            onClick = { if (!isReadOnly) timeOption = option },
                            label = { Text(option) },
                            enabled = !isReadOnly
                        )
                    }
                }

                if (timeOption == "Specific") {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = !isReadOnly) { showTimePicker = true }
                            .padding(vertical = 8.dp)
                    ) {
                        Icon(Icons.Default.AccessTime, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = selectedTime.format(DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH)),
                            style = MaterialTheme.typography.bodyLarge
                        )
                        if (isTimeInPast) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Past time selected",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }

                Text(stringResource(R.string.color), style = MaterialTheme.typography.labelLarge)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    colors.forEach { color ->
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(color)
                                .clickable(enabled = !isReadOnly) { selectedColor = color }
                                .border(
                                    width = if (selectedColor == color) 3.dp else 0.dp,
                                    color = if (selectedColor == color) MaterialTheme.colorScheme.outline else Color.Transparent,
                                    shape = CircleShape
                                )
                        )
                    }
                }

                OutlinedTextField(
                    value = description,
                    onValueChange = { if (!isReadOnly) description = it },
                    label = { Text(stringResource(R.string.description)) },
                    modifier = Modifier.fillMaxWidth(),
                    readOnly = isReadOnly,
                    minLines = 3
                )
            }
        },
        confirmButton = {
            if (!isReadOnly) {
                Button(
                    onClick = { 
                        val finalTime = when (timeOption) {
                            "All Day" -> "All Day"
                            "Half Day" -> "Half Day"
                            else -> selectedTime.format(DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH))
                        }
                        onConfirm(title, selectedColor, finalTime, description) 
                    },
                    enabled = canConfirm
                ) {
                    Text(stringResource(R.string.confirm))
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(if (isReadOnly) stringResource(R.string.close) else stringResource(R.string.cancel))
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
                    TimePicker(state = timePickerState)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showTimePicker = false }) {
                            Text(stringResource(R.string.cancel))
                        }
                        TextButton(onClick = {
                            selectedTime = LocalTime.of(timePickerState.hour, timePickerState.minute)
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
fun CalendarGrid(
    currentMonth: YearMonth,
    selectedDate: LocalDate,
    datesWithEvents: Set<LocalDate>,
    onDateSelected: (LocalDate) -> Unit,
    onDateDoubleClicked: (LocalDate) -> Unit
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
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
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
                                        isToday -> MaterialTheme.colorScheme.primaryContainer
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
                                        isToday -> MaterialTheme.colorScheme.onPrimaryContainer
                                        else -> MaterialTheme.colorScheme.onSurface
                                    },
                                    fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal
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
    onEdit: (CalendarEvent) -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = { onEdit(event) },
                onLongClick = { showMenu = true }
            ),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        border = BorderStroke(1.dp, event.color.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(event.color)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = event.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                Text(
                    text = event.time,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            if (event.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = event.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
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
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
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
                        color = MaterialTheme.colorScheme.onPrimaryContainer
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
    onResetSettings: () -> Unit
) {
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            onUpdateSettings(settings.copy(wallpaperUri = uri.toString()))
        }
    }

    val bannerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            onUpdateSettings(settings.copy(bannerUri = uri.toString()))
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
                    onClick = { bannerLauncher.launch("image/*") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text(stringResource(R.string.change_banner))
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

    AlertDialog(
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
                            selectedTime = LocalTime.of(timePickerState.hour, 0) // Hourly resolution
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
