package com.example.data.local

import androidx.room.*
import com.example.data.model.CustomPreset
import com.example.data.model.MediaAsset
import kotlinx.coroutines.flow.Flow

@Dao
interface MediaDao {
    @Query("SELECT * FROM media_assets ORDER BY dateImported DESC")
    fun getAllAssets(): Flow<List<MediaAsset>>

    @Query("SELECT * FROM media_assets WHERE id = :id")
    suspend fun getAssetById(id: Long): MediaAsset?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAsset(asset: MediaAsset): Long

    @Update
    suspend fun updateAsset(asset: MediaAsset)

    @Query("UPDATE media_assets SET editParamsJson = :editParamsJson WHERE id = :id")
    suspend fun updateAssetEditParams(id: Long, editParamsJson: String)

    @Query("DELETE FROM media_assets WHERE id = :id")
    suspend fun deleteAssetById(id: Long)

    // Presets
    @Query("SELECT * FROM custom_presets ORDER BY dateCreated DESC")
    fun getAllPresets(): Flow<List<CustomPreset>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPreset(preset: CustomPreset): Long

    @Query("DELETE FROM custom_presets WHERE id = :id")
    suspend fun deletePresetById(id: Long)
}
