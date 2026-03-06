package app1.pkg.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.NavigateBefore
import androidx.compose.material.icons.automirrored.filled.NavigateNext
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
import app1.pkg.model.AppTheme
import app1.pkg.model.CalendarEvent
import app1.pkg.model.CalendarSettings
import app1.pkg.viewmodel.CalendarViewModel
import coil.compose.AsyncImage
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.*
import kotlin.math.ceil

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(viewModel: CalendarViewModel = viewModel()) {
    val selectedDate by viewModel.selectedDate
    val settings by viewModel.settings
    val dayEvents by viewModel.selectedDayEvents
    val datesWithEvents by viewModel.datesWithEvents
    
    var currentMonth by remember { mutableStateOf(YearMonth.from(selectedDate)) }
    var showAddEventDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var editingEvent by remember { mutableStateOf<CalendarEvent?>(null) }

    val isDateInPast = remember(selectedDate) { selectedDate.isBefore(LocalDate.now()) }

    // Pagination State
    var currentPage by remember { mutableIntStateOf(0) }
    val pageSize = 1

    // Reset page when date changes
    LaunchedEffect(selectedDate) {
        currentPage = 0
    }

    // Ensure currentPage is valid if events are deleted
    LaunchedEffect(dayEvents.size) {
        if (currentPage >= dayEvents.size && dayEvents.isNotEmpty()) {
            currentPage = dayEvents.size - 1
        }
    }

    val paginatedEvents = remember(dayEvents, currentPage) {
        val start = currentPage * pageSize
        dayEvents.drop(start).take(pageSize)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Base Background
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            MaterialTheme.colorScheme.background,
                            MaterialTheme.colorScheme.surface
                        )
                    )
                )
        )

        // Wallpaper layer
        if (settings.wallpaperUri != null) {
            AsyncImage(
                model = settings.wallpaperUri,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                alpha = if (settings.isDarkMode) 0.3f else 0.5f
            )
        }

        Scaffold(
            modifier = Modifier.navigationBarsPadding(),
            containerColor = Color.Transparent,
            topBar = {
                CenterAlignedTopAppBar(
                    modifier = Modifier.statusBarsPadding(),
                    title = { 
                        val monthYearText = remember(currentMonth) {
                            "${currentMonth.month.getDisplayName(TextStyle.FULL, Locale.getDefault())} ${currentMonth.year}"
                        }
                        Text(
                            monthYearText,
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onBackground
                        ) 
                    },
                    navigationIcon = {
                        IconButton(onClick = { currentMonth = currentMonth.minusMonths(1) }) {
                            Icon(Icons.Default.ChevronLeft, contentDescription = stringResource(R.string.previous_month), tint = MaterialTheme.colorScheme.onBackground)
                        }
                    },
                    actions = {
                        IconButton(onClick = { currentMonth = currentMonth.plusMonths(1) }) {
                            Icon(Icons.Default.ChevronRight, contentDescription = stringResource(R.string.next_month), tint = MaterialTheme.colorScheme.onBackground)
                        }
                        IconButton(onClick = { showSettingsDialog = true }) {
                            Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.settings), tint = MaterialTheme.colorScheme.onBackground)
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
                            modifier = Modifier
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                        ) {
                            CalendarGrid(
                                currentMonth = currentMonth,
                                selectedDate = selectedDate,
                                datesWithEvents = datesWithEvents,
                                onDateSelected = { viewModel.onDateSelected(it) }
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
                                    paginatedEvents.forEach { event ->
                                        EventItem(
                                            event = event,
                                            onEdit = { editingEvent = it },
                                            onDelete = { viewModel.deleteEvent(it.id) }
                                        )
                                        Spacer(modifier = Modifier.height(16.dp))
                                    }

                                    // Color-block carousel for navigating plans
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
            onUpdateSettings = { viewModel.updateSettings(it) }
        )
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
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
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
                if (isDateInPast && event == null) {
                    Text(
                        stringResource(R.string.cannot_add_past),
                        color = Color.Red,
                        style = MaterialTheme.typography.bodySmall
                    )
                } else if (isReadOnly) {
                    Text(
                        stringResource(R.string.past_read_only),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Text(
                    dateDisplayString,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(stringResource(R.string.title_label)) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        focusedLabelColor = MaterialTheme.colorScheme.primary
                    ),
                    enabled = !isReadOnly && !isDateInPast
                )

                Text(stringResource(R.string.time_designation), style = MaterialTheme.typography.labelLarge)
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("All Day", "Half Day", "Specific").forEach { option ->
                        val label = when(option) {
                            "All Day" -> stringResource(R.string.all_day)
                            "Half Day" -> stringResource(R.string.half_day)
                            else -> stringResource(R.string.specific)
                        }
                        FilterChip(
                            selected = timeOption == option,
                            onClick = { timeOption = option },
                            label = { Text(label) },
                            enabled = !isReadOnly && !isDateInPast,
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            )
                        )
                    }
                }

                if (timeOption == "Specific") {
                    OutlinedCard(
                        onClick = { if (!isReadOnly && !isDateInPast) showTimePicker = true },
                        modifier = Modifier.fillMaxWidth(),
                        border = BorderStroke(1.dp, if (isTimeInPast) Color.Red else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f)),
                        enabled = !isReadOnly && !isDateInPast
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.AccessTime, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface)
                            Spacer(modifier = Modifier.width(12.dp))
                            val formattedTime = remember(selectedTime) {
                                selectedTime.format(DateTimeFormatter.ofPattern("hh:mm a", Locale.getDefault()))
                            }
                            Text(
                                formattedTime,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                    if (isTimeInPast && !isReadOnly) {
                        Text(
                            stringResource(R.string.selected_time_passed),
                            color = Color.Red,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text(stringResource(R.string.description_label)) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        focusedLabelColor = MaterialTheme.colorScheme.primary
                    ),
                    minLines = 3,
                    enabled = !isReadOnly && !isDateInPast
                )

                Text(stringResource(R.string.theme_color), style = MaterialTheme.typography.labelLarge)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    colors.forEach { color ->
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(color)
                                .clickable(enabled = !isReadOnly && !isDateInPast) { selectedColor = color }
                                .then(
                                    if (selectedColor == color) 
                                        Modifier.border(2.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                                    else Modifier
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (selectedColor == color) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = Color.White)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (!isReadOnly) {
                Button(
                    onClick = { 
                        val finalTime = when (timeOption) {
                            "Specific" -> selectedTime.format(DateTimeFormatter.ofPattern("hh:mm a", Locale.ENGLISH))
                            else -> timeOption
                        }
                        onConfirm(title, selectedColor, finalTime, description) 
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary),
                    enabled = canConfirm
                ) {
                    Text(stringResource(R.string.confirm))
                }
            } else {
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary)
                ) {
                    Text(stringResource(R.string.close))
                }
            }
        },
        dismissButton = {
            if (!isReadOnly) {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.cancel), color = MaterialTheme.colorScheme.onSurface)
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(16.dp)
    )

    if (showTimePicker) {
        val timePickerState = rememberTimePickerState(
            initialHour = selectedTime.hour,
            initialMinute = selectedTime.minute
        )
        var showKeyboard by remember { mutableStateOf(false) }
        
        Dialog(
            onDismissRequest = { showTimePicker = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp,
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .width(IntrinsicSize.Min)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if (showKeyboard) stringResource(R.string.enter_time) else stringResource(R.string.select_time),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    if (showKeyboard) {
                        TimeInput(
                            state = timePickerState,
                            colors = TimePickerDefaults.colors(
                                selectorColor = MaterialTheme.colorScheme.primary,
                                containerColor = MaterialTheme.colorScheme.surface
                            )
                        )
                    } else {
                        TimePicker(
                            state = timePickerState,
                            colors = TimePickerDefaults.colors(
                                clockDialColor = MaterialTheme.colorScheme.surfaceVariant,
                                selectorColor = MaterialTheme.colorScheme.primary,
                                containerColor = MaterialTheme.colorScheme.surface,
                                periodSelectorSelectedContainerColor = MaterialTheme.colorScheme.primary,
                                periodSelectorSelectedContentColor = MaterialTheme.colorScheme.onPrimary,
                                timeSelectorSelectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                timeSelectorSelectedContentColor = MaterialTheme.colorScheme.primary
                            )
                        )
                    }
                    
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { showKeyboard = !showKeyboard }) {
                            Icon(
                                if (showKeyboard) Icons.Default.Schedule else Icons.Default.Keyboard,
                                contentDescription = stringResource(R.string.toggle_input_mode),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(modifier = Modifier.weight(1f))
                        TextButton(onClick = { showTimePicker = false }) {
                            Text(stringResource(R.string.cancel), color = MaterialTheme.colorScheme.primary)
                        }
                        TextButton(onClick = {
                            selectedTime = LocalTime.of(timePickerState.hour, timePickerState.minute)
                            showTimePicker = false
                        }) {
                            Text(stringResource(R.string.ok), color = MaterialTheme.colorScheme.primary)
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
    onDateSelected: (LocalDate) -> Unit
) {
    val daysInMonth = remember(currentMonth) { currentMonth.lengthOfMonth() }
    val firstDayOfMonth = remember(currentMonth) { currentMonth.atDay(1).dayOfWeek.value % 7 }
    
    Column {
        Row(modifier = Modifier.fillMaxWidth()) {
            val dayLabels = remember { 
                listOf(DayOfWeek.SUNDAY, DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY)
                    .map { it.getDisplayName(TextStyle.NARROW, Locale.getDefault()) }
            }
            dayLabels.forEach { day ->
                Text(
                    text = day,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        var currentDay = 1
        for (i in 0 until 6) {
            Row(modifier = Modifier.fillMaxWidth()) {
                for (j in 0 until 7) {
                    val dayIndex = i * 7 + j
                    if (dayIndex < firstDayOfMonth || currentDay > daysInMonth) {
                        Spacer(modifier = Modifier.weight(1f))
                    } else {
                        val date = currentMonth.atDay(currentDay)
                        val isSelected = date == selectedDate
                        val hasEvents = remember(date, datesWithEvents) { datesWithEvents.contains(date) }
                        
                        DayCell(
                            dayNumber = currentDay,
                            isSelected = isSelected,
                            hasEvents = hasEvents,
                            onClick = { onDateSelected(date) },
                            modifier = Modifier.weight(1f)
                        )
                        currentDay++
                    }
                }
            }
            if (currentDay > daysInMonth) break
        }
    }
}

@Composable
fun DayCell(
    dayNumber: Int,
    isSelected: Boolean,
    hasEvents: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .padding(2.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = dayNumber.toString(),
                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            )
            if (hasEvents) {
                Box(
                    modifier = Modifier
                        .size(4.dp)
                        .clip(CircleShape)
                        .background(if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary)
                )
            }
        }
    }
}

@Composable
fun EventItem(
    event: CalendarEvent,
    onEdit: (CalendarEvent) -> Unit,
    onDelete: (CalendarEvent) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val isPastEvent = remember(event.date) { event.date.isBefore(LocalDate.now()) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(event.color),
                    contentAlignment = Alignment.Center
                ) {
                    Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(Color.White))
                }
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = event.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = event.time,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Icon(
                    if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (event.description.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = event.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = if (expanded) Int.MAX_VALUE else 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            AnimatedVisibility(visible = expanded) {
                Column {
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { onEdit(event) }) {
                            Text(
                                if (isPastEvent) stringResource(R.string.view_plan) else stringResource(R.string.edit_plan),
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                        if (!isPastEvent) {
                            TextButton(onClick = { onDelete(event) }) {
                                Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsDialog(
    settings: CalendarSettings,
    onDismiss: () -> Unit,
    onUpdateSettings: (CalendarSettings) -> Unit
) {
    val bannerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            onUpdateSettings(settings.copy(bannerUri = uri.toString()))
        }
    }

    val wallpaperLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            onUpdateSettings(settings.copy(wallpaperUri = uri.toString()))
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .wrapContentHeight(),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Text(
                    stringResource(R.string.settings_title),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.ExtraBold
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(stringResource(R.string.dark_mode), style = MaterialTheme.typography.bodyLarge)
                    Switch(
                        checked = settings.isDarkMode,
                        onCheckedChange = { onUpdateSettings(settings.copy(isDarkMode = it)) }
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(stringResource(R.string.app_theme), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        AppTheme.entries.forEach { theme ->
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(theme.primaryColor)
                                    .clickable { onUpdateSettings(settings.copy(theme = theme)) }
                                    .then(
                                        if (settings.theme == theme) 
                                            Modifier.border(2.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                                        else Modifier
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (settings.theme == theme) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = Color.White)
                                }
                            }
                        }
                    }
                }

                HorizontalDivider(modifier = Modifier.alpha(0.1f))

                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = { bannerLauncher.launch("image/*") },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(24.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurfaceVariant)
                    ) {
                        Text(stringResource(R.string.change_banner), fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = { wallpaperLauncher.launch("image/*") },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(24.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurfaceVariant)
                    ) {
                        Text(stringResource(R.string.change_wallpaper), fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = { 
                            onUpdateSettings(settings.copy(
                                bannerUri = "https://images.unsplash.com/photo-1557683316-973673baf926?q=80&w=2029&auto=format&fit=crop",
                                wallpaperUri = "https://images.unsplash.com/photo-1557683311-eac922347aa1?q=80&w=2029&auto=format&fit=crop"
                            )) 
                        },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(24.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f), contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text(stringResource(R.string.reset_visuals), fontWeight = FontWeight.Bold)
                    }
                }

                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                    Button(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier.width(100.dp).height(44.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Black, contentColor = Color.White)
                    ) {
                        Text(stringResource(R.string.close), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
