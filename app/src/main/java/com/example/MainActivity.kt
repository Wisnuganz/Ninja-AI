package com.example

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.HistoryScreen
import com.example.ui.HomeScreen
import com.example.ui.MainViewModel
import com.example.ui.SettingsScreen
import com.example.ui.StealthScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.StealthCyan
import com.example.ui.theme.StealthDarkBg
import com.example.ui.theme.StealthDarkSurface
import com.example.ui.theme.StealthTextSecondary

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val uiState by viewModel.uiState.collectAsState()
            val tasks by viewModel.tasksFlow.collectAsState()

            // MediaProjection Launcher for Live Screen
            val mediaProjectionLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.StartActivityForResult()
            ) { result ->
                if (result.resultCode == Activity.RESULT_OK && result.data != null) {
                    viewModel.onLiveScreenResult(result.resultCode, result.data)
                    Toast.makeText(this, "Izin Live Screen berhasil diaktifkan!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Izin Live Screen ditolak", Toast.LENGTH_SHORT).show()
                }
            }

            fun requestLiveScreenPermission() {
                val mpManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as? MediaProjectionManager
                if (mpManager != null) {
                    mediaProjectionLauncher.launch(mpManager.createScreenCaptureIntent())
                }
            }

            MyApplicationTheme(darkTheme = true) {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = StealthDarkBg,
                    bottomBar = {
                        NavigationBar(
                            containerColor = StealthDarkSurface,
                            tonalElevation = 8.dp
                        ) {
                            NavigationBarItem(
                                selected = uiState.currentTab == 0,
                                onClick = { viewModel.setTab(0) },
                                icon = {
                                    Icon(
                                        imageVector = Icons.Default.Home,
                                        contentDescription = "Beranda"
                                    )
                                },
                                label = { Text("Beranda", fontSize = 11.sp) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = StealthCyan,
                                    selectedTextColor = StealthCyan,
                                    unselectedIconColor = StealthTextSecondary,
                                    unselectedTextColor = StealthTextSecondary,
                                    indicatorColor = StealthCyan.copy(alpha = 0.15f)
                                ),
                                modifier = Modifier.testTag("nav_home")
                            )

                            NavigationBarItem(
                                selected = uiState.currentTab == 1,
                                onClick = { viewModel.setTab(1) },
                                icon = {
                                    Icon(
                                        imageVector = Icons.Default.History,
                                        contentDescription = "Riwayat"
                                    )
                                },
                                label = { Text("Riwayat", fontSize = 11.sp) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = StealthCyan,
                                    selectedTextColor = StealthCyan,
                                    unselectedIconColor = StealthTextSecondary,
                                    unselectedTextColor = StealthTextSecondary,
                                    indicatorColor = StealthCyan.copy(alpha = 0.15f)
                                ),
                                modifier = Modifier.testTag("nav_history")
                            )

                            NavigationBarItem(
                                selected = uiState.currentTab == 2,
                                onClick = { viewModel.setTab(2) },
                                icon = {
                                    Icon(
                                        imageVector = Icons.Default.Security,
                                        contentDescription = "Samaran"
                                    )
                                },
                                label = { Text("Samaran", fontSize = 11.sp) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = StealthCyan,
                                    selectedTextColor = StealthCyan,
                                    unselectedIconColor = StealthTextSecondary,
                                    unselectedTextColor = StealthTextSecondary,
                                    indicatorColor = StealthCyan.copy(alpha = 0.15f)
                                ),
                                modifier = Modifier.testTag("nav_stealth")
                            )

                            NavigationBarItem(
                                selected = uiState.currentTab == 3,
                                onClick = { viewModel.setTab(3) },
                                icon = {
                                    Icon(
                                        imageVector = Icons.Default.Settings,
                                        contentDescription = "Pengaturan"
                                    )
                                },
                                label = { Text("Pengaturan", fontSize = 11.sp) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = StealthCyan,
                                    selectedTextColor = StealthCyan,
                                    unselectedIconColor = StealthTextSecondary,
                                    unselectedTextColor = StealthTextSecondary,
                                    indicatorColor = StealthCyan.copy(alpha = 0.15f)
                                ),
                                modifier = Modifier.testTag("nav_settings")
                            )
                        }
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        when (uiState.currentTab) {
                            0 -> HomeScreen(
                                uiState = uiState,
                                onToggleOverlay = { viewModel.toggleOverlayService(this@MainActivity) },
                                onRequestLiveScreenPermission = { requestLiveScreenPermission() },
                                onSolveQuestion = { question, bitmap ->
                                    viewModel.solveQuestion(question, bitmap)
                                }
                            )
                            1 -> HistoryScreen(
                                tasks = tasks,
                                selectedSubject = uiState.selectedSubject,
                                searchQuery = uiState.searchQuery,
                                onSearchChange = { viewModel.setSearchQuery(it) },
                                onSubjectChange = { viewModel.setSubjectFilter(it) },
                                onDeleteTask = { viewModel.deleteTask(it) },
                                onClearAll = { viewModel.clearAllHistory() }
                            )
                            2 -> StealthScreen()
                            3 -> SettingsScreen(
                                uiState = uiState,
                                onSaveApiKey = { viewModel.saveApiKey(it) },
                                onRequestLiveScreenPermission = { requestLiveScreenPermission() }
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshState()
    }
}
