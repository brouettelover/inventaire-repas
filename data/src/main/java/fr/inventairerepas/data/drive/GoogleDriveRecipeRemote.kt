package fr.inventairerepas.data.drive

import fr.inventairerepas.domain.drive.RecipeRemote
import fr.inventairerepas.domain.drive.RemoteRecipeFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.time.Instant
import java.util.UUID

class GoogleDriveException(val code: Int, message: String) : IOException(message)

/**
 * Dossier de recettes sur Google Drive (API REST v3), un fichier par recette.
 * Les recettes créées par l'app sont des Google Docs ; les fichiers texte (.txt) déposés à la main sont lus aussi.
 * Le nom du fichier est le nom de la recette.
 */
class GoogleDriveRecipeRemote(
    private val accessToken: String,
    val folderId: String,
) : RecipeRemote {

    /** Type de chaque fichier listé, pour savoir comment le lire et le réécrire. */
    private val mimeTypes = mutableMapOf<String, String>()
    private val extensions = mutableMapOf<String, String>()

    override suspend fun list(): List<RemoteRecipeFile> = withContext(Dispatchers.IO) {
        val query = "'$folderId' in parents and trashed = false and " +
            "(mimeType = '$DOC_MIME' or mimeType = 'text/plain' or mimeType = 'text/markdown')"
        val files = mutableListOf<RemoteRecipeFile>()
        var pageToken: String? = null
        do {
            val url = "$API/files?q=${encode(query)}&pageSize=200&supportsAllDrives=true&includeItemsFromAllDrives=true" +
                "&fields=${encode("nextPageToken,files(id,name,mimeType,modifiedTime)")}" +
                (pageToken?.let { "&pageToken=${encode(it)}" } ?: "")
            val page = JSONObject(request("GET", url))
            val array = page.optJSONArray("files") ?: JSONArray()
            for (i in 0 until array.length()) {
                val json = array.getJSONObject(i)
                val id = json.getString("id")
                val mime = json.getString("mimeType")
                mimeTypes[id] = mime
                val (name, extension) = splitExtension(json.getString("name"), mime)
                extensions[id] = extension
                files += RemoteRecipeFile(id, name, modifiedAt(json))
            }
            pageToken = page.optString("nextPageToken").takeIf { it.isNotEmpty() }
        } while (pageToken != null)
        files
    }

    override suspend fun read(fileId: String): String = withContext(Dispatchers.IO) {
        if (mimeTypes[fileId] == DOC_MIME) {
            request("GET", "$API/files/$fileId/export?mimeType=text/plain")
        } else {
            request("GET", "$API/files/$fileId?alt=media&supportsAllDrives=true")
        }
    }

    override suspend fun create(name: String, text: String): RemoteRecipeFile = withContext(Dispatchers.IO) {
        val metadata = JSONObject()
            .put("name", name)
            .put("mimeType", DOC_MIME)
            .put("parents", JSONArray().put(folderId))
        val json = JSONObject(upload("POST", "$UPLOAD/files?uploadType=multipart&supportsAllDrives=true&$FIELDS", metadata, text))
        mimeTypes[json.getString("id")] = DOC_MIME
        RemoteRecipeFile(json.getString("id"), name, modifiedAt(json))
    }

    override suspend fun update(fileId: String, name: String, text: String): RemoteRecipeFile = withContext(Dispatchers.IO) {
        val metadata = JSONObject().put("name", name + extensions[fileId].orEmpty())
        val json = JSONObject(upload("PATCH", "$UPLOAD/files/$fileId?uploadType=multipart&supportsAllDrives=true&$FIELDS", metadata, text))
        RemoteRecipeFile(fileId, name, modifiedAt(json))
    }

    override suspend fun trash(fileId: String) {
        withContext(Dispatchers.IO) {
            request("PATCH", "$API/files/$fileId?supportsAllDrives=true", JSONObject().put("trashed", true).toString(), JSON_TYPE)
        }
    }

    private fun upload(method: String, url: String, metadata: JSONObject, text: String): String {
        val boundary = "recette-" + UUID.randomUUID()
        val body = buildString {
            append("--$boundary\r\n")
            append("Content-Type: $JSON_TYPE\r\n\r\n")
            append(metadata.toString()).append("\r\n")
            append("--$boundary\r\n")
            append("Content-Type: text/plain; charset=UTF-8\r\n\r\n")
            append(text).append("\r\n")
            append("--$boundary--\r\n")
        }
        return request(method, url, body, "multipart/related; boundary=$boundary")
    }

    companion object {
        const val FOLDER_NAME = "recettes"
        private const val API = "https://www.googleapis.com/drive/v3"
        private const val UPLOAD = "https://www.googleapis.com/upload/drive/v3"
        private const val DOC_MIME = "application/vnd.google-apps.document"
        private const val FOLDER_MIME = "application/vnd.google-apps.folder"
        private const val JSON_TYPE = "application/json; charset=UTF-8"
        private const val FIELDS = "fields=id,name,modifiedTime"
        private const val TIMEOUT_MS = 20_000

        /**
         * Trouve le dossier « recettes » : celui déjà mémorisé s'il existe encore, sinon le plus ancien dossier de ce nom
         * (à toi ou partagé avec toi, pour que deux personnes tombent sur le même), sinon il est créé à la racine de ton Drive.
         */
        suspend fun resolveFolder(accessToken: String, knownFolderId: String?): String = withContext(Dispatchers.IO) {
            knownFolderId?.let { id ->
                val existing = runCatching {
                    JSONObject(call(accessToken, "GET", "$API/files/$id?fields=id,trashed&supportsAllDrives=true"))
                }.getOrNull()
                if (existing != null && !existing.optBoolean("trashed")) return@withContext id
            }
            val query = "name = '$FOLDER_NAME' and mimeType = '$FOLDER_MIME' and trashed = false"
            val found = JSONObject(
                call(
                    accessToken,
                    "GET",
                    "$API/files?q=${encode(query)}&supportsAllDrives=true&includeItemsFromAllDrives=true" +
                        "&fields=${encode("files(id,createdTime)")}&orderBy=createdTime",
                ),
            ).optJSONArray("files") ?: JSONArray()
            if (found.length() > 0) return@withContext found.getJSONObject(0).getString("id")
            val created = call(
                accessToken,
                "POST",
                "$API/files?fields=id",
                JSONObject().put("name", FOLDER_NAME).put("mimeType", FOLDER_MIME).toString(),
                JSON_TYPE,
            )
            JSONObject(created).getString("id")
        }

        private fun encode(value: String) = URLEncoder.encode(value, "UTF-8")

        private fun modifiedAt(json: JSONObject): Long = Instant.parse(json.getString("modifiedTime")).toEpochMilli()

        /** Les fichiers texte gardent leur extension sur Drive, mais pas dans le nom de la recette. */
        private fun splitExtension(name: String, mime: String): Pair<String, String> {
            if (mime == DOC_MIME) return name to ""
            val dot = name.lastIndexOf('.')
            return if (dot > 0 && name.length - dot <= 4) name.substring(0, dot) to name.substring(dot) else name to ""
        }

        private fun call(token: String, method: String, url: String, body: String? = null, contentType: String? = null): String {
            val connection = URL(url).openConnection() as HttpURLConnection
            try {
                // HttpURLConnection ne connaît pas PATCH : les API Google acceptent cet en-tête à la place.
                connection.requestMethod = if (method == "PATCH") "POST" else method
                if (method == "PATCH") connection.setRequestProperty("X-HTTP-Method-Override", "PATCH")
                connection.connectTimeout = TIMEOUT_MS
                connection.readTimeout = TIMEOUT_MS
                connection.setRequestProperty("Authorization", "Bearer $token")
                if (body != null) {
                    connection.doOutput = true
                    connection.setRequestProperty("Content-Type", contentType ?: JSON_TYPE)
                    connection.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
                }
                val code = connection.responseCode
                val stream = if (code in 200..299) connection.inputStream else connection.errorStream
                val response = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
                if (code !in 200..299) throw GoogleDriveException(code, errorMessage(code, response))
                return response
            } finally {
                connection.disconnect()
            }
        }

        private fun errorMessage(code: Int, response: String): String {
            val detail = runCatching { JSONObject(response).getJSONObject("error").getString("message") }
                .getOrDefault(response.take(200))
            return when (code) {
                401 -> "Accès Google expiré : reconnecte-toi dans les réglages"
                403 -> "Accès à Google Drive refusé ($detail). Vérifie que l'API Google Drive est activée et reconnecte-toi."
                404 -> "Fichier ou dossier introuvable sur Drive"
                else -> "Google Drive a répondu $code : $detail"
            }
        }
    }

    private fun request(method: String, url: String, body: String? = null, contentType: String? = null): String =
        call(accessToken, method, url, body, contentType)
}
