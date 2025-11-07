package com.example.myapplication.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.data.Note
import com.example.myapplication.viewmodel.NoteViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesScreen(viewModel: NoteViewModel) {
    val notes by viewModel.allNotes.collectAsState(initial = emptyList())
    val completedNotes by viewModel.completedNotes.collectAsState(initial = emptyList())
    var showAddNoteDialog by remember { mutableStateOf(false) }
    var editingNote by remember { mutableStateOf<Note?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    var showStats by remember { mutableStateOf(false) }
    var showCompleted by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    var categories by remember { mutableStateOf<List<String>>(emptyList()) }
    var activeCount by remember { mutableStateOf(0) }
    var completedCount by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) {
        categories = viewModel.getAllCategories()
        activeCount = viewModel.getActiveNotesCount()
        completedCount = viewModel.getCompletedNotesCount()
    }

    val displayedNotes = remember(searchQuery, selectedCategory) {
        if (showCompleted) {
            completedNotes
        } else {
            notes.filter { note ->
                val matchesSearch = searchQuery.isBlank() || 
                    note.title.contains(searchQuery, ignoreCase = true) ||
                    note.description.contains(searchQuery, ignoreCase = true)
                val matchesCategory = selectedCategory == null || note.category == selectedCategory
                matchesSearch && matchesCategory
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Заметки") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                actions = {
                    IconButton(onClick = { showStats = true }) {
                        Icon(Icons.Default.BarChart, contentDescription = "Статистика")
                    }
                    IconButton(onClick = { showCompleted = !showCompleted }) {
                        Icon(
                            if (showCompleted) Icons.Default.List else Icons.Default.CheckCircle,
                            contentDescription = if (showCompleted) "Активные" else "Выполненные"
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddNoteDialog = true },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Добавить заметку")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Поиск
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                placeholder = { Text("Поиск заметок...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Очистить")
                        }
                    }
                },
                singleLine = true
            )

            // Фильтр по категориям
            if (categories.isNotEmpty()) {
                HorizontalScrollableChips(
                    categories = categories,
                    selectedCategory = selectedCategory,
                    onCategorySelected = { selectedCategory = if (selectedCategory == it) null else it }
                )
            }

            // Список заметок
            if (displayedNotes.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (showCompleted) "Нет выполненных заметок" else "Нет заметок",
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(displayedNotes, key = { it.id }) { note ->
                        NoteCard(
                            note = note,
                            onEdit = { editingNote = note },
                            onDelete = { viewModel.deleteNote(note) },
                            onToggleComplete = { viewModel.toggleNoteCompletion(note) }
                        )
                    }
                }
            }
        }
    }

    if (showAddNoteDialog) {
        AddNoteDialog(
            note = null,
            categories = categories,
            onDismiss = { showAddNoteDialog = false },
            onSave = { note ->
                viewModel.insertNote(note)
                showAddNoteDialog = false
                scope.launch {
                    categories = viewModel.getAllCategories()
                    activeCount = viewModel.getActiveNotesCount()
                }
            }
        )
    }

    editingNote?.let { note ->
        AddNoteDialog(
            note = note,
            categories = categories,
            onDismiss = { editingNote = null },
            onSave = { updatedNote ->
                viewModel.updateNote(updatedNote)
                editingNote = null
                scope.launch {
                    categories = viewModel.getAllCategories()
                }
            }
        )
    }

    if (showStats) {
        StatsDialog(
            activeCount = activeCount,
            completedCount = completedCount,
            onDismiss = { showStats = false }
        )
    }
}

@Composable
fun HorizontalScrollableChips(
    categories: List<String>,
    selectedCategory: String?,
    onCategorySelected: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilterChip(
            selected = selectedCategory == null,
            onClick = { onCategorySelected("") },
            label = { Text("Все") }
        )
        categories.forEach { category ->
            FilterChip(
                selected = selectedCategory == category,
                onClick = { onCategorySelected(category) },
                label = { Text(category) }
            )
        }
    }
}

