# Документация для разработчиков

Подробное описание архитектуры, компонентов и функций приложения "Заметки".

## Структура проекта

```
app/src/main/java/com/example/myapplication/
├── data/
│   ├── Note.kt              # Модель данных заметки
│   ├── NoteDao.kt           # Data Access Object для работы с БД
│   └── NoteDatabase.kt      # База данных Room
├── viewmodel/
│   └── NoteViewModel.kt     # ViewModel для управления состоянием
├── ui/
│   ├── NotesScreen.kt       # Основной UI экран
│   └── theme/               # Тема приложения
├── notification/
│   ├── NotificationScheduler.kt  # Планировщик уведомлений
│   └── NotificationWorker.kt      # Worker для отправки уведомлений
└── MainActivity.kt          # Главная активность
```

## Модель данных

### Note.kt

```kotlin
@Entity(tableName = "notes")
data class Note(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,                    // Заголовок заметки
    val description: String,              // Описание/содержание
    val scheduledDateTime: Long,         // Unix timestamp запланированного времени
    val createdAt: Long,                 // Время создания
    val category: String = "Общее",      // Категория заметки
    val priority: Int = 1,               // 1-низкий, 2-средний, 3-высокий
    val isCompleted: Boolean = false,    // Статус выполнения
    val color: Int = 0                   // Цвет заметки (ARGB)
)
```

**Поля:**

- `id` - уникальный идентификатор (автогенерируемый)
- `title` - обязательное поле, заголовок заметки
- `description` - описание задачи
- `scheduledDateTime` - запланированное время в миллисекундах (Unix timestamp)
- `createdAt` - время создания заметки
- `category` - категория для группировки заметок
- `priority` - приоритет (1-3)
- `isCompleted` - флаг выполнения задачи
- `color` - цвет заметки в формате ARGB (0 = без цвета)

## База данных (Room)

### NoteDatabase.kt

**Версия:** 2  
**Миграция:** `fallbackToDestructiveMigration()` (для разработки)

База данных использует паттерн Singleton для обеспечения единственного экземпляра.

### NoteDao.kt - Методы доступа к данным

#### Запросы заметок:

1. **getAllNotes()** - Flow<List<Note>>

   - Возвращает все активные заметки (isCompleted = false)
   - Сортировка: по приоритету (DESC), затем по дате (ASC)

2. **getCompletedNotes()** - Flow<List<Note>>

   - Возвращает все выполненные заметки
   - Сортировка: по дате (DESC)

3. **getNotesForDateRange(startTime, endTime)** - Flow<List<Note>>

   - Заметки в указанном диапазоне дат
   - Только активные заметки

4. **searchNotes(query)** - Flow<List<Note>>

   - Поиск по заголовку и описанию
   - Использует LIKE для частичного совпадения

5. **getNotesByCategory(category)** - Flow<List<Note>>
   - Фильтрация по категории
   - Только активные заметки

#### Вспомогательные методы:

6. **getAllCategories()** - suspend List<String>

   - Возвращает список всех уникальных категорий

7. **getActiveNotesCount()** - suspend Int

   - Количество активных заметок

8. **getCompletedNotesCount()** - suspend Int

   - Количество выполненных заметок

9. **getNoteById(id)** - suspend Note?
   - Получение заметки по ID

#### Операции изменения:

10. **insertNote(note)** - suspend Long

    - Вставка новой заметки
    - Возвращает ID созданной заметки

11. **updateNote(note)** - suspend Unit

    - Обновление существующей заметки

12. **deleteNote(note)** - suspend Unit
    - Удаление заметки

## ViewModel

### NoteViewModel.kt

**Наследование:** `AndroidViewModel(application)`

#### Потоки данных (Flow):

- `allNotes: Flow<List<Note>>` - все активные заметки
- `completedNotes: Flow<List<Note>>` - выполненные заметки

#### Методы:

**Запросы:**

