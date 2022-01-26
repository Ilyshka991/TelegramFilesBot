package com.pechuro.guitarbot.data

import com.pechuro.guitarbot.domain.RemoteData

interface DataRepository {

    fun getBySourceId(id: String = ""): List<RemoteData>

    fun findFiles(predicate: String): List<RemoteData.File>
}