package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.JsonUtils
import com.example.data.model.CustomPreset
import com.example.data.model.EditParams
import com.example.data.model.MediaAsset
import com.example.data.model.MediaType
import com.example.data.repository.MediaRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class EditViewModel(private val repository: MediaRepository) : ViewModel() {

    // All media files database flow
    val assets: StateFlow<List<MediaAsset>> = repository.allAssets
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // All custom user presets flow
    val customPresets: StateFlow<List<CustomPreset>> = repository.allPresets
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Currently selected active editing file
    private val _selectedAsset = MutableStateFlow<MediaAsset?>(null)
    val selectedAsset: StateFlow<MediaAsset?> = _selectedAsset.asStateFlow()

    // Current active parameters being updated
    private val _activeParams = MutableStateFlow(EditParams())
    val activeParams: StateFlow<EditParams> = _activeParams.asStateFlow()

    // Interactive history list tracking
    private val _undoStack = MutableStateFlow<List<Pair<String, EditParams>>>(emptyList())
    val undoStack: StateFlow<List<Pair<String, EditParams>>> = _undoStack.asStateFlow()

    private val _historyIndex = MutableStateFlow(-1)
    val historyIndex: StateFlow<Int> = _historyIndex.asStateFlow()

    // Video play states
    private val _videoTimeSeconds = MutableStateFlow(0.0f)
    val videoTimeSeconds: StateFlow<Float> = _videoTimeSeconds.asStateFlow()

    private val _isVideoPlaying = MutableStateFlow(false)
    val isVideoPlaying: StateFlow<Boolean> = _isVideoPlaying.asStateFlow()

    // Active Category Panels (0: Light, 1: Color, 2: Effects, 3: Detail, 4: Grading, 5: Curve, 6: RAW, 7: Crop, 8: History)
    private val _activePanel = MutableStateFlow(0)
    val activePanel: StateFlow<Int> = _activePanel.asStateFlow()

    init {
        viewModelScope.launch {
            repository.prepopulateIfEmpty()
        }
    }

    fun setPanel(panelIndex: Int) {
        _activePanel.value = panelIndex
    }

    fun selectAsset(asset: MediaAsset) {
        _selectedAsset.value = asset
        val params = JsonUtils.jsonToEditParams(asset.editParamsJson)
        _activeParams.value = params
        
        // Reset History undo queue for this file
        _undoStack.value = listOf("Original Import" to params)
        _historyIndex.value = 0
        
        _videoTimeSeconds.value = 0.0f
        _isVideoPlaying.value = false
    }

    fun clearSelection() {
        _selectedAsset.value = null
        _isVideoPlaying.value = false
    }

    // Interactive sliders update states in real time
    fun updateParams(newParams: EditParams) {
        _activeParams.value = newParams
    }

    // When the user lifts their finger off a slider, commit the change to the history database
    fun commitState(actionName: String) {
        val currentParams = _activeParams.value
        val stack = _undoStack.value.toMutableList()
        val index = _historyIndex.value

        // If index is in the middle of current stack (due to undos), truncate future before pushing
        val validStack = if (index >= 0 && index < stack.size - 1) {
            stack.subList(0, index + 1)
        } else {
            stack
        }

        validStack.add(actionName to currentParams)
        _undoStack.value = validStack
        _historyIndex.value = validStack.size - 1

        // Commit change asynchronously to local Room Database
        val asset = selectedAsset.value
        if (asset != null) {
            viewModelScope.launch {
                repository.updateAssetEditParams(asset.id, currentParams)
            }
        }
    }

    fun undo() {
        val index = _historyIndex.value
        val stack = _undoStack.value
        if (index > 0) {
            val prevIndex = index - 1
            _historyIndex.value = prevIndex
            val params = stack[prevIndex].second
            _activeParams.value = params
            
            // Sync to room
            selectedAsset.value?.let { asset ->
                viewModelScope.launch { repository.updateAssetEditParams(asset.id, params) }
            }
        }
    }

    fun redo() {
        val index = _historyIndex.value
        val stack = _undoStack.value
        if (index < stack.size - 1) {
            val nextIndex = index + 1
            _historyIndex.value = nextIndex
            val params = stack[nextIndex].second
            _activeParams.value = params

            // Sync to room
            selectedAsset.value?.let { asset ->
                viewModelScope.launch { repository.updateAssetEditParams(asset.id, params) }
            }
        }
    }

    fun rollBackToHistoryIndex(targetIndex: Int) {
        val stack = _undoStack.value
        if (targetIndex in stack.indices) {
            _historyIndex.value = targetIndex
            val params = stack[targetIndex].second
            _activeParams.value = params

            // Sync to room
            selectedAsset.value?.let { asset ->
                viewModelScope.launch { repository.updateAssetEditParams(asset.id, params) }
            }
        }
    }

    fun resetEdits() {
        val defaults = EditParams()
        _activeParams.value = defaults
        commitState("Reset Parameters")
    }

    // Fast Preset execution
    fun applyPreset(name: String, presetParams: EditParams) {
        _activeParams.value = presetParams
        commitState("Apply Preset: $name")
    }

    // Save Custom Preset
    fun saveCustomPreset(name: String) {
        val currentParams = _activeParams.value
        viewModelScope.launch {
            repository.insertPreset(name, currentParams)
        }
    }

    fun deletePreset(id: Long) {
        viewModelScope.launch {
            repository.deletePresetById(id)
        }
    }

    // Media Asset creation / deletes
    fun importAsset(name: String, path: String, type: MediaType, durationS: Float = 0f) {
        viewModelScope.launch {
            val item = when (type) {
                MediaType.RAW -> {
                    MediaAsset(
                        uri = path,
                        name = name,
                        mediaType = MediaType.RAW,
                        rawMetadataJson = JsonUtils.rawMetadataToJson(
                            com.example.data.model.RawMetadata(
                                cameraModel = "Imported RAW",
                                iso = 100,
                                aperture = "f/2.0",
                                shutterSpeed = "1/250s",
                                focalLength = "35mm"
                            )
                        ),
                        editParamsJson = JsonUtils.editParamsToJson(EditParams())
                    )
                }
                MediaType.VIDEO -> {
                    MediaAsset(
                        uri = path,
                        name = name,
                        mediaType = MediaType.VIDEO,
                        videoMetadataJson = JsonUtils.videoMetadataToJson(
                            com.example.data.model.VideoMetadata(
                                fps = 30,
                                resolution = "1080p Full HD",
                                durationSeconds = durationS
                            )
                        ),
                        editParamsJson = JsonUtils.editParamsToJson(EditParams())
                    )
                }
                MediaType.IMAGE -> {
                    MediaAsset(
                        uri = path,
                        name = name,
                        mediaType = MediaType.IMAGE,
                        editParamsJson = JsonUtils.editParamsToJson(EditParams())
                    )
                }
            }
            val id = repository.insertAsset(item)
            // Automatically select
            val insertedItem = repository.getAssetById(id)
            if (insertedItem != null) {
                selectAsset(insertedItem)
            }
        }
    }

    fun deleteAsset(id: Long) {
        viewModelScope.launch {
            repository.deleteAssetById(id)
            if (_selectedAsset.value?.id == id) {
                _selectedAsset.value = null
            }
        }
    }

    // Video player ticks
    fun updateVideoProgress(seconds: Float) {
        _videoTimeSeconds.value = seconds
    }

    fun toggleVideoPlay() {
        _isVideoPlaying.value = !_isVideoPlaying.value
    }
}

class EditViewModelFactory(private val repository: MediaRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(EditViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return EditViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