@Composable
fun NoteCard(
    note: Note,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onToggleComplete: () -> Unit
) {
    val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
    val dateTime = Date(note.scheduledDateTime)
    val priorityColors = listOf(
        Color(0xFF4CAF50), // Низкий - зеленый
        Color(0xFFFF9800), // Средний - оранжевый
        Color(0xFFF44336)  // Высокий - красный
    )
    val priorityNames = listOf("Низкий", "Средний", "Высокий")
    val priorityColor = priorityColors.getOrElse(note.priority - 1) { Color.Gray }
    val noteColor = if (note.color != 0) Color(note.color) else null

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onEdit() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = noteColor?.copy(alpha = 0.1f) ?: MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = note.title,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                        // Приоритет
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(priorityColor.copy(alpha = 0.3f))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = priorityNames.getOrElse(note.priority - 1) { "" },
                                fontSize = 10.sp,
                                color = priorityColor,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = note.description,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = dateFormat.format(dateTime),
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        if (note.category.isNotEmpty()) {
                            Text(
                                text = "• ${note.category}",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                }
                Column {
                    IconButton(onClick = onToggleComplete) {
                        Icon(
                            if (note.isCompleted) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                            contentDescription = if (note.isCompleted) "Отметить невыполненной" else "Отметить выполненной",
                            tint = if (note.isCompleted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onDelete) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Удалить",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddNoteDialog(
    note: Note?,
    categories: List<String>,
    onDismiss: () -> Unit,
    onSave: (Note) -> Unit
) {
    val isEditing = note != null
    var title by remember { mutableStateOf(note?.title ?: "") }
    var description by remember { mutableStateOf(note?.description ?: "") }
    var selectedDate by remember { mutableStateOf<Date?>(if (note != null) Date(note.scheduledDateTime) else null) }
    var selectedTime by remember { mutableStateOf<Date?>(if (note != null) Date(note.scheduledDateTime) else null) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var selectedCategory by remember { mutableStateOf(note?.category ?: "Общее") }
    var priority by remember { mutableStateOf(note?.priority ?: 2) }
    var showCategoryDialog by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isEditing) "Редактировать заметку" else "Добавить заметку") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Заголовок") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Описание") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )

                // Категория
                OutlinedButton(
                    onClick = { showCategoryDialog = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Категория: $selectedCategory")
                }

                // Приоритет
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Приоритет:")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("Низкий", "Средний", "Высокий").forEachIndexed { index, name ->
                            FilterChip(
                                selected = priority == index + 1,
                                onClick = { priority = index + 1 },
                                label = { Text(name) }
                            )
                        }
                    }
                }

                val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
                val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

                OutlinedButton(
                    onClick = { showDatePicker = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        if (selectedDate != null) dateFormat.format(selectedDate!!)
                        else "Выберите дату"
                    )
                }

                OutlinedButton(
                    onClick = { showTimePicker = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        if (selectedTime != null) timeFormat.format(selectedTime!!)
                        else "Выберите время"
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (title.isNotBlank() && selectedDate != null && selectedTime != null) {
                        val calendar = Calendar.getInstance().apply {
                            time = selectedDate!!
                            val timeCalendar = Calendar.getInstance().apply {
                                time = selectedTime!!
                            }
                            set(Calendar.HOUR_OF_DAY, timeCalendar.get(Calendar.HOUR_OF_DAY))
                            set(Calendar.MINUTE, timeCalendar.get(Calendar.MINUTE))
                            set(Calendar.SECOND, 0)
                            set(Calendar.MILLISECOND, 0)
                        }
                        val updatedNote = if (isEditing) {
                            note.copy(
                                title = title,
                                description = description,
                                scheduledDateTime = calendar.timeInMillis,
                                category = selectedCategory,
                                priority = priority
                            )
                        } else {
                            Note(
                                title = title,
                                description = description,
                                scheduledDateTime = calendar.timeInMillis,
                                category = selectedCategory,
                                priority = priority
                            )
                        }
                        onSave(updatedNote)
                    }
                }
            ) {
                Text("Сохранить")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена")
            }
        }
    )

    if (showCategoryDialog) {
        CategoryDialog(
            existingCategories = categories,
            selectedCategory = selectedCategory,
            onCategorySelected = { selectedCategory = it; showCategoryDialog = false },
            onDismiss = { showCategoryDialog = false }
        )
    }

    if (showDatePicker) {
        DatePickerDialog(
            initialDate = selectedDate ?: Date(),
            onDateSelected = { date ->
                selectedDate = date
                showDatePicker = false
            },
            onDismiss = { showDatePicker = false }
        )
    }

    if (showTimePicker) {
        TimePickerDialog(
            initialTime = selectedTime ?: Date(),
            onTimeSelected = { time ->
                selectedTime = time
                showTimePicker = false
            },
            onDismiss = { showTimePicker = false }
        )
    }
}

