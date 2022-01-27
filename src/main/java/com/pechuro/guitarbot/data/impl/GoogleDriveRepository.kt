package com.pechuro.guitarbot.data.impl

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.google.api.services.drive.model.File
import com.google.auth.http.HttpCredentialsAdapter
import com.google.auth.oauth2.ServiceAccountCredentials
import com.pechuro.guitarbot.app.Configuration
import com.pechuro.guitarbot.data.DataRepository
import com.pechuro.guitarbot.data.RemoteData
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

class GoogleDriveRepository : DataRepository {

    companion object {
        private const val FOLDER_MIME_TYPE = "application/vnd.google-apps.folder"
        private const val QUERY_DEFAULT_FILTER = "name != '.DS_Store'"
        private const val TEXT_FILE_NAME = "index.txt"
        private const val TEXT_FILE_SEPARATOR = "\n\n---***---\n\n"

        private val SCOPES = listOf(DriveScopes.DRIVE_READONLY)
    }

    private val jsonFactory = GsonFactory.getDefaultInstance()

    private val driveService by lazy(LazyThreadSafetyMode.NONE) {
        Drive.Builder(
            GoogleNetHttpTransport.newTrustedTransport(),
            jsonFactory,
            HttpCredentialsAdapter(getCredentials())
        )
            .setApplicationName(Configuration.App.APPLICATION_NAME)
            .build()
    }

    private val parentDir by lazy(LazyThreadSafetyMode.NONE) {
        queryFiles("name = '${Configuration.Google.ROOT_FILE_PATH}'").first()
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

    private fun List<File>.mapToDomainEntity() = flatMap {
        when {
            it.mimeType == FOLDER_MIME_TYPE -> listOf(
                RemoteData.Folder(
                    name = it.name,
                    id = it.id
                )
            )
            it.name == TEXT_FILE_NAME -> it.getFileContent()
                .split(TEXT_FILE_SEPARATOR)
                .mapIndexed { index, textPage ->
                    RemoteData.Text(
                        page = index,
                        text = textPage
                    )
                }
            else -> listOf(
                RemoteData.File(
                    name = it.name,
                    mimeType = it.mimeType,
                    url = it.getDownloadLink()
                )
            )
        }
    }

    private fun File.getDownloadLink() = "https://drive.google.com/uc?id=${id}&export=download"

    private fun File.getFileContent() = runCatching {
        ByteArrayOutputStream().use { output ->
            driveService.files().get(id).executeMediaAndDownloadTo(output)
            output.toString(Charsets.UTF_8)
        }
    }.getOrDefault("")

    private fun getCredentials() = ServiceAccountCredentials.fromStream(
        ByteArrayInputStream(Configuration.Google.API_KEY.toByteArray())
    ).createScoped(SCOPES)
}