package com.pechuro.guitarbot.domain

sealed class BotMessageInfo {

    companion object {

        private const val NORMAL_TYPE_ID = 1
        private const val SEARCH_TYPE_ID = 2
        private const val DELIMITER = '_'

        fun deserialize(value: String): BotMessageInfo? {
            val parts = value.split(DELIMITER)
            return when (parts.first().toInt()) {
                NORMAL_TYPE_ID -> Normal(parts.last().toLong())
                SEARCH_TYPE_ID -> Search(
                    parts[1].toLong(),
                    parts[2].toLong()
                )
                else -> null
            }
        }
    }

    abstract val id: Long

    data class Normal(
        override val id: Long
    ) : BotMessageInfo()

    data class Search(
        override val id: Long,
        val chatId: Long
    ) : BotMessageInfo()

    fun serialize() = buildString {
        val typeId = when (this@BotMessageInfo) {
            is Normal -> NORMAL_TYPE_ID
            is Search -> SEARCH_TYPE_ID
        }
        append(typeId)
        append(DELIMITER)
        append(id)
        if (this@BotMessageInfo is Search) {
            append(DELIMITER)
            append(chatId)
        }
    }
}
