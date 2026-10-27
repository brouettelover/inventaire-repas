package fr.inventairerepas.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Lien recette ↔ fichier Drive. Volontairement sans clé étrangère : le lien reste après la suppression
 * de la recette, pour que la synchronisation mette aussi le fichier Drive à la corbeille.
 */
@Entity(tableName = "drive_link")
data class DriveLinkEntity(
    @PrimaryKey @ColumnInfo(name = "recipe_id") val recipeId: Long,
    @ColumnInfo(name = "file_id") val fileId: String,
    @ColumnInfo(name = "remote_modified_at") val remoteModifiedAt: Long,
    @ColumnInfo(name = "local_updated_at") val localUpdatedAt: Long,
)
