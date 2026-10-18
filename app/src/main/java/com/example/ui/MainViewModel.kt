package com.example.ui

import android.app.Application
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
import android.provider.Settings
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.TaskDatabase
import com.example.data.gemini.GeminiHomeworkClient
import com.example.data.gemini.HomeworkSolution
import com.example.data.model.TaskItem
import com.example.service.FloatingOverlayService
import com.example.service.ScreenCaptureHolder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class MainUiState(
    val isOverlayRunning: Boolean = false,
    val hasOverlayPermission: Boolean = false,
    val hasLiveScreenPermission: Boolean = false,
    val isAnalyzing: Boolean = false,
    val statusMessage: String = "",
    val currentSolution: HomeworkSolution? = null,
    val selectedSubject: String = "Semua",
    val searchQuery: String = "",
    val apiKey: String = "",
    val isDisguiseModeActive: Boolean = false,
    val currentTab: Int = 0 // 0: Beranda, 1: Riwayat, 2: Uji Samaran, 3: Pengaturan
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val database = TaskDatabase.getInstance(application)
    private val geminiClient = GeminiHomeworkClient(application)

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    private val _searchQueryFlow = MutableStateFlow("")
    private val _subjectFilterFlow = MutableStateFlow("Semua")

    val tasksFlow: StateFlow<List<TaskItem>> = combine(
        database.taskDao().getAllTasks(),
        _searchQueryFlow,
        _subjectFilterFlow
    ) { tasks, query, subject ->
        tasks.filter { item ->
            val matchQuery = query.isBlank() ||
                    item.questionText.contains(query, ignoreCase = true) ||
                    item.shortAnswer.contains(query, ignoreCase = true) ||
                    item.detailedSteps.contains(query, ignoreCase = true)

            val matchSubject = subject == "Semua" || item.subject.equals(subject, ignoreCase = true)

            matchQuery && matchSubject
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        refreshState()
        // Collect overlay service running state
        viewModelScope.launch {
            FloatingOverlayService.isRunningFlow.collect { running ->
                _uiState.value = _uiState.value.copy(isOverlayRunning = running)
            }
        }
    }

    fun refreshState() {
        val context = getApplication<Application>()
        val hasOverlay = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(context)
        } else {
            true
        }

        _uiState.value = _uiState.value.copy(
            hasOverlayPermission = hasOverlay,
            hasLiveScreenPermission = ScreenCaptureHolder.isPermissionGranted,
            apiKey = geminiClient.getApiKey()
        )
    }

    fun setTab(tabIndex: Int) {
        _uiState.value = _uiState.value.copy(currentTab = tabIndex)
    }

    fun setSearchQuery(query: String) {
        _searchQueryFlow.value = query
        _uiState.value = _uiState.value.copy(searchQuery = query)
    }

    fun setSubjectFilter(subject: String) {
        _subjectFilterFlow.value = subject
        _uiState.value = _uiState.value.copy(selectedSubject = subject)
    }

    fun setDisguiseMode(active: Boolean) {
        _uiState.value = _uiState.value.copy(isDisguiseModeActive = active)
    }

    fun saveApiKey(newKey: String) {
        geminiClient.saveApiKey(newKey)
        _uiState.value = _uiState.value.copy(apiKey = newKey)
    }

    fun toggleOverlayService(context: Context) {
        if (_uiState.value.isOverlayRunning) {
            FloatingOverlayService.stopService(context)
        } else {
            if (_uiState.value.hasOverlayPermission) {
                FloatingOverlayService.startService(context)
            }
        }
    }

    fun onLiveScreenResult(resultCode: Int, data: Intent?) {
        ScreenCaptureHolder.setPermission(resultCode, data)
        _uiState.value = _uiState.value.copy(
            hasLiveScreenPermission = ScreenCaptureHolder.isPermissionGranted
        )
    }

    fun solveQuestion(question: String, bitmap: Bitmap?) {
        if (_uiState.value.isAnalyzing) return

        _uiState.value = _uiState.value.copy(
            isAnalyzing = true,
            statusMessage = "Menganalisis tugas secara hening..."
        )

        viewModelScope.launch {
            val result = geminiClient.solveHomework(
                questionText = question.ifBlank { null },
                imageBitmap = bitmap
            )

            result.onSuccess { sol ->
                _uiState.value = _uiState.value.copy(
                    isAnalyzing = false,
                    currentSolution = sol,
                    statusMessage = ""
                )

                // Save to history
                launch(Dispatchers.IO) {
                    database.taskDao().insertTask(
                        TaskItem(
                            subject = sol.subject,
                            questionText = question.ifBlank { "Tangkapan Layar / Foto Soal" },
                            shortAnswer = sol.shortAnswer,
                            detailedSteps = sol.detailedSteps,
                            keyFormulas = sol.keyFormulas,
                            source = if (bitmap != null) "LIVE_SCREEN" else "TEXT"
                        )
                    )
                }
            }.onFailure { err ->
                _uiState.value = _uiState.value.copy(
                    isAnalyzing = false,
                    statusMessage = "Error: ${err.message}"
                )
            }
        }
    }

    fun deleteTask(task: TaskItem) {
        viewModelScope.launch(Dispatchers.IO) {
            database.taskDao().deleteTask(task)
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch(Dispatchers.IO) {
            database.taskDao().clearAllTasks()
        }
    }
}
