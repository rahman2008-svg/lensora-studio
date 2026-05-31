package com.example.data.local

import com.example.data.model.EditParams
import com.example.data.model.RawMetadata
import com.example.data.model.VideoMetadata
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

object JsonUtils {
    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()
        
    private val editParamsAdapter = moshi.adapter(EditParams::class.java)
    private val rawMetadataAdapter = moshi.adapter(RawMetadata::class.java)
    private val videoMetadataAdapter = moshi.adapter(VideoMetadata::class.java)
    
    fun editParamsToJson(params: EditParams): String = editParamsAdapter.toJson(params)
    fun jsonToEditParams(json: String?): EditParams = try {
        json?.let { editParamsAdapter.fromJson(it) } ?: EditParams()
    } catch (e: Exception) {
        EditParams()
    }
    
    fun rawMetadataToJson(meta: RawMetadata): String = rawMetadataAdapter.toJson(meta)
    fun jsonToRawMetadata(json: String?): RawMetadata = try {
        json?.let { rawMetadataAdapter.fromJson(it) } ?: RawMetadata()
    } catch (e: Exception) {
        RawMetadata()
    }
    
    fun videoMetadataToJson(meta: VideoMetadata): String = videoMetadataAdapter.toJson(meta)
    fun jsonToVideoMetadata(json: String?): VideoMetadata = try {
        json?.let { videoMetadataAdapter.fromJson(it) } ?: VideoMetadata()
    } catch (e: Exception) {
        VideoMetadata()
    }
}
