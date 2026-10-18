package com.example.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.TaskItem
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks ORDER BY timestamp DESC")
    fun getAllTasks(): Flow<List<TaskItem>>

    @Query("SELECT * FROM tasks WHERE subject = :subject ORDER BY timestamp DESC")
    fun getTasksBySubject(subject: String): Flow<List<TaskItem>>

    @Query("SELECT * FROM tasks WHERE questionText LIKE '%' || :query || '%' OR shortAnswer LIKE '%' || :query || '%' ORDER BY timestamp DESC")
    fun searchTasks(query: String): Flow<List<TaskItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: TaskItem): Long

    @Update
    suspend fun updateTask(task: TaskItem)

    @Delete
    suspend fun deleteTask(task: TaskItem)

    @Query("DELETE FROM tasks")
    suspend fun clearAllTasks()
}
