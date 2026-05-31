@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class, androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.example.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.JsonUtils
import com.example.data.model.EditParams
import com.example.data.model.MediaAsset
import com.example.data.model.MediaType
import com.example.data.model.CustomPreset
import com.example.ui.components.ColorGradingWheels
import com.example.ui.components.LiveHistogram
import com.example.ui.components.MediaRenderer
import com.example.ui.components.ToneCurveEditor
import com.example.ui.viewmodel.EditViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EditorScreen(
    viewModel: EditViewModel,
    onBackToLibrary: () -> Unit,
    modifier: Modifier = Modifier
) {
    val asset by viewModel.selectedAsset.collectAsState()
    val params by viewModel.activeParams.collectAsState()
    val activePanel by viewModel.activePanel.collectAsState()
    val undoStack by viewModel.undoStack.collectAsState()
    val historyIndex by viewModel.historyIndex.collectAsState()
    val videoTime by viewModel.videoTimeSeconds.collectAsState()
    val isPlaying by viewModel.isVideoPlaying.collectAsState()
    val customPresets by viewModel.customPresets.collectAsState()

    val scope = rememberCoroutineScope()
    var isComparingOriginal by remember { mutableStateOf(false) }
    var showPresetSaveDialog by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }

    // Automatic ticking of loop for video types when playing
    LaunchedEffect(isPlaying, asset) {
        if (isPlaying && asset?.mediaType == MediaType.VIDEO) {
            val totalSec = JsonUtils.jsonToVideoMetadata(asset?.videoMetadataJson).durationSeconds
            while (isPlaying) {
                delay(30)
                val nextTime = videoTime + 0.03f
                viewModel.updateVideoProgress(if (nextTime > totalSec) 0.0f else nextTime)
            }
        }
    }

    if (asset == null) return

    val currentAsset = asset!!
    val isRaw = currentAsset.mediaType == MediaType.RAW
    val isVideo = currentAsset.mediaType == MediaType.VIDEO

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Color(0xFF121212),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            currentAsset.name,
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (isRaw) "RAW DNG Workspace" else if (isVideo) "Pro Video Editor" else "Original JPEG Image",
                            color = Color.Gray,
                            fontSize = 10.sp
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackToLibrary) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back to Library", tint = Color.White)
                    }
                },
                actions = {
                    // Undo & Redo Controls
                    IconButton(
                        onClick = { viewModel.undo() },
                        enabled = historyIndex > 0
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Undo,
                            contentDescription = "Undo",
                            tint = if (historyIndex > 0) Color.White else Color.DarkGray
                        )
                    }

                    IconButton(
                        onClick = { viewModel.redo() },
                        enabled = historyIndex < undoStack.size - 1
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Redo,
                            contentDescription = "Redo",
                            tint = if (historyIndex < undoStack.size - 1) Color.White else Color.DarkGray
                        )
                    }

                    // Reset Edits Button
                    IconButton(onClick = { viewModel.resetEdits() }) {
                        Icon(Icons.Default.SettingsBackupRestore, "Reset Edits", tint = Color.White)
                    }

                    // Save custom Preset Button
                    IconButton(onClick = { showPresetSaveDialog = true }) {
                        Icon(Icons.Default.Save, "Save Preset", tint = Color.White)
                    }

                    // Export Button
                    IconButton(onClick = { showExportDialog = true }) {
                        Icon(Icons.Default.IosShare, "Export", tint = Color(0xFF69F0AE))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF1A1A1A),
                    titleContentColor = Color.White
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Adaptive View: Splitting Photo Preview (Top) and Tool Workspace (Bottom)
            Box(
                modifier = Modifier
                    .weight(1.1f)
                    .fillMaxWidth()
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                // Rendered Canvas with interactive adjustments
                MediaRenderer(
                    asset = currentAsset,
                    editParams = params,
                    currentTimeSeconds = videoTime,
                    compareOriginal = isComparingOriginal,
                    modifier = Modifier.fillMaxSize()
                )

                // Live Overlapping HUDs
                // 1. Live Histogram (Top Right)
                LiveHistogram(
                    params = params,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp)
                        .width(130.dp)
                        .clip(RoundedCornerShape(6.dp))
                )

                // 2. RAW EXIF Metadata Floating Tag
                if (isRaw) {
                    val rawMeta = remember(currentAsset) {
                        JsonUtils.jsonToRawMetadata(currentAsset.rawMetadataJson)
                    }
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(12.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xE01A1A1A))
                            .padding(8.dp)
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Camera, "Camera", tint = Color(0xFFFFB300), modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(rawMeta.cameraModel, color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            }
                            Text(
                                "ISO ${rawMeta.iso}  •  ${rawMeta.aperture}  •  ${rawMeta.shutterSpeed}  •  ${rawMeta.focalLength}",
                                color = Color.LightGray,
                                fontSize = 8.sp,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                            Text(
                                "${rawMeta.bitDepth}-bit DNG  •  Sensor Pattern: ${rawMeta.bayerPattern}",
                                color = Color.Gray,
                                fontSize = 7.sp,
                                modifier = Modifier.padding(top = 1.dp)
                            )
                        }
                    }
                }

                // 3. Before/After Hold Button
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(12.dp)
                ) {
                    Button(
                        onClick = {},
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isComparingOriginal) Color(0xFFFFB300) else Color(0x99222222),
                            contentColor = Color.White
                        ),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        modifier = Modifier
                            .height(32.dp)
                            .pointerInput(Unit) {
                                detectTapGestures(
                                    onPress = {
                                        isComparingOriginal = true
                                        tryAwaitRelease()
                                        isComparingOriginal = false
                                    }
                                )
                            }
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Compare, "Compare", modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("HOLD FOR ORIGINAL", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // 4. Video scrubbing and playback timeline (Only for VIDEOS)
                if (isVideo) {
                    val meta = remember(currentAsset) {
                        JsonUtils.jsonToVideoMetadata(currentAsset.videoMetadataJson)
                    }
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .background(Color(0xD01A1A1A))
                            .padding(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            IconButton(
                                onClick = { viewModel.toggleVideoPlay() },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = "Play/Pause",
                                    tint = Color.White
                                )
                            }

                            Text(
                                text = String.format("%02.1fs", videoTime),
                                color = Color.White,
                                fontSize = 10.sp,
                                modifier = Modifier.padding(horizontal = 6.dp)
                            )

                            Slider(
                                value = videoTime,
                                onValueChange = { viewModel.updateVideoProgress(it) },
                                valueRange = 0f..meta.durationSeconds,
                                colors = SliderDefaults.colors(
                                    thumbColor = Color(0xFF448AFF),
                                    activeTrackColor = Color(0xFF1976D2)
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(24.dp)
                            )

                            Text(
                                text = String.format("%02.1fs", meta.durationSeconds),
                                color = Color.Gray,
                                fontSize = 10.sp,
                                modifier = Modifier.padding(horizontal = 6.dp)
                            )
                        }
                    }
                }
            }

            // Slider of develop category rows
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF1A1A1A))
                    .horizontalScroll(rememberScrollState())
                    .padding(vertical = 8.dp, horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                val panels = listOf("Presets", "Light", "Color", "Effects", "Detail", "Grading", "Curve", "RAW", "Crop", "History")
                panels.forEachIndexed { idx, name ->
                    if (name == "RAW" && !isRaw) return@forEachIndexed
                    val isSelected = activePanel == idx
                    Button(
                        onClick = { viewModel.setPanel(idx) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isSelected) Color(0xFF333333) else Color.Transparent,
                            contentColor = if (isSelected) Color.White else Color.Gray
                        ),
                        modifier = Modifier
                            .height(32.dp)
                            .border(
                                1.dp,
                                if (isSelected) Color.White else Color.Transparent,
                                RoundedCornerShape(16.dp)
                            ),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text(name, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Controls/Develop Panel workspace (Bottom half)
            Box(
                modifier = Modifier
                    .weight(0.9f)
                    .fillMaxWidth()
                    .background(Color(0xFF1E1E1E))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp)
                ) {
                    when (activePanel) {
                        0 -> PresetSelectionPanel(
                            customPresets = customPresets,
                            onApply = { name, p -> viewModel.applyPreset(name, p) },
                            onDeleteCustom = { viewModel.deletePreset(it) }
                        )
                        1 -> LightPanel(params = params, onParamsChange = { viewModel.updateParams(it) }, onCommit = { viewModel.commitState("Light Adjusted") })
                        2 -> ColorPanel(params = params, onParamsChange = { viewModel.updateParams(it) }, onCommit = { viewModel.commitState("Color Adjusted") })
                        3 -> EffectsPanel(params = params, onParamsChange = { viewModel.updateParams(it) }, onCommit = { viewModel.commitState("Effects Adjusted") })
                        4 -> DetailPanel(params = params, onParamsChange = { viewModel.updateParams(it) }, onCommit = { viewModel.commitState("Detail Adjusted") })
                        5 -> ColorGradingWheels(params = params, onParamsChange = { viewModel.updateParams(it); viewModel.commitState("Color Grading Adjusted") })
                        6 -> ToneCurveEditor(params = params, onParamsChange = { viewModel.updateParams(it); viewModel.commitState("Tone Curve Adjusted") })
                        7 -> RawSettingsPanel(params = params, onParamsChange = { viewModel.updateParams(it); viewModel.commitState("RAW Settings Changed") })
                        8 -> CropPanel(params = params, onParamsChange = { viewModel.updateParams(it); viewModel.commitState("Crop / Geometry Adjusted") })
                        9 -> HistoryStepPanel(undoStack = undoStack, activeIndex = historyIndex, onJumpToIndex = { viewModel.rollBackToHistoryIndex(it) })
                    }
                }
            }
        }
    }

    // Save Preset custom Dialog
    if (showPresetSaveDialog) {
        var presetName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showPresetSaveDialog = false },
            containerColor = Color(0xFF1E1E1E),
            title = { Text("Save Active Edits as Preset", color = Color.White) },
            text = {
                OutlinedTextField(
                    value = presetName,
                    onValueChange = { presetName = it },
                    label = { Text("Preset Custom Name") },
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
            },
            confirmButton = {
                Button(
                    onClick = {
                        val name = if (presetName.isBlank()) "Preset_Edits" else presetName
                        viewModel.saveCustomPreset(name)
                        showPresetSaveDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF007AFF))
                ) {
                    Text("Save", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showPresetSaveDialog = false }) {
                    Text("Cancel", color = Color.Gray)
                }
            }
        )
    }

    // High Quality Export simulator dialog
    if (showExportDialog) {
        var isProcessingExport by remember { mutableStateOf(false) }
        var exportCompleted by remember { mutableStateOf(false) }
        var exportMultiplier by remember { mutableStateOf(100f) }
        var includeWatermark by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { if (!isProcessingExport) showExportDialog = false },
            containerColor = Color(0xFF1E1E1E),
            title = { Text(if (exportCompleted) "Export Successful" else "Export Develop Options", color = Color.White) },
            text = {
                if (isProcessingExport) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(color = Color(0xFF007AFF))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Rendering pixel curves, exporting details...", color = Color.LightGray, fontSize = 12.sp)
                    }
                } else if (exportCompleted) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            "The file has been successfully written to local cache container.",
                            color = Color.LightGray,
                            fontSize = 12.sp
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF222222))
                                .padding(12.dp)
                        ) {
                            Text(
                                "Output Path: /storage/emulated/0/Pictures/LrOutputs/${currentAsset.name}_edited.jpg",
                                color = Color.Gray,
                                fontSize = 10.sp
                            )
                        }
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Format: High Quality JPEG (100% Quality)", color = Color.LightGray, fontSize = 12.sp)
                        Text("Dimensions scale: 100% (Full Quality)", color = Color.LightGray, fontSize = 12.sp)

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Checkbox(
                                checked = includeWatermark,
                                onCheckedChange = { includeWatermark = it },
                                colors = CheckboxDefaults.colors(checkedColor = Color(0xFF007AFF))
                            )
                            Text("Include 'Shot on Lensora Studio' Watermark", color = Color.LightGray, fontSize = 11.sp)
                        }
                    }
                }
            },
            confirmButton = {
                if (!isProcessingExport && !exportCompleted) {
                    Button(
                        onClick = {
                            isProcessingExport = true
                            // Simulate fast processing
                            scope.launch {
                                delay(1800)
                                isProcessingExport = false
                                exportCompleted = true
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF007AFF))
                    ) {
                        Text("Export Now", color = Color.White)
                    }
                } else if (exportCompleted) {
                    Button(
                        onClick = { showExportDialog = false },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF333333))
                    ) {
                        Text("Done", color = Color.White)
                    }
                }
            },
            dismissButton = {
                if (!isProcessingExport) {
                    TextButton(onClick = { showExportDialog = false }) {
                        Text("Cancel", color = Color.Gray)
                    }
                }
            }
        )
    }
}

