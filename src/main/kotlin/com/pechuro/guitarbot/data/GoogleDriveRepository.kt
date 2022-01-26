package com.pechuro.guitarbot.data

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.google.api.services.drive.model.File
import com.pechuro.guitarbot.app.Configuration
import com.pechuro.guitarbot.domain.RemoteData
import java.io.ByteArrayOutputStream

class GoogleDriveRepository : DataRepository {

    companion object {
        private val SCOPES = listOf(DriveScopes.DRIVE_READONLY)

        private const val FOLDER_MIME_TYPE = "application/vnd.google-apps.folder"
        private const val QUERY_DEFAULT_FILTER = "name != '.DS_Store'"
    }

    private val textContentRegex = """index(\d*)\.txt""".toRegex()

    private val jsonFactory = GsonFactory.getDefaultInstance()

    private val driveService by lazy(LazyThreadSafetyMode.NONE) {
        val httpTransport = GoogleNetHttpTransport.newTrustedTransport()
        Drive.Builder(httpTransport, jsonFactory, getCredentials(httpTransport))
            .setApplicationName(Configuration.App.applicationName)
            .build()
    }

    private val parentDir by lazy(LazyThreadSafetyMode.NONE) {
        queryFiles("name = '${Configuration.Google.googleRootFilePath}'").first()
    }

    override fun getBySourceId(id: String): List<RemoteData> {
        val folderId = id.ifEmpty { parentDir.id }
        return queryFiles("'$folderId' in parents").mapToDomainEntity()
    }

    override fun findFiles(predicate: String) = queryFiles(
        "name contains '$predicate' and mimeType != '$FOLDER_MIME_TYPE'"
    )
        .mapToDomainEntity()
        .filterIsInstance<RemoteData.File>()

    private fun queryFiles(query: String) = driveService.files()
        .list()
        .setQ("$query and $QUERY_DEFAULT_FILTER")
        .setSpaces("drive")
        .execute()
        .files

    private fun List<File>.mapToDomainEntity() = map {
        if (it.mimeType == FOLDER_MIME_TYPE) {
            RemoteData.Folder(
                name = it.name,
                id = it.id
            )
        } else {
            val textMatchResult = textContentRegex.matchEntire(it.name)
            if (textMatchResult != null) {
                RemoteData.Text(
                    page = textMatchResult.groupValues.getOrNull(1)?.toIntOrNull() ?: 0,
                    text = it.getFileContent()
                )
            } else {
                RemoteData.File(
                    name = it.name,
                    mimeType = it.mimeType,
                    url = it.getDownloadLink()
                )
            }
        }
    }

    private fun File.getDownloadLink() = if (mimeType != FOLDER_MIME_TYPE) {
        "https://drive.google.com/uc?id=${id}&export=download"
    } else {
        ""
    }

    private fun File.getFileContent() = runCatching {
        ByteArrayOutputStream().use { output ->
            driveService.files().get(id).executeMediaAndDownloadTo(output)
            output.toString(Charsets.UTF_8)
        }
    }.getOrDefault("")

    private fun getCredentials(httpTransport: NetHttpTransport) = GoogleCredential.Builder()
        .setTransport(httpTransport)
        .setJsonFactory(jsonFactory)
        .setServiceAccountId(Configuration.Google.googleServiceAccountEmail)
        .setServiceAccountScopes(SCOPES)
        .setServiceAccountPrivateKeyFromP12File(
            GoogleDriveRepository::class.java.getResourceAsStream(Configuration.Google.googleCredentialsFilePath)
        )
        .build()
}