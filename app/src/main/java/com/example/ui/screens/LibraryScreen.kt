@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.example.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.MediaAsset
import com.example.data.model.MediaType
import com.example.ui.viewmodel.EditViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LibraryScreen(
    viewModel: EditViewModel,
    modifier: Modifier = Modifier
) {
    val assets by viewModel.assets.collectAsState()
    var selectedFilter by remember { mutableStateOf("All") } // "All", "RAW", "Photos", "Videos"
    var showImportDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }

    // Filter local lists
    val filteredAssets = remember(assets, selectedFilter) {
        when (selectedFilter) {
            "RAW" -> assets.filter { it.mediaType == MediaType.RAW }
            "Photos" -> assets.filter { it.mediaType == MediaType.RAW || it.mediaType == MediaType.IMAGE }
            "Videos" -> assets.filter { it.mediaType == MediaType.VIDEO }
            else -> assets
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Color(0xFF121212),
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Brush.horizontalGradient(listOf(Color(0xFF007AFF), Color(0xFF00C8FF))))
                                .border(1.dp, Color(0xFF80D3FF), RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Ls", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Black)
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            "Lensora Studio",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showAboutDialog = true }) {
                        Icon(Icons.Default.Info, "About Lensora Studio", tint = Color.White)
                    }
                    IconButton(onClick = { showImportDialog = true }) {
                        Icon(Icons.Default.AddPhotoAlternate, "Import Media", tint = Color(0xFF007AFF))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF1A1A1A),
                    titleContentColor = Color.White
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showImportDialog = true },
                containerColor = Color(0xFF007AFF),
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, "Import File")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Category Filter Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF1A1A1A))
                    .padding(vertical = 10.dp, horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("All", "Photos", "Videos", "RAW").forEach { filter ->
                    val isSelected = selectedFilter == filter
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedFilter = filter },
                        label = { Text(filter, fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF333333),
                            selectedLabelColor = Color.White,
                            containerColor = Color.Transparent,
                            labelColor = Color.Gray
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = isSelected,
                            borderColor = Color(0xFF333333),
                            selectedBorderColor = Color.White
                        )
                    )
                }
            }

            // Grid content
            if (filteredAssets.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Filter,
                            contentDescription = "Empty",
                            tint = Color.Gray,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "No matching media files",
                            color = Color.LightGray,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            "Import simulated RAW, Image or Video to style professionally.",
                            color = Color.Gray,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(110.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp),
                    contentPadding = PaddingValues(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredAssets, key = { it.id }) { asset ->
                        Box(
                            modifier = Modifier
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .border(1.dp, Color(0xFF333333), RoundedCornerShape(6.dp))
                                .background(
                                    Brush.linearGradient(
                                        colors = when (asset.mediaType) {
                                            MediaType.RAW -> listOf(Color(0xFF2C251C), Color(0xFF1E1C1A))
                                            MediaType.VIDEO -> listOf(Color(0xFF1B252E), Color(0xFF12181F))
                                            MediaType.IMAGE -> listOf(Color(0xFF201D21), Color(0xFF141215))
                                        }
                                    )
                                )
                                .combinedClickable(
                                    onClick = { viewModel.selectAsset(asset) },
                                    onLongClick = { viewModel.deleteAsset(asset.id) }
                                )
                        ) {
                            // Centered visual design reflecting matching tags
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                val typeIcon = when (asset.mediaType) {
                                    MediaType.RAW -> Icons.Default.RawOn
                                    MediaType.VIDEO -> Icons.Default.VideoCameraBack
                                    MediaType.IMAGE -> Icons.Default.Photo
                                }

                                Icon(
                                    imageVector = typeIcon,
                                    contentDescription = asset.mediaType.name,
                                    tint = when (asset.mediaType) {
                                        MediaType.RAW -> Color(0xFFFFB300)
                                        MediaType.VIDEO -> Color(0xFF448AFF)
                                        MediaType.IMAGE -> Color(0xFF69F0AE)
                                    },
                                    modifier = Modifier.size(32.dp)
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                Text(
                                    text = asset.name,
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1
                                )
                            }

                            // Mediatype Label
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(4.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(
                                        when (asset.mediaType) {
                                            MediaType.RAW -> Color(0xB8FFA000)
                                            MediaType.VIDEO -> Color(0xB81976D2)
                                            MediaType.IMAGE -> Color(0xB8388E3C)
                                        }
                                    )
                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = asset.mediaType.name,
                                    color = Color.White,
                                    fontSize = 7.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            // If demo indicator
                            if (asset.isDemoSample) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomStart)
                                        .padding(4.dp)
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(Color(0xFF333333))
                                        .padding(horizontal = 3.dp, vertical = 1.dp)
                                ) {
                                    Text("DEMO", color = Color.LightGray, fontSize = 6.sp)
                                }
                                if (asset.mediaType == MediaType.VIDEO) {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.BottomEnd)
                                            .padding(4.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.PlayArrow, "Video", tint = Color.Gray, modifier = Modifier.size(8.dp))
                                            Text("15s", color = Color.Gray, fontSize = 8.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Beautiful simulated RAW / Photo / Video Import dialog
    if (showImportDialog) {
        var importName by remember { mutableStateOf("") }
        var selectedType by remember { mutableStateOf(MediaType.RAW) }
        var videoDur by remember { mutableStateOf(10f) }

        AlertDialog(
            onDismissRequest = { showImportDialog = false },
            containerColor = Color(0xFF1E1E1E),
            title = { Text("Import Media File", color = Color.White) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = importName,
                        onValueChange = { importName = it },
                        label = { Text("Filename / Shot Title") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF007AFF),
                            unfocusedBorderColor = Color(0xFF444444),
                            focusedLabelColor = Color(0xFF007AFF),
                            unfocusedLabelColor = Color.Gray,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text("File Type", color = Color.Gray, fontSize = 12.sp)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        MediaType.values().forEach { t ->
                            val isTypeSelected = selectedType == t
                            OutlinedButton(
                                onClick = { selectedType = t },
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(0.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = if (isTypeSelected) Color(0xFF333333) else Color.Transparent,
                                    contentColor = if (isTypeSelected) Color.White else Color.Gray
                                ),
                                border = BorderStroke(1.dp, if (isTypeSelected) Color.White else Color(0xFF444444))
                            ) {
                                Text(t.name, fontSize = 11.sp)
                            }
                        }
                    }

                    if (selectedType == MediaType.VIDEO) {
                        Text("Duration (seconds): ${videoDur.toInt()}s", color = Color.Gray, fontSize = 12.sp)
                        Slider(
                            value = videoDur,
                            onValueChange = { videoDur = it },
                            valueRange = 2f..30f,
                            colors = SliderDefaults.colors(
                                thumbColor = Color(0xFF448AFF),
                                activeTrackColor = Color(0xFF1976D2)
                            )
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val finalName = if (importName.isBlank()) "Imported_${System.currentTimeMillis() % 10000}" else importName
                        viewModel.importAsset(
                            name = finalName,
                            path = "user_import_${System.currentTimeMillis()}",
                            type = selectedType,
                            durationS = if (selectedType == MediaType.VIDEO) videoDur else 0f
                        )
                        showImportDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF007AFF))
                ) {
                    Text("Import", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showImportDialog = false }) {
                    Text("Cancel", color = Color.Gray)
                }
            }
        )
    }

    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            containerColor = Color(0xFF1E1E1E),
            icon = {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Brush.radialGradient(listOf(Color(0xFF007AFF), Color(0xFF121212))))
                        .border(1.5.dp, Color(0xFF00C8FF), RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Ls", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Black)
                }
            },
            title = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Lensora Studio",
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "v1.0.0 Pro Suite",
                        color = Color(0xFF00C8FF),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "A next-generation photo & video editing application built by NexVora Labs.",
                        color = Color.LightGray,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        lineHeight = 18.sp
                    )

                    HorizontalDivider(color = Color(0xFF333333))

                    // Developer Info Table/Grid Column
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = "Developer",
                                tint = Color(0xFF007AFF),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text("Developer", color = Color.Gray, fontSize = 10.sp)
                                Text("Abdur Rahman", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Email,
                                contentDescription = "Email",
                                tint = Color(0xFF007AFF),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text("Email", color = Color.Gray, fontSize = 10.sp)
                                Text("prince.ar.abdur.rahman2008@gmail.com", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = "Company",
                                tint = Color(0xFF007AFF),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text("Company", color = Color.Gray, fontSize = 10.sp)
                                Text("NexVora Lab's Ofc", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    HorizontalDivider(color = Color(0xFF333333))

                    // Vision Card
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF262626), RoundedCornerShape(8.dp))
                            .padding(10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = "Vision",
                                tint = Color(0xFFFFB300),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("VISION", color = Color(0xFFFFB300), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "To build powerful creative tools that are fast, offline-first, and accessible to everyone without dependency on external APIs.",
                            color = Color.LightGray,
                            fontSize = 11.sp,
                            lineHeight = 15.sp
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showAboutDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF333333))
                ) {
                    Text("Close", color = Color.White)
                }
            }
        )
    }
}