@Composable
private fun PresetSelectionPanel(
    customPresets: List<CustomPreset>,
    onApply: (String, EditParams) -> Unit,
    onDeleteCustom: (Long) -> Unit
) {
    val builtInPresets = mapOf(
        "Moody Matte" to EditParams(exposure = -0.1f, contrast = 0.12f, shadows = 0.25f, whites = -0.15f, blacks = 0.1f, temperature = 0.05f),
        "Cinematic Amber" to EditParams(temperature = 0.45f, tint = 0.1f, exposure = 0.1f, highlightsHue = 35f, highlightsSat = 0.35f, shadowsHue = 210f, shadowsSat = 0.2f),
        "Vignette Dream" to EditParams(vignette = -0.45f, texture = -0.2f, contrast = -0.1f, highlights = 0.15f),
        "B&W Dramatic" to EditParams(saturation = -1.0f, contrast = 0.45f, blacks = -0.25f, highlights = 0.2f, sharpening = 0.5f),
        "Winter Frost" to EditParams(temperature = -0.45f, tint = -0.05f, exposure = 0.15f, highlightsHue = 220f, highlightsSat = 0.25f),
        "Teal & Orange" to EditParams(vibrance = 0.35f, highlightsHue = 35f, highlightsSat = 0.35f, shadowsHue = 195f, shadowsSat = 0.4f, temperature = 0.1f, contrast = 0.15f)
    )

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Built-in Artistic Presets", color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            builtInPresets.forEach { (name, params) ->
                Button(
                    onClick = { onApply(name, params) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF333333),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.height(34.dp)
                ) {
                    Text(name, fontSize = 10.sp)
                }
            }
        }

        if (customPresets.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text("User Saved Presets", color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                customPresets.forEach { preset ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF222222), RoundedCornerShape(8.dp))
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(preset.name, color = Color.White, fontSize = 11.sp)
                        Row {
                            IconButton(
                                onClick = {
                                    val params = JsonUtils.jsonToEditParams(preset.editParamsJson)
                                    onApply(preset.name, params)
                                },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(Icons.Default.Check, "Apply", tint = Color(0xFF69F0AE), modifier = Modifier.size(16.dp))
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            IconButton(onClick = { onDeleteCustom(preset.id) }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.Delete, "Delete", tint = Color.Red, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LightPanel(params: EditParams, onParamsChange: (EditParams) -> Unit, onCommit: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        EditSlider(
            label = "Exposure",
            value = params.exposure,
            onValueChange = { onParamsChange(params.copy(exposure = it)) },
            onCommit = onCommit,
            valueRange = -4.0f..4.0f,
            valueString = "%.2f EV".format(params.exposure)
        )
        EditSlider(
            label = "Contrast",
            value = params.contrast,
            onValueChange = { onParamsChange(params.copy(contrast = it)) },
            onCommit = onCommit,
            valueRange = -1.0f..1.0f,
            valueString = "%+.0f%%".format(params.contrast * 100f)
        )
        EditSlider(
            label = "Highlights",
            value = params.highlights,
            onValueChange = { onParamsChange(params.copy(highlights = it)) },
            onCommit = onCommit,
            valueRange = -1.0f..1.0f,
            valueString = "%+.0f%%".format(params.highlights * 100f)
        )
        EditSlider(
            label = "Shadows",
            value = params.shadows,
            onValueChange = { onParamsChange(params.copy(shadows = it)) },
            onCommit = onCommit,
            valueRange = -1.0f..1.0f,
            valueString = "%+.0f%%".format(params.shadows * 100f)
        )
        EditSlider(
            label = "Whites",
            value = params.whites,
            onValueChange = { onParamsChange(params.copy(whites = it)) },
            onCommit = onCommit,
            valueRange = -1.0f..1.0f,
            valueString = "%+.0f%%".format(params.whites * 100f)
        )
        EditSlider(
            label = "Blacks",
            value = params.blacks,
            onValueChange = { onParamsChange(params.copy(blacks = it)) },
            onCommit = onCommit,
            valueRange = -1.0f..1.0f,
            valueString = "%+.0f%%".format(params.blacks * 100f)
        )
    }
}

@Composable
private fun ColorPanel(params: EditParams, onParamsChange: (EditParams) -> Unit, onCommit: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        EditSlider(
            label = "Temp (Kelvin Shift)",
            value = params.temperature,
            onValueChange = { onParamsChange(params.copy(temperature = it)) },
            onCommit = onCommit,
            valueRange = -1.0f..1.0f,
            valueString = if (params.temperature > 0) "+%dK".format((params.temperature * 15000f).toInt()) else "%dK".format(5500f + (params.temperature * 3500f).toInt()),
            color = Color(0xFFFFB300)
        )
        EditSlider(
            label = "Tint",
            value = params.tint,
            onValueChange = { onParamsChange(params.copy(tint = it)) },
            onCommit = onCommit,
            valueRange = -1.0f..1.0f,
            valueString = "%+.0f%%".format(params.tint * 100f),
            color = Color(0xFFE91E63)
        )
        EditSlider(
            label = "Vibrance",
            value = params.vibrance,
            onValueChange = { onParamsChange(params.copy(vibrance = it)) },
            onCommit = onCommit,
            valueRange = -1.0f..1.0f,
            valueString = "%+.0f%%".format(params.vibrance * 100f)
        )
        EditSlider(
            label = "Saturation",
            value = params.saturation,
            onValueChange = { onParamsChange(params.copy(saturation = it)) },
            onCommit = onCommit,
            valueRange = -1.0f..1.0f,
            valueString = "%+.0f%%".format(params.saturation * 100f)
        )
    }
}

@Composable
private fun EffectsPanel(params: EditParams, onParamsChange: (EditParams) -> Unit, onCommit: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        EditSlider(
            label = "Texture (Bayer Clarity)",
            value = params.texture,
            onValueChange = { onParamsChange(params.copy(texture = it)) },
            onCommit = onCommit,
            valueRange = -1.0f..1.0f,
            valueString = "%+.0f%%".format(params.texture * 100f)
        )
        EditSlider(
            label = "Clarity (Midtone Contrast)",
            value = params.clarity,
            onValueChange = { onParamsChange(params.copy(clarity = it)) },
            onCommit = onCommit,
            valueRange = -1.0f..1.0f,
            valueString = "%+.0f%%".format(params.clarity * 100f)
        )
        EditSlider(
            label = "Dehaze",
            value = params.dehaze,
            onValueChange = { onParamsChange(params.copy(dehaze = it)) },
            onCommit = onCommit,
            valueRange = -1.0f..1.0f,
            valueString = "%+.0f%%".format(params.dehaze * 100f)
        )
        EditSlider(
            label = "Vignette Amount",
            value = params.vignette,
            onValueChange = { onParamsChange(params.copy(vignette = it)) },
            onCommit = onCommit,
            valueRange = -1.0f..1.0f,
            valueString = "%+.0f%%".format(params.vignette * 100f),
            color = Color.LightGray
        )
    }
}

@Composable
private fun DetailPanel(params: EditParams, onParamsChange: (EditParams) -> Unit, onCommit: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        EditSlider(
            label = "Sharpening",
            value = params.sharpening,
            onValueChange = { onParamsChange(params.copy(sharpening = it)) },
            onCommit = onCommit,
            valueRange = 0.0f..1.0f,
            valueString = "%.0f%%".format(params.sharpening * 100f)
        )
        EditSlider(
            label = "Luminance Noise Reduction",
            value = params.noiseReduction,
            onValueChange = { onParamsChange(params.copy(noiseReduction = it)) },
            onCommit = onCommit,
            valueRange = 0.0f..1.0f,
            valueString = "%.0f%%".format(params.noiseReduction * 100f)
        )
        EditSlider(
            label = "Color Noise Reduction",
            value = params.colorNoiseReduction,
            onValueChange = { onParamsChange(params.copy(colorNoiseReduction = it)) },
            onCommit = onCommit,
            valueRange = 0.0f..1.0f,
            valueString = "%.0f%%".format(params.colorNoiseReduction * 100f)
        )
    }
}

