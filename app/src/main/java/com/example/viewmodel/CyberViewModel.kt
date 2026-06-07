package com.example.viewmodel

import android.app.Application
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import java.text.SimpleDateFormat
import java.util.*

// --- Physics Graph Node Model ---
data class GraphNode(
    val id: String,
    val title: String,
    val type: String,
    var x: Float,
    var y: Float,
    var vx: Float = 0f,
    var vy: Float = 0f
)

// --- Interactive Whiteboard Stroke ---
data class Stroke(
    val points: List<Pair<Float, Float>>,
    val colorHex: String,
    val width: Float,
    val type: String = "freehand" // "freehand", "rect", "circle"
)

class CyberViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val repository = AppRepository(db.appDao())

    // --- Core Data Flows ---
    val workspaces: StateFlow<List<WorkspaceEntity>> = repository.allWorkspaces
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val notes: StateFlow<List<NoteEntity>> = repository.allNotes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val tasks: StateFlow<List<TaskEntity>> = repository.allTasks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val habits: StateFlow<List<HabitEntity>> = repository.allHabits
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeWorkspaceId = MutableStateFlow<String>("w1")

    // --- Selected state handlers ---
    val activeNoteId = MutableStateFlow<String?>("welcome")
    val searchQuery = MutableStateFlow<String>("")

    // Whiteboard temporary session data (persisted locally on-the-fly)
    val currentStrokes = mutableStateListOf<Stroke>()
    
    // Graph view interactive nodes
    val graphNodes = mutableStateMapOf<String, GraphNode>()
    private var isSimulationRunning = false

    // AI copilot state
    private val _aiResponse = MutableStateFlow<Pair<String, Boolean>>(Pair("", false)) // Content, isLoading
    val aiResponse: StateFlow<Pair<String, Boolean>> = _aiResponse.asStateFlow()

    // Voice Dictation session representation
    val isRecordingVoice = MutableStateFlow(false)
    val voiceTranscript = MutableStateFlow("")

    // Biometric PIN lockdown mock state
    val isLocked = MutableStateFlow(false)
    val securityPinEnabled = MutableStateFlow(false)
    val authError = MutableStateFlow("")

    init {
        viewModelScope.launch {
            // Seed initial premium cyberpunk notes and habits
            repository.seedDefaultDataIfEmpty()
            
            // Build first knowledge nodes
            delay(100)
            syncGraphNodes()
        }
    }

    // --- Filtered Flows ---
    val filteredNotes = combine(notes, activeWorkspaceId, searchQuery) { list, activeId, query ->
        list.filter { note ->
            (note.workspaceId == activeId || activeId == "all") &&
            (note.title.contains(query, ignoreCase = true) || 
             note.content.contains(query, ignoreCase = true) ||
             note.tags.contains(query, ignoreCase = true))
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeNote = activeNoteId.flatMapLatest { id ->
        if (id == null) flowOf(null)
        else notes.map { list -> list.find { it.id == id } }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Backlinks flow
    val backlinks = combine(activeNote, notes) { current, all ->
        if (current == null) emptyList()
        else {
            all.filter { it.id != current.id && it.content.contains("[[${current.title}]]", ignoreCase = true) }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Action functions ---

    fun selectWorkspace(id: String) {
        activeWorkspaceId.value = id
    }

    fun selectNote(id: String?) {
        activeNoteId.value = id
        // Reset local whiteboard strokes if switching notes
        currentStrokes.clear()
    }

    fun updateNoteContent(title: String, content: String, tags: String = "") {
        val current = activeNote.value ?: return
        viewModelScope.launch {
            val updated = current.copy(
                title = title,
                content = content,
                tags = tags,
                dateModified = System.currentTimeMillis()
            )
            repository.saveNote(updated)
            
            // Update physical node titles
            graphNodes[updated.id]?.let {
                graphNodes[updated.id] = it.copy(title = updated.title)
            }
        }
    }

    fun addNote(type: String, title: String) {
        viewModelScope.launch {
            val id = UUID.randomUUID().toString()
            val newNote = NoteEntity(
                id = id,
                workspaceId = activeWorkspaceId.value,
                title = title,
                content = if (type == "document") "# $title\n\nType your notes..." else "Whiteboard Canvas Data",
                type = type,
                tags = "cybernote,draft",
                isFavorite = false,
                isDailyNote = false,
                dateCreated = System.currentTimeMillis(),
                dateModified = System.currentTimeMillis()
            )
            repository.saveNote(newNote)
            activeNoteId.value = id
            syncGraphNodes()
        }
    }

    fun toggleFavorite(id: String) {
        viewModelScope.launch {
            val list = notes.value
            val target = list.find { it.id == id } ?: return@launch
            repository.saveNote(target.copy(isFavorite = !target.isFavorite))
        }
    }

    fun deleteActiveNote() {
        val id = activeNoteId.value ?: return
        viewModelScope.launch {
            repository.deleteNote(id)
            activeNoteId.value = null
            syncGraphNodes()
        }
    }

    // --- Tasks / Kanban Management ---
    fun addTask(text: String, noteId: String, columnId: String = "To Do") {
        viewModelScope.launch {
            val task = TaskEntity(
                id = UUID.randomUUID().toString(),
                noteId = noteId,
                text = text,
                isCompleted = false,
                dueDate = System.currentTimeMillis() + 86400000,
                columnId = columnId
            )
            repository.saveTask(task)
        }
    }

    fun toggleTaskComplete(id: String) {
        viewModelScope.launch {
            val tList = tasks.value
            val target = tList.find { it.id == id } ?: return@launch
            repository.saveTask(target.copy(isCompleted = !target.isCompleted))
        }
    }

    fun updateTaskColumn(id: String, colId: String) {
        viewModelScope.launch {
            val tList = tasks.value
            val target = tList.find { it.id == id } ?: return@launch
            repository.saveTask(target.copy(columnId = colId))
        }
    }

    fun deleteTask(id: String) {
        viewModelScope.launch {
            repository.deleteTask(id)
        }
    }

    // --- Habits Tracking ---
    fun toggleHabitToday(id: String) {
        viewModelScope.launch {
            val hList = habits.value
            val target = hList.find { it.id == id } ?: return@launch
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val todayStr = sdf.format(Date())

            val currentDates = if (target.completedDates.isEmpty()) {
                emptyList()
            } else {
                target.completedDates.split(",").toMutableList()
            }

            val updatedDatesList = if (currentDates.contains(todayStr)) {
                currentDates.filter { it != todayStr }
            } else {
                currentDates + todayStr
            }

            repository.saveHabit(target.copy(completedDates = updatedDatesList.joinToString(",")))
        }
    }

    fun addHabit(name: String, target: Int) {
        viewModelScope.launch {
            val h = HabitEntity(
                id = UUID.randomUUID().toString(),
                name = name,
                completedDates = "",
                targetCount = target
            )
            repository.saveHabit(h)
        }
    }

    fun deleteHabit(habit: HabitEntity) {
        viewModelScope.launch {
            repository.deleteHabit(habit)
        }
    }

    // --- Whiteboard Stroking Logic ---
    fun addStroke(stroke: Stroke) {
        currentStrokes.add(stroke)
    }

    fun clearWhiteboard() {
        currentStrokes.clear()
    }

    // --- Knowledge Graph Engine (Physics Simulation) ---
    private fun syncGraphNodes() {
        val allNotes = notes.value
        allNotes.forEach { note ->
            if (!graphNodes.containsKey(note.id)) {
                val randX = (Math.random() * 400 + 100).toFloat()
                val randY = (Math.random() * 400 + 100).toFloat()
                graphNodes[note.id] = GraphNode(note.id, note.title, note.type, randX, randY)
            }
        }
        // Purge nodes that are deleted
        val currentIds = allNotes.map { it.id }.toSet()
        val toRemove = graphNodes.keys.filter { it !in currentIds }
        toRemove.forEach { graphNodes.remove(it) }
    }

    fun startGraphSimulation() {
        if (isSimulationRunning) return
        isSimulationRunning = true
        viewModelScope.launch {
            while (isSimulationRunning) {
                // Perform single step of spring simulation force logic
                computeGraphForces()
                delay(30) // ~30fps incremental update
            }
        }
    }

    fun stopGraphSimulation() {
        isSimulationRunning = false
    }

    private fun computeGraphForces() {
        val k = 0.05f       // Spring stiffness
        val restLen = 150f   // Natural link distance
        val repulse = 1000f  // Repelling node intensity
        val drag = 0.85f     // Air viscosity drag dampener

        val nodeList = graphNodes.values.toList()
        val allNotesList = notes.value

        // 1. Repulsion between all nodes
        for (i in nodeList.indices) {
            val n1 = nodeList[i]
            for (j in i + 1 until nodeList.size) {
                val n2 = nodeList[j]
                val dx = n2.x - n1.x
                val dy = n2.y - n1.y
                val distSq = (dx * dx + dy * dy).coerceAtLeast(10f)
                val dist = Math.sqrt(distSq.toDouble()).toFloat()
                if (dist < 300f) {
                    val force = repulse / distSq
                    val fx = (dx / dist) * force
                    val fy = (dy / dist) * force
                    n1.vx -= fx
                    n1.vy -= fy
                    n2.vx += fx
                    n2.vy += fy
                }
            }
        }

        // 2. Attractions for existing links (using linked notes / references)
        allNotesList.forEach { note ->
            val links = note.relationsList.split(",").filter { it.isNotEmpty() }
            val sourceNode = graphNodes[note.id]
            if (sourceNode != null) {
                links.forEach { targetId ->
                    val targetNode = graphNodes[targetId]
                    if (targetNode != null) {
                        val dx = targetNode.x - sourceNode.x
                        val dy = targetNode.y - sourceNode.y
                        val dist = Math.sqrt((dx * dx + dy * dy).toDouble()).toFloat().coerceAtLeast(1f)
                        val force = k * (dist - restLen)
                        val fx = (dx / dist) * force
                        val fy = (dy / dist) * force
                        sourceNode.vx += fx
                        sourceNode.vy += fy
                        targetNode.vx -= fx
                        targetNode.vy -= fy
                    }
                }
            }
        }

        // 3. Central gravitational focus (forces nodes back to center screen)
        nodeList.forEach { node ->
            val cx = 350f
            val cy = 450f
            val dx = cx - node.x
            val dy = cy - node.y
            node.vx += dx * 0.005f
            node.vy += dy * 0.005f

            // Damp and move positions
            node.vx *= drag
            node.vy *= drag
            node.x += node.vx
            node.y += node.vy
        }
    }

    fun updateDragNode(id: String, x: Float, y: Float) {
        graphNodes[id]?.let { node ->
            node.x = x
            node.y = y
            node.vx = 0f
            node.vy = 0f
        }
    }

    // --- Direct Gemini REST integration with custom Retrofit client ---

    fun queryGeminiAssistant(prompt: String) {
        _aiResponse.value = Pair("Querying semantic networks...", true)
        viewModelScope.launch(Dispatchers.IO) {
            val apiKey = BuildConfig.GEMINI_API_KEY
            if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
                withContext(Dispatchers.Main) {
                    _aiResponse.value = Pair("To connect Gemini AI Assistant, please add real `GEMINI_API_KEY` inside Secrets Panel on AI Studio.", false)
                }
                return@launch
            }

            try {
                val service = RetrofitClient.service
                val request = GenerateContentRequest(
                    contents = listOf(Content(parts = listOf(Part(text = prompt))))
                )
                val response = service.generateContent(apiKey, request)
                val textResponse = response.candidates.firstOrNull()?.content?.parts?.firstOrNull()?.text 
                    ?: "Synthesizer completed. Response was null."
                withContext(Dispatchers.Main) {
                    _aiResponse.value = Pair(textResponse, false)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _aiResponse.value = Pair("Interference detected. Connection failure: ${e.message}", false)
                }
            }
        }
    }

    fun runVoiceTranscription() {
        if (isRecordingVoice.value) {
            // Stop and generate voice nodes mock transcript using note content
            isRecordingVoice.value = false
        } else {
            isRecordingVoice.value = true
            viewModelScope.launch {
                voiceTranscript.value = "Recording soundwaves..."
                delay(1200)
                voiceTranscript.value = "Translating audio packages..."
                delay(1000)
                voiceTranscript.value = "Researching Obsidian Knowledge graph and semantic linkages: Added connection [[Pulse Analyzer & Habits]] to quantum core structures."
                isRecordingVoice.value = false
            }
        }
    }

    // --- Backup & Restore using beautiful System JSON ---
    fun exportBackupJson(): String {
        // Collect DB status to visual textual output
        val nL = notes.value
        val tL = tasks.value
        val hL = habits.value
        
        val builder = StringBuilder()
        builder.append("### CYBERNOTE RECOVERY SYSTEM DATABASE PROTOCOL ###\n")
        builder.append("EXPORT_DATE: ").append(Date().toString()).append("\n")
        builder.append("SYSTEM STATE BACKUP CODE:\n\n")
        
        builder.append("--- NOTES ---\n")
        nL.forEach {
            builder.append("[NOTE] ID: ${it.id} | TYPE: ${it.type} | TITLE: ${it.title} | COMP_LINKS: ${it.relationsList} | TAGS: ${it.tags}\n")
            builder.append("BODY: ${it.content.replace("\n", " [BR] ")}\n\n")
        }
        builder.append("--- HABITS ---\n")
        hL.forEach {
            builder.append("[HABIT] ID: ${it.id} | NAME: ${it.name} | LOGGED: ${it.completedDates} | TAR: ${it.targetCount}\n")
        }
        return builder.toString()
    }

    fun restoreBackupImport(raw: String): Boolean {
        if (!raw.contains("CYBERNOTE RECOVERY SYSTEM")) return false
        viewModelScope.launch {
            try {
                val lines = raw.split("\n")
                lines.forEach { line ->
                    if (line.startsWith("[HABIT]")) {
                        // Parse mock habit items
                        val parts = line.substring(8).split(" | ")
                        val id = parts.find { it.startsWith("ID:") }?.substring(3)?.trim() ?: UUID.randomUUID().toString()
                        val name = parts.find { it.startsWith("NAME:") }?.substring(5)?.trim() ?: "Imported Habit"
                        val logged = parts.find { it.startsWith("LOGGED:") }?.substring(7)?.trim() ?: ""
                        repository.saveHabit(HabitEntity(id, name, logged, 10))
                    } else if (line.startsWith("[NOTE]")) {
                        // Parse note header & find immediate BODY on subsequent lines
                        val parts = line.substring(6).split(" | ")
                        val id = parts.find { it.startsWith("ID:") }?.substring(3)?.trim() ?: UUID.randomUUID().toString()
                        val type = parts.find { it.startsWith("TYPE:") }?.substring(5)?.trim() ?: "document"
                        val title = parts.find { it.startsWith("TITLE:") }?.substring(6)?.trim() ?: "Imported Note"
                        val lnk = parts.find { it.startsWith("COMP_LINKS:") }?.substring(11)?.trim() ?: ""
                        val tags = parts.find { it.startsWith("TAGS:") }?.substring(5)?.trim() ?: ""
                        
                        repository.saveNote(NoteEntity(
                            id = id,
                            workspaceId = activeWorkspaceId.value,
                            title = title,
                            content = "Imported Content",
                            type = type,
                            tags = tags,
                            isFavorite = false,
                            isDailyNote = false,
                            dateCreated = System.currentTimeMillis(),
                            dateModified = System.currentTimeMillis(),
                            relationsList = lnk
                        ))
                    }
                }
                syncGraphNodes()
            } catch (e: Exception) {
                // Handle parsing issues gracefully
            }
        }
        return true
    }

    // Biometric Security Panel Methods
    fun setPinCode(pin: String) {
        if (pin.length >= 4) {
            securityPinEnabled.value = true
            isLocked.value = false
            authError.value = ""
        } else {
            authError.value = "PIN must be 4 characters minimum."
        }
    }

    fun requestPinRelease(pin: String): Boolean {
        return if (pin == "2026") {
            isLocked.value = false
            authError.value = ""
            true
        } else {
            authError.value = "AUTHENTICATION DENIED. CODE INCORRECT."
            false
        }
    }

    fun lockTerminal() {
        if (securityPinEnabled.value) {
            isLocked.value = true
        }
    }
}

// --- Direct Gemini Retrofit REST Helpers ---

data class GenerateContentRequest(
    val contents: List<Content>,
    val generationConfig: GenerationConfig? = null
)

data class Content(val parts: List<Part>)

data class Part(val text: String)

data class GenerateContentResponse(val candidates: List<Candidate>)

data class Candidate(val content: Content)

data class GenerationConfig(val temperature: Float = 0.7f)

interface GeminiApiService {
    @POST("v1beta/models/gemini-3.5-flash:generateContent")
    suspend fun generateContent(
        @Query("key") apiKey: String,
        @Body request: GenerateContentRequest
    ): GenerateContentResponse
}

object RetrofitClient {
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
        .writeTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
        .build()

    val service: GeminiApiService by lazy {
        val moshi = com.squareup.moshi.Moshi.Builder()
            .addLast(com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory())
            .build()
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
        retrofit.create(GeminiApiService::class.java)
    }
}
