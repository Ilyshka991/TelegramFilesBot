package com.pechuro.guitarbot.domain

sealed class RemoteData {

    data class Folder(
        val name: String,
        val id: String
    ) : RemoteData()

    data class File(
        val name: String,
        val mimeType: String,
        val url: String
    ) : RemoteData()

    data class Text(
        val page: Int,
        val text: String
    ) : RemoteData()
}