@Composable
private fun RawSettingsPanel(params: EditParams, onParamsChange: (EditParams) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("DNG RAW Demosaicing Profiles", color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold)

        // Profiles Selectors
        val profiles = listOf("Adobe Color", "Adobe Landscape", "Adobe Portrait", "Camera Standard", "Adobe Monochrome")
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            profiles.forEach { p ->
                val isSelected = params.rawProfile == p
                Button(
                    onClick = { onParamsChange(params.copy(rawProfile = p)) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isSelected) Color(0xFFFFB300) else Color(0xFF333333),
                        contentColor = if (isSelected) Color.Black else Color.White
                    ),
                    shape = RoundedCornerShape(6.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.height(30.dp)
                ) {
                    Text(p, fontSize = 9.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))
        Text("Optics & Bayer Patterns", color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Enable Profile Lens Correction", color = Color.White, fontSize = 11.sp)
            Switch(
                checked = params.lensCorrection,
                onCheckedChange = { onParamsChange(params.copy(lensCorrection = it)) },
                colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFFFFB300))
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Remove Chromatic Aberration", color = Color.White, fontSize = 11.sp)
            Switch(
                checked = params.chromaticAberration,
                onCheckedChange = { onParamsChange(params.copy(chromaticAberration = it)) },
                colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFFFFB300))
            )
        }

        Spacer(modifier = Modifier.height(4.dp))
        Text("RAW Interpolation Method", color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold)

        // Dropdown selection style inside row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF222222), RoundedCornerShape(8.dp))
                .padding(10.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Method:", color = Color.Gray, fontSize = 11.sp)
            Text(params.demosaicingMethod, color = Color(0xFFFFB300), fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun CropPanel(params: EditParams, onParamsChange: (EditParams) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        EditSlider(
            label = "Rotation Angle",
            value = params.cropAngle,
            onValueChange = { onParamsChange(params.copy(cropAngle = it)) },
            onCommit = {},
            valueRange = -45.0f..45.0f,
            valueString = "%.1f°".format(params.cropAngle),
            color = Color(0xFF448AFF)
        )

        Text("Constraint Aspect Ratio", color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            val ratios = listOf("Original", "Free", "1:1", "4:3", "16:9")
            ratios.forEach { r ->
                val isSelected = params.cropRatio == r
                OutlinedButton(
                    onClick = { onParamsChange(params.copy(cropRatio = r)) },
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = if (isSelected) Color(0xFF333333) else Color.Transparent,
                        contentColor = if (isSelected) Color.White else Color.Gray
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 2.dp)
                        .height(34.dp),
                    contentPadding = PaddingValues(0.dp),
                    border = BorderStroke(1.dp, if (isSelected) Color.White else Color(0xFF444444))
                ) {
                    Text(r, fontSize = 9.sp)
                }
            }
        }
    }
}

