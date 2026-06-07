package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.util.UUID

class AppRepository(private val appDao: AppDao) {

    val allWorkspaces: Flow<List<WorkspaceEntity>> = appDao.getAllWorkspaces()
    val allNotes: Flow<List<NoteEntity>> = appDao.getAllNotes()
    val allTasks: Flow<List<TaskEntity>> = appDao.getAllTasks()
    val allHabits: Flow<List<HabitEntity>> = appDao.getAllHabits()

    fun getNotesByWorkspace(workspaceId: String): Flow<List<NoteEntity>> =
        appDao.getNotesByWorkspace(workspaceId)

    suspend fun getNoteById(id: String): NoteEntity? =
        appDao.getNoteById(id)

    suspend fun saveNote(note: NoteEntity) {
        appDao.insertNote(note)
    }

    suspend fun deleteNote(id: String) {
        appDao.deleteNoteById(id)
    }

    suspend fun saveWorkspace(workspace: WorkspaceEntity) {
        appDao.insertWorkspace(workspace)
    }

    suspend fun deleteWorkspace(workspace: WorkspaceEntity) {
        appDao.deleteWorkspace(workspace)
    }

    suspend fun saveTask(task: TaskEntity) {
        appDao.insertTask(task)
    }

    suspend fun deleteTask(id: String) {
        appDao.deleteTaskById(id)
    }

    suspend fun saveHabit(habit: HabitEntity) {
        appDao.insertHabit(habit)
    }

    suspend fun deleteHabit(habit: HabitEntity) {
        appDao.deleteHabit(habit)
    }

    // Seed beautiful default records for the user
    suspend fun seedDefaultDataIfEmpty() {
        val wkList = allWorkspaces.first()
        if (wkList.isNotEmpty()) return

        // 1. Seed workspaces
        val mainWorkspace = WorkspaceEntity(
            id = "w1",
            name = "Aether Sphere",
            icon = "SpaceDashboard",
            colorHex = "#00E5FF"
        )
        val secondWorkspace = WorkspaceEntity(
            id = "w2",
            name = "K-Graph Archives",
            icon = "Hub",
            colorHex = "#C700FF"
        )

        appDao.insertWorkspace(mainWorkspace)
        appDao.insertWorkspace(secondWorkspace)

        // 2. Seed default notes with bidirectional linkages
        val welcomeNote = NoteEntity(
            id = "welcome",
            workspaceId = "w1",
            title = "🪐 Welcome to CyberNote",
            type = "document",
            tags = "guide,welcome",
            isFavorite = true,
            isDailyNote = false,
            dateCreated = System.currentTimeMillis() - 86400000,
            dateModified = System.currentTimeMillis() - 86400000,
            relationsList = "habits,board,whiteboard",
            content = """
                # Welcome to CyberNote, Operator.
                
                You have synchronized with the ultimate mobile dynamic hyper-editor. Inspired by Notion, Obsidian, and capacities, this system runs fully offline-first.
                
                ### Core Functionality
                - **Obsidian Graph View**: Show relationships among files visually.
                - **[[Double Brace]] links**: Type `[[` or tap Link icon to create solid bilateral references.
                - **Infinite whiteboard**: Custom freeform layout and geometric elements.
                - **Fast local search**: Instantly scan headers and internal elements.
                
                Enjoy defragging your thoughts. Look at [[Pulse Analyzer & Habits]] or check out your daily sprint flow on [[Cyber Task Sprint]].
            """.trimIndent()
        )

        val habitsNote = NoteEntity(
            id = "habits",
            workspaceId = "w1",
            title = "⚡ Pulse Analyzer & Habits",
            type = "document",
            tags = "habits,routine",
            isFavorite = true,
            isDailyNote = false,
            dateCreated = System.currentTimeMillis() - 40000000,
            dateModified = System.currentTimeMillis() - 40000000,
            relationsList = "welcome",
            content = """
                # Habit Sync Module
                
                Consistent repetition is the core formula of neural alignment. Use the Habit Widget in the main Dashboard to quickly check off your protocols:
                
                - Check-in dates are stored safely in local Room encryption blocks.
                - Connect tracking directly back to custom workspace journals.
                
                Ensure you log back into [[Welcome to CyberNote]] for guidance.
            """.trimIndent()
        )

        val boardNote = NoteEntity(
            id = "board",
            workspaceId = "w1",
            title = "👾 Cyber Task Sprint",
            type = "kanban",
            tags = "tasks,kanban",
            isFavorite = false,
            isDailyNote = false,
            dateCreated = System.currentTimeMillis() - 20000000,
            dateModified = System.currentTimeMillis() - 20000000,
            relationsList = "welcome,whiteboard",
            content = "Kanban layout"
        )

        val whiteboardNote = NoteEntity(
            id = "whiteboard",
            workspaceId = "w2",
            title = "🌌 Cosmic Blueprint Canvas",
            type = "whiteboard",
            tags = "whiteboard,canvas",
            isFavorite = false,
            isDailyNote = false,
            dateCreated = System.currentTimeMillis() - 10000000,
            dateModified = System.currentTimeMillis() - 10000000,
            relationsList = "welcome,board",
            content = "Whiteboard data" // Loaded in states
        )

        val dailyNote = NoteEntity(
            id = "daily_note",
            workspaceId = "w1",
            title = "📅 Cyber Log: June 7",
            type = "document",
            tags = "daily,log",
            isFavorite = false,
            isDailyNote = true,
            dateCreated = System.currentTimeMillis(),
            dateModified = System.currentTimeMillis(),
            relationsList = "welcome",
            content = """
                # Cyber Log Protocol: 2026-06-07
                
                Subsystems calibrated. Working on optimizing the interactive physics knowledge graph view inside Jetpack Compose coordinate bounds today.
                
                - Streak factor: 100%
                - Focus mode: Quantum
                - Linked nodes: [[Welcome to CyberNote]]
            """.trimIndent()
        )

        appDao.insertNote(welcomeNote)
        appDao.insertNote(habitsNote)
        appDao.insertNote(boardNote)
        appDao.insertNote(whiteboardNote)
        appDao.insertNote(dailyNote)

        // 3. Seed tasks for Kanban Board
        appDao.insertTask(TaskEntity(UUID.randomUUID().toString(), "board", "Inject Neural Interface pipeline", false, System.currentTimeMillis() + 86400000, "To Do"))
        appDao.insertTask(TaskEntity(UUID.randomUUID().toString(), "board", "Calibrate Ambient synthesizer layout", false, System.currentTimeMillis() + 172800000, "In Progress"))
        appDao.insertTask(TaskEntity(UUID.randomUUID().toString(), "board", "Sync biometric security gateways", true, System.currentTimeMillis() - 10000000, "Done"))
        appDao.insertTask(TaskEntity(UUID.randomUUID().toString(), "board", "Enable Obsidian physics linkages", false, System.currentTimeMillis() + 259200000, "To Do"))

        // 4. Seed habits
        val h1 = HabitEntity("h1", "Quantum Defrag Core", "2026-06-05,2026-06-06", 5)
        val h2 = HabitEntity("h2", "Assemble Android Applets", "2026-06-06", 7)
        val h3 = HabitEntity("h3", "Hydrate Biological Shells", "2026-06-04,2026-06-05,2026-06-06", 30)

        appDao.insertHabit(h1)
        appDao.insertHabit(h2)
        appDao.insertHabit(h3)
    }
}
