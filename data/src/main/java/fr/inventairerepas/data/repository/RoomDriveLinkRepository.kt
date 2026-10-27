package fr.inventairerepas.data.repository

import fr.inventairerepas.data.db.dao.DriveLinkDao
import fr.inventairerepas.data.db.entity.DriveLinkEntity
import fr.inventairerepas.domain.drive.DriveLink
import fr.inventairerepas.domain.drive.DriveLinkRepository
import javax.inject.Inject

class RoomDriveLinkRepository @Inject constructor(private val dao: DriveLinkDao) : DriveLinkRepository {

    override suspend fun getAll(): List<DriveLink> =
        dao.getAll().map { DriveLink(it.recipeId, it.fileId, it.remoteModifiedAt, it.localUpdatedAt) }

    override suspend fun save(link: DriveLink) =
        dao.upsert(DriveLinkEntity(link.recipeId, link.fileId, link.remoteModifiedAt, link.localUpdatedAt))

    override suspend fun delete(recipeId: Long) = dao.delete(recipeId)

    /** Oublie tous les liens, par exemple après un changement de compte ou de dossier. */
    suspend fun clear() = dao.clear()
}
