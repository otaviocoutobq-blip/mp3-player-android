package com.example.mp3player.ui

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Recommend
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.mp3player.data.TrackEntity

private enum class TabDestination(val label: String) {
    Library("Biblioteca"),
    Recommendations("Recomendadas"),
    NowPlaying("Tocando agora")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    val playerViewModel: PlayerViewModel = viewModel()
    var currentTab by remember { mutableStateOf(TabDestination.Library) }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text(text = currentTab.label) })
        },
        bottomBar = {
            Column {
                MiniPlayer(playerViewModel = playerViewModel, onOpen = {
                    currentTab = TabDestination.NowPlaying
                })
                NavigationBar {
                    NavigationBarItem(
                        selected = currentTab == TabDestination.Library,
                        onClick = { currentTab = TabDestination.Library },
                        icon = { Icon(Icons.Default.LibraryMusic, contentDescription = null) },
                        label = { Text("Library") }
                    )
                    NavigationBarItem(
                        selected = currentTab == TabDestination.Recommendations,
                        onClick = { currentTab = TabDestination.Recommendations },
                        icon = { Icon(Icons.Default.Recommend, contentDescription = null) },
                        label = { Text("Recomendadas") }
                    )
                    NavigationBarItem(
                        selected = currentTab == TabDestination.NowPlaying,
                        onClick = { currentTab = TabDestination.NowPlaying },
                        icon = { Icon(Icons.Default.MusicNote, contentDescription = null) },
                        label = { Text("Now") }
                    )
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (currentTab) {
                TabDestination.Library -> LibraryScreen(playerViewModel = playerViewModel)
                TabDestination.Recommendations -> RecommendationsScreen(playerViewModel = playerViewModel)
                TabDestination.NowPlaying -> NowPlayingScreen(playerViewModel = playerViewModel)
            }
        }
    }
}

@Composable
private fun LibraryScreen(
    playerViewModel: PlayerViewModel,
    viewModel: LibraryViewModel = viewModel()
) {
    val context = LocalContext.current
    val tracks by viewModel.tracks.collectAsState()
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri ->
            uri?.let {
                context.contentResolver.takePersistableUriPermission(
                    it,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
                viewModel.addTrack(it)
            }
        }
    )

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Button(onClick = { launcher.launch(arrayOf("audio/*")) }) {
            Text("Upload / Adicionar música")
        }
        Spacer(modifier = Modifier.height(12.dp))
        LazyColumn {
            items(tracks) { track ->
                TrackRow(track = track, onClick = {
                    val startIndex = tracks.indexOf(track).coerceAtLeast(0)
                    playerViewModel.playQueue(tracks, startIndex)
                })
            }
        }
    }
}

@Composable
private fun RecommendationsScreen(
    playerViewModel: PlayerViewModel,
    viewModel: RecommendationsViewModel = viewModel()
) {
    val uiState by playerViewModel.uiState.collectAsState()
    val recommendations by viewModel.recommendations.collectAsState()

    LaunchedEffect(uiState.currentMediaId) {
        viewModel.loadRecommendations(uiState.currentMediaId)
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        if (recommendations.isEmpty()) {
            Text("Sem recomendações ainda. Toque músicas para gerar eventos.")
        } else {
            LazyColumn {
                items(recommendations) { track ->
                    TrackRow(track = track, onClick = {
                        playerViewModel.playQueue(recommendations, recommendations.indexOf(track))
                    })
                }
            }
        }
    }
}

@Composable
private fun NowPlayingScreen(playerViewModel: PlayerViewModel) {
    val uiState by playerViewModel.uiState.collectAsState()
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(text = uiState.title, style = MaterialTheme.typography.headlineSmall)
        Text(text = uiState.artist, style = MaterialTheme.typography.bodyMedium)
        Spacer(modifier = Modifier.height(24.dp))
        Slider(
            value = uiState.position.toFloat(),
            onValueChange = { playerViewModel.seekTo(it.toLong()) },
            valueRange = 0f..uiState.duration.coerceAtLeast(1L).toFloat()
        )
        Spacer(modifier = Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = { playerViewModel.skipPrevious() }) {
                Text("Anterior")
            }
            TextButton(onClick = { playerViewModel.togglePlayPause() }) {
                Text(if (uiState.isPlaying) "Pausar" else "Play")
            }
            TextButton(onClick = { playerViewModel.skipNext() }) {
                Text("Próxima")
            }
        }
        TextButton(onClick = { playerViewModel.likeCurrent() }) {
            Text("Curtir")
        }
    }
}

@Composable
private fun MiniPlayer(playerViewModel: PlayerViewModel, onOpen: () -> Unit) {
    val uiState by playerViewModel.uiState.collectAsState()
    if (!uiState.isReady) return
    Card(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(text = uiState.title, style = MaterialTheme.typography.bodyLarge)
            Text(text = uiState.artist, style = MaterialTheme.typography.bodySmall)
            Spacer(modifier = Modifier.height(8.dp))
            Column {
                TextButton(onClick = onOpen) {
                    Text("Abrir Now Playing")
                }
                TextButton(onClick = { playerViewModel.togglePlayPause() }) {
                    Text(if (uiState.isPlaying) "Pausar" else "Play")
                }
                TextButton(onClick = { playerViewModel.skipPrevious() }) {
                    Text("Anterior")
                }
                TextButton(onClick = { playerViewModel.skipNext() }) {
                    Text("Próxima")
                }
            }
        }
    }
}

@Composable
private fun TrackRow(track: TrackEntity, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .padding(vertical = 6.dp)
            .fillMaxWidth(),
        onClick = onClick
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(text = track.title, style = MaterialTheme.typography.bodyLarge)
            Text(text = track.artist, style = MaterialTheme.typography.bodySmall)
            Text(text = track.album, style = MaterialTheme.typography.bodySmall)
        }
    }
}
