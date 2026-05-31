package com.example.data.repository

import com.example.data.local.JsonUtils
import com.example.data.local.MediaDao
import com.example.data.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class MediaRepository(private val mediaDao: MediaDao) {

    val allAssets: Flow<List<MediaAsset>> = mediaDao.getAllAssets()
    val allPresets: Flow<List<CustomPreset>> = mediaDao.getAllPresets()

    suspend fun getAssetById(id: Long): MediaAsset? {
        return mediaDao.getAssetById(id)
    }

    suspend fun insertAsset(asset: MediaAsset): Long {
        return mediaDao.insertAsset(asset)
    }

    suspend fun updateAsset(asset: MediaAsset) {
        mediaDao.updateAsset(asset)
    }

    suspend fun updateAssetEditParams(id: Long, params: EditParams) {
        val json = JsonUtils.editParamsToJson(params)
        mediaDao.updateAssetEditParams(id, json)
    }

    suspend fun deleteAssetById(id: Long) {
        mediaDao.deleteAssetById(id)
    }

    suspend fun insertPreset(name: String, params: EditParams): Long {
        val json = JsonUtils.editParamsToJson(params)
        val preset = CustomPreset(name = name, editParamsJson = json)
        return mediaDao.insertPreset(preset)
    }

    suspend fun deletePresetById(id: Long) {
        mediaDao.deletePresetById(id)
    }

    suspend fun prepopulateIfEmpty() {
        // Run check
        val assets = allAssets.first()
        if (assets.isEmpty()) {
            val demoAssets = createDemoAssets()
            for (asset in demoAssets) {
                mediaDao.insertAsset(asset)
            }
        }
    }

    private fun createDemoAssets(): List<MediaAsset> {
        return listOf(
            MediaAsset(
                uri = "demo_iceland_glacier",
                name = "Icelandic Glacier",
                mediaType = MediaType.RAW,
                rawMetadataJson = JsonUtils.rawMetadataToJson(
                    RawMetadata(
                        cameraModel = "Canon EOS R5",
                        iso = 64,
                        aperture = "f/4.0",
                        shutterSpeed = "1/320s",
                        focalLength = "24mm",
                        bitDepth = 14,
                        bayerPattern = "RGGB"
                    )
                ),
                editParamsJson = JsonUtils.editParamsToJson(EditParams()),
                isDemoSample = true
            ),
            MediaAsset(
                uri = "demo_tokyo_neon",
                name = "Tokyo Neon Streets",
                mediaType = MediaType.IMAGE,
                editParamsJson = JsonUtils.editParamsToJson(
                    EditParams(
                        exposure = 0.1f,
                        vibrance = 0.45f,
                        saturation = 0.15f,
                        shadowsHue = 210f, // blue shadows
                        shadowsSat = 0.35f,
                        highlightsHue = 330f, // magenta highlights
                        highlightsSat = 0.40f
                    )
                ),
                isDemoSample = true
            ),
            MediaAsset(
                uri = "demo_serengeti",
                name = "Serengeti Golden Hour",
                mediaType = MediaType.RAW,
                rawMetadataJson = JsonUtils.rawMetadataToJson(
                    RawMetadata(
                        cameraModel = "Sony A7R V",
                        iso = 100,
                        aperture = "f/2.8",
                        shutterSpeed = "1/1000s",
                        focalLength = "135mm",
                        bitDepth = 16,
                        bayerPattern = "X-Trans"
                    )
                ),
                editParamsJson = JsonUtils.editParamsToJson(
                    EditParams(
                        temperature = 0.5f, // warm
                        tint = 0.1f,
                        contrast = 0.2f,
                        vibrance = 0.3f,
                        highlightsHue = 40f, // amber
                        highlightsSat = 0.3f
                    )
                ),
                isDemoSample = true
            ),
            MediaAsset(
                uri = "demo_highway_cruise",
                name = "Pacific Highway Drive",
                mediaType = MediaType.VIDEO,
                videoMetadataJson = JsonUtils.videoMetadataToJson(
                    VideoMetadata(
                        fps = 60,
                        resolution = "4K UHD (2160p)",
                        codec = "HEVC ProRes 422",
                        audioChannels = "Stereo - 48kHz",
                        durationSeconds = 15.0f
                    )
                ),
                editParamsJson = JsonUtils.editParamsToJson(EditParams()),
                isDemoSample = true
            ),
            MediaAsset(
                uri = "demo_rainy_cafe",
                name = "Mood Coffee Shop",
                mediaType = MediaType.VIDEO,
                videoMetadataJson = JsonUtils.videoMetadataToJson(
                    VideoMetadata(
                        fps = 24,
                        resolution = "Cinematic 1080p",
                        codec = "H.264 Linear PCM",
                        audioChannels = "Monaural",
                        durationSeconds = 8.5f
                    )
                ),
                editParamsJson = JsonUtils.editParamsToJson(
                    EditParams(
                        temperature = -0.3f, // cool moody
                        tint = -0.1f,
                        exposure = -0.4f,
                        contrast = 0.15f,
                        shadows = -0.3f,
                        shadowsHue = 220f, // moody cyber teal/blue
                        shadowsSat = 0.25f
                    )
                ),
                isDemoSample = true
            )
        )
    }
}