@Composable
private fun HistoryStepPanel(
    undoStack: List<Pair<String, EditParams>>,
    activeIndex: Int,
    onJumpToIndex: (Int) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Develop Edit History", color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold)

        if (undoStack.isEmpty()) {
            Text("No adjustments made yet.", color = Color.DarkGray, fontSize = 10.sp)
        } else {
            undoStack.forEachIndexed { idx, (action, _) ->
                val isActiveStep = idx == activeIndex
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            if (isActiveStep) Color(0xFF333333) else Color.Transparent,
                            RoundedCornerShape(6.dp)
                        )
                        .clickable { onJumpToIndex(idx) }
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(if (isActiveStep) Color(0xFF69F0AE) else Color.DarkGray, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = action,
                            color = if (isActiveStep) Color.White else Color.LightGray,
                            fontSize = 11.sp,
                            fontWeight = if (isActiveStep) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                    if (isActiveStep) {
                        Text("Active", color = Color(0xFF69F0AE), fontSize = 8.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun EditSlider(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    onCommit: () -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    valueString: String,
    color: Color = Color(0xFF007AFF)
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, color = Color.Gray, fontSize = 11.sp)
            Text(valueString, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            onValueChangeFinished = onCommit,
            valueRange = valueRange,
            colors = SliderDefaults.colors(
                thumbColor = color,
                activeTrackColor = color.copy(alpha = 0.5f),
                inactiveTrackColor = Color(0xFF333333)
            ),
            modifier = Modifier.height(28.dp)
        )
    }
}