- `getNotesForDateRange(startTime, endTime)` - заметки в диапазоне дат
- `searchNotes(query)` - поиск заметок
- `getNotesByCategory(category)` - заметки по категории
- `getAllCategories()` - все категории
- `getActiveNotesCount()` - количество активных
- `getCompletedNotesCount()` - количество выполненных

**Изменения:**

- `insertNote(note)` - создание заметки + планирование уведомления
- `updateNote(note)` - обновление + перепланирование уведомления
- `toggleNoteCompletion(note)` - переключение статуса выполнения
- `deleteNote(note)` - удаление + отмена уведомления

#### Логика уведомлений:

- Уведомление планируется за **10 минут** до запланированного времени
- Уведомление отменяется при:
  - Удалении заметки
  - Отметке как выполненной
  - Обновлении на прошедшее время

## Система уведомлений

### NotificationScheduler.kt

**Ответственность:** Планирование и отмена уведомлений через WorkManager

#### Методы:

- `scheduleNotification(note, notificationTime)` - планирование уведомления
- `cancelNotification(noteId)` - отмена уведомления по ID заметки

**Канал уведомлений:**

- ID: `"notes_channel"`
- Название: "Заметки"
- Важность: `IMPORTANCE_HIGH`

### NotificationWorker.kt

**Наследование:** `CoroutineWorker`

**Ответственность:** Отправка уведомления в запланированное время

**Данные из WorkRequest:**

- `note_id` - ID заметки
- `note_title` - заголовок
- `note_description` - описание

**Результат:** `Result.success()` при успешной отправке

## UI Компоненты

### NotesScreen.kt

**Основной экран приложения**

#### Состояние:

- `notes` - список активных заметок
- `completedNotes` - список выполненных заметок
- `showAddNoteDialog` - показ диалога добавления
- `editingNote` - редактируемая заметка
- `searchQuery` - текст поиска
- `selectedCategory` - выбранная категория для фильтрации
- `showStats` - показ статистики
- `showCompleted` - режим просмотра выполненных

#### Компоненты:

1. **NotesScreen** - главный экран

   - TopAppBar с кнопками статистики и переключения режима
   - Поле поиска
   - Фильтры по категориям
   - Список заметок (LazyColumn)
   - FAB для добавления заметки

2. **NoteCard** - карточка заметки

   - Отображение заголовка, описания, даты
   - Индикатор приоритета (цветной чип)
   - Категория
   - Кнопки: выполнить/отменить, удалить
   - Клик по карточке открывает редактирование

3. **AddNoteDialog** - диалог создания/редактирования

   - Поля: заголовок, описание
   - Выбор категории
   - Выбор приоритета (3 уровня)
   - Выбор даты и времени
   - Кнопки: Сохранить, Отмена

4. **CategoryDialog** - диалог выбора категории

   - Список существующих категорий (RadioButton)
   - Поле для создания новой категории

5. **DatePickerDialog** - выбор даты

   - Отображение текущей даты
   - Кнопки: -1 день, +1 день, -7 дней, +7 дней

6. **TimePickerDialog** - выбор времени

   - Отдельные контролы для часов и минут
   - Кнопки +/- для изменения значений

7. **StatsDialog** - статистика

   - Количество активных заметок
   - Количество выполненных заметок
   - Процент выполнения

8. **HorizontalScrollableChips** - фильтры категорий
   - Горизонтальный скроллируемый список чипов
   - Чип "Все" для сброса фильтра

## Цветовая схема приоритетов

```kotlin
val priorityColors = listOf(
    Color(0xFF4CAF50),  // Низкий - зеленый
    Color(0xFFFF9800),  // Средний - оранжевый
    Color(0xFFF44336)   // Высокий - красный
)
```

## Разрешения

### AndroidManifest.xml

```xml
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
<uses-permission android:name="android.permission.SCHEDULE_EXACT_ALARM" />
<uses-permission android:name="android.permission.USE_EXACT_ALARM" />
```

### Runtime разрешения

В `MainActivity.onCreate()`:

