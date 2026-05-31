package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import com.example.data.local.MediaDatabase
import com.example.data.repository.MediaRepository
import com.example.ui.screens.EditorScreen
import com.example.ui.screens.LibraryScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.EditViewModel
import com.example.ui.viewmodel.EditViewModelFactory

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    
    // Initialize Room Database persistence & Repository
    val database = MediaDatabase.getDatabase(this)
    val repository = MediaRepository(database.mediaDao())
    
    // Inject repository using Factory
    val viewModel = ViewModelProvider(
        this, 
        EditViewModelFactory(repository)
    )[EditViewModel::class.java]

    enableEdgeToEdge()
    setContent {
      MyApplicationTheme {
        val selectedAsset by viewModel.selectedAsset.collectAsState()

        Scaffold(
          modifier = Modifier.fillMaxSize()
        ) { innerPadding ->
          AnimatedContent(
            targetState = selectedAsset,
            transitionSpec = {
              fadeIn() togetherWith fadeOut()
            },
            label = "screen_navigation",
            modifier = Modifier.padding(innerPadding)
          ) { asset ->
            if (asset == null) {
              LibraryScreen(
                viewModel = viewModel,
                modifier = Modifier.fillMaxSize()
              )
            } else {
              EditorScreen(
                viewModel = viewModel,
                onBackToLibrary = { viewModel.clearSelection() },
                modifier = Modifier.fillMaxSize()
              )
            }
          }
        }
      }
    }
  }
}

