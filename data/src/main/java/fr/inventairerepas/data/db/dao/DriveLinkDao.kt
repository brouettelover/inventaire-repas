package fr.inventairerepas.data.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import fr.inventairerepas.data.db.entity.DriveLinkEntity

@Dao
interface DriveLinkDao {

    @Query("SELECT * FROM drive_link")
    suspend fun getAll(): List<DriveLinkEntity>

    @Upsert
    suspend fun upsert(link: DriveLinkEntity)

    @Query("DELETE FROM drive_link WHERE recipe_id = :recipeId")
    suspend fun delete(recipeId: Long)

    @Query("DELETE FROM drive_link")
    suspend fun clear()
}