- Запрос `POST_NOTIFICATIONS` для Android 13+ (API 33+)
- Используется `ActivityResultContracts.RequestPermission()`

## Зависимости

### Основные библиотеки:

```kotlin
// Room
implementation("androidx.room:room-runtime:2.6.1")
implementation("androidx.room:room-ktx:2.6.1")
ksp("androidx.room:room-compiler:2.6.1")

// WorkManager
implementation("androidx.work:work-runtime-ktx:2.9.0")

// Compose
implementation(platform("androidx.compose:compose-bom:2024.09.00"))
implementation("androidx.compose.ui:ui")
implementation("androidx.compose.material3:material3")
implementation("androidx.compose.material:material-icons-extended")

// ViewModel
implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
```

## Архитектура

### Паттерн: MVVM (Model-View-ViewModel)

```
View (Compose UI)
    ↓ (наблюдает)
ViewModel
    ↓ (использует)
Repository (Room Database)
    ↓ (хранит)
Database
```

**Потоки данных:**

- View → ViewModel: события пользователя (клики, ввод)
- ViewModel → View: Flow<List<Note>> (реактивные данные)
- ViewModel → Repository: операции с БД (suspend функции)
- Repository → Database: Room операции

## Жизненный цикл уведомлений

1. **Создание заметки:**

   ```
   insertNote() →
   Вычисление времени уведомления (scheduledDateTime - 10 минут) →
   scheduleNotification() →
   WorkManager.enqueue()
   ```

2. **Обновление заметки:**

   ```
   updateNote() →
   cancelNotification() (старое) →
   scheduleNotification() (новое)
   ```

3. **Выполнение задачи:**

   ```
   toggleNoteCompletion() →
   cancelNotification()
   ```

4. **Удаление заметки:**
   ```
   deleteNote() →
   cancelNotification()
   ```

## Тестирование

### Рекомендуемые тесты:

1. **NoteDao тесты:**

   - Вставка, обновление, удаление
   - Запросы с фильтрами
   - Поиск

2. **NoteViewModel тесты:**

   - Логика планирования уведомлений
   - Переключение статуса выполнения
   - Поиск и фильтрация

3. **UI тесты:**
   - Создание заметки
   - Редактирование
   - Удаление
   - Поиск

## Отладка

### Полезные команды:

```bash
# Просмотр логов Room
adb shell setprop log.tag.Room VERBOSE

# Просмотр логов WorkManager
adb shell setprop log.tag.WorkManager VERBOSE

# Очистка данных приложения
adb shell pm clear com.example.myapplication
```

### Проверка уведомлений:

1. Проверьте разрешения в настройках приложения
2. Убедитесь, что канал уведомлений создан
3. Проверьте WorkManager через `adb shell dumpsys jobscheduler`

## Форматирование дат

Используется `SimpleDateFormat`:

- Дата: `"dd.MM.yyyy"`
- Время: `"HH:mm"`
- Дата и время: `"dd.MM.yyyy HH:mm"`

Локаль: `Locale.getDefault()`

## Возможные улучшения

1. **Миграции БД:**

   - Заменить `fallbackToDestructiveMigration()` на реальные миграции
   - Версионирование схемы БД

2. **Репозиторий:**

   - Вынести логику доступа к данным в отдельный Repository класс
   - Добавить кэширование

3. **Тестирование:**

   - Unit тесты для ViewModel
   - UI тесты для Compose
   - Интеграционные тесты

4. **Производительность:**

   - Пагинация для больших списков
   - Оптимизация запросов к БД

5. **Функциональность:**
   - Экспорт/импорт заметок
   - Синхронизация с облаком
   - Повторяющиеся задачи

## Дополнительные ресурсы

- [Room Database](https://developer.android.com/training/data-storage/room)
- [WorkManager](https://developer.android.com/topic/libraries/architecture/workmanager)
- [Jetpack Compose](https://developer.android.com/jetpack/compose)
- [Material Design 3](https://m3.material.io/)

---

**Версия документации:** 1.0.0  
**Последнее обновление:** 07.11.2025
