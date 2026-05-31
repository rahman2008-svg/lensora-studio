package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

enum class MediaType {
    IMAGE, VIDEO, RAW
}

data class RawMetadata(
    val cameraModel: String = "Unknown Camera",
    val iso: Int = 100,
    val aperture: String = "f/2.8",
    val shutterSpeed: String = "1/125s",
    val focalLength: String = "50mm",
    val bitDepth: Int = 14,
    val bayerPattern: String = "RGGB",
    val dngVersion: String = "1.6.0.0"
) : Serializable

data class VideoMetadata(
    val fps: Int = 24,
    val resolution: String = "4K UHD",
    val codec: String = "HEVC 10-bit",
    val audioChannels: String = "Stereo",
    val durationSeconds: Float = 10.0f
) : Serializable

@Entity(tableName = "media_assets")
data class MediaAsset(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val uri: String,
    val name: String,
    val mediaType: MediaType,
    val dateImported: Long = System.currentTimeMillis(),
    
    // JSON Strings for fast nested parsing
    val rawMetadataJson: String? = null,
    val videoMetadataJson: String? = null,
    val editParamsJson: String? = null,
    val isDemoSample: Boolean = false
)

data class EditParams(
    // Light
    val exposure: Float = 0.0f,     // -4.0..4.0
    val contrast: Float = 0.0f,     // -1.0..1.0
    val highlights: Float = 0.0f,   // -1.0..1.0
    val shadows: Float = 0.0f,      // -1.0..1.0
    val whites: Float = 0.0f,       // -1.0..1.0
    val blacks: Float = 0.0f,       // -1.0..1.0
    
    // Color
    val temperature: Float = 0.0f,  // -1.0..1.0
    val tint: Float = 0.0f,         // -1.0..1.0
    val vibrance: Float = 0.0f,     // -1.0..1.0
    val saturation: Float = 0.0f,   // -1.0..1.0
    
    // Effects
    val texture: Float = 0.0f,      // -1.0..1.0
    val clarity: Float = 0.0f,      // -1.0..1.0
    val dehaze: Float = 0.0f,       // -1.0..1.0
    val vignette: Float = 0.0f,     // -1.0..1.0
    
    // Detail
    val sharpening: Float = 0.0f,   // 0.0..1.0
    val noiseReduction: Float = 0.0f, // 0.0..1.0
    val colorNoiseReduction: Float = 0.0f, // 0.0..1.0
    
    // Color Grading (Shadows, Midtones, Highlights)
    // Hue: 0..360, Sat: 0..1, Lum: -1..1
    val shadowsHue: Float = 0f,
    val shadowsSat: Float = 0f,
    val shadowsLum: Float = 0f,
    
    val midtonesHue: Float = 0f,
    val midtonesSat: Float = 0f,
    val midtonesLum: Float = 0f,
    
    val highlightsHue: Float = 0f,
    val highlightsSat: Float = 0f,
    val highlightsLum: Float = 0f,
    
    val gradingBalance: Float = 0f,  // -1..1
    
    // RGB Tone Curve Points (Control mid-points, base 0..1 range)
    val curveShadowPoint: Float = 0.25f,
    val curveMidPoint: Float = 0.50f,
    val curveHighlightPoint: Float = 0.75f,
    
    // RAW specific options
    val rawProfile: String = "Adobe Color",
    val lensCorrection: Boolean = true,
    val chromaticAberration: Boolean = true,
    val demosaicingMethod: String = "AHD (High Quality)",
    
    // Crop & Rotation
    val cropAngle: Float = 0.0f,     // -45..45
    val cropRatio: String = "Original" // "Original", "Free", "1:1", "4:3", "16:9"
) : Serializable

@Entity(tableName = "custom_presets")
data class CustomPreset(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val editParamsJson: String,
    val dateCreated: Long = System.currentTimeMillis()
)
