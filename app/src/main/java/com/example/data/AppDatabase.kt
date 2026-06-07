package com.example.data

import android.content.Context
import androidx.room.*
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import kotlinx.coroutines.flow.Flow

// --- Room Entities ---

@Entity(tableName = "workspaces")
data class WorkspaceEntity(
    @PrimaryKey val id: String,
    val name: String,
    val icon: String,
    val colorHex: String
)

@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey val id: String,
    val workspaceId: String,
    val title: String,
    val content: String, // Notion blocks JSON, markdown text, or whiteboard strokes JSON
    val type: String,    // "document", "whiteboard", "kanban"
    val tags: String,    // Comma-separated (e.g., "work,cyberpunk")
    val isFavorite: Boolean,
    val isDailyNote: Boolean,
    val dateCreated: Long,
    val dateModified: Long,
    val relationsList: String = "" // Serialized note IDs showing links (e.g. "note1,note2")
)

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey val id: String,
    val noteId: String, // Link to custom pages/boards
    val text: String,
    val isCompleted: Boolean,
    val dueDate: Long,
    val columnId: String // "To Do", "In Progress", "Done" for Kanban boards
)

@Entity(tableName = "habits")
data class HabitEntity(
    @PrimaryKey val id: String,
    val name: String,
    val completedDates: String, // CSV of formatted dates (e.g., "2026-06-07,2026-06-08")
    val targetCount: Int
)

// --- Type Converters ---

class Converters {
    @TypeConverter
    fun fromString(value: String): List<String> {
        if (value.isEmpty()) return emptyList()
        return value.split(",")
    }

    @TypeConverter
    fun fromList(list: List<String>): String {
        return list.joinToString(",")
    }
}

// --- App DAO (Data Access Object) ---

@Dao
interface AppDao {
    // Workspace Queres
    @Query("SELECT * FROM workspaces")
    fun getAllWorkspaces(): Flow<List<WorkspaceEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkspace(workspace: WorkspaceEntity)

    @Delete
    suspend fun deleteWorkspace(workspace: WorkspaceEntity)

    // Note Queries
    @Query("SELECT * FROM notes ORDER BY dateModified DESC")
    fun getAllNotes(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE id = :id")
    suspend fun getNoteById(id: String): NoteEntity?

    @Query("SELECT * FROM notes WHERE workspaceId = :workspaceId ORDER BY dateModified DESC")
    fun getNotesByWorkspace(workspaceId: String): Flow<List<NoteEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: NoteEntity)

    @Query("DELETE FROM notes WHERE id = :id")
    suspend fun deleteNoteById(id: String)

    // Task Queries
    @Query("SELECT * FROM tasks")
    fun getAllTasks(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE noteId = :noteId")
    fun getTasksByNote(noteId: String): Flow<List<TaskEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: TaskEntity)

    @Query("DELETE FROM tasks WHERE id = :id")
    suspend fun deleteTaskById(id: String)

    // Habit Queries
    @Query("SELECT * FROM habits")
    fun getAllHabits(): Flow<List<HabitEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHabit(habit: HabitEntity)

    @Delete
    suspend fun deleteHabit(habit: HabitEntity)
}

// --- App Room Database ---

@Database(
    entities = [WorkspaceEntity::class, NoteEntity::class, TaskEntity::class, HabitEntity::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun appDao(): AppDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "cyber_note_db"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
