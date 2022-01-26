package com.pechuro.guitarbot.data

interface DataRepository {

    fun getBySourceId(id: String = ""): List<RemoteData>

    fun findFiles(predicate: String): List<RemoteData.File>
}