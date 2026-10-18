package fr.inventairerepas.data.export

import fr.inventairerepas.domain.export.DocsRequestPlan
import fr.inventairerepas.domain.export.ExportDocument
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject

class GoogleDocsException(message: String) : IOException(message)

data class CreatedDocument(val id: String, val url: String)

/**
 * Client minimal de l'API Google Docs : crée un document dans le Drive de l'utilisateur
 * puis y insère le contenu exporté. L'accès se fait avec un jeton OAuth (portée drive.file)
 * obtenu par l'Authorization API.
 */
class GoogleDocsClient @Inject constructor() {

    /** Crée le document et renvoie son identifiant et son adresse d'édition. */
    suspend fun create(accessToken: String, document: ExportDocument): CreatedDocument = withContext(Dispatchers.IO) {
        val created = post(accessToken, "$BASE_URL/documents", JSONObject().put("title", document.title))
        val documentId = created.getString("documentId")
        val plan = DocsRequestPlan.from(document)
        post(accessToken, "$BASE_URL/documents/$documentId:batchUpdate", JSONObject().put("requests", requests(plan)))
        CreatedDocument(documentId, "https://docs.google.com/document/d/$documentId/edit")
    }

    /**
     * Donne à [email] le droit de modifier le document (API Drive, autorisée par la portée drive.file
     * car le document a été créé par l'application). Google envoie un e-mail avec le lien.
     */
    suspend fun share(accessToken: String, documentId: String, email: String) = withContext(Dispatchers.IO) {
        post(
            accessToken,
            "$DRIVE_URL/files/$documentId/permissions?sendNotificationEmail=true",
            JSONObject().put("role", "writer").put("type", "user").put("emailAddress", email),
            api = "Google Drive",
        )
        Unit
    }

    private fun requests(plan: DocsRequestPlan): JSONArray {
        val requests = JSONArray()
        requests.put(
            JSONObject().put(
                "insertText",
                JSONObject().put("location", JSONObject().put("index", 1)).put("text", plan.text),
            ),
        )
        plan.paragraphStyles.forEach { style ->
            requests.put(
                JSONObject().put(
                    "updateParagraphStyle",
                    JSONObject()
                        .put("range", range(style.start, style.end))
                        .put("paragraphStyle", JSONObject().put("namedStyleType", style.namedStyle))
                        .put("fields", "namedStyleType"),
                ),
            )
        }
        plan.bullets.forEach { bullet ->
            requests.put(
                JSONObject().put(
                    "createParagraphBullets",
                    JSONObject().put("range", range(bullet.start, bullet.end)).put("bulletPreset", bullet.preset),
                ),
            )
        }
        return requests
    }

    private fun range(start: Int, end: Int) = JSONObject().put("startIndex", start).put("endIndex", end)

    private fun post(accessToken: String, url: String, body: JSONObject, api: String = "Google Docs"): JSONObject {
        val connection = URL(url).openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "POST"
            connection.connectTimeout = TIMEOUT_MS
            connection.readTimeout = TIMEOUT_MS
            connection.doOutput = true
            connection.setRequestProperty("Authorization", "Bearer $accessToken")
            connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
            connection.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }

            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val response = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
            if (code !in 200..299) {
                throw GoogleDocsException("L'API $api a répondu $code : ${errorMessage(response)}")
            }
            return if (response.isBlank()) JSONObject() else JSONObject(response)
        } finally {
            connection.disconnect()
        }
    }

    private fun errorMessage(response: String): String =
        runCatching { JSONObject(response).getJSONObject("error").getString("message") }.getOrDefault(response.take(200))

    private companion object {
        const val BASE_URL = "https://docs.googleapis.com/v1"
        const val DRIVE_URL = "https://www.googleapis.com/drive/v3"
        const val TIMEOUT_MS = 20_000
    }
}
