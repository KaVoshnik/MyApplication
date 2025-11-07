package com.example.myapplication.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
fun EnhancedNotesScreen(viewModel: NoteViewModel) {
    var selectedTab by remember { mutableStateOf(0) } // 0 - активные, 1 - выполненные, 2 - статистика
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    var showAddNoteDialog by remember { mutableStateOf(false) }
    var noteToEdit by remember { mutableStateOf<Note?>(null) }
    val scope = rememberCoroutineScope()
    
    val activeNotes by viewModel.allNotes.collectAsState(initial = emptyList())
    val completedNotes by viewModel.completedNotes.collectAsState(initial = emptyList())
    val filteredNotes = remember(activeNotes, searchQuery, selectedCategory) {
        var notes = if (selectedTab == 0) activeNotes else completedNotes
        if (searchQuery.isNotBlank()) {
            notes = notes.filter { 
                it.title.contains(searchQuery, ignoreCase = true) || 
                it.description.contains(searchQuery, ignoreCase = true)
            }
        }
        if (selectedCategory != null) {
            notes = notes.filter { it.category == selectedCategory }
        }
        notes
    }
    
    val categories by remember {
        derivedStateOf {
            (activeNotes + completedNotes).map { it.category }.distinct()
        }
    }
    
    var activeCount by remember { mutableStateOf(0) }
    var completedCount by remember { mutableStateOf(0) }
    
    LaunchedEffect(Unit) {
        scope.launch {
            activeCount = viewModel.getActiveNotesCount()
            completedCount = viewModel.getCompletedNotesCount()
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
                    IconButton(onClick = { showAddNoteDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Добавить")
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Default.List, contentDescription = "Активные") },
                    label = { Text("Активные ($activeCount)") }
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Default.CheckCircle, contentDescription = "Выполненные") },
                    label = { Text("Выполненные ($completedCount)") }
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Icon(Icons.Default.BarChart, contentDescription = "Статистика") },
                    label = { Text("Статистика") }
                )
            }
        },
        floatingActionButton = {
            if (selectedTab != 2) {
                FloatingActionButton(
                    onClick = { showAddNoteDialog = true },
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Добавить заметку")
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (selectedTab) {
                0, 1 -> {
                    // Поиск
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        placeholder = { Text("Поиск заметок...") },
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = "Поиск")
                        },
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
                        LazyRow(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            item {
                                FilterChip(
                                    selected = selectedCategory == null,
                                    onClick = { selectedCategory = null },
                                    label = { Text("Все") }
                                )
                            }
                            items(categories) { category ->
                                FilterChip(
                                    selected = selectedCategory == category,
                                    onClick = { 
                                        selectedCategory = if (selectedCategory == category) null else category
                                    },
                                    label = { Text(category) }
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    
                    // Список заметок
                    if (filteredNotes.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (selectedTab == 0) "Нет активных заметок" else "Нет выполненных заметок",
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
                            items(filteredNotes, key = { it.id }) { note ->
                                EnhancedNoteCard(
                                    note = note,
                                    onEdit = { noteToEdit = note },
                                    onDelete = { viewModel.deleteNote(note) },
                                    onToggleComplete = { viewModel.toggleNoteCompletion(note) }
                                )
                            }
                        }
                    }
                }
                2 -> {
                    StatisticsScreen(
                        activeCount = activeCount,
                        completedCount = completedCount,
                        categories = categories,
                        notes = activeNotes + completedNotes
                    )
                }
            }
        }
    }

    if (showAddNoteDialog || noteToEdit != null) {
        EnhancedNoteDialog(
            note = noteToEdit,
            onDismiss = { 
                showAddNoteDialog = false
                noteToEdit = null
            },
            onSave = { note ->
                if (noteToEdit != null) {
                    viewModel.updateNote(note)
                    noteToEdit = null
                } else {
                    viewModel.insertNote(note)
                }
                showAddNoteDialog = false
            }
        )
    }
}

@Composable
fun EnhancedNoteCard(
    note: Note,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onToggleComplete: () -> Unit
) {
    val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
    val dateTime = Date(note.scheduledDateTime)
    val priorityColor = when (note.priority) {
        3 -> Color(0xFFFF5252) // Высокий - красный
        2 -> Color(0xFFFFA726) // Средний - оранжевый
        else -> Color(0xFF66BB6A) // Низкий - зеленый
    }
    
    val categoryColors = mapOf(
        "Работа" to Color(0xFF42A5F5),
        "Личное" to Color(0xFFAB47BC),
        "Здоровье" to Color(0xFF26A69A),
        "Покупки" to Color(0xFFFFA726),
        "Общее" to Color(0xFF78909C)
    )
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onEdit() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (note.isCompleted) 
                MaterialTheme.colorScheme.surfaceVariant 
            else 
                MaterialTheme.colorScheme.surface
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
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Приоритет
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(priorityColor)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = note.title,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (note.isCompleted) 
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            else 
                                MaterialTheme.colorScheme.onSurface
                        )
                        if (note.description.isNotBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = note.description,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2
                            )
                        }
                    }
                }
                
                Row {
                    IconButton(onClick = onToggleComplete) {
                        Icon(
                            if (note.isCompleted) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                            contentDescription = if (note.isCompleted) "Не выполнено" else "Выполнено",
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
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Категория
                    Surface(
                        color = categoryColors[note.category] ?: MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = note.category,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    
                    // Приоритет
                    Text(
                        text = when (note.priority) {
                            3 -> "Высокий"
                            2 -> "Средний"
                            else -> "Низкий"
                        },
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Text(
                    text = dateFormat.format(dateTime),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
fun StatisticsScreen(
    activeCount: Int,
    completedCount: Int,
    categories: List<String>,
    notes: List<Note>
) {
    val total = activeCount + completedCount
    val completionRate = if (total > 0) (completedCount.toFloat() / total * 100).toInt() else 0
    
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Общая статистика",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        EnhancedStatItem("Активных", activeCount.toString())
                        EnhancedStatItem("Выполнено", completedCount.toString())
                        EnhancedStatItem("Всего", total.toString())
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Процент выполнения: $completionRate%",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
        
        item {
            Text(
                text = "По категориям",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }
        
        items(categories) { category ->
            val categoryNotes = notes.filter { it.category == category }
            val categoryCompleted = categoryNotes.count { it.isCompleted }
            val categoryActive = categoryNotes.size - categoryCompleted
            
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = category,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Активных: $categoryActive")
                        Text("Выполнено: $categoryCompleted")
                        Text("Всего: ${categoryNotes.size}")
                    }
                }
            }
        }
    }
}

@Composable
private fun EnhancedStatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