@Composable
fun CategoryDialog(
    existingCategories: List<String>,
    selectedCategory: String,
    onCategorySelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var newCategory by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Выберите категорию") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                existingCategories.forEach { category ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onCategorySelected(category) }
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedCategory == category,
                            onClick = { onCategorySelected(category) }
                        )
                        Text(category, modifier = Modifier.padding(start = 8.dp))
                    }
                }
                Divider()
                OutlinedTextField(
                    value = newCategory,
                    onValueChange = { newCategory = it },
                    label = { Text("Новая категория") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (newCategory.isNotBlank()) {
                        onCategorySelected(newCategory)
                    }
                }
            ) {
                Text("Создать")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена")
            }
        }
    )
}

@Composable
fun StatsDialog(
    activeCount: Int,
    completedCount: Int,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Статистика") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                StatItem("Активных заметок", activeCount.toString())
                StatItem("Выполнено заметок", completedCount.toString())
                val total = activeCount + completedCount
                val percentage = if (total > 0) (completedCount * 100 / total) else 0
                StatItem("Процент выполнения", "$percentage%")
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("ОК")
            }
        }
    )
}

@Composable
private fun StatItem(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label)
        Text(value, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun DatePickerDialog(
    initialDate: Date,
    onDateSelected: (Date) -> Unit,
    onDismiss: () -> Unit
) {
    val calendar = remember { Calendar.getInstance().apply { time = initialDate } }
    var selectedDate by remember { mutableStateOf(calendar.time) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Выберите дату") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
                Text(
                    "Выбранная дата: ${dateFormat.format(selectedDate)}",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Button(
                        onClick = {
                            calendar.time = selectedDate
                            calendar.add(Calendar.DAY_OF_MONTH, -1)
                            selectedDate = calendar.time
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("←")
                    }
                    Button(
                        onClick = {
                            calendar.time = selectedDate
                            calendar.add(Calendar.DAY_OF_MONTH, 1)
                            selectedDate = calendar.time
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("→")
                    }
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Button(
                        onClick = {
                            calendar.time = selectedDate
                            calendar.add(Calendar.DAY_OF_MONTH, -7)
                            selectedDate = calendar.time
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("-7 дней")
                    }
                    Button(
                        onClick = {
                            calendar.time = selectedDate
                            calendar.add(Calendar.DAY_OF_MONTH, 7)
                            selectedDate = calendar.time
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("+7 дней")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onDateSelected(selectedDate) }) {
                Text("ОК")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена")
            }
        }
    )
}

@Composable
private fun TimePickerDialog(
    initialTime: Date,
    onTimeSelected: (Date) -> Unit,
    onDismiss: () -> Unit
) {
    val calendar = remember { Calendar.getInstance().apply { time = initialTime } }
    var selectedHour by remember { mutableStateOf(calendar.get(Calendar.HOUR_OF_DAY)) }
    var selectedMinute by remember { mutableStateOf(calendar.get(Calendar.MINUTE)) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Выберите время") },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Text(
                    "${selectedHour.toString().padStart(2, '0')}:${selectedMinute.toString().padStart(2, '0')}",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("Часы", fontSize = 14.sp)
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = { if (selectedHour > 0) selectedHour-- }) {
                                Text("-", fontSize = 20.sp)
                            }
                            Text("$selectedHour", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                            IconButton(onClick = { if (selectedHour < 23) selectedHour++ }) {
                                Text("+", fontSize = 20.sp)
                            }
                        }
                    }
                    Text(":", fontSize = 24.sp)
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("Минуты", fontSize = 14.sp)
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = {
                                if (selectedMinute > 0) selectedMinute--
                                else {
                                    selectedMinute = 59
                                    if (selectedHour > 0) selectedHour--
                                }
                            }) {
                                Text("-", fontSize = 20.sp)
                            }
                            Text("$selectedMinute", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                            IconButton(onClick = {
                                if (selectedMinute < 59) selectedMinute++
                                else {
                                    selectedMinute = 0
                                    if (selectedHour < 23) selectedHour++
                                }
                            }) {
                                Text("+", fontSize = 20.sp)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                calendar.set(Calendar.HOUR_OF_DAY, selectedHour)
                calendar.set(Calendar.MINUTE, selectedMinute)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                onTimeSelected(calendar.time)
            }) {
                Text("ОК")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена")
            }
        }
    )
}
