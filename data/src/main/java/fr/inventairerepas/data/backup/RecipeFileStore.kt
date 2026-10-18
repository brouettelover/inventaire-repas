package fr.inventairerepas.data.backup

import android.content.Context
import android.content.Intent
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.FileNotFoundException
import javax.inject.Inject
import javax.inject.Singleton

class RecipeFileException(message: String, cause: Throwable? = null) : Exception(message, cause)

/**
 * Lecture et écriture du fichier de recettes choisi avec le sélecteur de documents Android.
 * Fonctionne avec n'importe quel fournisseur : stockage local, Google Drive, etc.
 */
@Singleton
class RecipeFileStore @Inject constructor(@ApplicationContext private val context: Context) {

    /** Garde l'accès au fichier après redémarrage du téléphone, pour « Synchroniser ». */
    fun remember(uri: Uri) {
        runCatching {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
            )
        }.recoverCatching {
            // Certains fournisseurs n'accordent que la lecture.
            context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    fun forget(uri: Uri) {
        runCatching {
            context.contentResolver.releasePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
            )
        }
    }

    suspend fun read(uri: Uri): String = withContext(Dispatchers.IO) {
        try {
            context.contentResolver.openInputStream(uri)?.use { it.readBytes().toString(Charsets.UTF_8) }
                ?: throw RecipeFileException("Impossible d'ouvrir le fichier de recettes")
        } catch (e: FileNotFoundException) {
            throw RecipeFileException("Fichier de recettes introuvable : choisis-le à nouveau", e)
        } catch (e: SecurityException) {
            throw RecipeFileException("Accès au fichier de recettes perdu : choisis-le à nouveau", e)
        }
    }

    suspend fun write(uri: Uri, content: String) = withContext(Dispatchers.IO) {
        val bytes = content.toByteArray(Charsets.UTF_8)
        try {
            // « wt » vide le fichier avant d'écrire ; certains fournisseurs ne connaissent que « w ».
            val stream = runCatching { context.contentResolver.openOutputStream(uri, "wt") }.getOrNull()
                ?: context.contentResolver.openOutputStream(uri, "w")
                ?: throw RecipeFileException("Impossible d'écrire le fichier de recettes")
            stream.use { it.write(bytes) }
        } catch (e: FileNotFoundException) {
            throw RecipeFileException("Fichier de recettes introuvable : choisis-le à nouveau", e)
        } catch (e: SecurityException) {
            throw RecipeFileException("Ce fichier est en lecture seule : demande l'accès en modification", e)
        }
    }

    /** Nom du fichier affiché à l'utilisateur, ou null s'il n'est plus accessible. */
    suspend fun displayName(uri: Uri): String? = withContext(Dispatchers.IO) {
        runCatching {
            context.contentResolver.query(uri, arrayOf(android.provider.OpenableColumns.DISPLAY_NAME), null, null, null)
                ?.use { cursor -> if (cursor.moveToFirst()) cursor.getString(0) else null }
        }.getOrNull()
    }
}
